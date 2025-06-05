package io.kestra.blueprint.dto

import com.fasterxml.jackson.annotation.JsonInclude
import io.micronaut.core.annotation.Introspected
import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant

/**
 * API蓝图项目DTO
 * 兼容现有前端的数据格式
 */
@Introspected
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "蓝图项目信息")
data class ApiBlueprintItem(
    @Schema(description = "蓝图ID", example = "hello-world")
    val id: String,
    
    @Schema(description = "蓝图标题", example = "Hello World Flow")
    val title: String,
    
    @Schema(description = "蓝图描述", example = "A simple hello world flow")
    val description: String?,
    
    @Schema(description = "蓝图标签", example = "[\"hello\", \"world\", \"demo\"]")
    val tags: List<String>,
    
    @Schema(description = "包含的任务类型", example = "[\"io.kestra.plugin.core.log.Log\"]")
    val includedTasks: List<String>,
    
    @Schema(description = "蓝图类型", example = "FLOW")
    val kind: String?,
    
    @Schema(description = "是否为公开蓝图", example = "true")
    val isPublic: Boolean,
    
    @Schema(description = "是否为模板", example = "false")
    val isTemplate: Boolean,
    
    @Schema(description = "创建者", example = "system")
    val createdBy: String,
    
    @Schema(description = "创建时间")
    val createdAt: Instant,
    
    @Schema(description = "更新时间")
    val updatedAt: Instant
) {
    companion object {
        /**
         * 从BlueprintDto转换为ApiBlueprintItem
         */
        fun fromBlueprintDto(dto: BlueprintDto): ApiBlueprintItem {
            return ApiBlueprintItem(
                id = dto.id ?: "unknown",
                title = dto.title,
                description = dto.description ?: "",
                tags = dto.tags,
                includedTasks = dto.includedTasks,
                kind = dto.kind ?: "FLOW",
                isPublic = dto.isPublic,
                isTemplate = dto.isTemplate,
                createdBy = dto.createdBy ?: "system",
                createdAt = dto.createdAt ?: java.time.Instant.now(),
                updatedAt = dto.updatedAt ?: java.time.Instant.now()
            )
        }
    }
}

/**
 * API蓝图项目（包含源码）DTO
 * 兼容现有前端的数据格式
 */
@Introspected
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "蓝图项目信息（包含源码）")
data class ApiBlueprintItemWithSource(
    @Schema(description = "蓝图ID", example = "hello-world")
    val id: String,
    
    @Schema(description = "蓝图标题", example = "Hello World Flow")
    val title: String,
    
    @Schema(description = "蓝图描述", example = "A simple hello world flow")
    val description: String?,
    
    @Schema(description = "蓝图标签", example = "[\"hello\", \"world\", \"demo\"]")
    val tags: List<String>,
    
    @Schema(description = "包含的任务类型", example = "[\"io.kestra.plugin.core.log.Log\"]")
    val includedTasks: List<String>,
    
    @Schema(description = "蓝图类型", example = "FLOW")
    val kind: String?,
    
    @Schema(description = "是否为公开蓝图", example = "true")
    val isPublic: Boolean,
    
    @Schema(description = "是否为模板", example = "false")
    val isTemplate: Boolean,
    
    @Schema(description = "创建者", example = "system")
    val createdBy: String,
    
    @Schema(description = "创建时间")
    val createdAt: Instant,
    
    @Schema(description = "更新时间")
    val updatedAt: Instant,
    
    @Schema(description = "蓝图源码", example = "id: hello-world\\nnamespace: demo\\n\\ntasks:\\n  - id: hello\\n    type: io.kestra.plugin.core.log.Log\\n    message: Hello World!")
    val source: String
) {
    companion object {
        /**
         * 从BlueprintDto转换为ApiBlueprintItemWithSource
         */
        fun fromBlueprintDto(dto: BlueprintDto): ApiBlueprintItemWithSource {
            return ApiBlueprintItemWithSource(
                id = dto.id ?: "unknown",
                title = dto.title,
                description = dto.description ?: "",
                tags = dto.tags,
                includedTasks = dto.includedTasks,
                kind = dto.kind ?: "FLOW",
                isPublic = dto.isPublic,
                isTemplate = dto.isTemplate,
                createdBy = dto.createdBy ?: "system",
                createdAt = dto.createdAt ?: java.time.Instant.now(),
                updatedAt = dto.updatedAt ?: java.time.Instant.now(),
                source = dto.content
            )
        }
    }
}

/**
 * API蓝图标签项目DTO
 * 兼容现有前端的数据格式
 */
@Introspected
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "蓝图标签信息")
data class ApiBlueprintTagItem(
    @Schema(description = "标签ID", example = "demo")
    val id: String,
    
    @Schema(description = "标签名称", example = "Demo")
    val name: String,
    
    @Schema(description = "使用该标签的蓝图数量", example = "5")
    val count: Int
)

/**
 * 分页结果DTO
 * 兼容现有前端的数据格式
 */
@Introspected
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "分页结果")
data class PagedResults<T>(
    @Schema(description = "结果列表")
    val results: List<T>,
    
    @Schema(description = "总数", example = "100")
    val total: Long,
    
    @Schema(description = "当前页", example = "1")
    val page: Int,
    
    @Schema(description = "每页大小", example = "20")
    val size: Int
) {
    companion object {
        /**
         * 从BlueprintListResponse转换为PagedResults
         */
        fun <T> fromBlueprintListResponse(
            response: BlueprintListResponse,
            mapper: (BlueprintDto) -> T
        ): PagedResults<T> {
            return PagedResults(
                results = response.blueprints.map(mapper),
                total = response.total,
                page = response.page,
                size = response.size
            )
        }
    }
}
