#!/bin/bash

# Kestra with Fluvio Queue 启动脚本
# 自动设置本地Fluvio集群并启动Kestra

set -e

echo "🚀 Kestra with Fluvio Queue 启动脚本"
echo "=================================="

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 检查Fluvio CLI是否安装
check_fluvio_cli() {
    echo -e "${BLUE}📋 检查Fluvio CLI...${NC}"
    if ! command -v fluvio &> /dev/null; then
        echo -e "${RED}❌ Fluvio CLI未安装${NC}"
        echo -e "${YELLOW}请运行以下命令安装Fluvio CLI:${NC}"
        echo "curl -fsS https://hub.fluvio.io/install/install.sh | bash"
        echo "或者使用Homebrew: brew install fluvio/tap/fluvio"
        exit 1
    fi
    echo -e "${GREEN}✅ Fluvio CLI已安装${NC}"
}

# 检查并启动Fluvio集群
start_fluvio_cluster() {
    echo -e "${BLUE}🔧 检查Fluvio集群状态...${NC}"
    
    # 检查集群是否已经运行
    if fluvio cluster status &> /dev/null; then
        echo -e "${GREEN}✅ Fluvio集群已运行${NC}"
        return 0
    fi
    
    echo -e "${YELLOW}⚡ 启动Fluvio集群...${NC}"
    if fluvio cluster start; then
        echo -e "${GREEN}✅ Fluvio集群启动成功${NC}"
        
        # 等待集群完全启动
        echo -e "${BLUE}⏳ 等待集群初始化...${NC}"
        sleep 5
        
        # 验证集群状态
        if fluvio cluster status; then
            echo -e "${GREEN}✅ 集群状态验证成功${NC}"
        else
            echo -e "${RED}❌ 集群状态验证失败${NC}"
            exit 1
        fi
    else
        echo -e "${RED}❌ Fluvio集群启动失败${NC}"
        echo -e "${YELLOW}尝试清理并重新启动...${NC}"
        fluvio cluster delete || true
        sleep 2
        if fluvio cluster start; then
            echo -e "${GREEN}✅ 重新启动成功${NC}"
        else
            echo -e "${RED}❌ 重新启动失败，请手动检查${NC}"
            exit 1
        fi
    fi
}

# 验证连接
verify_connection() {
    echo -e "${BLUE}🔍 验证Fluvio连接...${NC}"
    
    # 尝试列出主题
    if fluvio topic list &> /dev/null; then
        echo -e "${GREEN}✅ Fluvio连接正常${NC}"
    else
        echo -e "${RED}❌ Fluvio连接失败${NC}"
        exit 1
    fi
}

# 构建项目
build_project() {
    echo -e "${BLUE}🔨 构建Kestra项目...${NC}"
    
    # 构建Fluvio队列模块
    echo -e "${YELLOW}构建Fluvio队列模块...${NC}"
    if ./gradlew :queue-fluvio:build -x test; then
        echo -e "${GREEN}✅ Fluvio队列模块构建成功${NC}"
    else
        echo -e "${RED}❌ Fluvio队列模块构建失败${NC}"
        exit 1
    fi
    
    # 构建CLI模块
    echo -e "${YELLOW}构建CLI模块...${NC}"
    if ./gradlew :cli:build -x test; then
        echo -e "${GREEN}✅ CLI模块构建成功${NC}"
    else
        echo -e "${RED}❌ CLI模块构建失败${NC}"
        exit 1
    fi
}

# 启动Kestra
start_kestra() {
    echo -e "${BLUE}🚀 启动Kestra (Standalone模式 + Fluvio队列)...${NC}"
    echo -e "${YELLOW}配置信息:${NC}"
    echo "  - 队列类型: Fluvio"
    echo "  - 集群端点: localhost:9003"
    echo "  - Web界面: http://localhost:8080"
    echo ""
    echo -e "${YELLOW}按 Ctrl+C 停止服务${NC}"
    echo ""
    
    # 启动Kestra
    ./gradlew runStandalone
}

# 清理函数
cleanup() {
    echo -e "\n${YELLOW}🛑 正在停止服务...${NC}"
    # Kestra会被Gradle自动停止
    echo -e "${GREEN}✅ 服务已停止${NC}"
}

# 设置信号处理
trap cleanup SIGINT SIGTERM

# 主函数
main() {
    echo -e "${BLUE}开始启动流程...${NC}"
    echo ""
    
    check_fluvio_cli
    start_fluvio_cluster
    verify_connection
    build_project
    
    echo ""
    echo -e "${GREEN}🎉 准备工作完成！${NC}"
    echo ""
    
    start_kestra
}

# 帮助信息
show_help() {
    echo "Kestra with Fluvio Queue 启动脚本"
    echo ""
    echo "用法: $0 [选项]"
    echo ""
    echo "选项:"
    echo "  -h, --help     显示此帮助信息"
    echo "  --no-build     跳过构建步骤"
    echo "  --clean        清理并重新启动Fluvio集群"
    echo ""
    echo "示例:"
    echo "  $0              # 正常启动"
    echo "  $0 --no-build   # 跳过构建直接启动"
    echo "  $0 --clean      # 清理重启"
}

# 解析命令行参数
SKIP_BUILD=false
CLEAN_START=false

while [[ $# -gt 0 ]]; do
    case $1 in
        -h|--help)
            show_help
            exit 0
            ;;
        --no-build)
            SKIP_BUILD=true
            shift
            ;;
        --clean)
            CLEAN_START=true
            shift
            ;;
        *)
            echo -e "${RED}未知选项: $1${NC}"
            show_help
            exit 1
            ;;
    esac
done

# 清理启动
if [ "$CLEAN_START" = true ]; then
    echo -e "${YELLOW}🧹 清理Fluvio集群...${NC}"
    fluvio cluster delete || true
    sleep 2
fi

# 跳过构建
if [ "$SKIP_BUILD" = true ]; then
    build_project() {
        echo -e "${YELLOW}⏭️  跳过构建步骤${NC}"
    }
fi

# 运行主函数
main
