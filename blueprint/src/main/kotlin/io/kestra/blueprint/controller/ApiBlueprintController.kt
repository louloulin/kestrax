package io.kestra.blueprint.controller

import io.kestra.blueprint.dto.*
import io.kestra.blueprint.service.BlueprintService
import io.micronaut.core.annotation.Nullable
import io.micronaut.data.model.Pageable
import io.micronaut.http.HttpResponse
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.*
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import io.micronaut.validation.Validated
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import org.slf4j.LoggerFactory
import java.util.*

/**
 * API蓝图控制器
 * 提供与现有前端兼容的API端点
 */
@Controller("/api/v1/blueprints")
@Secured(SecurityRule.IS_ANONYMOUS)
@Validated
@Tag(name = "API Blueprint", description = "兼容现有前端的蓝图API")
open class ApiBlueprintController(
    private val blueprintService: BlueprintService
) {
    
    private val logger = LoggerFactory.getLogger(ApiBlueprintController::class.java)
    
    /**
     * 获取蓝图列表（兼容现有前端格式）
     */
    @Get
    @Operation(
        summary = "获取蓝图列表",
        description = "获取分页的蓝图列表，兼容现有前端格式"
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "成功获取蓝图列表")
    )
    open fun getBlueprints(
        @Parameter(description = "搜索关键词")
        @QueryValue @Nullable q: String?,
        
        @Parameter(description = "排序方式")
        @QueryValue @Nullable sort: String?,
        
        @Parameter(description = "标签过滤")
        @QueryValue @Nullable tags: List<String>?,
        
        @Parameter(description = "页码", example = "1")
        @QueryValue(defaultValue = "1") @Min(1) page: Int,
        
        @Parameter(description = "每页大小", example = "20")
        @QueryValue(defaultValue = "20") @Min(1) size: Int,
        
        @Parameter(description = "是否为公开蓝图")
        @QueryValue @Nullable isPublic: Boolean?,
        
        @Parameter(description = "是否为模板")
        @QueryValue @Nullable isTemplate: Boolean?,
        
        @Parameter(description = "蓝图类型")
        @QueryValue @Nullable kind: String?,
        
        @Parameter(description = "创建者")
        @QueryValue @Nullable createdBy: String?
    ): HttpResponse<PagedResults<ApiBlueprintItem>> {
        logger.debug("获取蓝图列表 - q: {}, tags: {}, page: {}, size: {}", q, tags, page, size)
        
        val pageable = Pageable.from(page - 1, size) // 前端页码从1开始，后端从0开始
        val response = blueprintService.getBlueprints(
            pageable = pageable,
            keyword = q,
            tags = tags,
            kind = kind,
            isPublic = isPublic,
            isTemplate = isTemplate,
            createdBy = createdBy
        )
        
        val pagedResults = PagedResults.fromBlueprintListResponse(response) { dto ->
            ApiBlueprintItem.fromBlueprintDto(dto)
        }
        
        return HttpResponse.ok(pagedResults)
    }
    
    /**
     * 获取单个蓝图（兼容现有前端格式）
     */
    @Get("/{id}")
    @Operation(
        summary = "获取蓝图详情",
        description = "根据ID获取蓝图详情，包含源码"
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "成功获取蓝图详情"),
        ApiResponse(responseCode = "404", description = "蓝图不存在")
    )
    open fun getBlueprint(
        @Parameter(description = "蓝图ID", required = true)
        @PathVariable @NotBlank id: String
    ): HttpResponse<ApiBlueprintItemWithSource> {
        logger.debug("获取蓝图详情 - id: {}", id)
        
        val blueprint = blueprintService.getBlueprintById(id)
        val result = ApiBlueprintItemWithSource.fromBlueprintDto(blueprint)
        
        return HttpResponse.ok(result)
    }
    
    /**
     * 获取蓝图源码（兼容现有前端格式）
     */
    @Get(value = "/{id}/source", produces = [MediaType.APPLICATION_YAML])
    @Operation(
        summary = "获取蓝图源码",
        description = "获取指定蓝图的YAML源码"
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "成功获取蓝图源码"),
        ApiResponse(responseCode = "404", description = "蓝图不存在")
    )
    open fun getBlueprintSource(
        @Parameter(description = "蓝图ID", required = true)
        @PathVariable @NotBlank id: String
    ): HttpResponse<String> {
        logger.debug("获取蓝图源码 - id: {}", id)
        
        val blueprint = blueprintService.getBlueprintById(id)
        return HttpResponse.ok(blueprint.content)
    }
    
    /**
     * 获取蓝图图形（暂时返回空对象）
     */
    @Get("/{id}/graph")
    @Operation(
        summary = "获取蓝图图形",
        description = "获取蓝图的图形表示（暂时返回空对象）"
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "成功获取蓝图图形"),
        ApiResponse(responseCode = "404", description = "蓝图不存在")
    )
    open fun getBlueprintGraph(
        @Parameter(description = "蓝图ID", required = true)
        @PathVariable @NotBlank id: String
    ): HttpResponse<Map<String, Any>> {
        logger.debug("获取蓝图图形 - id: {}", id)
        
        // 暂时返回空对象，后续可以实现图形生成逻辑
        val graph = mapOf<String, Any>(
            "nodes" to emptyList<Any>(),
            "edges" to emptyList<Any>(),
            "clusters" to emptyList<Any>()
        )
        
        return HttpResponse.ok(graph)
    }
    
    /**
     * 获取蓝图标签列表（兼容现有前端格式）
     */
    @Get("/tags")
    @Operation(
        summary = "获取蓝图标签列表",
        description = "获取所有蓝图标签及其使用次数"
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "成功获取标签列表")
    )
    open fun getBlueprintTags(
        @Parameter(description = "搜索关键词")
        @QueryValue @Nullable q: String?
    ): HttpResponse<List<ApiBlueprintTagItem>> {
        logger.debug("获取蓝图标签列表 - q: {}", q)
        
        // 暂时返回空列表，后续可以实现标签统计逻辑
        val tags = emptyList<ApiBlueprintTagItem>()
        
        return HttpResponse.ok(tags)
    }
}
