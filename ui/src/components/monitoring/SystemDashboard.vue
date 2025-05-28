<template>
    <div class="system-dashboard">
        <PermissionGuard permission="SYSTEM_MONITOR_READ">
            <!-- Header -->
            <div class="dashboard-header">
                <h2 class="dashboard-title">
                    <el-icon><Monitor /></el-icon>
                    {{ $t('monitoring.dashboard.title') }}
                </h2>
                <div class="dashboard-actions">
                    <el-button
                        :icon="Setting"
                        @click="showSettings = true"
                    >
                        {{ $t('monitoring.dashboard.settings') }}
                    </el-button>
                    <el-button
                        :icon="Refresh"
                        @click="refreshAll"
                        :loading="loading"
                    >
                        {{ $t('common.refresh') }}
                    </el-button>
                </div>
            </div>

            <!-- System Overview -->
            <el-card class="system-overview" shadow="never">
                <template #header>
                    <div class="overview-header">
                        <el-icon><DataAnalysis /></el-icon>
                        <span>{{ $t('monitoring.dashboard.systemOverview') }}</span>
                        <div class="header-actions">
                            <el-tag
                                :type="systemStatus.overall === 'healthy' ? 'success' : 'danger'"
                                size="large"
                            >
                                {{ $t(`monitoring.status.${systemStatus.overall}`) }}
                            </el-tag>
                        </div>
                    </div>
                </template>
                
                <el-row :gutter="16">
                    <el-col :span="6">
                        <div class="metric-card">
                            <div class="metric-icon cpu">
                                <el-icon><Cpu /></el-icon>
                            </div>
                            <div class="metric-content">
                                <div class="metric-value">{{ systemMetrics.cpuUsage }}%</div>
                                <div class="metric-label">{{ $t('monitoring.metrics.cpuUsage') }}</div>
                                <el-progress
                                    :percentage="systemMetrics.cpuUsage"
                                    :color="getProgressColor(systemMetrics.cpuUsage)"
                                    :show-text="false"
                                    :stroke-width="4"
                                />
                            </div>
                        </div>
                    </el-col>
                    <el-col :span="6">
                        <div class="metric-card">
                            <div class="metric-icon memory">
                                <el-icon><MemoryCard /></el-icon>
                            </div>
                            <div class="metric-content">
                                <div class="metric-value">{{ systemMetrics.memoryUsage }}%</div>
                                <div class="metric-label">{{ $t('monitoring.metrics.memoryUsage') }}</div>
                                <el-progress
                                    :percentage="systemMetrics.memoryUsage"
                                    :color="getProgressColor(systemMetrics.memoryUsage)"
                                    :show-text="false"
                                    :stroke-width="4"
                                />
                            </div>
                        </div>
                    </el-col>
                    <el-col :span="6">
                        <div class="metric-card">
                            <div class="metric-icon disk">
                                <el-icon><HardDrive /></el-icon>
                            </div>
                            <div class="metric-content">
                                <div class="metric-value">{{ systemMetrics.diskUsage }}%</div>
                                <div class="metric-label">{{ $t('monitoring.metrics.diskUsage') }}</div>
                                <el-progress
                                    :percentage="systemMetrics.diskUsage"
                                    :color="getProgressColor(systemMetrics.diskUsage)"
                                    :show-text="false"
                                    :stroke-width="4"
                                />
                            </div>
                        </div>
                    </el-col>
                    <el-col :span="6">
                        <div class="metric-card">
                            <div class="metric-icon network">
                                <el-icon><Connection /></el-icon>
                            </div>
                            <div class="metric-content">
                                <div class="metric-value">{{ formatBytes(systemMetrics.networkIO) }}/s</div>
                                <div class="metric-label">{{ $t('monitoring.metrics.networkIO') }}</div>
                                <div class="metric-trend">
                                    <el-icon :class="systemMetrics.networkTrend === 'up' ? 'trend-up' : 'trend-down'">
                                        <component :is="systemMetrics.networkTrend === 'up' ? 'ArrowUp' : 'ArrowDown'" />
                                    </el-icon>
                                </div>
                            </div>
                        </div>
                    </el-col>
                </el-row>
            </el-card>

            <!-- Fallback for no permission -->
            <template #fallback>
                <AccessDeniedMessage permission="SYSTEM_MONITOR_READ" />
            </template>
        </PermissionGuard>
    </div>
