package io.kestra.blueprint.service

import io.kestra.blueprint.dto.SyncBlueprintResponse
import io.kestra.blueprint.dto.FailedBlueprintInfo
import io.kestra.blueprint.models.Blueprint
import io.kestra.blueprint.models.Namespace
import io.kestra.blueprint.repository.BlueprintRepository
import io.kestra.blueprint.repository.NamespaceRepository
import jakarta.inject.Singleton
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.*

/**
 * 简化的蓝图同步服务
 * 创建示例蓝图数据，模拟官方蓝图同步
 */
@Singleton
class SimpleBlueprintSyncService(
    private val blueprintRepository: BlueprintRepository,
    private val namespaceRepository: NamespaceRepository
) {
    
    private val logger = LoggerFactory.getLogger(SimpleBlueprintSyncService::class.java)
    
    /**
     * 同步官方蓝图（创建示例数据）
     */
    fun syncOfficialBlueprints(): SyncBlueprintResponse {
        logger.info("开始同步官方蓝图...")
        
        val startTime = System.currentTimeMillis()
        var successCount = 0
        var failedCount = 0
        val failedItems = mutableListOf<String>()
        
        try {
            // 确保官方命名空间存在
            ensureOfficialNamespace()
            
            // 创建示例蓝图
            val sampleBlueprints = createSampleBlueprints()
            
            logger.info("准备同步 {} 个示例蓝图", sampleBlueprints.size)
            
            // 同步每个蓝图
            for (blueprintData in sampleBlueprints) {
                try {
                    syncBlueprint(blueprintData)
                    successCount++
                    logger.debug("成功同步蓝图: {}", blueprintData.title)
                } catch (e: Exception) {
                    failedCount++
                    failedItems.add("${blueprintData.title}: ${e.message}")
                    logger.error("同步蓝图失败: {} - {}", blueprintData.title, e.message, e)
                }
            }
            
            val duration = System.currentTimeMillis() - startTime
            
            logger.info("蓝图同步完成 - 成功: {}, 失败: {}, 耗时: {}ms", successCount, failedCount, duration)
            
            return SyncBlueprintResponse(
                success = true,
                message = "官方蓝图同步完成",
                syncedCount = successCount,
                failedCount = failedCount,
                failedBlueprints = failedItems.map { FailedBlueprintInfo("unknown", it) }
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
     * 确保官方命名空间存在
     */
    private fun ensureOfficialNamespace() {
        val officialNamespace = namespaceRepository.findByName("official")
        if (officialNamespace.isEmpty) {
            val namespace = Namespace(
                id = UUID.randomUUID().toString(),
                name = "official",
                description = "官方蓝图命名空间",
                tenantId = "default-tenant",
                parentId = null,
                isActive = true,
                createdAt = Instant.now(),
                updatedAt = Instant.now(),
                version = 0L
            )
            namespaceRepository.save(namespace)
            logger.info("创建官方命名空间: official")
        }
    }
    
    /**
     * 同步单个蓝图
     */
    private fun syncBlueprint(blueprintData: SampleBlueprintData) {
        // 检查蓝图是否已存在
        val existingBlueprint = blueprintRepository.findByTitleAndNamespaceId(blueprintData.title, "official")
        
        if (existingBlueprint.isPresent) {
            logger.debug("蓝图已存在，跳过: {}", blueprintData.title)
            return
        }
        
        // 创建新蓝图
        val blueprint = Blueprint(
            id = UUID.randomUUID().toString(),
            namespaceId = "official",
            title = blueprintData.title,
            description = blueprintData.description,
            content = blueprintData.content,
            tags = blueprintData.tags,
            includedTasks = blueprintData.includedTasks,
            kind = blueprintData.kind,
            isPublic = true,
            isTemplate = blueprintData.isTemplate,
            createdBy = "system",
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
            version = 0L
        )
        
        blueprintRepository.save(blueprint)
        logger.debug("创建新蓝图: {}", blueprintData.title)
    }
    
    /**
     * 创建示例蓝图数据
     */
    private fun createSampleBlueprints(): List<SampleBlueprintData> {
        return listOf(
            SampleBlueprintData(
                title = "数据ETL流水线",
                description = "一个完整的数据ETL流水线，包含数据提取、转换和加载步骤",
                content = """
                    id: data-etl-pipeline
                    namespace: official
                    
                    tasks:
                      - id: extract
                        type: io.kestra.plugin.jdbc.mysql.Query
                        sql: SELECT * FROM source_table
                        
                      - id: transform
                        type: io.kestra.plugin.scripts.python.Script
                        script: |
                          import pandas as pd
                          # 数据转换逻辑
                          
                      - id: load
                        type: io.kestra.plugin.jdbc.postgresql.Query
                        sql: INSERT INTO target_table VALUES (?)
                """.trimIndent(),
                tags = listOf("ETL", "数据处理", "MySQL", "PostgreSQL"),
                includedTasks = listOf("io.kestra.plugin.jdbc.mysql.Query", "io.kestra.plugin.scripts.python.Script"),
                kind = "FLOW",
                isTemplate = true
            ),
            SampleBlueprintData(
                title = "API数据同步",
                description = "从REST API获取数据并同步到数据库",
                content = """
                    id: api-data-sync
                    namespace: official
                    
                    tasks:
                      - id: fetch_api_data
                        type: io.kestra.plugin.core.http.Request
                        uri: https://api.example.com/data
                        
                      - id: process_data
                        type: io.kestra.plugin.scripts.python.Script
                        script: |
                          import json
                          # 处理API响应数据
                          
                      - id: save_to_db
                        type: io.kestra.plugin.jdbc.mysql.Query
                        sql: INSERT INTO api_data VALUES (?)
                """.trimIndent(),
                tags = listOf("API", "同步", "HTTP", "数据库"),
                includedTasks = listOf("io.kestra.plugin.core.http.Request", "io.kestra.plugin.scripts.python.Script"),
                kind = "FLOW",
                isTemplate = true
            ),
            SampleBlueprintData(
                title = "文件处理流水线",
                description = "处理上传的文件，包括验证、转换和存储",
                content = """
                    id: file-processing-pipeline
                    namespace: official
                    
                    tasks:
                      - id: validate_file
                        type: io.kestra.plugin.scripts.python.Script
                        script: |
                          # 文件验证逻辑
                          
                      - id: transform_file
                        type: io.kestra.plugin.compress.ArchiveCompress
                        format: ZIP
                        
                      - id: upload_to_storage
                        type: io.kestra.plugin.aws.s3.Upload
                        bucket: my-bucket
                """.trimIndent(),
                tags = listOf("文件处理", "验证", "压缩", "存储"),
                includedTasks = listOf("io.kestra.plugin.scripts.python.Script", "io.kestra.plugin.compress.ArchiveCompress"),
                kind = "FLOW",
                isTemplate = true
            ),
            SampleBlueprintData(
                title = "定时报告生成",
                description = "定时生成业务报告并发送邮件",
                content = """
                    id: scheduled-report-generation
                    namespace: official
                    
                    triggers:
                      - id: daily_schedule
                        type: io.kestra.plugin.core.trigger.Schedule
                        cron: "0 8 * * *"
                    
                    tasks:
                      - id: generate_report
                        type: io.kestra.plugin.scripts.python.Script
                        script: |
                          # 生成报告逻辑
                          
                      - id: send_email
                        type: io.kestra.plugin.notifications.mail.MailSend
                        to: admin@company.com
                        subject: "Daily Report"
                """.trimIndent(),
                tags = listOf("报告", "定时任务", "邮件", "调度"),
                includedTasks = listOf("io.kestra.plugin.scripts.python.Script", "io.kestra.plugin.notifications.mail.MailSend"),
                kind = "FLOW",
                isTemplate = true
            ),
            SampleBlueprintData(
                title = "数据质量检查",
                description = "对数据进行质量检查和异常检测",
                content = """
                    id: data-quality-check
                    namespace: official
                    
                    tasks:
                      - id: load_data
                        type: io.kestra.plugin.jdbc.mysql.Query
                        sql: SELECT * FROM data_table
                        
                      - id: quality_check
                        type: io.kestra.plugin.scripts.python.Script
                        script: |
                          # 数据质量检查逻辑
                          
                      - id: alert_if_issues
                        type: io.kestra.plugin.notifications.slack.SlackSend
                        channel: "#data-alerts"
                """.trimIndent(),
                tags = listOf("数据质量", "检查", "监控", "告警"),
                includedTasks = listOf("io.kestra.plugin.jdbc.mysql.Query", "io.kestra.plugin.scripts.python.Script"),
                kind = "FLOW",
                isTemplate = true
            )
        )
    }
}

/**
 * 示例蓝图数据
 */
data class SampleBlueprintData(
    val title: String,
    val description: String,
    val content: String,
    val tags: List<String>,
    val includedTasks: List<String>,
    val kind: String,
    val isTemplate: Boolean
)
