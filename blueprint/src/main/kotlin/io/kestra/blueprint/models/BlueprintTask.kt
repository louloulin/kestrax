package io.kestra.blueprint.models

import io.micronaut.data.annotation.*
import io.micronaut.data.model.naming.NamingStrategies

/**
 * 蓝图任务关联表
 */
@MappedEntity("blueprint_included_tasks")
data class BlueprintTask(
    @field:Id
    @field:GeneratedValue
    val id: Long? = null,

    val blueprintId: String,

    val task: String
)
