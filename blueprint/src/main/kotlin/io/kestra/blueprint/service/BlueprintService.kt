package io.kestra.blueprint.service

import io.kestra.blueprint.dto.*
import io.kestra.blueprint.models.Blueprint
import io.kestra.blueprint.models.BlueprintVersion
import io.kestra.blueprint.repository.BlueprintRepository
import io.kestra.blueprint.repository.BlueprintVersionRepository
import io.kestra.blueprint.security.TenantContext
import io.micronaut.cache.annotation.CacheInvalidate
import io.micronaut.cache.annotation.Cacheable
import io.micronaut.data.model.Page
import io.micronaut.data.model.Pageable
import io.micronaut.http.HttpStatus
import io.micronaut.http.exceptions.HttpStatusException
import jakarta.inject.Singleton
import jakarta.transaction.Transactional
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.*

/**
 * 蓝图服务层
 * 实现蓝图的CRUD操作和业务逻辑
 */
@Singleton
@Transactional
class BlueprintService(
    private val blueprintRepository: BlueprintRepository,
    private val blueprintVersionRepository: BlueprintVersionRepository,
    private val tenantContext: TenantContext
) {
    
    private val logger = LoggerFactory.getLogger(BlueprintService::class.java)
    
    /**
     * 获取蓝图列表（支持分页和搜索）
     */
    @Cacheable("blueprints")
    fun getBlueprints(
        pageable: Pageable,
        keyword: String? = null,
        tags: List<String>? = null,
        kind: String? = null,
        isPublic: Boolean? = null,
        isTemplate: Boolean? = null,
        createdBy: String? = null
    ): BlueprintListResponse {
        val namespaceId = getCurrentNamespaceId()
        
        val page = when {
            !keyword.isNullOrBlank() -> blueprintRepository.searchByKeyword(namespaceId, keyword, pageable)
            !tags.isNullOrEmpty() -> blueprintRepository.findByNamespaceIdAndTagsIn(namespaceId, tags, pageable)
            !kind.isNullOrBlank() -> blueprintRepository.findByNamespaceIdAndKind(namespaceId, kind, pageable)
            isPublic != null -> blueprintRepository.findByNamespaceIdAndIsPublic(namespaceId, isPublic, pageable)
            isTemplate != null -> blueprintRepository.findByNamespaceIdAndIsTemplate(namespaceId, isTemplate, pageable)
            !createdBy.isNullOrBlank() -> blueprintRepository.findByNamespaceIdAndCreatedBy(namespaceId, createdBy, pageable)
            else -> blueprintRepository.findByNamespaceId(namespaceId, pageable)
        }
        
        val blueprints = page.content.map { it.toDto() }
        
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
    fun getBlueprintById(id: String): BlueprintDto {
        val namespaceId = getCurrentNamespaceId()
        val blueprint = blueprintRepository.findByIdAndNamespaceId(id, namespaceId)
            .orElseThrow { HttpStatusException(HttpStatus.NOT_FOUND, "蓝图不存在") }
        
        return blueprint.toDto()
    }
    
    /**
     * 创建蓝图
     */
    @CacheInvalidate("blueprints")
    fun createBlueprint(request: CreateBlueprintRequest): BlueprintDto {
        val namespaceId = getCurrentNamespaceId()
        val userId = getCurrentUserId()
        
        // 检查标题是否重复
        if (blueprintRepository.findByTitleAndNamespaceId(request.title, namespaceId).isPresent) {
            throw HttpStatusException(HttpStatus.CONFLICT, "蓝图标题已存在")
        }
        
        val blueprint = Blueprint(
            namespaceId = namespaceId,
            title = request.title,
            description = request.description,
            content = request.content,
            tags = request.tags,
            includedTasks = request.includedTasks,
            kind = request.kind,
            isPublic = request.isPublic,
            isTemplate = request.isTemplate,
            createdBy = userId
        )
        
        val savedBlueprint = blueprintRepository.save(blueprint)
        
        // 创建初始版本
        createInitialVersion(savedBlueprint)
        
        logger.info("用户 {} 在命名空间 {} 中创建了蓝图 {}", userId, namespaceId, savedBlueprint.id)
        
        return savedBlueprint.toDto()
    }
    
    /**
     * 更新蓝图
     */
    @CacheInvalidate("blueprint", "blueprints")
    fun updateBlueprint(id: String, request: UpdateBlueprintRequest): BlueprintDto {
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
            tags = request.tags ?: blueprint.tags,
            includedTasks = request.includedTasks ?: blueprint.includedTasks,
            kind = request.kind ?: blueprint.kind,
            isPublic = request.isPublic ?: blueprint.isPublic,
            isTemplate = request.isTemplate ?: blueprint.isTemplate,
            updatedAt = Instant.now()
        )
        
        val savedBlueprint = blueprintRepository.update(updatedBlueprint)
        
        // 如果内容发生变化，创建新版本
        if (request.content != null && request.content != blueprint.content) {
            createNewVersion(savedBlueprint, userId)
        }
        
        logger.info("用户 {} 在命名空间 {} 中更新了蓝图 {}", userId, namespaceId, id)
        
        return savedBlueprint.toDto()
    }
    
    /**
     * 删除蓝图
     */
    @CacheInvalidate("blueprint", "blueprints")
    fun deleteBlueprint(id: String) {
        val namespaceId = getCurrentNamespaceId()
        val userId = getCurrentUserId()
        
        if (!blueprintRepository.existsByIdAndNamespaceId(id, namespaceId)) {
            throw HttpStatusException(HttpStatus.NOT_FOUND, "蓝图不存在")
        }
        
        // 删除所有版本
        blueprintVersionRepository.deleteByBlueprintId(id)
        
        // 删除蓝图
        val deletedCount = blueprintRepository.deleteByIdAndNamespaceId(id, namespaceId)
        
        if (deletedCount == 0L) {
            throw HttpStatusException(HttpStatus.NOT_FOUND, "蓝图不存在")
        }
        
        logger.info("用户 {} 在命名空间 {} 中删除了蓝图 {}", userId, namespaceId, id)
    }
    
    /**
     * 获取蓝图版本列表
     */
    fun getBlueprintVersions(blueprintId: String, pageable: Pageable): Page<BlueprintVersion> {
        val namespaceId = getCurrentNamespaceId()
        
        // 验证蓝图存在
        if (!blueprintRepository.existsByIdAndNamespaceId(blueprintId, namespaceId)) {
            throw HttpStatusException(HttpStatus.NOT_FOUND, "蓝图不存在")
        }
        
        return blueprintVersionRepository.findByBlueprintIdOrderByVersionNumberDesc(blueprintId, pageable)
    }
    
    /**
     * 获取指定版本的蓝图
     */
    fun getBlueprintVersion(blueprintId: String, versionNumber: Int): BlueprintVersion {
        val namespaceId = getCurrentNamespaceId()
        
        // 验证蓝图存在
        if (!blueprintRepository.existsByIdAndNamespaceId(blueprintId, namespaceId)) {
            throw HttpStatusException(HttpStatus.NOT_FOUND, "蓝图不存在")
        }
        
        return blueprintVersionRepository.findByBlueprintIdAndVersionNumber(blueprintId, versionNumber)
            .orElseThrow { HttpStatusException(HttpStatus.NOT_FOUND, "蓝图版本不存在") }
    }
    
    /**
     * 创建初始版本
     */
    private fun createInitialVersion(blueprint: Blueprint) {
        val version = BlueprintVersion(
            blueprintId = blueprint.id,
            versionNumber = 1,
            title = blueprint.title,
            description = blueprint.description,
            content = blueprint.content,
            tags = blueprint.tags,
            includedTasks = blueprint.includedTasks,
            kind = blueprint.kind,
            isPublic = blueprint.isPublic,
            isTemplate = blueprint.isTemplate,
            changeLog = "初始版本",
            createdBy = blueprint.createdBy
        )
        
        blueprintVersionRepository.save(version)
    }
    
    /**
     * 创建新版本
     */
    private fun createNewVersion(blueprint: Blueprint, userId: String) {
        val maxVersion = blueprintVersionRepository.findMaxVersionNumberByBlueprintId(blueprint.id)
            .orElse(0)
        
        val version = BlueprintVersion(
            blueprintId = blueprint.id,
            versionNumber = maxVersion + 1,
            title = blueprint.title,
            description = blueprint.description,
            content = blueprint.content,
            tags = blueprint.tags,
            includedTasks = blueprint.includedTasks,
            kind = blueprint.kind,
            isPublic = blueprint.isPublic,
            isTemplate = blueprint.isTemplate,
            changeLog = "内容更新",
            createdBy = userId
        )
        
        blueprintVersionRepository.save(version)
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

/**
 * Blueprint实体转DTO扩展函数
 */
fun Blueprint.toDto(): BlueprintDto {
    return BlueprintDto(
        id = this.id,
        namespaceId = this.namespaceId,
        title = this.title,
        description = this.description,
        content = this.content,
        tags = this.tags,
        includedTasks = this.includedTasks,
        kind = this.kind,
        isPublic = this.isPublic,
        isTemplate = this.isTemplate,
        createdBy = this.createdBy,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt,
        version = this.version
    )
}