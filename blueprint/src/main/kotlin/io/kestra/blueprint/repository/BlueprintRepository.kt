package io.kestra.blueprint.repository

import io.kestra.blueprint.models.Blueprint
import io.micronaut.data.annotation.Repository
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.CrudRepository
import io.micronaut.data.model.Page
import io.micronaut.data.model.Pageable
import java.util.*

/**
 * 蓝图数据访问层
 * 基于Micronaut Data JDBC实现
 */
@Repository
@JdbcRepository(dialect = Dialect.H2)
interface BlueprintRepository : CrudRepository<Blueprint, String> {
    
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
     * 查找所有公开的蓝图（不受命名空间限制）
     */
    fun findByIsPublic(isPublic: Boolean, pageable: Pageable): Page<Blueprint>

    /**
     * 根据是否公开和类型查找蓝图
     */
    fun findByIsPublicAndKind(isPublic: Boolean, kind: String, pageable: Pageable): Page<Blueprint>

    /**
     * 根据是否公开和是否为模板查找蓝图
     */
    fun findByIsPublicAndIsTemplate(isPublic: Boolean, isTemplate: Boolean, pageable: Pageable): Page<Blueprint>

    /**
     * 根据是否公开和创建者查找蓝图
     */
    fun findByIsPublicAndCreatedBy(isPublic: Boolean, createdBy: String, pageable: Pageable): Page<Blueprint>


    
    /**
     * 删除指定命名空间下的蓝图
     */
    fun deleteByIdAndNamespaceId(id: String, namespaceId: String): Long
    
    /**
     * 检查蓝图是否存在
     */
    fun existsByIdAndNamespaceId(id: String, namespaceId: String): Boolean
}