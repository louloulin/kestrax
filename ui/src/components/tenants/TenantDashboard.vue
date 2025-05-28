<template>
    <div class="tenant-dashboard">
        <PermissionGuard permission="TENANT_READ">
            <!-- Header -->
            <div class="dashboard-header">
                <div class="tenant-info">
                    <el-avatar :size="48" :src="currentTenant?.logo">
                        {{ currentTenant?.name?.[0] || 'T' }}
                    </el-avatar>
                    <div class="tenant-details">
                        <h2 class="tenant-name">{{ currentTenant?.name || $t('tenants.dashboard.noTenant') }}</h2>
                        <div class="tenant-meta">
                            <el-tag
                                :type="currentTenant?.enabled ? 'success' : 'danger'"
                                size="small"
                            >
                                {{ currentTenant?.enabled ? $t('common.enabled') : $t('common.disabled') }}
                            </el-tag>
                            <el-tag
                                :type="getPlanType(currentTenant?.subscriptionPlan)"
                                size="small"
                            >
                                {{ $t(`tenants.plans.${currentTenant?.subscriptionPlan}`) }}
                            </el-tag>
                        </div>
                    </div>
                </div>
                <div class="dashboard-actions">
                    <el-button
                        :icon="Setting"
                        @click="openTenantSettings"
                        v-if="hasPermission('TENANT_UPDATE')"
                    >
                        {{ $t('tenants.dashboard.settings') }}
                    </el-button>
                    <el-button
                        :icon="Refresh"
                        @click="refreshDashboard"
                        :loading="loading"
                    >
                        {{ $t('common.refresh') }}
                    </el-button>
                </div>
            </div>

            <!-- Quick Stats -->
            <el-card class="quick-stats" shadow="never" v-loading="loading">
                <template #header>
                    <div class="stats-header">
                        <el-icon><DataAnalysis /></el-icon>
                        <span>{{ $t('tenants.dashboard.quickStats') }}</span>
                    </div>
                </template>

                <el-row :gutter="16">
                    <el-col :span="6">
                        <div class="stat-card">
                            <div class="stat-icon flows">
                                <el-icon><Document /></el-icon>
                            </div>
                            <div class="stat-content">
                                <div class="stat-value">{{ tenantStats.totalFlows }}</div>
                                <div class="stat-label">{{ $t('tenants.dashboard.totalFlows') }}</div>
                                <div class="stat-change" :class="getChangeClass(tenantStats.flowsChange)">
                                    <el-icon><component :is="getChangeIcon(tenantStats.flowsChange)" /></el-icon>
                                    {{ Math.abs(tenantStats.flowsChange) }}%
                                </div>
                            </div>
                        </div>
                    </el-col>
                    <el-col :span="6">
                        <div class="stat-card">
                            <div class="stat-icon executions">
                                <el-icon><VideoPlay /></el-icon>
                            </div>
                            <div class="stat-content">
                                <div class="stat-value">{{ tenantStats.totalExecutions }}</div>
                                <div class="stat-label">{{ $t('tenants.dashboard.totalExecutions') }}</div>
                                <div class="stat-change" :class="getChangeClass(tenantStats.executionsChange)">
                                    <el-icon><component :is="getChangeIcon(tenantStats.executionsChange)" /></el-icon>
                                    {{ Math.abs(tenantStats.executionsChange) }}%
                                </div>
                            </div>
                        </div>
                    </el-col>
                    <el-col :span="6">
                        <div class="stat-card">
                            <div class="stat-icon users">
                                <el-icon><User /></el-icon>
                            </div>
                            <div class="stat-content">
                                <div class="stat-value">{{ tenantStats.totalUsers }}</div>
                                <div class="stat-label">{{ $t('tenants.dashboard.totalUsers') }}</div>
                                <div class="stat-change" :class="getChangeClass(tenantStats.usersChange)">
                                    <el-icon><component :is="getChangeIcon(tenantStats.usersChange)" /></el-icon>
                                    {{ Math.abs(tenantStats.usersChange) }}%
                                </div>
                            </div>
                        </div>
                    </el-col>
                    <el-col :span="6">
                        <div class="stat-card">
                            <div class="stat-icon storage">
                                <el-icon><FolderOpened /></el-icon>
                            </div>
                            <div class="stat-content">
                                <div class="stat-value">{{ formatBytes(tenantStats.storageUsed) }}</div>
                                <div class="stat-label">{{ $t('tenants.dashboard.storageUsed') }}</div>
                                <div class="stat-usage">
                                    {{ getStoragePercentage() }}% {{ $t('tenants.dashboard.of') }} {{ formatBytes(tenantStats.storageLimit) }}
                                </div>
                            </div>
                        </div>
                    </el-col>
                </el-row>
            </el-card>

            <!-- Resource Usage -->
            <el-row :gutter="16">
                <el-col :span="12">
                    <el-card class="resource-usage" shadow="never">
                        <template #header>
                            <div class="usage-header">
                                <el-icon><Monitor /></el-icon>
                                <span>{{ $t('tenants.dashboard.resourceUsage') }}</span>
                            </div>
                        </template>

                        <div class="usage-items">
                            <div class="usage-item">
                                <div class="usage-info">
                                    <span class="usage-label">{{ $t('tenants.dashboard.storage') }}</span>
                                    <span class="usage-value">
                                        {{ formatBytes(tenantStats.storageUsed) }} / {{ formatBytes(tenantStats.storageLimit) }}
                                    </span>
                                </div>
                                <el-progress
                                    :percentage="getStoragePercentage()"
                                    :color="getUsageColor(getStoragePercentage())"
                                    :stroke-width="8"
                                />
                            </div>
                            <div class="usage-item">
                                <div class="usage-info">
                                    <span class="usage-label">{{ $t('tenants.dashboard.bandwidth') }}</span>
                                    <span class="usage-value">
                                        {{ formatBytes(tenantStats.bandwidthUsed) }} / {{ formatBytes(tenantStats.bandwidthLimit) }}
                                    </span>
                                </div>
                                <el-progress
                                    :percentage="getBandwidthPercentage()"
                                    :color="getUsageColor(getBandwidthPercentage())"
                                    :stroke-width="8"
                                />
                            </div>
                            <div class="usage-item">
                                <div class="usage-info">
                                    <span class="usage-label">{{ $t('tenants.dashboard.apiCalls') }}</span>
                                    <span class="usage-value">
                                        {{ tenantStats.apiCallsUsed.toLocaleString() }} / {{ tenantStats.apiCallsLimit.toLocaleString() }}
                                    </span>
                                </div>
                                <el-progress
                                    :percentage="getApiCallsPercentage()"
                                    :color="getUsageColor(getApiCallsPercentage())"
                                    :stroke-width="8"
                                />
                            </div>
                        </div>
                    </el-card>
                </el-col>
                <el-col :span="12">
                    <el-card class="recent-activity" shadow="never">
                        <template #header>
                            <div class="activity-header">
                                <el-icon><Clock /></el-icon>
                                <span>{{ $t('tenants.dashboard.recentActivity') }}</span>
                            </div>
                        </template>

                        <div class="activity-list">
                            <div
                                v-for="activity in recentActivities"
                                :key="activity.id"
                                class="activity-item"
                            >
                                <div class="activity-icon">
                                    <el-icon><component :is="getActivityIcon(activity.type)" /></el-icon>
                                </div>
                                <div class="activity-content">
                                    <div class="activity-title">{{ activity.title }}</div>
                                    <div class="activity-description">{{ activity.description }}</div>
                                    <div class="activity-time">{{ formatRelativeTime(activity.timestamp) }}</div>
                                </div>
                                <div class="activity-status">
                                    <el-tag
                                        :type="getActivityStatusType(activity.status)"
                                        size="small"
                                    >
                                        {{ $t(`tenants.activity.${activity.status}`) }}
                                    </el-tag>
                                </div>
                            </div>
                        </div>

                        <div v-if="recentActivities.length === 0" class="no-activity">
                            <el-empty
                                :description="$t('tenants.dashboard.noRecentActivity')"
                                :image-size="80"
                            />
                        </div>
                    </el-card>
                </el-col>
            </el-row>

            <!-- Charts -->
            <el-row :gutter="16">
                <el-col :span="12">
                    <el-card class="execution-chart" shadow="never">
                        <template #header>
                            <div class="chart-header">
                                <el-icon><TrendCharts /></el-icon>
                                <span>{{ $t('tenants.dashboard.executionTrends') }}</span>
                                <div class="header-actions">
                                    <el-select
                                        v-model="chartTimeRange"
                                        size="small"
                                        @change="updateCharts"
                                    >
                                        <el-option
                                            v-for="range in timeRanges"
                                            :key="range.value"
                                            :label="$t(range.label)"
                                            :value="range.value"
                                        />
                                    </el-select>
                                </div>
                            </div>
                        </template>

                        <div class="chart-container">
                            <div ref="executionChart" class="chart"></div>
                        </div>
                    </el-card>
                </el-col>
                <el-col :span="12">
                    <el-card class="flow-chart" shadow="never">
                        <template #header>
                            <div class="chart-header">
                                <el-icon><PieChart /></el-icon>
                                <span>{{ $t('tenants.dashboard.flowDistribution') }}</span>
                            </div>
                        </template>

                        <div class="chart-container">
                            <div ref="flowChart" class="chart"></div>
                        </div>
                    </el-card>
                </el-col>
            </el-row>

            <!-- Quick Actions -->
            <el-card class="quick-actions" shadow="never">
                <template #header>
                    <div class="actions-header">
                        <el-icon><Operation /></el-icon>
                        <span>{{ $t('tenants.dashboard.quickActions') }}</span>
                    </div>
                </template>

                <div class="actions-grid">
                    <PermissionGuard permission="FLOW_CREATE">
                        <div class="action-card" @click="createFlow">
                            <div class="action-icon">
                                <el-icon><Plus /></el-icon>
                            </div>
                            <div class="action-content">
                                <div class="action-title">{{ $t('tenants.dashboard.createFlow') }}</div>
                                <div class="action-description">{{ $t('tenants.dashboard.createFlowDesc') }}</div>
                            </div>
                        </div>
                    </PermissionGuard>
                    <PermissionGuard permission="USER_CREATE">
                        <div class="action-card" @click="inviteUser">
                            <div class="action-icon">
                                <el-icon><UserFilled /></el-icon>
                            </div>
                            <div class="action-content">
                                <div class="action-title">{{ $t('tenants.dashboard.inviteUser') }}</div>
                                <div class="action-description">{{ $t('tenants.dashboard.inviteUserDesc') }}</div>
                            </div>
                        </div>
                    </PermissionGuard>
                    <PermissionGuard permission="TENANT_READ">
                        <div class="action-card" @click="viewReports">
                            <div class="action-icon">
                                <el-icon><Document /></el-icon>
                            </div>
                            <div class="action-content">
                                <div class="action-title">{{ $t('tenants.dashboard.viewReports') }}</div>
                                <div class="action-description">{{ $t('tenants.dashboard.viewReportsDesc') }}</div>
                            </div>
                        </div>
                    </PermissionGuard>
                    <PermissionGuard permission="TENANT_UPDATE">
                        <div class="action-card" @click="manageBilling">
                            <div class="action-icon">
                                <el-icon><CreditCard /></el-icon>
                            </div>
                            <div class="action-content">
                                <div class="action-title">{{ $t('tenants.dashboard.manageBilling') }}</div>
                                <div class="action-description">{{ $t('tenants.dashboard.manageBillingDesc') }}</div>
                            </div>
                        </div>
                    </PermissionGuard>
                </div>
            </el-card>

            <!-- Fallback for no permission -->
            <template #fallback>
                <AccessDeniedMessage permission="TENANT_READ" />
            </template>
        </PermissionGuard>
    </div>
