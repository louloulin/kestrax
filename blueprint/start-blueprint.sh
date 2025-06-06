#!/bin/bash

# Kestra Blueprint Service Docker Startup Script
# This script starts the Blueprint service in Docker container

set -e

echo "🚀 Starting Kestra Blueprint Service..."

# 设置默认环境变量（如果未设置）
export MICRONAUT_ENVIRONMENTS=${MICRONAUT_ENVIRONMENTS:-docker}
export MICRONAUT_SERVER_PORT=${MICRONAUT_SERVER_PORT:-8084}
export DATASOURCES_DEFAULT_URL=${DATASOURCES_DEFAULT_URL:-jdbc:h2:file:/app/data/blueprint;DB_CLOSE_DELAY=-1}
export DATASOURCES_DEFAULT_DRIVER_CLASS_NAME=${DATASOURCES_DEFAULT_DRIVER_CLASS_NAME:-org.h2.Driver}
export DATASOURCES_DEFAULT_USERNAME=${DATASOURCES_DEFAULT_USERNAME:-sa}
export DATASOURCES_DEFAULT_PASSWORD=${DATASOURCES_DEFAULT_PASSWORD:-}

# 确保数据目录存在
mkdir -p /app/data /app/logs

echo "📋 Configuration:"
echo "  Environment: ${MICRONAUT_ENVIRONMENTS}"
echo "  Port: ${MICRONAUT_SERVER_PORT}"
echo "  Database: ${DATASOURCES_DEFAULT_URL}"
echo "  Java Options: ${JAVA_OPTS}"

echo ""
echo "🌐 Available endpoints:"
echo "  GET    /api/v1/blueprints/community     - 获取社区蓝图"
echo "  POST   /api/v1/blueprints/sync/official - 同步官方蓝图"
echo "  GET    /health                          - 健康检查"
echo "  GET    /metrics                         - 监控指标"
echo ""

# 启动应用
echo "🎯 Starting Blueprint Service on port ${MICRONAUT_SERVER_PORT}..."
exec java ${JAVA_OPTS} -jar /app/blueprint.jar