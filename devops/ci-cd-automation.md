# DataFlare Enterprise DevOps Automation

## 🚀 CI/CD和运维自动化方案

### 🎯 自动化目标
- 实现端到端的自动化部署
- 建立可靠的质量门禁
- 实现基础设施即代码
- 提供自动化运维能力

### 🔄 CI/CD流水线设计

#### 1. 持续集成 (CI) 流程
```yaml
代码提交触发:
  1. 代码检出
  2. 依赖安装
  3. 单元测试
  4. 代码质量检查
  5. 安全扫描
  6. 构建打包
  7. 镜像构建
  8. 制品上传

质量门禁:
  - 单元测试覆盖率 > 80%
  - 代码质量评分 > A
  - 安全漏洞数量 = 0
  - 构建成功率 = 100%
```

#### 2. 持续部署 (CD) 流程
```yaml
部署流程:
  1. 环境准备
  2. 数据库迁移
  3. 配置更新
  4. 应用部署
  5. 健康检查
  6. 集成测试
  7. 性能测试
  8. 生产发布

部署策略:
  - 蓝绿部署
  - 金丝雀发布
  - 滚动更新
  - 自动回滚
```

### 🛠️ GitHub Actions工作流

#### 1. CI工作流配置
```yaml
# .github/workflows/ci.yml
name: DataFlare CI

on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main ]

jobs:
  test:
    runs-on: ubuntu-latest
    
    services:
      postgres:
        image: postgres:15
        env:
          POSTGRES_PASSWORD: postgres
        options: >-
          --health-cmd pg_isready
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5
      
      redis:
        image: redis:7
        options: >-
          --health-cmd "redis-cli ping"
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5

    steps:
    - uses: actions/checkout@v4
    
    - name: Set up JDK 17
      uses: actions/setup-java@v4
      with:
        java-version: '17'
        distribution: 'temurin'
    
    - name: Cache Gradle packages
      uses: actions/cache@v3
      with:
        path: |
          ~/.gradle/caches
          ~/.gradle/wrapper
        key: ${{ runner.os }}-gradle-${{ hashFiles('**/*.gradle*', '**/gradle-wrapper.properties') }}
        restore-keys: |
          ${{ runner.os }}-gradle-
    
    - name: Run tests
      run: ./gradlew test
      env:
        DATABASE_URL: jdbc:postgresql://localhost:5432/postgres
        DATABASE_USERNAME: postgres
        DATABASE_PASSWORD: postgres
        REDIS_URL: redis://localhost:6379
    
    - name: Generate test report
      uses: dorny/test-reporter@v1
      if: success() || failure()
      with:
        name: Test Results
        path: '**/build/test-results/test/TEST-*.xml'
        reporter: java-junit
    
    - name: Code coverage
      run: ./gradlew jacocoTestReport
    
    - name: Upload coverage to Codecov
      uses: codecov/codecov-action@v3
      with:
        file: ./build/reports/jacoco/test/jacocoTestReport.xml

  security-scan:
    runs-on: ubuntu-latest
    steps:
    - uses: actions/checkout@v4
    
    - name: Run Trivy vulnerability scanner
      uses: aquasecurity/trivy-action@master
      with:
        scan-type: 'fs'
        scan-ref: '.'
        format: 'sarif'
        output: 'trivy-results.sarif'
    
    - name: Upload Trivy scan results
      uses: github/codeql-action/upload-sarif@v2
      with:
        sarif_file: 'trivy-results.sarif'

  build:
    needs: [test, security-scan]
    runs-on: ubuntu-latest
    
    steps:
    - uses: actions/checkout@v4
    
    - name: Set up JDK 17
      uses: actions/setup-java@v4
      with:
        java-version: '17'
        distribution: 'temurin'
    
    - name: Build application
      run: ./gradlew build -x test
    
    - name: Build Docker image
      run: |
        docker build -t dataflare:${{ github.sha }} .
        docker tag dataflare:${{ github.sha }} dataflare:latest
    
    - name: Login to Container Registry
      uses: docker/login-action@v2
      with:
        registry: ghcr.io
        username: ${{ github.actor }}
        password: ${{ secrets.GITHUB_TOKEN }}
    
    - name: Push Docker image
      run: |
        docker tag dataflare:${{ github.sha }} ghcr.io/${{ github.repository }}:${{ github.sha }}
        docker tag dataflare:${{ github.sha }} ghcr.io/${{ github.repository }}:latest
        docker push ghcr.io/${{ github.repository }}:${{ github.sha }}
        docker push ghcr.io/${{ github.repository }}:latest
```

