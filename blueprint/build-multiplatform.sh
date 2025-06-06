#!/bin/bash

# Kestra Blueprint Service Multi-Platform Docker Build Script
# 构建支持多平台的蓝图服务Docker镜像（Linux/AMD64, Linux/ARM64）

set -e

echo "🐳 Building Multi-Platform Kestra Blueprint Service Docker Image..."

# 检查是否在blueprint目录
if [ ! -f "build.gradle" ]; then
    echo "❌ Error: Please run this script from the blueprint directory"
    exit 1
fi

# 设置变量
IMAGE_NAME="dataflare/kestra-blueprint"
VERSION=${1:-latest}
FULL_IMAGE_NAME="${IMAGE_NAME}:${VERSION}"

# 检查Docker buildx是否可用
if ! docker buildx version > /dev/null 2>&1; then
    echo "❌ Error: Docker buildx is not available. Please install Docker Desktop or enable buildx."
    exit 1
fi

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

# 创建buildx builder（如果不存在）
BUILDER_NAME="kestra-builder"
if ! docker buildx ls | grep -q "$BUILDER_NAME"; then
    echo "🔧 Creating buildx builder: $BUILDER_NAME"
    docker buildx create --name "$BUILDER_NAME" --driver docker-container --bootstrap
fi

echo "🔄 Using buildx builder: $BUILDER_NAME"
docker buildx use "$BUILDER_NAME"

echo "🐳 Building multi-platform Docker image: $FULL_IMAGE_NAME"
echo "   Platforms: linux/amd64, linux/arm64"

# 构建多平台Docker镜像
docker buildx build \
    --platform linux/amd64,linux/arm64 \
    --tag "$FULL_IMAGE_NAME" \
    --push \
    .

echo "✅ Multi-platform Docker image built and pushed successfully: $FULL_IMAGE_NAME"

echo ""
echo "🌍 Supported platforms:"
echo "  - linux/amd64 (Intel/AMD x64)"
echo "  - linux/arm64 (Apple Silicon, ARM64)"
echo ""
echo "🚀 To run the container:"
echo "  docker run -d -p 8084:8084 --name kestra-blueprint $FULL_IMAGE_NAME"
echo ""
echo "🐙 To run with docker-compose:"
echo "  docker-compose up -d"
echo ""
echo "📋 Note: This image has been pushed to the registry and supports multiple platforms."
