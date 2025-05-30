# Kestra Blueprint Module

## 概述

Kestra Blueprint 模块是 Kestra 工作流编排平台的核心组件之一，提供了完整的蓝图管理功能。蓝图是预定义的工作流模板，用户可以基于蓝图快速创建和部署工作流。

## 主要功能

### 🎯 核心功能
- **蓝图管理**: 创建、更新、删除和查询蓝图
- **版本控制**: 完整的蓝图版本历史管理
- **模板引擎**: 基于 Pebble 的动态模板渲染
- **多租户支持**: 基于命名空间的租户隔离
- **权限控制**: 细粒度的访问权限管理

### 🔧 技术特性
- **RESTful API**: 完整的 REST API 接口
- **数据持久化**: 基于 JPA/Hibernate 的数据存储
- **缓存支持**: Caffeine 缓存提升性能
- **健康检查**: 内置健康检查和监控
- **指标收集**: Micrometer 指标监控
- **API 文档**: Swagger/OpenAPI 3.0 文档

## 技术栈

- **语言**: Kotlin 1.8+
- **框架**: Micronaut 4.x
- **数据库**: PostgreSQL (生产) / H2 (开发/测试)
- **ORM**: Hibernate JPA
- **缓存**: Caffeine
- **模板引擎**: Pebble
- **安全**: JWT 认证
- **监控**: Micrometer + Prometheus
- **文档**: OpenAPI 3.0
- **测试**: JUnit 5 + Testcontainers

## 快速开始

### 环境要求

- JDK 17+
- Gradle 7.6+
- PostgreSQL 12+ (生产环境)

### 构建和运行

```bash
# 克隆项目
git clone <repository-url>
cd kestra/blueprint

# 构建项目
./gradlew build

# 运行测试
./gradlew test

# 启动应用 (开发模式)
./gradlew run

# 或者运行 JAR 文件
java -jar build/libs/blueprint-0.1.0.jar
```

### 配置

应用程序支持多环境配置：

- **开发环境**: 使用 H2 内存数据库
- **测试环境**: 使用 H2 内存数据库，详细日志
- **生产环境**: 使用 PostgreSQL，优化配置

主要配置项：

```yaml
# application.yml
micronaut:
  application:
    name: kestra-blueprint
  server:
    port: 8080
    cors:
      enabled: true

# 数据库配置
datasources:
  default:
    url: jdbc:postgresql://localhost:5432/kestra
    username: kestra
    password: kestra
    driver-class-name: org.postgresql.Driver

# JWT 安全配置
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

## API 文档

### 端点概览

| 方法 | 路径 | 描述 |
|------|------|------|
| GET | `/api/v1/blueprints` | 获取蓝图列表 |
| GET | `/api/v1/blueprints/{id}` | 获取蓝图详情 |
| POST | `/api/v1/blueprints` | 创建蓝图 |
| PUT | `/api/v1/blueprints/{id}` | 更新蓝图 |
| DELETE | `/api/v1/blueprints/{id}` | 删除蓝图 |
| GET | `/api/v1/blueprints/{id}/versions` | 获取蓝图版本列表 |
| GET | `/api/v1/blueprints/{id}/versions/{version}` | 获取指定版本蓝图 |

### 认证和授权

所有 API 端点都需要 JWT 认证：

```bash
curl -H "Authorization: Bearer <jwt-token>" \
     -H "X-Namespace-Id: <namespace-id>" \
     http://localhost:8080/api/v1/blueprints
```

### 示例请求

#### 创建蓝图

```bash
curl -X POST http://localhost:8080/api/v1/blueprints \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -H "X-Namespace-Id: <namespace-id>" \
  -d '{
    "title": "Hello World Blueprint",
    "description": "A simple hello world workflow",
    "content": "id: hello-world\nnamespace: {{ namespace }}\ntasks:\n  - id: hello\n    type: io.kestra.core.tasks.log.Log\n    message: Hello {{ name }}!",
    "tags": ["example", "hello-world"],
    "kind": "workflow",
    "isPublic": true,
    "isTemplate": true
  }'
```

#### 获取蓝图列表

```bash
curl "http://localhost:8080/api/v1/blueprints?page=0&size=20&search=hello" \
  -H "Authorization: Bearer <token>" \
  -H "X-Namespace-Id: <namespace-id>"
```

## 数据模型

### 蓝图 (Blueprint)

```kotlin
data class Blueprint(
    val id: String,
    val namespaceId: String,
    val title: String,
    val description: String?,
    val content: String,
    val tags: List<String>,
    val kind: String,
    val isPublic: Boolean,
    val isTemplate: Boolean,
    val version: Int,
    val createdBy: String,
    val createdAt: Instant,
    val updatedAt: Instant
)
```

### 命名空间 (Namespace)

```kotlin
data class Namespace(
    val id: String,
    val name: String,
    val tenantId: String,
    val description: String?,
    val isActive: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant
)
```

## 模板引擎

蓝图支持 Pebble 模板语法，允许动态生成工作流：

### 基本语法

```yaml
id: {{ flowId }}
namespace: {{ namespace }}
description: {{ description | default('Default description') }}

tasks:
{% for task in tasks %}
  - id: {{ task.id }}
    type: {{ task.type }}
    message: {{ task.message }}
{% endfor %}
```

### 条件渲染

```yaml
tasks:
  - id: hello
    type: io.kestra.core.tasks.log.Log
    message: Hello World!
    
