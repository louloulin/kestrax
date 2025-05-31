# Kestra Blueprint Management Module

## 概述

Blueprint 模块是 Kestra 工作流引擎的蓝图管理组件，提供了工作流模板的创建、管理、版本控制和渲染功能。该模块支持多租户架构，具备完整的权限控制和监控能力。

## 功能特性

### 核心功能
- **蓝图管理**: 创建、更新、删除和查询工作流蓝图
- **版本控制**: 支持蓝图的版本管理和历史追踪
- **模板引擎**: 基于 Pebble 模板引擎的动态内容渲染
- **多租户支持**: 基于命名空间的租户隔离
- **权限控制**: 细粒度的访问权限管理
- **监控指标**: 完整的性能监控和健康检查

### 技术栈
- **框架**: Micronaut 4.x
- **语言**: Kotlin
- **数据库**: 支持 H2、PostgreSQL
- **模板引擎**: Pebble
- **监控**: Micrometer + Prometheus
- **安全**: JWT 认证

## 项目结构

```
blueprint/
├── src/main/kotlin/io/kestra/blueprint/
│   ├── Application.kt                 # 应用程序入口
│   ├── controller/                    # REST API 控制器
│   │   └── BlueprintController.kt
│   ├── dto/                          # 数据传输对象
│   │   ├── BlueprintCreateRequest.kt
│   │   ├── BlueprintResponse.kt
│   │   ├── BlueprintUpdateRequest.kt
│   │   └── BlueprintVersionResponse.kt
│   ├── health/                       # 健康检查
│   │   └── BlueprintHealthIndicator.kt
│   ├── metrics/                      # 监控指标
│   │   └── BlueprintMetrics.kt
│   ├── models/                       # 数据模型
│   │   ├── Blueprint.kt
│   │   ├── BlueprintVersion.kt
│   │   └── Namespace.kt
│   ├── repository/                   # 数据访问层
│   │   ├── BlueprintRepository.kt
│   │   ├── BlueprintVersionRepository.kt
│   │   └── NamespaceRepository.kt
│   ├── security/                     # 安全组件
│   │   ├── PermissionInterceptor.kt
│   │   ├── RequirePermission.kt
│   │   └── TenantContext.kt
│   └── service/                      # 业务逻辑层
│       ├── BlueprintService.kt
│       └── BlueprintTemplateService.kt
├── src/test/kotlin/                  # 测试代码
└── src/main/resources/
    └── application.yml               # 配置文件
```

## 📋 API 接口

### 蓝图管理
- `GET /api/v1/blueprints` - 获取蓝图列表
- `POST /api/v1/blueprints` - 创建蓝图
- `GET /api/v1/blueprints/{id}` - 获取蓝图详情
- `PUT /api/v1/blueprints/{id}` - 更新蓝图
- `DELETE /api/v1/blueprints/{id}` - 删除蓝图

### 版本管理
- `GET /api/v1/blueprints/{id}/versions` - 获取版本列表
- `GET /api/v1/blueprints/{id}/versions/{version}` - 获取特定版本

### 模板引擎
- `POST /api/v1/blueprints/render` - 渲染蓝图模板
- `POST /api/v1/blueprints/validate` - 验证模板语法

### 官网蓝图同步
- `POST /api/v1/blueprints/sync/official` - 同步Kestra官网蓝图模板

#### 获取蓝图列表
```http
GET /api/v1/blueprints
Parameters:
  - namespace: 命名空间 (可选)
  - page: 页码 (默认: 0)
  - size: 页面大小 (默认: 20)
```

#### 创建蓝图
```http
POST /api/v1/blueprints
Content-Type: application/json

{
  "name": "my-workflow-blueprint",
  "description": "示例工作流蓝图",
  "namespace": "default",
  "template": "id: {{ name }}\ntasks:\n  - id: hello\n    type: io.kestra.core.tasks.log.Log\n    message: Hello {{ message }}!",
  "tags": ["example", "demo"]
}
```

