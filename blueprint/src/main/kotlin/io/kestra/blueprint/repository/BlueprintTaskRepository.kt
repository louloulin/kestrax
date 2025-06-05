package io.kestra.blueprint.repository

import io.kestra.blueprint.models.BlueprintTask
import io.micronaut.data.annotation.Repository
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.CrudRepository

/**
 * 蓝图任务数据访问层
 */
@Repository
@JdbcRepository(dialect = Dialect.H2)
interface BlueprintTaskRepository : CrudRepository<BlueprintTask, Long> {
    
    /**
     * 根据蓝图ID查找所有任务
     */
    fun findByBlueprintId(blueprintId: String): List<BlueprintTask>
    
    /**
     * 删除指定蓝图的所有任务
     */
    fun deleteByBlueprintId(blueprintId: String): Long
    
    /**
     * 根据任务名查找蓝图任务
     */
    fun findByTask(task: String): List<BlueprintTask>
}
