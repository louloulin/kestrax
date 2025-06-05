package io.kestra.blueprint.models

import io.micronaut.data.annotation.*
import io.micronaut.data.model.naming.NamingStrategies

/**
 * 蓝图标签关联表
 */
@MappedEntity("blueprint_tags")
data class BlueprintTag(
    @field:Id
    @field:GeneratedValue
    val id: Long? = null,

    val blueprintId: String,

    val tag: String
)
