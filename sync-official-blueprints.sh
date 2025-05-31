#!/bin/bash

# Kestra Blueprint 官网蓝图同步脚本
# 此脚本演示如何同步Kestra官网的蓝图模板

echo "=== Kestra Blueprint 官网蓝图同步演示 ==="
echo

# 设置环境变量
export MICRONAUT_ENVIRONMENTS=dev
export DATASOURCES_DEFAULT_URL="jdbc:h2:mem:blueprint_db;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE"
export DATASOURCES_DEFAULT_DRIVER_CLASS_NAME="org.h2.Driver"
export DATASOURCES_DEFAULT_USERNAME="sa"
export DATASOURCES_DEFAULT_PASSWORD=""
export DATASOURCES_DEFAULT_DIALECT="H2"

echo "1. 环境配置完成"
echo "   - 数据库: H2 内存数据库"
echo "   - 环境: dev"
echo

# 构建Blueprint模块
echo "2. 构建Blueprint模块..."
./gradlew blueprint:build -x test
if [ $? -eq 0 ]; then
    echo "   ✓ Blueprint模块构建成功"
else
    echo "   ✗ Blueprint模块构建失败"
    exit 1
fi
echo

# 启动Blueprint服务
echo "3. 启动Blueprint服务..."
echo "   服务将在后台启动，请稍等..."
./gradlew blueprint:run &
BLUEPRINT_PID=$!

# 等待服务启动
echo "   等待服务启动..."
sleep 15

# 检查服务是否启动成功
echo "4. 检查服务状态..."
curl -s http://localhost:8080/health > /dev/null
if [ $? -eq 0 ]; then
    echo "   ✓ Blueprint服务启动成功"
    echo "   ✓ 服务地址: http://localhost:8080"
else
    echo "   ✗ Blueprint服务启动失败"
    kill $BLUEPRINT_PID 2>/dev/null
    exit 1
fi
echo

echo "5. 可用的API接口:"
echo "   - 健康检查: GET http://localhost:8080/health"
echo "   - API文档: GET http://localhost:8080/swagger-ui"
echo "   - 蓝图列表: GET http://localhost:8080/api/v1/blueprints"
echo "   - 同步官网蓝图: POST http://localhost:8080/api/v1/blueprints/sync/official"
echo

echo "6. 同步官网蓝图示例:"
echo "   使用以下命令同步官网蓝图:"
echo
echo "   curl -X POST http://localhost:8080/api/v1/blueprints/sync/official \\"
echo "        -H 'Content-Type: application/json' \\"
echo "        -H 'Authorization: Bearer YOUR_TOKEN'"
echo
echo "   注意: 需要管理员权限 (blueprint:admin)"
echo

echo "7. 查看同步的蓝图:"
echo "   curl -X GET http://localhost:8080/api/v1/blueprints?isTemplate=true"
echo

echo "=== 官网蓝图同步功能已就绪 ==="
echo "按 Ctrl+C 停止服务"
echo

# 等待用户中断
trap "echo; echo '正在停止服务...'; kill $BLUEPRINT_PID 2>/dev/null; echo '服务已停止'; exit 0" INT
wait $BLUEPRINT_PID