#### 2. CD工作流配置
```yaml
# .github/workflows/cd.yml
name: DataFlare CD

on:
  push:
    branches: [ main ]
    tags: [ 'v*' ]

jobs:
  deploy-staging:
    runs-on: ubuntu-latest
    environment: staging
    
    steps:
    - uses: actions/checkout@v4
    
    - name: Deploy to Staging
      uses: azure/k8s-deploy@v1
      with:
        manifests: |
          k8s/staging/deployment.yaml
          k8s/staging/service.yaml
          k8s/staging/ingress.yaml
        images: |
          ghcr.io/${{ github.repository }}:${{ github.sha }}
        kubectl-version: 'latest'
    
    - name: Run integration tests
      run: |
        npm install -g newman
        newman run tests/integration/dataflare-api.postman_collection.json \
          --environment tests/integration/staging.postman_environment.json \
          --reporters cli,junit --reporter-junit-export results.xml
    
    - name: Performance test
      run: |
        docker run --rm -v $(pwd)/tests/performance:/tests \
          grafana/k6 run /tests/load-test.js

  deploy-production:
    needs: deploy-staging
    runs-on: ubuntu-latest
    environment: production
    if: startsWith(github.ref, 'refs/tags/v')
    
    steps:
    - uses: actions/checkout@v4
    
    - name: Blue-Green Deployment
      uses: azure/k8s-deploy@v1
      with:
        strategy: blue-green
        manifests: |
          k8s/production/deployment.yaml
          k8s/production/service.yaml
          k8s/production/ingress.yaml
        images: |
          ghcr.io/${{ github.repository }}:${{ github.sha }}
        kubectl-version: 'latest'
    
    - name: Health check
      run: |
        for i in {1..30}; do
          if curl -f https://api.dataflare.com/health; then
            echo "Health check passed"
            exit 0
          fi
          sleep 10
        done
        echo "Health check failed"
        exit 1
    
    - name: Notify deployment
      uses: 8398a7/action-slack@v3
      with:
        status: ${{ job.status }}
        channel: '#deployments'
        webhook_url: ${{ secrets.SLACK_WEBHOOK }}
```

### 🏗️ 基础设施即代码 (IaC)

#### 1. Terraform配置
```hcl
# infrastructure/main.tf
terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
  
  backend "s3" {
    bucket = "dataflare-terraform-state"
    key    = "infrastructure/terraform.tfstate"
    region = "us-west-2"
  }
}

provider "aws" {
  region = var.aws_region
}

# VPC配置
module "vpc" {
  source = "terraform-aws-modules/vpc/aws"
  
  name = "dataflare-vpc"
  cidr = "10.0.0.0/16"
  
  azs             = ["us-west-2a", "us-west-2b", "us-west-2c"]
  private_subnets = ["10.0.1.0/24", "10.0.2.0/24", "10.0.3.0/24"]
  public_subnets  = ["10.0.101.0/24", "10.0.102.0/24", "10.0.103.0/24"]
  
  enable_nat_gateway = true
  enable_vpn_gateway = true
  
  tags = {
    Environment = var.environment
    Project     = "DataFlare"
  }
}

# EKS集群
module "eks" {
  source = "terraform-aws-modules/eks/aws"
  
  cluster_name    = "dataflare-${var.environment}"
  cluster_version = "1.28"
  
  vpc_id     = module.vpc.vpc_id
  subnet_ids = module.vpc.private_subnets
  
  node_groups = {
    main = {
      desired_capacity = 3
      max_capacity     = 10
      min_capacity     = 1
      
      instance_types = ["t3.large"]
      
      k8s_labels = {
        Environment = var.environment
        Application = "dataflare"
      }
    }
  }
}

# RDS数据库
resource "aws_db_instance" "postgres" {
  identifier = "dataflare-${var.environment}"
  
  engine         = "postgres"
  engine_version = "15.4"
  instance_class = "db.t3.medium"
  
  allocated_storage     = 100
  max_allocated_storage = 1000
  storage_encrypted     = true
  
  db_name  = "dataflare"
  username = var.db_username
  password = var.db_password
  
  vpc_security_group_ids = [aws_security_group.rds.id]
  db_subnet_group_name   = aws_db_subnet_group.main.name
  
  backup_retention_period = 7
  backup_window          = "03:00-04:00"
  maintenance_window     = "sun:04:00-sun:05:00"
  
  skip_final_snapshot = var.environment != "production"
  
  tags = {
    Environment = var.environment
    Project     = "DataFlare"
  }
}

# ElastiCache Redis
resource "aws_elasticache_subnet_group" "main" {
  name       = "dataflare-${var.environment}"
  subnet_ids = module.vpc.private_subnets
}

resource "aws_elasticache_replication_group" "redis" {
  replication_group_id       = "dataflare-${var.environment}"
  description                = "DataFlare Redis cluster"
  
  node_type                  = "cache.t3.micro"
  port                       = 6379
  parameter_group_name       = "default.redis7"
  
  num_cache_clusters         = 2
  automatic_failover_enabled = true
  multi_az_enabled          = true
  
  subnet_group_name = aws_elasticache_subnet_group.main.name
  security_group_ids = [aws_security_group.redis.id]
  
  at_rest_encryption_enabled = true
  transit_encryption_enabled = true
  
  tags = {
    Environment = var.environment
    Project     = "DataFlare"
  }
}
```

