# Kestra Blueprint Service Dockerfile
# 基于Eclipse Temurin JDK 21构建蓝图服务
FROM eclipse-temurin:21-jre-jammy

# 设置环境变量
ENV JAVA_OPTS="-Xmx512m -Xms256m" \
    MICRONAUT_ENVIRONMENTS="docker" \
    MICRONAUT_SERVER_PORT="8084" \
    DATASOURCES_DEFAULT_URL="jdbc:h2:file:/app/data/blueprint;DB_CLOSE_DELAY=-1"

# 创建应用目录和用户
WORKDIR /app
RUN groupadd -r blueprint && \
    useradd -r -g blueprint -d /app -s /bin/bash blueprint && \
    mkdir -p /app/data /app/logs && \
    chown -R blueprint:blueprint /app

# 安装必要的系统包
RUN apt-get update && \
    apt-get install -y --no-install-recommends \
        curl \
        ca-certificates \
        tzdata && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/* /var/tmp/* /tmp/*

# 复制构建好的JAR文件
COPY --chown=blueprint:blueprint build/libs/blueprint-*-all.jar /app/blueprint.jar

# 复制启动脚本
COPY --chown=blueprint:blueprint start-blueprint.sh /app/start-blueprint.sh
RUN chmod +x /app/start-blueprint.sh

# 切换到非root用户
USER blueprint

# 暴露端口
EXPOSE 8084

# 健康检查
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8084/health || exit 1

# 启动命令
ENTRYPOINT ["/app/start-blueprint.sh"]
