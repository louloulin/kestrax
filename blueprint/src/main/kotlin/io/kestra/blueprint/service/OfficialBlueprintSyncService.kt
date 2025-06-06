package io.kestra.blueprint.service

import io.kestra.blueprint.dto.SyncBlueprintResponse
import io.kestra.blueprint.dto.FailedBlueprintInfo
import io.kestra.blueprint.models.Blueprint
import io.kestra.blueprint.models.BlueprintTag
import io.kestra.blueprint.models.BlueprintTask
import io.kestra.blueprint.repository.BlueprintRepository
import io.kestra.blueprint.repository.BlueprintTagRepository
import io.kestra.blueprint.repository.BlueprintTaskRepository
import io.kestra.blueprint.config.GitHubConfig
import io.micronaut.http.HttpRequest
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.http.client.exceptions.HttpClientResponseException
import jakarta.inject.Named
import jakarta.inject.Singleton
import org.slf4j.LoggerFactory
import org.yaml.snakeyaml.Yaml
import java.time.Instant
import java.util.*

/**
 * 官方蓝图同步服务
 * 从Kestra官方GitHub仓库同步真实的蓝图数据
 */
@Singleton
class OfficialBlueprintSyncService(
    private val blueprintRepository: BlueprintRepository,
    private val blueprintTagRepository: BlueprintTagRepository,
    private val blueprintTaskRepository: BlueprintTaskRepository,
    @Named("github-api") private val githubClient: HttpClient,
    @Named("github-raw") private val rawClient: HttpClient,
    private val gitHubConfig: GitHubConfig
) {
    
    private val logger = LoggerFactory.getLogger(OfficialBlueprintSyncService::class.java)
    private val yaml = Yaml()
    
    companion object {
        private const val GITHUB_REPO = "kestra-io/blueprints"
        private const val FLOWS_PATH = "flows"
        private const val OFFICIAL_NAMESPACE = "official"
    }

    /**
     * 执行带重试的操作
     */
    private fun <T> executeWithRetry(operation: String, block: () -> T): T {
        var lastException: Exception? = null

        repeat(gitHubConfig.retryAttempts) { attempt ->
            try {
                return block()
            } catch (e: HttpClientResponseException) {
                lastException = e
                if (e.status.code == 403 && e.message?.contains("rate limit") == true) {
                    logger.warn("{} 失败 (尝试 ${attempt + 1}/${gitHubConfig.retryAttempts}): GitHub API速率限制", operation)
                    if (attempt < gitHubConfig.retryAttempts - 1) {
                        val delay = gitHubConfig.retryDelay.toMillis() * (attempt + 1) // 指数退避
                        logger.info("等待 {}ms 后重试...", delay)
                        Thread.sleep(delay)
                    }
                } else {
                    throw e // 其他HTTP错误直接抛出
                }
            } catch (e: Exception) {
                lastException = e
                logger.warn("{} 失败 (尝试 ${attempt + 1}/${gitHubConfig.retryAttempts}): {}", operation, e.message)
                if (attempt < gitHubConfig.retryAttempts - 1) {
                    Thread.sleep(gitHubConfig.retryDelay.toMillis())
                }
            }
        }

        throw lastException ?: Exception("$operation 失败")
    }
    
    /**
     * 同步官方蓝图
     */
    fun syncOfficialBlueprints(): SyncBlueprintResponse {
        logger.info("开始同步Kestra官方蓝图...")
        
        val startTime = System.currentTimeMillis()
        var successCount = 0
        var failedCount = 0
        val failedItems = mutableListOf<FailedBlueprintInfo>()
        
        try {
            // 获取flows目录下的所有文件
            val flowFiles = getFlowFiles()
            logger.info("发现 {} 个蓝图文件", flowFiles.size)
            
            // 同步每个蓝图文件
            for (file in flowFiles) {
                try {
                    val content = downloadFileContent(file.downloadUrl)
                    val blueprint = parseBlueprint(file.name, content)

                    if (blueprint != null) {
                        syncBlueprint(blueprint)
                        successCount++
                        logger.debug("成功同步蓝图: {}", blueprint.title)
                    } else {
                        failedCount++
                        failedItems.add(FailedBlueprintInfo(file.name, "无法解析蓝图内容"))
                        logger.warn("跳过无效蓝图文件: {}", file.name)
                    }
                } catch (e: Exception) {
                    failedCount++
                    failedItems.add(FailedBlueprintInfo(file.name, e.message ?: "未知错误"))
                    logger.error("同步蓝图失败: {} - {}", file.name, e.message, e)
                }
            }
            
            val duration = System.currentTimeMillis() - startTime
            logger.info("官方蓝图同步完成 - 成功: {}, 失败: {}, 耗时: {}ms", successCount, failedCount, duration)
            
            return SyncBlueprintResponse(
                success = true,
                message = "官方蓝图同步完成",
                syncedCount = successCount,
                failedCount = failedCount,
                failedBlueprints = failedItems
            )
            
        } catch (e: Exception) {
            logger.error("同步官方蓝图失败", e)
            return SyncBlueprintResponse(
                success = false,
                message = "同步失败: ${e.message}",
                syncedCount = 0,
                failedCount = 0,
                failedBlueprints = listOf(FailedBlueprintInfo("unknown", e.message ?: "未知错误"))
            )
        }
    }
    
    /**
     * 获取flows目录下的所有文件
     */
    private fun getFlowFiles(): List<GitHubFile> {
        return executeWithRetry("获取GitHub文件列表") {
            logger.debug("获取GitHub仓库文件列表: {}/{}", GITHUB_REPO, FLOWS_PATH)
            val request = HttpRequest.GET<Any>("/repos/$GITHUB_REPO/contents/$FLOWS_PATH")
                .header("User-Agent", "Kestra-Blueprint-Sync/1.0")
                .header("Accept", "application/vnd.github.v3+json")
                .apply {
                    gitHubConfig.getAuthHeader()?.let { token ->
                        header("Authorization", token)
                    }
                }

            val response = githubClient.toBlocking().exchange(request, Array<GitHubFile>::class.java)
            val files = response.body()?.filter { it.type == "file" && it.name.endsWith(".yaml") } ?: emptyList()
            logger.info("发现 {} 个YAML文件", files.size)
            files
        }
    }
    
    /**
     * 下载文件内容
     */
    private fun downloadFileContent(downloadUrl: String): String {
        return executeWithRetry("下载文件内容") {
            logger.debug("下载文件内容: {}", downloadUrl)

            // 解析URL，提取路径部分
            val url = java.net.URL(downloadUrl)
            val path = url.path

            val request = HttpRequest.GET<String>(path)
                .header("User-Agent", "Kestra-Blueprint-Sync/1.0")
                .apply {
                    gitHubConfig.getAuthHeader()?.let { token ->
                        header("Authorization", token)
                    }
                }

            val response = rawClient.toBlocking().exchange(request, String::class.java)
            response.body() ?: throw Exception("响应体为空")
        }
    }
    
    /**
     * 解析蓝图YAML内容
     */
    private fun parseBlueprint(fileName: String, content: String): ParsedBlueprint? {
        try {
            val yamlData = yaml.load(content) as? Map<String, Any> ?: return null

            val id = yamlData["id"] as? String ?: fileName.removeSuffix(".yaml")
            val extend = yamlData["extend"] as? Map<String, Any>

            val title = extend?.get("title") as? String ?: id.replace("-", " ").replaceFirstChar { it.uppercase() }
            val description = extend?.get("description") as? String ?: "从Kestra官方仓库同步的工作流蓝图"
            val tags = (extend?.get("tags") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
            val isDemo = extend?.get("demo") as? Boolean ?: false
            val isEE = extend?.get("ee") as? Boolean ?: false

            // 提取任务类型
            val tasks = yamlData["tasks"] as? List<*> ?: emptyList<Any>()
            val includedTasks = extractTaskTypes(tasks)

            return ParsedBlueprint(
                id = id,
                title = title,
                description = description,
                content = content,
                tags = tags,
                includedTasks = includedTasks,
                kind = "FLOW",
                isTemplate = !isDemo,
                isDemo = isDemo,
                isEE = isEE
            )

        } catch (e: Exception) {
            logger.error("解析蓝图失败: {} - {}", fileName, e.message)
            return null
        }
    }

    /**
     * 提取任务类型
     */
    private fun extractTaskTypes(tasks: List<*>): List<String> {
        val taskTypes = mutableSetOf<String>()

        for (task in tasks) {
            if (task is Map<*, *>) {
                val type = task["type"] as? String
                if (type != null) {
                    taskTypes.add(type)
                }
            }
        }

        return taskTypes.toList()
    }

    /**
     * 同步单个蓝图
     */
    private fun syncBlueprint(parsedBlueprint: ParsedBlueprint) {
        // 检查蓝图是否已存在
        val existingBlueprint = blueprintRepository.findByTitleAndNamespaceId(parsedBlueprint.title, OFFICIAL_NAMESPACE)

        if (existingBlueprint.isPresent) {
            logger.debug("蓝图已存在，跳过: {}", parsedBlueprint.title)
            return
        }

        // 创建新蓝图
        val blueprintId = UUID.randomUUID().toString()
        val blueprint = Blueprint(
            id = blueprintId,
            namespaceId = OFFICIAL_NAMESPACE,
            title = parsedBlueprint.title,
            description = parsedBlueprint.description,
            content = parsedBlueprint.content,
            kind = parsedBlueprint.kind,
            isPublic = true,
            isTemplate = parsedBlueprint.isTemplate,
            createdBy = "system",
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
            version = 0L
        )

        // 保存蓝图
        blueprintRepository.save(blueprint)

        // 保存标签
        val allTags = parsedBlueprint.tags + listOf("官方", "Kestra")
        allTags.forEach { tag ->
            val blueprintTag = BlueprintTag(
                blueprintId = blueprintId,
                tag = tag
            )
            blueprintTagRepository.save(blueprintTag)
        }

        // 保存任务类型
        parsedBlueprint.includedTasks.forEach { task ->
            val blueprintTask = BlueprintTask(
                blueprintId = blueprintId,
                task = task
            )
            blueprintTaskRepository.save(blueprintTask)
        }

        logger.debug("创建新蓝图: {}", parsedBlueprint.title)
    }
}

/**
 * GitHub文件信息
 */
@com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
data class GitHubFile(
    @com.fasterxml.jackson.annotation.JsonProperty("name")
    val name: String = "",
    @com.fasterxml.jackson.annotation.JsonProperty("path")
    val path: String = "",
    @com.fasterxml.jackson.annotation.JsonProperty("type")
    val type: String = "",
    @com.fasterxml.jackson.annotation.JsonProperty("download_url")
    val downloadUrl: String = ""
)

/**
 * 解析后的蓝图数据
 */
data class ParsedBlueprint(
    val id: String,
    val title: String,
    val description: String,
    val content: String,
    val tags: List<String>,
    val includedTasks: List<String>,
    val kind: String,
    val isTemplate: Boolean,
    val isDemo: Boolean,
    val isEE: Boolean
)


