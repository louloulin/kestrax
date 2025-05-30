package io.kestra.blueprint.repository

import io.kestra.blueprint.models.BlueprintVersion
import io.micronaut.data.annotation.Query
import io.micronaut.data.annotation.Repository
import io.micronaut.data.jpa.repository.JpaRepository
import io.micronaut.data.model.Page
import io.micronaut.data.model.Pageable
import java.util.*

/**
 * 蓝图版本数据访问层
 */
@Repository
interface BlueprintVersionRepository : JpaRepository<BlueprintVersion, String> {
    
    /**
     * 根据蓝图ID查找所有版本（按版本号降序）
     */
    fun findByBlueprintIdOrderByVersionNumberDesc(blueprintId: String, pageable: Pageable): Page<BlueprintVersion>
    
    /**
     * 根据蓝图ID和版本号查找版本
     */
    fun findByBlueprintIdAndVersionNumber(blueprintId: String, versionNumber: Int): Optional<BlueprintVersion>
    
    /**
     * 根据蓝图ID查找最新版本
     */
    @Query("SELECT bv FROM BlueprintVersion bv WHERE bv.blueprintId = :blueprintId ORDER BY bv.versionNumber DESC LIMIT 1")
    fun findLatestByBlueprintId(blueprintId: String): Optional<BlueprintVersion>
    
    /**
     * 根据蓝图ID获取最大版本号
     */
    @Query("SELECT MAX(bv.versionNumber) FROM BlueprintVersion bv WHERE bv.blueprintId = :blueprintId")
    fun findMaxVersionNumberByBlueprintId(blueprintId: String): Optional<Int>
    
    /**
     * 根据蓝图ID统计版本数量
     */
    fun countByBlueprintId(blueprintId: String): Long
    
    /**
     * 根据蓝图ID和创建者查找版本
     */
    fun findByBlueprintIdAndCreatedBy(blueprintId: String, createdBy: String, pageable: Pageable): Page<BlueprintVersion>
    
    /**
     * 删除蓝图的所有版本
     */
    fun deleteByBlueprintId(blueprintId: String): Long
    
    /**
     * 检查版本是否存在
     */
    fun existsByBlueprintIdAndVersionNumber(blueprintId: String, versionNumber: Int): Boolean
}