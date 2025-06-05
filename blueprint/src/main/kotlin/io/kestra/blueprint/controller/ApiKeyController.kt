package io.kestra.blueprint.controller

import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.inject.Singleton

/**
 * API Key信息控制器
 */
@Controller("/api/v1/auth")
@Tag(name = "认证信息", description = "API Key使用说明")
@Singleton
class ApiKeyController {
    
    /**
     * 获取API Key使用说明
     */
    @Get("/api-keys")
    @Operation(
        summary = "获取API Key使用说明",
        description = "获取可用的API Key列表和使用方法"
    )
    @ApiResponse(
        responseCode = "200",
        description = "API Key信息",
        content = [Content(schema = Schema(implementation = ApiKeyInfo::class))]
    )
    fun getApiKeyInfo(): HttpResponse<ApiKeyInfo> {
        return HttpResponse.ok(
            ApiKeyInfo(
                message = "蓝图服务API Key认证说明",
                status = "当前安全认证已禁用，可直接访问API",
                usage = mapOf(
                    "current" to "无需认证，直接访问",
                    "production" to "在HTTP请求头中添加 'X-API-Key: your-api-key'"
                ),
                availableEndpoints = listOf(
                    "/api/v1/blueprints - 蓝图管理",
                    "/api/v1/namespaces - 命名空间管理", 
                    "/swagger-ui/index.html - API文档",
                    "/health - 健康检查",
                    "/metrics - 监控指标"
                ),
                futureApiKeys = listOf(
                    ApiKeyDetail(
                        key = "dataflare-blueprint-key-2024",
                        description = "DataFlare管理员API Key",
                        permissions = listOf("BLUEPRINT_ADMIN", "BLUEPRINT_USER"),
                        user = "dataflare-admin",
                        namespace = "dataflare-namespace"
                    ),
                    ApiKeyDetail(
                        key = "kestra-admin-key", 
                        description = "Kestra管理员API Key",
                        permissions = listOf("BLUEPRINT_ADMIN", "BLUEPRINT_USER"),
                        user = "kestra-admin",
                        namespace = "kestra-namespace"
                    ),
                    ApiKeyDetail(
                        key = "demo-api-key",
                        description = "演示用户API Key",
                        permissions = listOf("BLUEPRINT_USER"),
                        user = "demo-user",
                        namespace = "demo-namespace"
                    )
                )
            )
        )
    }
    
    /**
     * 获取服务状态
     */
    @Get("/status")
    @Operation(
        summary = "获取认证状态",
        description = "获取当前认证服务的状态信息"
    )
    fun getAuthStatus(): HttpResponse<Map<String, Any>> {
        return HttpResponse.ok(
            mapOf(
                "service" to "Blueprint Authentication Service",
                "status" to "running",
                "security" to "disabled",
                "version" to "1.0.0",
                "timestamp" to System.currentTimeMillis(),
                "message" to "认证已禁用，可直接访问所有API端点"
            )
        )
    }
}

/**
 * API Key信息DTO
 */
data class ApiKeyInfo(
    val message: String,
    val status: String,
    val usage: Map<String, String>,
    val availableEndpoints: List<String>,
    val futureApiKeys: List<ApiKeyDetail>
)

/**
 * API Key详情DTO
 */
data class ApiKeyDetail(
    val key: String,
    val description: String,
    val permissions: List<String>,
    val user: String,
    val namespace: String
)
