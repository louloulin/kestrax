#!/bin/bash

# 快速启动Kestra with Fluvio
echo "🚀 快速启动 Kestra with Fluvio Queue"
echo "=================================="

# 检查Fluvio CLI
if ! command -v fluvio &> /dev/null; then
    echo "❌ 请先安装Fluvio CLI:"
    echo "curl -fsS https://hub.fluvio.io/install/install.sh | bash"
    exit 1
fi

# 启动Fluvio集群
echo "📡 启动Fluvio集群..."
fluvio cluster start || {
    echo "🔄 重试启动..."
    fluvio cluster delete
    sleep 2
    fluvio cluster start
}

# 验证连接
echo "🔍 验证连接..."
fluvio topic list > /dev/null || {
    echo "❌ Fluvio连接失败"
    exit 1
}

echo "✅ Fluvio集群就绪"

# 构建并启动Kestra
echo "🔨 构建项目..."
./gradlew :queue-fluvio:build -x test
./gradlew :cli:build -x test

echo "🚀 启动Kestra..."
echo "Web界面: http://localhost:8080"
echo "按 Ctrl+C 停止"
echo ""

./gradlew runStandalone
