# Fluvio本地开发环境设置指南

## 🎯 概述

本指南将帮助您在本地开发环境中设置Fluvio队列，并通过`gradle runStandalone`启动Kestra。

## 📋 前置条件

### 1. 安装Fluvio CLI

```bash
# macOS (使用Homebrew)
brew install fluvio/tap/fluvio

# 或者使用curl安装
curl -fsS https://hub.fluvio.io/install/install.sh | bash
```

### 2. 验证安装

```bash
fluvio version
```

## 🚀 启动步骤

### 1. 启动本地Fluvio集群

```bash
# 启动本地Fluvio集群
fluvio cluster start

# 验证集群状态
fluvio cluster status
```

预期输出：
```
Fluvio cluster is running
Platform: local
```

### 2. 验证连接

```bash
# 检查集群连接
fluvio topic list

# 创建测试主题（可选）
fluvio topic create test-topic
fluvio topic list
```

### 3. 启动Kestra

```bash
# 在Kestra项目根目录下运行
./gradlew runStandalone
```

## 🔧 配置说明

当前配置位于 `cli/src/main/resources/application-standalone.yml`：

```yaml
kestra:
  queue:
    type: fluvio
    fluvio:
      cluster-endpoint: "localhost:9003"  # 本地Fluvio集群
      topic-prefix: "kestra"
      replication-factor: 1               # 单节点集群
      partitions: 1                       # 本地开发使用1个分区
      retention:
        time: "P1D"                       # 1天保留期
        size: 1073741824                  # 1GB限制
```

## 📊 验证Fluvio队列工作

### 1. 检查Kestra日志

启动后查看日志中的Fluvio相关信息：

```
INFO  io.kestra.queue.fluvio.FluvioClientManager - Connected to Fluvio cluster
INFO  io.kestra.queue.fluvio.FluvioQueueFactory - Created Fluvio queue for executions
```

### 2. 检查Fluvio主题

```bash
# 查看Kestra创建的主题
fluvio topic list

# 应该看到类似以下主题：
# kestra-executions
# kestra-worker-jobs
# kestra-logs
# kestra-metrics
```

### 3. 监控消息流

```bash
# 监控执行队列的消息
fluvio consume kestra-executions --from-beginning

# 在另一个终端中，创建一个简单的工作流来测试
```

## 🛠️ 故障排除

### 问题1: Fluvio集群启动失败

```bash
# 清理并重新启动
fluvio cluster delete
fluvio cluster start
```

### 问题2: 连接超时

检查端口9003是否被占用：
```bash
lsof -i :9003
```

### 问题3: 权限问题

确保有足够的权限创建主题：
```bash
# 检查集群权限
fluvio cluster status
```

### 问题4: Kestra启动失败

检查依赖是否正确构建：
```bash
./gradlew :queue-fluvio:build
./gradlew :cli:build
```

## 📈 性能监控

### 1. Fluvio指标

```bash
# 查看主题统计
fluvio topic describe kestra-executions

# 监控消费者组
fluvio consumer list
```

### 2. Kestra健康检查

访问：http://localhost:8080/health

查找Fluvio相关的健康检查信息。

## 🔄 切换回JDBC队列

如果需要切换回JDBC队列，修改 `application-standalone.yml`：

```yaml
kestra:
  queue:
    type: memory  # 或 h2
```

然后重启Kestra。

## 📝 开发提示

1. **日志级别**: 设置 `io.kestra.queue.fluvio: DEBUG` 获取详细日志
2. **性能测试**: 使用小的批次大小和分区数进行本地开发
3. **数据清理**: 定期清理测试数据以节省磁盘空间

## 🎉 成功标志

当看到以下日志时，表示Fluvio队列成功运行：

```
INFO  io.kestra.queue.fluvio.FluvioQueue - Successfully emitted message to topic kestra-executions
INFO  io.kestra.queue.fluvio.FluvioMetricsCollector - Message sent to queue: executions
```

现在您可以享受Fluvio队列带来的高性能消息处理体验！🚀
