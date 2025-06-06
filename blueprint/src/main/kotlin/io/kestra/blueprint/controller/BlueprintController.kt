package io.kestra.blueprint.controller

import io.kestra.blueprint.dto.*
import io.kestra.blueprint.security.RequirePermission
import io.kestra.blueprint.service.BlueprintService
import io.kestra.blueprint.service.SimpleBlueprintSyncService
import io.kestra.blueprint.service.OfficialBlueprintSyncService
import io.micronaut.data.model.Page
import io.micronaut.data.model.Pageable
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.annotation.*
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import io.micronaut.validation.Validated
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import org.slf4j.LoggerFactory

/**
 * 蓝图控制器
 * 基于Kotlin + Micronaut实现的RESTful API
 */
@Controller("/api/v1/blueprints")
@Secured(SecurityRule.IS_ANONYMOUS)
@Validated
@Tag(name = "Blueprint", description = "蓝图管理API")
open class BlueprintController(
    private val blueprintService: BlueprintService,
    private val simpleBlueprintSyncService: SimpleBlueprintSyncService,
    private val officialBlueprintSyncService: OfficialBlueprintSyncService
) {

    private val logger = LoggerFactory.getLogger(BlueprintController::class.java)
    
    /**
     * 获取蓝图列表
     */
    @Get
    @RequirePermission(["blueprint:read"])
    @Operation(
        summary = "获取蓝图列表",
        description = "支持分页、搜索和过滤的蓝图列表查询"
    )
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "成功获取蓝图列表",
            content = [Content(schema = Schema(implementation = BlueprintListResponse::class))]
        ),
        ApiResponse(responseCode = "401", description = "未认证"),
        ApiResponse(responseCode = "403", description = "权限不足")
    )
    open fun getBlueprints(
        @Parameter(description = "页码", example = "0")
        @QueryValue(defaultValue = "0") @Min(0) page: Int,
        
        @Parameter(description = "每页大小", example = "20")
        @QueryValue(defaultValue = "20") @Min(1) size: Int,
        
        @Parameter(description = "搜索关键词")
        @QueryValue keyword: String?,
        
        @Parameter(description = "标签过滤")
        @QueryValue tags: List<String>?,
        
        @Parameter(description = "类型过滤")
        @QueryValue kind: String?,
        
        @Parameter(description = "是否公开")
        @QueryValue isPublic: Boolean?,
        
        @Parameter(description = "是否为模板")
        @QueryValue isTemplate: Boolean?,
        
        @Parameter(description = "创建者过滤")
        @QueryValue createdBy: String?
    ): HttpResponse<BlueprintListResponse> {
        val pageable = Pageable.from(page, size)
        val response = blueprintService.getBlueprints(
            pageable = pageable,
            keyword = keyword,
            tags = tags,
            kind = kind,
            isPublic = isPublic,
            isTemplate = isTemplate,
            createdBy = createdBy
        )
        return HttpResponse.ok(response)
    }
    
    /**
     * 根据ID获取蓝图
     */
    @Get("/{id}")
    @RequirePermission(["blueprint:read"])
    @Operation(
        summary = "获取蓝图详情",
        description = "根据ID获取指定蓝图的详细信息"
    )
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "成功获取蓝图详情",
            content = [Content(schema = Schema(implementation = BlueprintDto::class))]
        ),
        ApiResponse(responseCode = "404", description = "蓝图不存在"),
        ApiResponse(responseCode = "401", description = "未认证"),
        ApiResponse(responseCode = "403", description = "权限不足")
    )
    open fun getBlueprintById(
        @Parameter(description = "蓝图ID", required = true)
        @PathVariable @NotBlank id: String
    ): HttpResponse<BlueprintDto> {
        val blueprint = blueprintService.getBlueprintById(id)
        return HttpResponse.ok(blueprint)
    }
    
    /**
     * 创建蓝图
     */
    @Post
    @Operation(
        summary = "创建蓝图",
        description = "创建新的蓝图"
    )
    @ApiResponses(
        ApiResponse(
            responseCode = "201",
            description = "成功创建蓝图",
            content = [Content(schema = Schema(implementation = BlueprintDto::class))]
        ),
        ApiResponse(responseCode = "400", description = "请求参数错误"),
        ApiResponse(responseCode = "409", description = "蓝图标题已存在"),
        ApiResponse(responseCode = "401", description = "未认证"),
        ApiResponse(responseCode = "403", description = "权限不足")
    )
    open fun createBlueprint(
        @Parameter(description = "创建蓝图请求", required = true)
        @Body @Valid request: CreateBlueprintRequest
    ): HttpResponse<BlueprintDto> {
        logger.debug("创建蓝图请求: {}", request)

        try {
            val blueprint = blueprintService.createBlueprint(request)
            logger.debug("蓝图创建成功: {}", blueprint.id)
            return HttpResponse.status<BlueprintDto>(HttpStatus.CREATED).body(blueprint)
        } catch (e: Exception) {
            logger.error("创建蓝图失败", e)
            throw e
        }
    }
    
    /**
     * 更新蓝图
     */
    @Put("/{id}")
    @RequirePermission(["blueprint:write"])
    @Operation(
        summary = "更新蓝图",
        description = "更新指定ID的蓝图信息"
    )
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "成功更新蓝图",
            content = [Content(schema = Schema(implementation = BlueprintDto::class))]
        ),
        ApiResponse(responseCode = "400", description = "请求参数错误"),
        ApiResponse(responseCode = "404", description = "蓝图不存在"),
        ApiResponse(responseCode = "409", description = "蓝图标题已存在"),
        ApiResponse(responseCode = "401", description = "未认证"),
        ApiResponse(responseCode = "403", description = "权限不足")
    )
    open fun updateBlueprint(
        @Parameter(description = "蓝图ID", required = true)
        @PathVariable @NotBlank id: String,
        
        @Parameter(description = "更新蓝图请求", required = true)
        @Body @Valid request: UpdateBlueprintRequest
    ): HttpResponse<BlueprintDto> {
        val blueprint = blueprintService.updateBlueprint(id, request)
        return HttpResponse.ok(blueprint)
    }
    
    /**
     * 删除蓝图
     */
    @Delete("/{id}")
    @RequirePermission(["blueprint:delete"])
    @Operation(
        summary = "删除蓝图",
        description = "删除指定ID的蓝图"
    )
    @ApiResponses(
        ApiResponse(responseCode = "204", description = "成功删除蓝图"),
        ApiResponse(responseCode = "404", description = "蓝图不存在"),
        ApiResponse(responseCode = "401", description = "未认证"),
        ApiResponse(responseCode = "403", description = "权限不足")
    )
    open fun deleteBlueprint(
        @Parameter(description = "蓝图ID", required = true)
        @PathVariable @NotBlank id: String
    ): HttpResponse<Void> {
        blueprintService.deleteBlueprint(id)
        return HttpResponse.noContent()
    }

    
    /**
     * 同步官网蓝图
     */
    @Post("/sync/official")
    @RequirePermission(["blueprint:admin"])
    @Operation(
        summary = "同步官网蓝图",
        description = "从Kestra官网同步最新的蓝图模板"
    )
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "同步成功",
            content = [Content(schema = Schema(implementation = SyncBlueprintResponse::class))]
        ),
        ApiResponse(responseCode = "401", description = "未认证"),
        ApiResponse(responseCode = "403", description = "权限不足"),
        ApiResponse(responseCode = "500", description = "同步失败")
    )
    open fun syncOfficialBlueprints(): HttpResponse<SyncBlueprintResponse> {
        return try {
            logger.info("开始同步Kestra官方蓝图...")
            val response = officialBlueprintSyncService.syncOfficialBlueprints()
            HttpResponse.ok(response)
        } catch (e: Exception) {
            logger.error("同步官方蓝图失败", e)
            val response = SyncBlueprintResponse(
                success = false,
                message = "同步失败: ${e.message}",
                syncedCount = 0,
                failedCount = 0,
                failedBlueprints = listOf(FailedBlueprintInfo("unknown", e.message ?: "未知错误"))
            )
            HttpResponse.status<SyncBlueprintResponse>(HttpStatus.INTERNAL_SERVER_ERROR).body(response)
        }
    }
}