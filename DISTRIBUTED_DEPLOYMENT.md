# DataFlare 分布式部署指南

本文档介绍如何使用 Docker Compose 部署 DataFlare 的分布式架构。

## 架构概述

分布式部署包含以下组件：

### 核心组件
- **PostgreSQL**: 主数据库，存储工作流定义、执行历史等
- **Redis**: 缓存和会话管理（可选）
- **DataFlare WebServer**: Web UI 和 REST API 服务
- **DataFlare Executor**: 执行引擎，管理工作流执行
- **DataFlare Scheduler**: 调度器，处理定时触发
- **DataFlare Worker**: 工作节点，执行具体任务（可扩展多个）
- **DataFlare Indexer**: 搜索和索引服务

### 监控组件（可选）
- **Nginx**: 负载均衡和反向代理
- **Prometheus**: 指标收集
- **Grafana**: 监控仪表板

## 快速开始

### 1. 准备环境

确保已安装：
- Docker 20.10+
- Docker Compose 2.0+

### 2. 下载配置文件

```bash
# 下载主要的 docker-compose 文件
curl -O https://raw.githubusercontent.com/your-repo/dataflare/main/docker-compose-distributed.yml

# 下载配置文件
curl -O https://raw.githubusercontent.com/your-repo/dataflare/main/nginx.conf
curl -O https://raw.githubusercontent.com/your-repo/dataflare/main/prometheus.yml
```

### 3. 启动服务

```bash
# 启动所有服务
docker-compose -f docker-compose-distributed.yml up -d

# 或者分步启动
# 1. 先启动数据库
docker-compose -f docker-compose-distributed.yml up -d postgres redis

# 2. 等待数据库就绪后启动核心服务
docker-compose -f docker-compose-distributed.yml up -d dataflare-webserver dataflare-executor dataflare-scheduler dataflare-indexer

# 3. 启动工作节点
docker-compose -f docker-compose-distributed.yml up -d dataflare-worker-1 dataflare-worker-2

# 4. 启动监控组件（可选）
docker-compose -f docker-compose-distributed.yml up -d nginx prometheus grafana
```

### 4. 验证部署

```bash
# 检查服务状态
docker-compose -f docker-compose-distributed.yml ps

# 查看日志
docker-compose -f docker-compose-distributed.yml logs -f dataflare-webserver

# 健康检查
curl http://localhost:8080/health
```

## 访问地址

- **DataFlare UI**: http://localhost:8080 (通过 Nginx) 或 http://localhost:8080 (直接访问)
- **DataFlare API**: http://localhost:8080/api/v1/
- **Prometheus**: http://localhost:9090
- **Grafana**: http://localhost:3000 (admin/admin)

## 扩展配置

### 扩展 Worker 节点

```bash
# 复制 worker 配置并修改容器名
docker-compose -f docker-compose-distributed.yml up -d --scale dataflare-worker-2=3
```

### 启用 HTTPS

1. 准备 SSL 证书文件：
   ```bash
   mkdir ssl
   # 将证书文件放入 ssl 目录
   # cert.pem, key.pem
   ```

2. 取消注释 nginx.conf 中的 HTTPS 配置

3. 重启 Nginx：
   ```bash
   docker-compose -f docker-compose-distributed.yml restart nginx
   ```

### 数据库配置

#### 使用外部 PostgreSQL

修改所有 DataFlare 服务的数据库连接配置：

```yaml
datasources:
  postgres:
    url: jdbc:postgresql://your-external-db:5432/dataflare
    driverClassName: org.postgresql.Driver
    username: your-username
    password: your-password
```

#### 数据库备份

```bash
# 备份数据库
docker exec dataflare-postgres pg_dump -U dataflare dataflare > dataflare_backup.sql

# 恢复数据库
docker exec -i dataflare-postgres psql -U dataflare dataflare < dataflare_backup.sql
```

## 监控配置

### Prometheus 指标

DataFlare 暴露以下指标端点：
- `/prometheus` - Prometheus 格式指标
- `/health` - 健康检查
- `/info` - 应用信息

### Grafana 仪表板

1. 访问 Grafana: http://localhost:3000
2. 使用 admin/admin 登录
3. 添加 Prometheus 数据源: http://prometheus:9090
4. 导入 DataFlare 仪表板模板

## 生产环境建议

### 安全配置

1. **启用认证**：
   ```yaml
   kestra:
     server:
       basic-auth:
         enabled: true
         username: "admin@your-domain.com"
         password: "secure-password"
   ```

2. **使用 HTTPS**
3. **配置防火墙规则**
4. **定期更新镜像**

### 性能优化

1. **调整 Worker 线程数**：
   ```bash
   command: server worker --worker-thread 8
   ```

2. **配置资源限制**：
   ```yaml
   deploy:
     resources:
       limits:
         memory: 2G
         cpus: '1.0'
   ```

3. **使用 SSD 存储**
4. **配置数据库连接池**

### 高可用配置

1. **多个 WebServer 实例**
2. **数据库主从复制**
3. **共享存储（如 NFS、S3）**
4. **负载均衡器**

## 故障排除

### 常见问题

1. **服务启动失败**：
   ```bash
   # 查看详细日志
   docker-compose -f docker-compose-distributed.yml logs service-name
   ```

2. **数据库连接问题**：
   - 检查数据库是否就绪
   - 验证连接字符串
   - 检查网络连接

3. **存储权限问题**：
   ```bash
   # 修复存储权限
   sudo chown -R 1000:1000 /path/to/kestra-storage
   ```

### 日志收集

```bash
# 收集所有服务日志
docker-compose -f docker-compose-distributed.yml logs > kestra-logs.txt

# 实时查看特定服务日志
docker-compose -f docker-compose-distributed.yml logs -f kestra-executor
```

## 维护操作

### 更新版本

```bash
# 拉取最新镜像
docker-compose -f docker-compose-distributed.yml pull

# 重启服务
docker-compose -f docker-compose-distributed.yml up -d
```

### 清理资源

```bash
# 停止所有服务
docker-compose -f docker-compose-distributed.yml down

# 清理未使用的镜像
docker image prune -f

# 清理未使用的卷（注意：会删除数据）
docker volume prune -f
```