#### 2. Kubernetes部署配置
```yaml
# k8s/production/deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: dataflare
  namespace: dataflare-production
  labels:
    app: dataflare
    version: v1
spec:
  replicas: 3
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxSurge: 1
      maxUnavailable: 0
  selector:
    matchLabels:
      app: dataflare
  template:
    metadata:
      labels:
        app: dataflare
        version: v1
    spec:
      serviceAccountName: dataflare
      securityContext:
        runAsNonRoot: true
        runAsUser: 1000
        fsGroup: 1000
      containers:
      - name: dataflare
        image: ghcr.io/company/dataflare:latest
        imagePullPolicy: Always
        ports:
        - containerPort: 8080
          name: http
        - containerPort: 9090
          name: metrics
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "production"
        - name: DATABASE_URL
          valueFrom:
            secretKeyRef:
              name: dataflare-secrets
              key: database-url
        - name: REDIS_URL
          valueFrom:
            secretKeyRef:
              name: dataflare-secrets
              key: redis-url
        - name: JWT_SECRET
          valueFrom:
            secretKeyRef:
              name: dataflare-secrets
              key: jwt-secret
        resources:
          requests:
            memory: "2Gi"
            cpu: "1000m"
          limits:
            memory: "4Gi"
            cpu: "2000m"
        livenessProbe:
          httpGet:
            path: /health
            port: 8080
          initialDelaySeconds: 60
          periodSeconds: 30
          timeoutSeconds: 10
          failureThreshold: 3
        readinessProbe:
          httpGet:
            path: /health/ready
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 10
          timeoutSeconds: 5
          failureThreshold: 3
        volumeMounts:
        - name: config
          mountPath: /app/config
          readOnly: true
        - name: logs
          mountPath: /app/logs
      volumes:
      - name: config
        configMap:
          name: dataflare-config
      - name: logs
        emptyDir: {}
      imagePullSecrets:
      - name: ghcr-secret
```

### 🔧 自动化运维脚本