#### 获取蓝图详情
```http
GET /api/v1/blueprints/{id}
```

#### 更新蓝图
```http
PUT /api/v1/blueprints/{id}
Content-Type: application/json

{
  "description": "更新后的描述",
  "template": "更新后的模板内容",
  "tags": ["updated"]
}
```

#### 删除蓝图
```http
DELETE /api/v1/blueprints/{id}
```

#### 获取蓝图版本列表
```http
GET /api/v1/blueprints/{id}/versions
```

#### 渲染蓝图模板
```http
POST /api/v1/blueprints/{id}/render
Content-Type: application/json

{
  "variables": {
    "name": "my-workflow",
    "message": "World"
  }
}
```

### 监控端点

#### 健康检查
```http
GET /health
```

#### 监控指标
```http
GET /metrics
```

## 配置说明

### 数据库配置
```yaml
datasources:
  default:
    url: jdbc:h2:mem:blueprint;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
    driver-class-name: org.h2.Driver
    username: sa
    password: ""
```

### 安全配置
```yaml
micronaut:
  security:
    authentication: bearer
    token:
      jwt:
        signatures:
          secret:
            generator:
              secret: "${JWT_GENERATOR_SIGNATURE_SECRET:pleaseChangeThisSecretForANewOne}"
```

## 🚀 快速开始

### 启动服务

```bash
# 使用提供的启动脚本
./start-blueprint.sh

# 或手动启动
./gradlew blueprint:run
```

服务启动后，访问以下地址：
- API文档: http://localhost:8080/swagger-ui
- 健康检查: http://localhost:8080/health
- 指标监控: http://localhost:8080/metrics

### 同步官网蓝图

```bash
# 使用官网蓝图同步脚本
./sync-official-blueprints.sh

# 或手动同步
curl -X POST http://localhost:8080/api/v1/blueprints/sync/official \
     -H 'Content-Type: application/json' \
     -H 'Authorization: Bearer YOUR_TOKEN'
```

**注意**: 同步官网蓝图需要管理员权限 (`blueprint:admin`)

### 验证服务
```bash
# 健康检查
curl http://localhost:8080/health

# 获取蓝图列表
curl http://localhost:8080/api/v1/blueprints
```

## 开发指南

### 添加新功能
1. 在相应的包中创建新的类文件
2. 遵循现有的代码结构和命名约定
3. 添加相应的单元测试
4. 更新 API 文档

### 测试
```bash
# 运行所有测试
./gradlew blueprint:test

# 运行特定测试
./gradlew blueprint:test --tests "*BlueprintServiceTest*"
```

### 代码质量
- 遵循 Kotlin 编码规范
- 使用有意义的变量和方法名
- 添加适当的注释和文档
- 保持测试覆盖率

## 部署

### Docker 部署
```dockerfile
FROM openjdk:17-jre-slim
COPY build/libs/blueprint-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

### 环境变量
- `MICRONAUT_ENVIRONMENTS`: 运行环境 (dev, test, prod)
- `DATASOURCES_DEFAULT_URL`: 数据库连接 URL
- `JWT_GENERATOR_SIGNATURE_SECRET`: JWT 签名密钥

## 故障排除

### 常见问题

1. **构建失败**
   - 检查 Java 版本 (需要 17+)
   - 确保网络连接正常
   - 清理构建缓存: `./gradlew clean`

2. **数据库连接问题**
   - 检查数据库配置
   - 确保数据库服务正在运行
   - 验证连接字符串和凭据

3. **权限错误**
   - 检查 JWT 令牌是否有效
   - 验证用户权限配置
   - 确认命名空间访问权限

### 日志配置
```yaml
logger:
  levels:
    io.kestra.blueprint: DEBUG
    root: INFO
```

## 贡献

1. Fork 项目
2. 创建功能分支
3. 提交更改
4. 推送到分支
5. 创建 Pull Request

## 许可证

本项目采用 Apache 2.0 许可证。详情请参阅 LICENSE 文件。