</template>

<script setup lang="ts">
    import {ref, onMounted, onUnmounted} from "vue"
    import {useI18n} from "vue-i18n"
    import {useRouter} from "vue-router"
    import {ElMessage} from "element-plus"
    import {
        Monitor,
        Setting,
        Refresh,
        DataAnalysis,
        Cpu,
        MemoryCard,
        HardDrive,
        Connection,
        Service,
        Warning,
        TrendCharts,
        ArrowUp,
        ArrowDown
    } from "@element-plus/icons-vue"
    import {useMonitoringStore} from "@/stores/monitoring"
    import PermissionGuard from "@/components/rbac/PermissionGuard.vue"
    import AccessDeniedMessage from "@/components/rbac/AccessDeniedMessage.vue"
    import {formatBytes, formatUptime, formatRelativeTime} from "@/utils/format"
    import type {SystemMetrics, Service, Alert} from "@/types/monitoring"

    const {t} = useI18n()
    const router = useRouter()
    const monitoringStore = useMonitoringStore()

    // State
    const loading = ref(false)
    const showSettings = ref(false)
    const refreshInterval = ref<NodeJS.Timeout | null>(null)

    // Data
    const systemStatus = ref({
        overall: 'healthy'
    })

    const systemMetrics = ref<SystemMetrics>({
        cpuUsage: 0,
        memoryUsage: 0,
        diskUsage: 0,
        networkIO: 0,
        networkTrend: 'up'
    })

    // Methods
    const loadSystemMetrics = async () => {
        try {
            systemMetrics.value = await monitoringStore.fetchSystemMetrics()
            systemStatus.value = await monitoringStore.fetchSystemStatus()
        } catch (error) {
            console.error('Failed to load system metrics:', error)
        }
    }

    const refreshAll = async () => {
        loading.value = true
        try {
            await loadSystemMetrics()
        } catch (error) {
            ElMessage.error(t('monitoring.dashboard.loadError'))
        } finally {
            loading.value = false
        }
    }

    const getProgressColor = (percentage: number) => {
        if (percentage < 60) return '#67c23a'
        if (percentage < 80) return '#e6a23c'
        return '#f56c6c'
    }

    const startAutoRefresh = () => {
        refreshInterval.value = setInterval(() => {
            refreshAll()
        }, 30000) // Refresh every 30 seconds
    }

    const stopAutoRefresh = () => {
        if (refreshInterval.value) {
            clearInterval(refreshInterval.value)
            refreshInterval.value = null
        }
    }

    // Lifecycle
    onMounted(() => {
        refreshAll()
        startAutoRefresh()
    })

    onUnmounted(() => {
        stopAutoRefresh()
    })
</script>

<style scoped>
.system-dashboard {
  padding: 1.5rem;
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.dashboard-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-bottom: 1rem;
  border-bottom: 1px solid #e4e7ed;
}

.dashboard-title {
  margin: 0;
  color: #303133;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.dashboard-actions {
  display: flex;
  gap: 0.5rem;
}

.overview-header {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-weight: 600;
  color: #303133;
}

.header-actions {
  margin-left: auto;
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.metric-card {
  display: flex;
  align-items: center;
  gap: 1rem;
  padding: 1.5rem;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  background: white;
  transition: all 0.3s ease;
}

.metric-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  border-color: #409eff;
}

.metric-icon {
  width: 48px;
  height: 48px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.5rem;
  color: white;
}

.metric-icon.cpu {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.metric-icon.memory {
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
}

.metric-icon.disk {
  background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
}

.metric-icon.network {
  background: linear-gradient(135deg, #43e97b 0%, #38f9d7 100%);
}

.metric-content {
  flex: 1;
}

.metric-value {
  font-size: 1.5rem;
  font-weight: 600;
  color: #303133;
  margin-bottom: 0.25rem;
}

.metric-label {
  font-size: 0.9rem;
  color: #606266;
  margin-bottom: 0.5rem;
}

.metric-trend {
  margin-top: 0.5rem;
}

.trend-up {
  color: #67c23a;
}

.trend-down {
  color: #f56c6c;
}
</style>
