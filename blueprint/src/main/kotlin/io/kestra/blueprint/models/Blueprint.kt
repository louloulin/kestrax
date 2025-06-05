package io.kestra.blueprint.models

import io.micronaut.data.annotation.*
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.time.Instant

/**
 * 蓝图实体类
 * 使用Micronaut Data JDBC实现的简化蓝图数据模型
 */
@MappedEntity("blueprints")
data class Blueprint(
    @field:Id
    val id: String,

    @field:NotBlank(message = "命名空间ID不能为空")
    val namespaceId: String,

    @field:NotBlank(message = "标题不能为空")
    val title: String,

    val description: String?,

    @field:NotBlank(message = "内容不能为空")
    val content: String,

    val kind: String?,

    @field:NotNull(message = "公开状态不能为空")
    val isPublic: Boolean = false,

    @field:NotNull(message = "模板状态不能为空")
    val isTemplate: Boolean = false,

    @field:NotBlank(message = "创建者不能为空")
    val createdBy: String,

    @field:NotNull(message = "创建时间不能为空")
    val createdAt: Instant,

    @field:NotNull(message = "更新时间不能为空")
    val updatedAt: Instant,

    @field:Version
    val version: Long = 0L
) {
    override fun toString(): String {
        return "Blueprint(id='$id', title='$title', namespaceId='$namespaceId', kind='$kind')"
    }
}