{% if includeEmail %}
  - id: email
    type: io.kestra.plugin.notifications.mail.MailSend
    to: {{ emailTo }}
    subject: Notification
{% endif %}
```

### 模板变量

模板引擎支持自动提取变量和生成示例值：

```kotlin
// 提取模板变量
val variables = templateService.extractVariables(template)

// 生成示例变量
val sampleVariables = templateService.generateSampleVariables(template)

// 渲染模板
val renderedContent = templateService.renderTemplate(template, variables)
```

## 开发指南

### 项目结构

```
src/
├── main/kotlin/io/kestra/blueprint/
│   ├── controller/          # REST 控制器
│   ├── service/            # 业务逻辑层
│   ├── repository/         # 数据访问层
│   ├── models/             # 数据模型
│   ├── dto/                # 数据传输对象
│   ├── security/           # 安全相关
│   ├── health/             # 健康检查
│   ├── metrics/            # 指标收集
│   └── Application.kt      # 应用主类
├── main/resources/
│   └── application.yml     # 配置文件
└── test/kotlin/io/kestra/blueprint/
    ├── controller/         # 控制器测试
    ├── service/            # 服务测试
    └── integration/        # 集成测试
```

### 代码规范

- 使用 Kotlin 编码规范
- 所有公共 API 必须有完整的文档注释
- 单元测试覆盖率不低于 80%
- 使用 `@RequirePermission` 注解进行权限控制
- 遵循 RESTful API 设计原则

### 测试

```bash
# 运行所有测试
./gradlew test

# 运行特定测试类
./gradlew test --tests "BlueprintControllerTest"

# 运行集成测试
./gradlew test --tests "*IntegrationTest"

# 生成测试报告
./gradlew test jacocoTestReport
```

## 监控和运维

### 健康检查

```bash
# 检查应用健康状态
curl http://localhost:8080/health

# 检查详细健康信息
curl http://localhost:8080/health/details
```

### 指标监控

```bash
# Prometheus 指标
curl http://localhost:8080/prometheus

# Micrometer 指标
curl http://localhost:8080/metrics
```

### 日志

应用使用 Logback 进行日志管理，支持不同级别的日志输出：

- **ERROR**: 错误信息
- **WARN**: 警告信息
- **INFO**: 一般信息
- **DEBUG**: 调试信息
- **TRACE**: 详细跟踪信息

## 部署

### Docker 部署

```dockerfile
FROM openjdk:17-jre-slim

COPY build/libs/blueprint-*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app.jar"]
```

```bash
# 构建镜像
docker build -t kestra/blueprint .

# 运行容器
docker run -p 8080:8080 \
  -e DATASOURCES_DEFAULT_URL=jdbc:postgresql://db:5432/kestra \
  -e DATASOURCES_DEFAULT_USERNAME=kestra \
  -e DATASOURCES_DEFAULT_PASSWORD=kestra \
  kestra/blueprint
```

### Kubernetes 部署

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: kestra-blueprint
spec:
  replicas: 3
  selector:
    matchLabels:
      app: kestra-blueprint
  template:
    metadata:
      labels:
        app: kestra-blueprint
    spec:
      containers:
      - name: blueprint
        image: kestra/blueprint:latest
        ports:
        - containerPort: 8080
        env:
        - name: DATASOURCES_DEFAULT_URL
          value: "jdbc:postgresql://postgres:5432/kestra"
        - name: DATASOURCES_DEFAULT_USERNAME
          valueFrom:
            secretKeyRef:
              name: postgres-secret
              key: username
        - name: DATASOURCES_DEFAULT_PASSWORD
          valueFrom:
            secretKeyRef:
              name: postgres-secret
              key: password
```

## 故障排除

### 常见问题

1. **数据库连接失败**
   - 检查数据库配置
   - 确认数据库服务正在运行
   - 验证网络连接

2. **JWT 认证失败**
   - 检查 JWT 密钥配置
   - 验证令牌格式和有效期
   - 确认权限设置

3. **模板渲染错误**
   - 检查模板语法
   - 验证变量名称和类型
   - 查看详细错误日志

### 日志分析

```bash
# 查看应用日志
docker logs <container-id>

# 实时跟踪日志
docker logs -f <container-id>

# 过滤错误日志
docker logs <container-id> 2>&1 | grep ERROR
```

## 贡献指南

1. Fork 项目
2. 创建功能分支 (`git checkout -b feature/amazing-feature`)
3. 提交更改 (`git commit -m 'Add some amazing feature'`)
4. 推送到分支 (`git push origin feature/amazing-feature`)
5. 创建 Pull Request

### 开发环境设置

```bash
# 安装依赖
./gradlew dependencies

# 运行开发服务器
./gradlew run --continuous

# 运行测试
./gradlew test --continuous
```

## 许可证

本项目采用 Apache 2.0 许可证。详情请参阅 [LICENSE](LICENSE) 文件。

## 联系方式

- **项目主页**: https://kestra.io
- **文档**: https://kestra.io/docs
- **社区**: https://kestra.io/slack
- **问题反馈**: https://github.com/kestra-io/kestra/issues

---

**注意**: 这是一个示例模块，实际部署时请根据具体环境调整配置。