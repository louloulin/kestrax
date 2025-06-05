package io.kestra.blueprint.service

import io.kestra.blueprint.dto.*
import io.kestra.blueprint.models.Blueprint
import io.kestra.blueprint.models.BlueprintTag
import io.kestra.blueprint.models.BlueprintTask
import io.kestra.blueprint.repository.BlueprintRepository
import io.kestra.blueprint.repository.BlueprintTagRepository
import io.kestra.blueprint.repository.BlueprintTaskRepository
import io.kestra.blueprint.security.TenantContext
import io.micronaut.cache.annotation.CacheInvalidate
import io.micronaut.cache.annotation.Cacheable
import io.micronaut.data.model.Page
import io.micronaut.data.model.Pageable
import io.micronaut.http.HttpStatus
import io.micronaut.http.exceptions.HttpStatusException
import jakarta.inject.Singleton
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.*

/**
 * 蓝图服务层
 * 实现蓝图的CRUD操作和业务逻辑
 */
@Singleton
open class BlueprintService(
    private val blueprintRepository: BlueprintRepository,
    private val blueprintTagRepository: BlueprintTagRepository,
    private val blueprintTaskRepository: BlueprintTaskRepository,
    private val tenantContext: TenantContext
) {
    
    private val logger = LoggerFactory.getLogger(BlueprintService::class.java)
    
    /**
     * 获取蓝图列表（简化版本）
     */
    @Cacheable("blueprints")
    open fun getBlueprints(
        pageable: Pageable,
        keyword: String? = null,
        tags: List<String>? = null,
        kind: String? = null,
        isPublic: Boolean? = null,
        isTemplate: Boolean? = null,
        createdBy: String? = null
    ): BlueprintListResponse {
        val namespaceId = getCurrentNamespaceId()

        // 简化版本：只支持基本查询
        val page = when {
            !kind.isNullOrBlank() -> blueprintRepository.findByNamespaceIdAndKind(namespaceId, kind, pageable)
            isPublic != null -> blueprintRepository.findByNamespaceIdAndIsPublic(namespaceId, isPublic, pageable)
            isTemplate != null -> blueprintRepository.findByNamespaceIdAndIsTemplate(namespaceId, isTemplate, pageable)
            !createdBy.isNullOrBlank() -> blueprintRepository.findByNamespaceIdAndCreatedBy(namespaceId, createdBy, pageable)
            else -> blueprintRepository.findByNamespaceId(namespaceId, pageable)
        }

        val blueprints = page.content.map { blueprint ->
            // 获取标签和任务
            val blueprintTags = blueprintTagRepository.findByBlueprintId(blueprint.id).map { it.tag }
            val blueprintTasks = blueprintTaskRepository.findByBlueprintId(blueprint.id).map { it.task }

            BlueprintDto.from(blueprint, blueprintTags, blueprintTasks)
        }

        return BlueprintListResponse(
            blueprints = blueprints,
            total = page.totalSize,
            page = page.pageNumber,
            size = page.size
        )
    }
    
    /**
     * 根据ID获取蓝图
     */
    @Cacheable("blueprint")
    open fun getBlueprintById(id: String): BlueprintDto {
        val namespaceId = getCurrentNamespaceId()
        val blueprint = blueprintRepository.findByIdAndNamespaceId(id, namespaceId)
            .orElseThrow { HttpStatusException(HttpStatus.NOT_FOUND, "蓝图不存在") }

        // 获取标签和任务
        val blueprintTags = blueprintTagRepository.findByBlueprintId(id).map { it.tag }
        val blueprintTasks = blueprintTaskRepository.findByBlueprintId(id).map { it.task }

        return BlueprintDto.from(blueprint, blueprintTags, blueprintTasks)
    }
    
    /**
     * 创建蓝图
     */
    @CacheInvalidate("blueprints")
    open fun createBlueprint(request: CreateBlueprintRequest): BlueprintDto {
        val namespaceId = getCurrentNamespaceId()
        val userId = getCurrentUserId()

        // 检查标题是否重复
        if (blueprintRepository.findByTitleAndNamespaceId(request.title, namespaceId).isPresent) {
            throw HttpStatusException(HttpStatus.CONFLICT, "蓝图标题已存在")
        }

        val blueprintId = UUID.randomUUID().toString()
        val blueprint = Blueprint(
            id = blueprintId,
            namespaceId = namespaceId,
            title = request.title,
            description = request.description,
            content = request.content,
            kind = request.kind,
            isPublic = request.isPublic,
            isTemplate = request.isTemplate,
            createdBy = userId,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )

        val savedBlueprint = blueprintRepository.save(blueprint)

        // 保存标签
        request.tags.forEach { tag ->
            blueprintTagRepository.save(BlueprintTag(blueprintId = blueprintId, tag = tag))
        }

        // 保存任务
        request.includedTasks.forEach { task ->
            blueprintTaskRepository.save(BlueprintTask(blueprintId = blueprintId, task = task))
        }

        logger.info("用户 {} 在命名空间 {} 中创建了蓝图 {}", userId, namespaceId, savedBlueprint.id)

        return BlueprintDto.from(savedBlueprint, request.tags, request.includedTasks)
    }
    
    /**
     * 更新蓝图（简化版本）
     */
    @CacheInvalidate("blueprint", "blueprints")
    open fun updateBlueprint(id: String, request: UpdateBlueprintRequest): BlueprintDto {
        val namespaceId = getCurrentNamespaceId()
        val userId = getCurrentUserId()

        val blueprint = blueprintRepository.findByIdAndNamespaceId(id, namespaceId)
            .orElseThrow { HttpStatusException(HttpStatus.NOT_FOUND, "蓝图不存在") }

        // 检查标题是否重复（排除当前蓝图）
        request.title?.let { newTitle ->
            if (newTitle != blueprint.title) {
                blueprintRepository.findByTitleAndNamespaceId(newTitle, namespaceId)
                    .ifPresent { throw HttpStatusException(HttpStatus.CONFLICT, "蓝图标题已存在") }
            }
        }

        val updatedBlueprint = blueprint.copy(
            title = request.title ?: blueprint.title,
            description = request.description ?: blueprint.description,
            content = request.content ?: blueprint.content,
            kind = request.kind ?: blueprint.kind,
            isPublic = request.isPublic ?: blueprint.isPublic,
            isTemplate = request.isTemplate ?: blueprint.isTemplate,
            updatedAt = Instant.now()
        )

        val savedBlueprint = blueprintRepository.update(updatedBlueprint)

        // 更新标签
        request.tags?.let { newTags ->
            blueprintTagRepository.deleteByBlueprintId(id)
            newTags.forEach { tag ->
                blueprintTagRepository.save(BlueprintTag(blueprintId = id, tag = tag))
            }
        }

        // 更新任务
        request.includedTasks?.let { newTasks ->
            blueprintTaskRepository.deleteByBlueprintId(id)
            newTasks.forEach { task ->
                blueprintTaskRepository.save(BlueprintTask(blueprintId = id, task = task))
            }
        }

        logger.info("用户 {} 在命名空间 {} 中更新了蓝图 {}", userId, namespaceId, id)

        // 获取更新后的标签和任务
        val tags = blueprintTagRepository.findByBlueprintId(id).map { it.tag }
        val tasks = blueprintTaskRepository.findByBlueprintId(id).map { it.task }

        return BlueprintDto.from(savedBlueprint, tags, tasks)
    }
    
    /**
     * 删除蓝图
     */
    @CacheInvalidate("blueprint", "blueprints")
    open fun deleteBlueprint(id: String) {
        val namespaceId = getCurrentNamespaceId()
        val userId = getCurrentUserId()

        if (!blueprintRepository.existsByIdAndNamespaceId(id, namespaceId)) {
            throw HttpStatusException(HttpStatus.NOT_FOUND, "蓝图不存在")
        }

        // 删除标签和任务
        blueprintTagRepository.deleteByBlueprintId(id)
        blueprintTaskRepository.deleteByBlueprintId(id)

        // 删除蓝图
        val deletedCount = blueprintRepository.deleteByIdAndNamespaceId(id, namespaceId)

        if (deletedCount == 0L) {
            throw HttpStatusException(HttpStatus.NOT_FOUND, "蓝图不存在")
        }

        logger.info("用户 {} 在命名空间 {} 中删除了蓝图 {}", userId, namespaceId, id)
    }

    
    /**
     * 获取当前命名空间ID
     */
    private fun getCurrentNamespaceId(): String {
        return tenantContext.getNamespaceId()
            ?: throw HttpStatusException(HttpStatus.BAD_REQUEST, "缺少命名空间信息")
    }
    
    /**
     * 获取当前用户ID
     */
    private fun getCurrentUserId(): String {
        return tenantContext.getUserId()
            ?: throw HttpStatusException(HttpStatus.BAD_REQUEST, "缺少用户信息")
    }
}