package io.kestra.blueprint.repository

import io.kestra.blueprint.models.Blueprint
import io.micronaut.data.annotation.Query
import io.micronaut.data.annotation.Repository
import io.micronaut.data.jpa.repository.JpaRepository
import io.micronaut.data.model.Page
import io.micronaut.data.model.Pageable
import java.util.*

/**
 * 蓝图数据访问层
 * 基于Micronaut Data JPA实现
 */
@Repository
interface BlueprintRepository : JpaRepository<Blueprint, String> {
    
    /**
     * 根据命名空间ID查找蓝图列表（支持分页）
     */
    fun findByNamespaceId(namespaceId: String, pageable: Pageable): Page<Blueprint>
    
    /**
     * 根据命名空间ID和ID查找蓝图
     */
    fun findByIdAndNamespaceId(id: String, namespaceId: String): Optional<Blueprint>
    
    /**
     * 根据命名空间ID和标题查找蓝图
     */
    fun findByTitleAndNamespaceId(title: String, namespaceId: String): Optional<Blueprint>
    
    /**
     * 根据命名空间ID和标签查找蓝图
     */
    @Query("SELECT b FROM Blueprint b JOIN b.tags t WHERE b.namespaceId = :namespaceId AND t IN :tags")
    fun findByNamespaceIdAndTagsIn(namespaceId: String, tags: List<String>, pageable: Pageable): Page<Blueprint>
    
    /**
     * 根据命名空间ID和类型查找蓝图
     */
    fun findByNamespaceIdAndKind(namespaceId: String, kind: String, pageable: Pageable): Page<Blueprint>
    
    /**
     * 根据命名空间ID和是否公开查找蓝图
     */
    fun findByNamespaceIdAndIsPublic(namespaceId: String, isPublic: Boolean, pageable: Pageable): Page<Blueprint>
    
    /**
     * 根据命名空间ID和是否为模板查找蓝图
     */
    fun findByNamespaceIdAndIsTemplate(namespaceId: String, isTemplate: Boolean, pageable: Pageable): Page<Blueprint>
    
    /**
     * 根据命名空间ID和创建者查找蓝图
     */
    fun findByNamespaceIdAndCreatedBy(namespaceId: String, createdBy: String, pageable: Pageable): Page<Blueprint>
    
    /**
     * 根据命名空间ID统计蓝图数量
     */
    fun countByNamespaceId(namespaceId: String): Long
    
    /**
     * 根据命名空间ID和标题模糊搜索蓝图
     */
    @Query("SELECT b FROM Blueprint b WHERE b.namespaceId = :namespaceId AND LOWER(b.title) LIKE LOWER(CONCAT('%', :title, '%'))")
    fun searchByNamespaceIdAndTitle(namespaceId: String, title: String, pageable: Pageable): Page<Blueprint>
    
    /**
     * 根据命名空间ID和描述模糊搜索蓝图
     */
    @Query("SELECT b FROM Blueprint b WHERE b.namespaceId = :namespaceId AND LOWER(b.description) LIKE LOWER(CONCAT('%', :description, '%'))")
    fun searchByNamespaceIdAndDescription(namespaceId: String, description: String, pageable: Pageable): Page<Blueprint>
    
    /**
     * 复合搜索：根据命名空间ID、标题、描述、标签进行模糊搜索
     */
    @Query("""
        SELECT DISTINCT b FROM Blueprint b LEFT JOIN b.tags t 
        WHERE b.namespaceId = :namespaceId 
        AND (
            LOWER(b.title) LIKE LOWER(CONCAT('%', :keyword, '%')) 
            OR LOWER(b.description) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(t) LIKE LOWER(CONCAT('%', :keyword, '%'))
        )
    """)
    fun searchByKeyword(namespaceId: String, keyword: String, pageable: Pageable): Page<Blueprint>
    
    /**
     * 删除指定命名空间下的蓝图
     */
    fun deleteByIdAndNamespaceId(id: String, namespaceId: String): Long
    
    /**
     * 检查蓝图是否存在
     */
    fun existsByIdAndNamespaceId(id: String, namespaceId: String): Boolean
}