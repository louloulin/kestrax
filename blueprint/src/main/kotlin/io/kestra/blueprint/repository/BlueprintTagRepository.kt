package io.kestra.blueprint.repository

import io.kestra.blueprint.models.BlueprintTag
import io.micronaut.data.annotation.Repository
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.CrudRepository

/**
 * 蓝图标签数据访问层
 */
@Repository
@JdbcRepository(dialect = Dialect.H2)
interface BlueprintTagRepository : CrudRepository<BlueprintTag, Long> {
    
    /**
     * 根据蓝图ID查找所有标签
     */
    fun findByBlueprintId(blueprintId: String): List<BlueprintTag>
    
    /**
     * 删除指定蓝图的所有标签
     */
    fun deleteByBlueprintId(blueprintId: String): Long
    
    /**
     * 根据标签名查找蓝图标签
     */
    fun findByTag(tag: String): List<BlueprintTag>
}
