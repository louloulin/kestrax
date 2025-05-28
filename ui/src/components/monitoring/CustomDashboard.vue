<template>
    <div class="custom-dashboard">
        <PermissionGuard permission="SYSTEM_MONITOR_READ">
            <!-- Header -->
            <div class="dashboard-header">
                <h2 class="dashboard-title">
                    <el-icon><Monitor /></el-icon>
                    {{ $t('monitoring.custom.title') }}
                </h2>
                <div class="dashboard-actions">
                    <el-button
                        :icon="Setting"
                        @click="showSettings = true"
                    >
                        {{ $t('monitoring.custom.customize') }}
                    </el-button>
                    <el-button
                        :icon="Plus"
                        @click="addWidget"
                        type="primary"
                    >
                        {{ $t('monitoring.custom.addWidget') }}
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

            <!-- Dashboard Grid -->
            <div class="dashboard-grid" v-loading="loading">
                <!-- Widget placeholders -->
                <div class="widget-placeholder">
                    <el-empty
                        :description="$t('monitoring.custom.emptyDashboard')"
                        :image-size="120"
                    >
                        <el-button type="primary" @click="addWidget">
                            {{ $t('monitoring.custom.addFirstWidget') }}
                        </el-button>
                    </el-empty>
                </div>
            </div>

            <!-- Edit Mode Toggle -->
            <div class="edit-mode-toggle">
                <el-switch
                    v-model="editMode"
                    :active-text="$t('monitoring.custom.editMode')"
                    :inactive-text="$t('monitoring.custom.viewMode')"
                />
            </div>

            <!-- Dashboard Settings Dialog -->
            <el-dialog
                v-model="showSettings"
                :title="$t('monitoring.custom.dashboardSettings')"
                width="600px"
            >
                <el-form :model="dashboardConfig" label-width="120px">
                    <el-form-item :label="$t('monitoring.custom.dashboardName')">
                        <el-input v-model="dashboardConfig.name" />
                    </el-form-item>
                    <el-form-item :label="$t('monitoring.custom.description')">
                        <el-input
                            v-model="dashboardConfig.description"
                            type="textarea"
                            :rows="3"
                        />
                    </el-form-item>
                    <el-form-item :label="$t('monitoring.custom.refreshInterval')">
                        <el-select v-model="dashboardConfig.refreshInterval">
                            <el-option
                                v-for="interval in refreshIntervals"
                                :key="interval.value"
                                :label="$t(interval.label)"
                                :value="interval.value"
                            />
                        </el-select>
                    </el-form-item>
                    <el-form-item :label="$t('monitoring.custom.isPublic')">
                        <el-switch v-model="dashboardConfig.isPublic" />
                    </el-form-item>
                </el-form>
                <template #footer>
                    <el-button @click="showSettings = false">
                        {{ $t('common.cancel') }}
                    </el-button>
                    <el-button type="primary" @click="saveDashboardConfig">
                        {{ $t('common.save') }}
                    </el-button>
                </template>
            </el-dialog>

            <!-- Fallback for no permission -->
            <template #fallback>
                <AccessDeniedMessage permission="SYSTEM_MONITOR_READ" />
            </template>
        </PermissionGuard>
    </div>
</template>

<script setup lang="ts">
    import {ref, reactive, onMounted, onUnmounted} from "vue"
    import {useI18n} from "vue-i18n"
    import {ElMessage} from "element-plus"
    import {
        Monitor,
        Setting,
        Plus,
        Delete,
        Refresh,
        DataAnalysis,
        TrendCharts,
        PieChart,
        BarChart,
        Timer
    } from "@element-plus/icons-vue"
    import {useMonitoringStore} from "@/stores/monitoring"
    import PermissionGuard from "@/components/rbac/PermissionGuard.vue"
    import AccessDeniedMessage from "@/components/rbac/AccessDeniedMessage.vue"

    const {t} = useI18n()
    const monitoringStore = useMonitoringStore()

    // State
    const loading = ref(false)
    const editMode = ref(false)
    const showSettings = ref(false)
    const refreshInterval = ref<NodeJS.Timeout | null>(null)

    // Dashboard Configuration
    const dashboardConfig = reactive({
        name: 'Custom Dashboard',
        description: 'My custom monitoring dashboard',
        refreshInterval: 30000,
        isPublic: false
    })

    // Refresh Intervals
    const refreshIntervals = [
        {value: 5000, label: 'monitoring.intervals.5seconds'},
        {value: 10000, label: 'monitoring.intervals.10seconds'},
        {value: 30000, label: 'monitoring.intervals.30seconds'},
        {value: 60000, label: 'monitoring.intervals.1minute'},
        {value: 300000, label: 'monitoring.intervals.5minutes'}
    ]

    // Methods
    const refreshDashboard = async () => {
        loading.value = true
        try {
            // Load dashboard data
            await new Promise(resolve => setTimeout(resolve, 1000))
        } catch (error) {
            ElMessage.error(t('monitoring.custom.loadError'))
        } finally {
            loading.value = false
        }
    }

    const addWidget = () => {
        ElMessage.info(t('monitoring.custom.addWidgetInfo'))
    }

    const saveDashboardConfig = async () => {
        try {
            await monitoringStore.saveDashboardConfig(dashboardConfig)
            ElMessage.success(t('monitoring.custom.saveSuccess'))
            showSettings.value = false
        } catch (error) {
            ElMessage.error(t('monitoring.custom.saveError'))
        }
    }

    const startAutoRefresh = () => {
        refreshInterval.value = setInterval(() => {
            if (!editMode.value) {
                refreshDashboard()
            }
        }, dashboardConfig.refreshInterval)
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
.custom-dashboard {
  padding: 1.5rem;
  min-height: 100vh;
  background: #f5f7fa;
}

.dashboard-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1.5rem;
  padding: 1rem;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
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

.dashboard-grid {
  min-height: 600px;
  position: relative;
}

.widget-placeholder {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 400px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.edit-mode-toggle {
  position: fixed;
  bottom: 2rem;
  right: 2rem;
  background: white;
  padding: 1rem;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  z-index: 1000;
}
</style>
