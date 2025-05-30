package io.kestra.blueprint.repository

import io.kestra.blueprint.models.Namespace
import io.micronaut.data.annotation.Repository
import io.micronaut.data.jpa.repository.JpaRepository
import java.util.*

/**
 * 命名空间数据访问层
 * 用于多租户架构的租户隔离
 */
@Repository
interface NamespaceRepository : JpaRepository<Namespace, String> {
    
    /**
     * 根据名称查找命名空间
     */
    fun findByName(name: String): Optional<Namespace>
    
    /**
     * 根据租户ID查找命名空间列表
     */
    fun findByTenantId(tenantId: String): List<Namespace>
    
    /**
     * 根据租户ID和激活状态查找命名空间列表
     */
    fun findByTenantIdAndIsActive(tenantId: String, isActive: Boolean): List<Namespace>
    
    /**
     * 根据父命名空间ID查找子命名空间列表
     */
    fun findByParentId(parentId: String): List<Namespace>
    
    /**
     * 检查命名空间名称是否存在
     */
    fun existsByName(name: String): Boolean
    
    /**
     * 检查租户下是否存在指定名称的命名空间
     */
    fun existsByNameAndTenantId(name: String, tenantId: String): Boolean
}