</template>

<script setup lang="ts">
    import {ref, computed, onMounted, onUnmounted} from "vue"
    import {useI18n} from "vue-i18n"
    import {useRouter} from "vue-router"
    import {ElMessage} from "element-plus"
    import {
        Setting,
        Refresh,
        DataAnalysis,
        Document,
        VideoPlay,
        User,
        FolderOpened,
        Monitor,
        Clock,
        TrendCharts,
        PieChart,
        Operation,
        Plus,
        UserFilled,
        CreditCard,
        ArrowUp,
        ArrowDown
    } from "@element-plus/icons-vue"
    import {useTenantStore} from "@/stores/tenant"
    import {usePermissionStore} from "@/stores/permission"
    import PermissionGuard from "@/components/rbac/PermissionGuard.vue"
    import AccessDeniedMessage from "@/components/rbac/AccessDeniedMessage.vue"
    import {formatBytes, formatRelativeTime} from "@/utils/format"
    import type {TenantStats, Activity} from "@/types/tenant"

    const {t} = useI18n()
    const router = useRouter()
    const tenantStore = useTenantStore()
    const permissionStore = usePermissionStore()

    // State
    const loading = ref(false)
    const chartTimeRange = ref('7d')
    const refreshInterval = ref<NodeJS.Timeout | null>(null)

    // Data
    const tenantStats = ref<TenantStats>({
        totalFlows: 0,
        totalExecutions: 0,
        totalUsers: 0,
        storageUsed: 0,
        storageLimit: 0,
        bandwidthUsed: 0,
        bandwidthLimit: 0,
        apiCallsUsed: 0,
        apiCallsLimit: 0,
        flowsChange: 0,
        executionsChange: 0,
        usersChange: 0
    })

    const recentActivities = ref<Activity[]>([])

    // Constants
    const timeRanges = [
        { value: '24h', label: 'tenants.timeRange.24hours' },
        { value: '7d', label: 'tenants.timeRange.7days' },
        { value: '30d', label: 'tenants.timeRange.30days' },
        { value: '90d', label: 'tenants.timeRange.90days' }
    ]

    // Computed
    const currentTenant = computed(() => tenantStore.currentTenant)

    // Methods
    const hasPermission = (permission: string) => {
        return permissionStore.hasPermission(permission)
    }

    const loadTenantStats = async () => {
        try {
            tenantStats.value = await tenantStore.fetchTenantDashboardStats()
        } catch (error) {
            console.error('Failed to load tenant stats:', error)
        }
    }

    const loadRecentActivities = async () => {
        try {
            recentActivities.value = await tenantStore.fetchRecentActivities()
        } catch (error) {
            console.error('Failed to load recent activities:', error)
        }
    }

    const refreshDashboard = async () => {
        loading.value = true
        try {
            await Promise.all([
                loadTenantStats(),
                loadRecentActivities()
            ])
        } catch (error) {
            ElMessage.error(t('tenants.dashboard.loadError'))
        } finally {
            loading.value = false
        }
    }

    const getPlanType = (plan?: string) => {
        switch (plan) {
            case 'FREE': return 'info'
            case 'BASIC': return 'primary'
            case 'PROFESSIONAL': return 'warning'
            case 'ENTERPRISE': return 'success'
            default: return 'info'
        }
    }

    const getStoragePercentage = () => {
        if (!tenantStats.value.storageLimit) return 0
        return Math.round((tenantStats.value.storageUsed / tenantStats.value.storageLimit) * 100)
    }

    const getBandwidthPercentage = () => {
        if (!tenantStats.value.bandwidthLimit) return 0
        return Math.round((tenantStats.value.bandwidthUsed / tenantStats.value.bandwidthLimit) * 100)
    }

    const getApiCallsPercentage = () => {
        if (!tenantStats.value.apiCallsLimit) return 0
        return Math.round((tenantStats.value.apiCallsUsed / tenantStats.value.apiCallsLimit) * 100)
    }

    const getUsageColor = (percentage: number) => {
        if (percentage < 60) return '#67c23a'
        if (percentage < 80) return '#e6a23c'
        return '#f56c6c'
    }

    const getChangeClass = (change: number) => {
        return change >= 0 ? 'positive' : 'negative'
    }

    const getChangeIcon = (change: number) => {
        return change >= 0 ? 'ArrowUp' : 'ArrowDown'
    }

    const getActivityIcon = (type: string) => {
        switch (type) {
            case 'flow_created': return 'Plus'
            case 'flow_executed': return 'VideoPlay'
            case 'user_invited': return 'UserFilled'
            case 'settings_updated': return 'Setting'
            default: return 'Document'
        }
    }

    const getActivityStatusType = (status: string) => {
        switch (status) {
            case 'success': return 'success'
            case 'failed': return 'danger'
            case 'pending': return 'warning'
            default: return 'info'
        }
    }

    const updateCharts = () => {
        // Update chart data based on time range
        // This would integrate with a charting library like ECharts
    }

    const openTenantSettings = () => {
        router.push('/tenant/settings')
    }

    const createFlow = () => {
        router.push('/flows/create')
    }

    const inviteUser = () => {
        router.push('/users/invite')
    }

    const viewReports = () => {
        router.push('/reports')
    }

    const manageBilling = () => {
        router.push('/tenant/billing')
    }

    const startAutoRefresh = () => {
        refreshInterval.value = setInterval(() => {
            refreshDashboard()
        }, 60000) // Refresh every minute
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
.tenant-dashboard {
  padding: 1.5rem;
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.dashboard-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1.5rem;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 12px;
  color: white;
}

.tenant-info {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.tenant-details {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.tenant-name {
  margin: 0;
  color: white;
  font-size: 1.5rem;
  font-weight: 600;
}

.tenant-meta {
  display: flex;
  gap: 0.5rem;
}

.dashboard-actions {
  display: flex;
  gap: 0.5rem;
}

.stats-header,
.usage-header,
.activity-header,
.chart-header,
.actions-header {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-weight: 600;
  color: #303133;
}

.header-actions {
  margin-left: auto;
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 1rem;
  padding: 1.5rem;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  background: white;
  transition: all 0.3s ease;
}

.stat-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  border-color: #409eff;
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.5rem;
  color: white;
}

.stat-icon.flows {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.stat-icon.executions {
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
}

.stat-icon.users {
  background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
}

.stat-icon.storage {
  background: linear-gradient(135deg, #43e97b 0%, #38f9d7 100%);
}

.stat-content {
  flex: 1;
}

.stat-value {
  font-size: 1.8rem;
  font-weight: 600;
  color: #303133;
  margin-bottom: 0.25rem;
}

.stat-label {
  font-size: 0.9rem;
  color: #606266;
  margin-bottom: 0.5rem;
}

.stat-change {
  display: flex;
  align-items: center;
  gap: 0.25rem;
  font-size: 0.8rem;
  font-weight: 600;
}

.stat-change.positive {
  color: #67c23a;
}

.stat-change.negative {
  color: #f56c6c;
}

.stat-usage {
  font-size: 0.8rem;
  color: #909399;
  margin-top: 0.25rem;
}

.usage-items {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.usage-item {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.usage-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.usage-label {
  font-weight: 600;
  color: #303133;
}

.usage-value {
  font-size: 0.9rem;
  color: #606266;
}

.activity-list {
  display: flex;
  flex-direction: column;
  gap: 1rem;
  max-height: 400px;
  overflow-y: auto;
}

.activity-item {
  display: flex;
  align-items: flex-start;
  gap: 1rem;
  padding: 1rem;
  border: 1px solid #f0f0f0;
  border-radius: 8px;
  transition: all 0.3s ease;
}

.activity-item:hover {
  background: #f8f9fa;
  border-color: #e4e7ed;
}

.activity-icon {
  width: 32px;
  height: 32px;
  border-radius: 6px;
  background: #409eff;
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.activity-content {
  flex: 1;
}

.activity-title {
  font-weight: 600;
  color: #303133;
  margin-bottom: 0.25rem;
}

.activity-description {
  color: #606266;
  font-size: 0.9rem;
  line-height: 1.4;
  margin-bottom: 0.5rem;
}

.activity-time {
  font-size: 0.8rem;
  color: #909399;
}

.activity-status {
  flex-shrink: 0;
}

.no-activity {
  padding: 2rem;
  text-align: center;
}

.chart-container {
  height: 300px;
  padding: 1rem;
}

.chart {
  width: 100%;
  height: 100%;
  background: #f8f9fa;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #909399;
}

.actions-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 1rem;
}

.action-card {
  display: flex;
  align-items: center;
  gap: 1rem;
  padding: 1.5rem;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  background: white;
  cursor: pointer;
  transition: all 0.3s ease;
}

.action-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  border-color: #409eff;
  transform: translateY(-2px);
}

.action-icon {
  width: 40px;
  height: 40px;
  border-radius: 8px;
  background: #409eff;
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.2rem;
}

.action-content {
  flex: 1;
}

.action-title {
  font-weight: 600;
  color: #303133;
  margin-bottom: 0.25rem;
}

.action-description {
  color: #606266;
  font-size: 0.9rem;
  line-height: 1.4;
}

:deep(.el-card__header) {
  padding: 1rem;
  background: #f8f9fa;
  border-bottom: 1px solid #e4e7ed;
}

:deep(.el-progress-bar__outer) {
  border-radius: 4px;
}

:deep(.el-progress-bar__inner) {
  border-radius: 4px;
}

:deep(.el-button--primary) {
  background: rgba(255, 255, 255, 0.2);
  border-color: rgba(255, 255, 255, 0.3);
  color: white;
}

:deep(.el-button--primary:hover) {
  background: rgba(255, 255, 255, 0.3);
  border-color: rgba(255, 255, 255, 0.5);
}
</style>
