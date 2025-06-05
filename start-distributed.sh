#!/bin/bash

# DataFlare 分布式部署启动脚本
# 使用方法: ./start-distributed.sh [选项]

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 配置
COMPOSE_FILE="docker-compose-distributed.yml"
PROJECT_NAME="dataflare-distributed"

# 函数定义
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 检查依赖
check_dependencies() {
    log_info "检查依赖..."
    
    if ! command -v docker &> /dev/null; then
        log_error "Docker 未安装或不在 PATH 中"
        exit 1
    fi
    
    if ! command -v docker-compose &> /dev/null; then
        log_error "Docker Compose 未安装或不在 PATH 中"
        exit 1
    fi
    
    if [ ! -f "$COMPOSE_FILE" ]; then
        log_error "找不到 $COMPOSE_FILE 文件"
        exit 1
    fi
    
    log_success "依赖检查通过"
}

# 创建必要的目录
create_directories() {
    log_info "创建必要的目录..."
    
    mkdir -p ssl
    mkdir -p data/postgres
    mkdir -p data/dataflare-storage
    mkdir -p data/grafana
    
    # 设置权限
    chmod 755 ssl data
    
    log_success "目录创建完成"
}

# 启动基础服务
start_infrastructure() {
    log_info "启动基础设施服务..."
    
    docker-compose -f "$COMPOSE_FILE" -p "$PROJECT_NAME" up -d postgres redis
    
    log_info "等待数据库就绪..."
    sleep 30
    
    # 检查数据库健康状态
    local retries=0
    local max_retries=30
    
    while [ $retries -lt $max_retries ]; do
        if docker-compose -f "$COMPOSE_FILE" -p "$PROJECT_NAME" exec -T postgres pg_isready -U dataflare -d dataflare; then
            log_success "数据库已就绪"
            break
        fi
        
        retries=$((retries + 1))
        log_info "等待数据库就绪... ($retries/$max_retries)"
        sleep 2
    done
    
    if [ $retries -eq $max_retries ]; then
        log_error "数据库启动超时"
        exit 1
    fi
}

# 启动核心服务
start_core_services() {
    log_info "启动 DataFlare 核心服务..."

    docker-compose -f "$COMPOSE_FILE" -p "$PROJECT_NAME" up -d \
        dataflare-webserver \
        dataflare-executor \
        dataflare-scheduler \
        dataflare-indexer

    log_info "等待核心服务启动..."
    sleep 20
}

# 启动工作节点
start_workers() {
    log_info "启动 Worker 节点..."

    docker-compose -f "$COMPOSE_FILE" -p "$PROJECT_NAME" up -d \
        dataflare-worker-1 \
        dataflare-worker-2

    log_success "Worker 节点已启动"
}

# 启动监控服务
start_monitoring() {
    log_info "启动监控服务..."
    
    docker-compose -f "$COMPOSE_FILE" -p "$PROJECT_NAME" up -d \
        nginx \
        prometheus \
        grafana
    
    log_success "监控服务已启动"
}

# 检查服务状态
check_services() {
    log_info "检查服务状态..."
    
    docker-compose -f "$COMPOSE_FILE" -p "$PROJECT_NAME" ps
    
    echo ""
    log_info "健康检查..."
    
    # 检查 WebServer
    if curl -f -s http://localhost:8080/health > /dev/null; then
        log_success "DataFlare WebServer 运行正常"
    else
        log_warning "DataFlare WebServer 可能未就绪"
    fi
    
    # 检查 Prometheus
    if curl -f -s http://localhost:9090/-/healthy > /dev/null; then
        log_success "Prometheus 运行正常"
    else
        log_warning "Prometheus 可能未就绪"
    fi
    
    # 检查 Grafana
    if curl -f -s http://localhost:3000/api/health > /dev/null; then
        log_success "Grafana 运行正常"
    else
        log_warning "Grafana 可能未就绪"
    fi
}

# 显示访问信息
show_access_info() {
    echo ""
    log_success "=== DataFlare 分布式部署完成 ==="
    echo ""
    echo "访问地址："
    echo "  🌐 DataFlare UI: http://localhost:8080"
    echo "  📊 Prometheus:   http://localhost:9090"
    echo "  📈 Grafana:      http://localhost:3000 (admin/admin)"
    echo ""
    echo "管理命令："
    echo "  查看状态: docker-compose -f $COMPOSE_FILE -p $PROJECT_NAME ps"
    echo "  查看日志: docker-compose -f $COMPOSE_FILE -p $PROJECT_NAME logs -f [service]"
    echo "  停止服务: docker-compose -f $COMPOSE_FILE -p $PROJECT_NAME down"
    echo ""
}

# 停止所有服务
stop_services() {
    log_info "停止所有服务..."
    docker-compose -f "$COMPOSE_FILE" -p "$PROJECT_NAME" down
    log_success "所有服务已停止"
}

# 清理资源
cleanup() {
    log_info "清理资源..."
    docker-compose -f "$COMPOSE_FILE" -p "$PROJECT_NAME" down -v
    docker system prune -f
    log_success "资源清理完成"
}

# 显示帮助信息
show_help() {
    echo "DataFlare 分布式部署脚本"
    echo ""
    echo "使用方法:"
    echo "  $0 [选项]"
    echo ""
    echo "选项:"
    echo "  start     启动所有服务（默认）"
    echo "  stop      停止所有服务"
    echo "  restart   重启所有服务"
    echo "  status    查看服务状态"
    echo "  cleanup   清理所有资源（包括数据卷）"
    echo "  help      显示此帮助信息"
    echo ""
}

# 主函数
main() {
    local action="${1:-start}"
    
    case "$action" in
        "start")
            check_dependencies
            create_directories
            start_infrastructure
            start_core_services
            start_workers
            start_monitoring
            check_services
            show_access_info
            ;;
        "stop")
            stop_services
            ;;
        "restart")
            stop_services
            sleep 5
            main start
            ;;
        "status")
            check_services
            ;;
        "cleanup")
            cleanup
            ;;
        "help"|"-h"|"--help")
            show_help
            ;;
        *)
            log_error "未知选项: $action"
            show_help
            exit 1
            ;;
    esac
}

# 执行主函数
main "$@"
