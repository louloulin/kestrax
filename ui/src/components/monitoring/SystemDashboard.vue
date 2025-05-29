<template>
    <div class="system-dashboard">
        <PermissionGuard permission="MONITORING_READ">
            <!-- Header -->
            <div class="dashboard-header">
                <h2 class="dashboard-title">
                    <el-icon><Monitor /></el-icon>
                    {{ $t('monitoring.system.title') }}
                </h2>
                <div class="dashboard-actions">
                    <el-button
                        :icon="Refresh"
                        @click="refreshDashboard"
                        :loading="loading"
                    >
                        {{ $t('common.refresh') }}
                    </el-button>
                    <el-button
                        :icon="Setting"
                        @click="openSettings"
                    >
                        {{ $t('monitoring.system.settings') }}
                    </el-button>
                </div>
            </div>

            <!-- System Overview -->
            <el-row :gutter="16" class="overview-cards">
                <el-col :span="6">
                    <el-card class="metric-card cpu" shadow="hover">
                        <div class="metric-content">
                            <div class="metric-icon">
                                <el-icon><Cpu /></el-icon>
                            </div>
                            <div class="metric-info">
                                <div class="metric-value">
                                    {{ systemStats.cpu.usage }}%
                                </div>
                                <div class="metric-label">
                                    {{ $t('monitoring.system.cpu') }}
                                </div>
                                <div class="metric-detail">
                                    {{ systemStats.cpu.cores }} {{ $t('monitoring.system.cores') }}
                                </div>
                            </div>
                        </div>
                        <div class="metric-progress">
                            <el-progress
                                :percentage="systemStats.cpu.usage"
                                :color="getProgressColor(systemStats.cpu.usage)"
                                :show-text="false"
                                :stroke-width="4"
                            />
                        </div>
                    </el-card>
                </el-col>

                <el-col :span="6">
                    <el-card class="metric-card memory" shadow="hover">
                        <div class="metric-content">
                            <div class="metric-icon">
                                <el-icon><MemoryCard /></el-icon>
                            </div>
                            <div class="metric-info">
                                <div class="metric-value">
                                    {{ systemStats.memory.usage }}%
                                </div>
                                <div class="metric-label">
                                    {{ $t('monitoring.system.memory') }}
                                </div>
                                <div class="metric-detail">
                                    {{ formatBytes(systemStats.memory.used) }} / {{ formatBytes(systemStats.memory.total) }}
                                </div>
                            </div>
                        </div>
                        <div class="metric-progress">
                            <el-progress
                                :percentage="systemStats.memory.usage"
                                :color="getProgressColor(systemStats.memory.usage)"
                                :show-text="false"
                                :stroke-width="4"
                            />
                        </div>
                    </el-card>
                </el-col>

                <el-col :span="6">
                    <el-card class="metric-card disk" shadow="hover">
                        <div class="metric-content">
                            <div class="metric-icon">
                                <el-icon><HardDisk /></el-icon>
                            </div>
                            <div class="metric-info">
                                <div class="metric-value">
                                    {{ systemStats.disk.usage }}%
                                </div>
                                <div class="metric-label">
                                    {{ $t('monitoring.system.disk') }}
                                </div>
                                <div class="metric-detail">
                                    {{ formatBytes(systemStats.disk.used) }} / {{ formatBytes(systemStats.disk.total) }}
                                </div>
                            </div>
                        </div>
                        <div class="metric-progress">
                            <el-progress
                                :percentage="systemStats.disk.usage"
                                :color="getProgressColor(systemStats.disk.usage)"
                                :show-text="false"
                                :stroke-width="4"
                            />
                        </div>
                    </el-card>
                </el-col>

                <el-col :span="6">
                    <el-card class="metric-card network" shadow="hover">
                        <div class="metric-content">
                            <div class="metric-icon">
                                <el-icon><Connection /></el-icon>
                            </div>
                            <div class="metric-info">
                                <div class="metric-value">
                                    {{ formatBytes(systemStats.network.bytesIn + systemStats.network.bytesOut) }}
                                </div>
                                <div class="metric-label">
                                    {{ $t('monitoring.system.network') }}
                                </div>
                                <div class="metric-detail">
                                    ↓{{ formatBytes(systemStats.network.bytesIn) }} ↑{{ formatBytes(systemStats.network.bytesOut) }}
                                </div>
                            </div>
                        </div>
                    </el-card>
                </el-col>
            </el-row>

            <!-- Service Status -->
            <el-card class="service-status" shadow="never" v-loading="loading">
                <template #header>
                    <div class="card-header">
                        <el-icon><Service /></el-icon>
                        <span>{{ $t('monitoring.system.services') }}</span>
                    </div>
                </template>

                <el-table :data="serviceStatus" style="width: 100%">
                    <el-table-column prop="name" :label="$t('monitoring.system.serviceName')" min-width="150">
                        <template #default="{row}">
                            <div class="service-name">
                                <el-icon class="service-icon" :class="getServiceIconClass(row.status)">
                                    <component :is="getServiceIcon(row.status)" />
                                </el-icon>
                                {{ row.name }}
                            </div>
                        </template>
                    </el-table-column>
                    <el-table-column prop="status" :label="$t('monitoring.system.status')" width="120">
                        <template #default="{row}">
                            <el-tag :type="getStatusType(row.status)" size="small">
                                {{ $t(`monitoring.system.status.${row.status}`) }}
                            </el-tag>
                        </template>
                    </el-table-column>
                    <el-table-column prop="uptime" :label="$t('monitoring.system.uptime')" width="120">
                        <template #default="{row}">
                            {{ formatUptime(row.uptime) }}
                        </template>
                    </el-table-column>
                    <el-table-column prop="version" :label="$t('monitoring.system.version')" width="150" />
                    <el-table-column prop="instances" :label="$t('monitoring.system.instances')" width="120">
                        <template #default="{row}">
                            <span :class="{'text-danger': row.healthyInstances < row.instances}">
                                {{ row.healthyInstances }}/{{ row.instances }}
                            </span>
                        </template>
                    </el-table-column>
                    <el-table-column :label="$t('common.actions')" width="120">
                        <template #default="{row}">
                            <el-button
                                size="small"
                                type="primary"
                                link
                                @click="viewServiceDetails(row)"
                            >
                                {{ $t('common.details') }}
                            </el-button>
                        </template>
                    </el-table-column>
                </el-table>
            </el-card>

            <!-- Recent Alerts -->
            <el-card class="recent-alerts" shadow="never" v-loading="loading">
                <template #header>
                    <div class="card-header">
                        <el-icon><Warning /></el-icon>
                        <span>{{ $t('monitoring.system.recentAlerts') }}</span>
                        <el-button
                            type="primary"
                            link
                            size="small"
                            @click="viewAllAlerts"
                        >
                            {{ $t('monitoring.system.viewAll') }}
                        </el-button>
                    </div>
                </template>

                <div v-if="recentAlerts.length === 0" class="no-alerts">
                    <el-empty
                        :description="$t('monitoring.system.noAlerts')"
                        :image-size="80"
                    />
                </div>

                <div v-else class="alerts-list">
                    <div
                        v-for="alert in recentAlerts"
                        :key="alert.id"
                        class="alert-item"
                        :class="`alert-${alert.severity}`"
                    >
                        <div class="alert-icon">
                            <el-icon><component :is="getAlertIcon(alert.severity)" /></el-icon>
                        </div>
                        <div class="alert-content">
                            <div class="alert-title">
                                {{ alert.title }}
                            </div>
                            <div class="alert-description">
                                {{ alert.description }}
                            </div>
                            <div class="alert-meta">
                                <span class="alert-service">{{ alert.service }}</span>
                                <span class="alert-time">{{ formatRelativeTime(alert.timestamp) }}</span>
                            </div>
                        </div>
                        <div class="alert-status">
                            <el-tag
                                :type="alert.status === 'active' ? 'danger' : 'success'"
                                size="small"
                            >
                                {{ $t(`monitoring.system.alertStatus.${alert.status}`) }}
                            </el-tag>
                        </div>
                    </div>
                </div>
            </el-card>

            <!-- Fallback for no permission -->
            <template #fallback>
                <AccessDeniedMessage permission="MONITORING_READ" />
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
        Refresh,
        Setting,
        Cpu,
        MemoryCard,
        HardDisk,
        Connection,
        Service,
        Warning,

    } from "@element-plus/icons-vue"
    import {useMonitoringStore} from "@/stores/monitoring"
    import {usePermissionStore} from "@/stores/permission"
    import PermissionGuard from "@/components/rbac/PermissionGuard.vue"
    import AccessDeniedMessage from "@/components/rbac/AccessDeniedMessage.vue"
    import {formatBytes, formatUptime, formatRelativeTime} from "@/utils/format"

    const {t} = useI18n()
    const router = useRouter()
    const monitoringStore = useMonitoringStore()
    const _permissionStore = usePermissionStore()

    // State
    const loading = ref(false)
    const refreshInterval = ref<number | null>(null)

    // Data
    const systemStats = ref({
        cpu: {usage: 0, cores: 0, load: []},
        memory: {used: 0, total: 0, usage: 0},
        disk: {used: 0, total: 0, usage: 0},
        network: {bytesIn: 0, bytesOut: 0, packetsIn: 0, packetsOut: 0}
    })

    const serviceStatus = ref([])
    const recentAlerts = ref([])

    // Methods
    const loadSystemStats = async () => {
        try {
            systemStats.value = await monitoringStore.fetchSystemStats()
        } catch (error) {
            console.error("Failed to load system stats:", error)
        }
    }

    const loadServiceStatus = async () => {
        try {
            serviceStatus.value = await monitoringStore.fetchServiceStatus()
        } catch (error) {
            console.error("Failed to load service status:", error)
        }
    }

    const loadRecentAlerts = async () => {
        try {
            recentAlerts.value = await monitoringStore.fetchRecentAlerts()
        } catch (error) {
            console.error("Failed to load recent alerts:", error)
        }
    }

    const refreshDashboard = async () => {
        loading.value = true
        try {
            await Promise.all([
                loadSystemStats(),
                loadServiceStatus(),
                loadRecentAlerts()
            ])
        } catch {
            ElMessage.error(t("monitoring.system.loadError"))
        } finally {
            loading.value = false
        }
    }

    const getProgressColor = (percentage: number) => {
        if (percentage < 60) return "#67c23a"
        if (percentage < 80) return "#e6a23c"
        return "#f56c6c"
    }

    const getStatusType = (status: string) => {
        switch (status) {
        case "healthy": return "success"
        case "warning": return "warning"
        case "error": return "danger"
        default: return "info"
        }
    }

    const getServiceIcon = (status: string) => {
        switch (status) {
        case "healthy": return "CircleCheckFilled"
        case "warning": return "WarningFilled"
        case "error": return "CircleCloseFilled"
        default: return "CircleCheckFilled"
        }
    }

    const getServiceIconClass = (status: string) => {
        return `status-${status}`
    }

    const getAlertIcon = (severity: string) => {
        switch (severity) {
        case "warning": return "WarningFilled"
        case "error": return "CircleCloseFilled"
        default: return "WarningFilled"
        }
    }

    const viewServiceDetails = (service: any) => {
        // Navigate to service details
        router.push(`/monitoring/service/${service.name}`)
    }

    const viewAllAlerts = () => {
        router.push("/monitoring/alerts")
    }

    const openSettings = () => {
        router.push("/monitoring/settings")
    }

    const startAutoRefresh = () => {
        refreshInterval.value = setInterval(() => {
            refreshDashboard()
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
        refreshDashboard()
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

.overview-cards {
  margin-bottom: 1.5rem;
}

.metric-card {
  height: 120px;
  transition: all 0.3s ease;
}

.metric-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 25px rgba(0, 0, 0, 0.15);
}

.metric-content {
  display: flex;
  align-items: center;
  gap: 1rem;
  margin-bottom: 0.5rem;
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

.metric-card.cpu .metric-icon {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.metric-card.memory .metric-icon {
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
}

.metric-card.disk .metric-icon {
  background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
}

.metric-card.network .metric-icon {
  background: linear-gradient(135deg, #43e97b 0%, #38f9d7 100%);
}

.metric-info {
  flex: 1;
}

.metric-value {
  font-size: 1.8rem;
  font-weight: 600;
  color: #303133;
  margin-bottom: 0.25rem;
}

.metric-label {
  font-size: 0.9rem;
  color: #606266;
  margin-bottom: 0.25rem;
}

.metric-detail {
  font-size: 0.8rem;
  color: #909399;
}

.metric-progress {
  margin-top: 0.5rem;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-weight: 600;
  color: #303133;
}

.service-name {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.service-icon.status-healthy {
  color: #67c23a;
}

.service-icon.status-warning {
  color: #e6a23c;
}

.service-icon.status-error {
  color: #f56c6c;
}

.text-danger {
  color: #f56c6c;
}

.no-alerts {
  text-align: center;
  padding: 2rem;
}

.alerts-list {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.alert-item {
  display: flex;
  align-items: center;
  gap: 1rem;
  padding: 1rem;
  border-radius: 8px;
  border-left: 4px solid;
}

.alert-item.alert-warning {
  background: #fdf6ec;
  border-left-color: #e6a23c;
}

.alert-item.alert-error {
  background: #fef0f0;
  border-left-color: #f56c6c;
}

.alert-item.alert-info {
  background: #f4f4f5;
  border-left-color: #909399;
}

.alert-icon {
  font-size: 1.2rem;
}

.alert-item.alert-warning .alert-icon {
  color: #e6a23c;
}

.alert-item.alert-error .alert-icon {
  color: #f56c6c;
}

.alert-content {
  flex: 1;
}

.alert-title {
  font-weight: 600;
  color: #303133;
  margin-bottom: 0.25rem;
}

.alert-description {
  color: #606266;
  font-size: 0.9rem;
  margin-bottom: 0.5rem;
}

.alert-meta {
  display: flex;
  gap: 1rem;
  font-size: 0.8rem;
  color: #909399;
}
</style>
