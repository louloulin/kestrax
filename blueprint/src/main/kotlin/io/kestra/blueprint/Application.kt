package io.kestra.blueprint

import io.micronaut.runtime.Micronaut
import io.swagger.v3.oas.annotations.OpenAPIDefinition
import io.swagger.v3.oas.annotations.info.Contact
import io.swagger.v3.oas.annotations.info.Info
import io.swagger.v3.oas.annotations.info.License
import io.swagger.v3.oas.annotations.servers.Server

/**
 * Kestra Blueprint 模块应用程序主类
 * 
 * 该模块提供蓝图管理功能，包括：
 * - 蓝图的创建、更新、删除和查询
 * - 蓝图版本管理
 * - 蓝图模板引擎
 * - 多租户支持
 * - 权限控制
 * - 健康检查和指标监控
 */
@OpenAPIDefinition(
    info = Info(
        title = "Kestra Blueprint API",
        version = "1.0.0",
        description = """Kestra Blueprint 模块 API 文档

该 API 提供了完整的蓝图管理功能，包括：

## 核心功能
- **蓝图管理**: 创建、更新、删除和查询蓝图
- **版本控制**: 蓝图版本历史管理
- **模板引擎**: 支持 Pebble 模板语法的动态蓝图生成
- **多租户**: 基于命名空间的租户隔离
- **权限控制**: 细粒度的权限管理

## 认证和授权
所有 API 端点都需要有效的 JWT 令牌进行认证。
请在请求头中包含 `Authorization: Bearer <token>`。

## 多租户支持
请在请求头中包含 `X-Namespace-Id` 来指定操作的命名空间。

## 分页
列表 API 支持分页参数：
- `page`: 页码（从 0 开始）
- `size`: 每页大小（默认 20，最大 100）

## 搜索和过滤
支持多种搜索和过滤选项：
- `search`: 关键词搜索（标题、描述）
- `tags`: 标签过滤
- `kind`: 类型过滤
- `isPublic`: 公开状态过滤
- `isTemplate`: 模板状态过滤""",
        contact = Contact(
            name = "Kestra Team",
            url = "https://kestra.io",
            email = "support@kestra.io"
        ),
        license = License(
            name = "Apache 2.0",
            url = "https://www.apache.org/licenses/LICENSE-2.0"
        )
    ),
    servers = [
        Server(
            url = "http://localhost:8084",
            description = "开发环境"
        ),
        Server(
            url = "https://api.kestra.io",
            description = "生产环境"
        )
    ]
)
class Application {
    companion object {
        /**
         * 应用程序入口点
         */
        @JvmStatic
        fun main(args: Array<String>) {
            Micronaut.run(Application::class.java, *args)
        }
    }
}