#### 1. 部署脚本
```bash
#!/bin/bash
# scripts/deploy.sh

set -euo pipefail

ENVIRONMENT=${1:-staging}
VERSION=${2:-latest}
NAMESPACE="dataflare-${ENVIRONMENT}"

echo "🚀 Deploying DataFlare ${VERSION} to ${ENVIRONMENT}"

# 验证环境
if [[ ! "${ENVIRONMENT}" =~ ^(staging|production)$ ]]; then
    echo "❌ Invalid environment: ${ENVIRONMENT}"
    exit 1
fi

# 检查kubectl连接
if ! kubectl cluster-info &> /dev/null; then
    echo "❌ Cannot connect to Kubernetes cluster"
    exit 1
fi

# 创建命名空间
kubectl create namespace "${NAMESPACE}" --dry-run=client -o yaml | kubectl apply -f -

# 应用配置
echo "📝 Applying configuration..."
kubectl apply -f "k8s/${ENVIRONMENT}/" -n "${NAMESPACE}"

# 更新镜像
echo "🔄 Updating image to ${VERSION}..."
kubectl set image deployment/dataflare dataflare="ghcr.io/company/dataflare:${VERSION}" -n "${NAMESPACE}"

# 等待部署完成
echo "⏳ Waiting for deployment to complete..."
kubectl rollout status deployment/dataflare -n "${NAMESPACE}" --timeout=600s

# 健康检查
echo "🏥 Performing health check..."
for i in {1..30}; do
    if kubectl exec -n "${NAMESPACE}" deployment/dataflare -- curl -f http://localhost:8080/health; then
        echo "✅ Health check passed"
        break
    fi
    if [ $i -eq 30 ]; then
        echo "❌ Health check failed"
        exit 1
    fi
    sleep 10
done

echo "🎉 Deployment completed successfully!"
```

#### 2. 备份脚本
```bash
#!/bin/bash
# scripts/backup.sh

set -euo pipefail

ENVIRONMENT=${1:-production}
BACKUP_TYPE=${2:-full}
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
BACKUP_DIR="/backups/${ENVIRONMENT}/${TIMESTAMP}"

echo "💾 Starting ${BACKUP_TYPE} backup for ${ENVIRONMENT}"

# 创建备份目录
mkdir -p "${BACKUP_DIR}"

# 数据库备份
echo "📊 Backing up database..."
kubectl exec -n "dataflare-${ENVIRONMENT}" deployment/postgres -- \
    pg_dump -U postgres dataflare | gzip > "${BACKUP_DIR}/database.sql.gz"

# 配置备份
echo "⚙️ Backing up configuration..."
kubectl get configmaps,secrets -n "dataflare-${ENVIRONMENT}" -o yaml > "${BACKUP_DIR}/config.yaml"

# 应用状态备份
echo "📋 Backing up application state..."
kubectl get all -n "dataflare-${ENVIRONMENT}" -o yaml > "${BACKUP_DIR}/state.yaml"

# 上传到S3
echo "☁️ Uploading to S3..."
aws s3 sync "${BACKUP_DIR}" "s3://dataflare-backups/${ENVIRONMENT}/${TIMESTAMP}/"

# 清理本地文件
rm -rf "${BACKUP_DIR}"

echo "✅ Backup completed: s3://dataflare-backups/${ENVIRONMENT}/${TIMESTAMP}/"
```

### 📊 监控和告警配置

#### 1. Prometheus配置
```yaml
# monitoring/prometheus.yml
global:
  scrape_interval: 15s
  evaluation_interval: 15s

rule_files:
  - "alert_rules.yml"

alerting:
  alertmanagers:
    - static_configs:
        - targets:
          - alertmanager:9093

scrape_configs:
  - job_name: 'dataflare'
    static_configs:
      - targets: ['dataflare:9090']
    metrics_path: /metrics
    scrape_interval: 30s
    
  - job_name: 'kubernetes-pods'
    kubernetes_sd_configs:
      - role: pod
    relabel_configs:
      - source_labels: [__meta_kubernetes_pod_annotation_prometheus_io_scrape]
        action: keep
        regex: true
```

#### 2. 告警规则
```yaml
# monitoring/alert_rules.yml
groups:
  - name: dataflare.rules
    rules:
      - alert: DataFlareDown
        expr: up{job="dataflare"} == 0
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "DataFlare instance is down"
          description: "DataFlare instance {{ $labels.instance }} has been down for more than 1 minute."
      
      - alert: HighErrorRate
        expr: rate(http_requests_total{status=~"5.."}[5m]) > 0.1
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "High error rate detected"
          description: "Error rate is {{ $value }} errors per second."
      
      - alert: HighMemoryUsage
        expr: (node_memory_MemTotal_bytes - node_memory_MemAvailable_bytes) / node_memory_MemTotal_bytes > 0.9
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "High memory usage"
          description: "Memory usage is above 90%."
```

这个DevOps自动化方案将帮助您：
1. 实现完全自动化的CI/CD流水线
2. 建立可靠的基础设施即代码
3. 提供全面的监控和告警
4. 简化日常运维操作

您希望我详细说明哪个方面的自动化配置？
