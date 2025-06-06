#!/bin/bash

# Kestra Blueprint Service Docker Build Script
# 构建蓝图服务的Docker镜像

set -e

echo "🐳 Building Kestra Blueprint Service Docker Image..."

# 检查是否在blueprint目录
if [ ! -f "build.gradle" ]; then
    echo "❌ Error: Please run this script from the blueprint directory"
    exit 1
fi

# 设置变量
IMAGE_NAME="dataflare/kestra-blueprint"
VERSION=${1:-latest}
FULL_IMAGE_NAME="${IMAGE_NAME}:${VERSION}"

echo "📦 Building JAR file..."
# 构建JAR文件
../gradlew clean build -x test

# 检查JAR文件是否存在
JAR_FILE=$(find build/libs -name "blueprint-*-all.jar" | head -1)
if [ ! -f "$JAR_FILE" ]; then
    echo "❌ Error: JAR file not found. Build may have failed."
    exit 1
fi

echo "✅ JAR file built: $JAR_FILE"

echo "🐳 Building Docker image: $FULL_IMAGE_NAME"
# 构建Docker镜像
docker build -t "$FULL_IMAGE_NAME" .

echo "✅ Docker image built successfully: $FULL_IMAGE_NAME"

# 显示镜像信息
echo ""
echo "📊 Image information:"
docker images "$IMAGE_NAME" --format "table {{.Repository}}\t{{.Tag}}\t{{.Size}}\t{{.CreatedAt}}"

echo ""
echo "🚀 To run the container:"
echo "  docker run -d -p 8084:8084 --name kestra-blueprint $FULL_IMAGE_NAME"
echo ""
echo "🐙 To run with docker-compose:"
echo "  docker-compose up -d"
echo ""
echo "🔍 To check logs:"
echo "  docker logs kestra-blueprint"
