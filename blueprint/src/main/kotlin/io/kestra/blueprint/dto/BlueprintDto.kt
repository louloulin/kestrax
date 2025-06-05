package io.kestra.blueprint.dto

import com.fasterxml.jackson.annotation.JsonInclude
import io.kestra.blueprint.models.Blueprint
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.time.Instant

/**
 * 蓝图数据传输对象
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "蓝图数据传输对象")
data class BlueprintDto(
    @Schema(description = "蓝图ID", example = "blueprint-123")
    val id: String? = null,
    
    @Schema(description = "命名空间ID", example = "namespace-456")
    val namespaceId: String? = null,
    
    @field:NotBlank(message = "标题不能为空")
    @Schema(description = "蓝图标题", example = "数据处理流水线")
    val title: String,
    
    @Schema(description = "蓝图描述", example = "用于处理用户数据的ETL流水线")
    val description: String? = null,
    
    @field:NotNull(message = "内容不能为空")
    @Schema(description = "蓝图YAML内容")
    val content: String,
    
    @Schema(description = "标签列表", example = "[\"etl\", \"data-processing\"]")
    val tags: List<String> = emptyList(),
    
    @Schema(description = "包含的任务列表")
    val includedTasks: List<String> = emptyList(),
    
    @Schema(description = "蓝图类型", example = "workflow")
    val kind: String? = null,
    
    @Schema(description = "是否公开", example = "false")
    val isPublic: Boolean = false,
    
    @Schema(description = "是否为模板", example = "false")
    val isTemplate: Boolean = false,
    
    @Schema(description = "创建者ID", example = "user-789")
    val createdBy: String? = null,
    
    @Schema(description = "创建时间")
    val createdAt: Instant? = null,
    
    @Schema(description = "更新时间")
    val updatedAt: Instant? = null,
    
    @Schema(description = "版本号")
    val version: Long? = null
) {
    companion object {
        /**
         * 从Blueprint实体创建DTO
         */
        fun from(blueprint: Blueprint, tags: List<String> = emptyList(), tasks: List<String> = emptyList()): BlueprintDto {
            return BlueprintDto(
                id = blueprint.id,
                namespaceId = blueprint.namespaceId,
                title = blueprint.title,
                description = blueprint.description,
                content = blueprint.content,
                kind = blueprint.kind,
                isPublic = blueprint.isPublic,
                isTemplate = blueprint.isTemplate,
                createdBy = blueprint.createdBy,
                createdAt = blueprint.createdAt,
                updatedAt = blueprint.updatedAt,
                version = blueprint.version,
                tags = tags,
                includedTasks = tasks
            )
        }
    }
}

/**
 * 创建蓝图请求对象
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "创建蓝图请求对象")
data class CreateBlueprintRequest(
    @field:NotBlank(message = "标题不能为空")
    @Schema(description = "蓝图标题", example = "数据处理流水线")
    val title: String,
    
    @Schema(description = "蓝图描述", example = "用于处理用户数据的ETL流水线")
    val description: String? = null,
    
    @field:NotNull(message = "内容不能为空")
    @Schema(description = "蓝图YAML内容")
    val content: String,
    
    @Schema(description = "标签列表", example = "[\"etl\", \"data-processing\"]")
    val tags: List<String> = emptyList(),
    
    @Schema(description = "包含的任务列表")
    val includedTasks: List<String> = emptyList(),
    
    @Schema(description = "蓝图类型", example = "workflow")
    val kind: String? = null,
    
    @Schema(description = "是否公开", example = "false")
    val isPublic: Boolean = false,
    
    @Schema(description = "是否为模板", example = "false")
    val isTemplate: Boolean = false
)

/**
 * 更新蓝图请求对象
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "更新蓝图请求对象")
data class UpdateBlueprintRequest(
    @Schema(description = "蓝图标题", example = "数据处理流水线")
    val title: String? = null,
    
    @Schema(description = "蓝图描述", example = "用于处理用户数据的ETL流水线")
    val description: String? = null,
    
    @Schema(description = "蓝图YAML内容")
    val content: String? = null,
    
    @Schema(description = "标签列表", example = "[\"etl\", \"data-processing\"]")
    val tags: List<String>? = null,
    
    @Schema(description = "包含的任务列表")
    val includedTasks: List<String>? = null,
    
    @Schema(description = "蓝图类型", example = "workflow")
    val kind: String? = null,
    
    @Schema(description = "是否公开", example = "false")
    val isPublic: Boolean? = null,
    
    @Schema(description = "是否为模板", example = "false")
    val isTemplate: Boolean? = null
)

/**
 * 蓝图列表响应对象
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "蓝图列表响应对象")
data class BlueprintListResponse(
    @Schema(description = "蓝图列表")
    val blueprints: List<BlueprintDto>,
    
    @Schema(description = "总数量")
    val total: Long,
    
    @Schema(description = "页码")
    val page: Int,
    
    @Schema(description = "每页大小")
    val size: Int
)

/**
 * 同步蓝图响应对象
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "同步蓝图响应对象")
data class SyncBlueprintResponse(
    @Schema(description = "是否成功", example = "true")
    val success: Boolean,
    
    @Schema(description = "响应消息", example = "官网蓝图同步完成")
    val message: String,
    
    @Schema(description = "成功同步数量", example = "5")
    val syncedCount: Int,
    
    @Schema(description = "失败数量", example = "0")
    val failedCount: Int,
    
    @Schema(description = "失败的蓝图信息")
    val failedBlueprints: List<FailedBlueprintInfo>
)

/**
 * 失败的蓝图信息
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "失败的蓝图信息")
data class FailedBlueprintInfo(
    @Schema(description = "蓝图名称", example = "business-process")
    val name: String,
    
    @Schema(description = "失败原因", example = "网络连接超时")
    val reason: String
)