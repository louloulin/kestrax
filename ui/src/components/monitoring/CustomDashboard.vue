<template>
    <div class="custom-dashboard">
        <PermissionGuard permission="MONITORING_READ">
            <!-- Header -->
            <div class="dashboard-header">
                <div class="header-left">
                    <h2 class="dashboard-title">
                        <el-icon><Grid /></el-icon>
                        {{ $t('monitoring.custom.title') }}
                    </h2>
                    <el-select
                        v-model="selectedDashboard"
                        :placeholder="$t('monitoring.custom.selectDashboard')"
                        @change="loadDashboard"
                        style="width: 200px; margin-left: 1rem;"
                    >
                        <el-option
                            v-for="dashboard in dashboards"
                            :key="dashboard.id"
                            :label="dashboard.name"
                            :value="dashboard.id"
                        />
                    </el-select>
                </div>
                <div class="dashboard-actions">
                    <el-button
                        v-if="!editMode"
                        :icon="Edit"
                        @click="toggleEditMode"
                        type="primary"
                    >
                        {{ $t('monitoring.custom.edit') }}
                    </el-button>
                    <el-button
                        v-if="editMode"
                        :icon="Check"
                        @click="saveDashboard"
                        type="success"
                    >
                        {{ $t('common.save') }}
                    </el-button>
                    <el-button
                        v-if="editMode"
                        :icon="Close"
                        @click="cancelEdit"
                    >
                        {{ $t('common.cancel') }}
                    </el-button>
                    <el-button
                        :icon="Plus"
                        @click="createDashboard"
                        v-if="hasPermission('MONITORING_CREATE')"
                    >
                        {{ $t('monitoring.custom.create') }}
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

            <!-- Edit Mode Toolbar -->
            <div v-if="editMode" class="edit-toolbar">
                <div class="toolbar-section">
                    <span class="toolbar-label">{{ $t('monitoring.custom.addWidget') }}:</span>
                    <el-button
                        v-for="widget in availableWidgets"
                        :key="widget.type"
                        size="small"
                        @click="addWidget(widget)"
                    >
                        <el-icon><component :is="widget.icon" /></el-icon>
                        {{ $t(`monitoring.custom.widgets.${widget.type}`) }}
                    </el-button>
                </div>
                <div class="toolbar-section">
                    <el-button
                        size="small"
                        type="warning"
                        :icon="Delete"
                        @click="clearDashboard"
                    >
                        {{ $t('monitoring.custom.clearAll') }}
                    </el-button>
                </div>
            </div>

            <!-- Dashboard Grid -->
            <div class="dashboard-grid" v-loading="loading">
                <div v-if="currentDashboard && currentDashboard.layout.length === 0" class="empty-dashboard">
                    <el-empty
                        :description="$t('monitoring.custom.emptyDashboard')"
                        :image-size="120"
                    >
                        <el-button
                            v-if="editMode"
                            type="primary"
                            @click="addWidget(availableWidgets[0])"
                        >
                            {{ $t('monitoring.custom.addFirstWidget') }}
                        </el-button>
                    </el-empty>
                </div>

                <grid-layout
                    v-else
                    v-model:layout="currentDashboard.layout"
                    :col-num="12"
                    :row-height="60"
                    :is-draggable="editMode"
                    :is-resizable="editMode"
                    :is-mirrored="false"
                    :vertical-compact="true"
                    :margin="[10, 10]"
                    :use-css-transforms="true"
                >
                    <grid-item
                        v-for="item in currentDashboard.layout"
                        :key="item.i"
                        :x="item.x"
                        :y="item.y"
                        :w="item.w"
                        :h="item.h"
                        :i="item.i"
                        class="dashboard-widget"
                    >
                        <div class="widget-container">
                            <div class="widget-header">
                                <span class="widget-title">{{ getWidgetTitle(item) }}</span>
                                <div v-if="editMode" class="widget-actions">
                                    <el-button
                                        size="small"
                                        type="primary"
                                        link
                                        :icon="Setting"
                                        @click="configureWidget(item)"
                                    />
                                    <el-button
                                        size="small"
                                        type="danger"
                                        link
                                        :icon="Delete"
                                        @click="removeWidget(item)"
                                    />
                                </div>
                            </div>
                            <div class="widget-content">
                                <component
                                    :is="getWidgetComponent(item.type)"
                                    :config="item.config"
                                    :data="getWidgetData(item)"
                                />
                            </div>
                        </div>
                    </grid-item>
                </grid-layout>
            </div>

            <!-- Dashboard Settings Dialog -->
            <el-dialog
                v-model="showSettingsDialog"
                :title="$t('monitoring.custom.dashboardSettings')"
                width="500px"
            >
                <el-form :model="dashboardForm" label-width="120px">
                    <el-form-item :label="$t('monitoring.custom.name')">
                        <el-input v-model="dashboardForm.name" />
                    </el-form-item>
                    <el-form-item :label="$t('monitoring.custom.description')">
                        <el-input
                            v-model="dashboardForm.description"
                            type="textarea"
                            :rows="3"
                        />
                    </el-form-item>
                    <el-form-item :label="$t('monitoring.custom.isDefault')">
                        <el-switch v-model="dashboardForm.isDefault" />
                    </el-form-item>
                </el-form>
                <template #footer>
                    <el-button @click="showSettingsDialog = false">
                        {{ $t('common.cancel') }}
                    </el-button>
                    <el-button type="primary" @click="saveDashboardSettings">
                        {{ $t('common.save') }}
                    </el-button>
                </template>
            </el-dialog>

            <!-- Widget Configuration Dialog -->
            <el-dialog
                v-model="showWidgetDialog"
                :title="$t('monitoring.custom.widgetSettings')"
                width="600px"
            >
                <component
                    :is="getWidgetConfigComponent(selectedWidget?.type)"
                    v-model="widgetConfig"
                    :widget="selectedWidget"
                />
                <template #footer>
                    <el-button @click="showWidgetDialog = false">
                        {{ $t('common.cancel') }}
                    </el-button>
                    <el-button type="primary" @click="saveWidgetConfig">
                        {{ $t('common.save') }}
                    </el-button>
                </template>
            </el-dialog>

            <!-- Fallback for no permission -->
            <template #fallback>
                <AccessDeniedMessage permission="MONITORING_READ" />
            </template>
        </PermissionGuard>
    </div>
</template>

<script setup lang="ts">
    import {ref, onMounted} from "vue"
    import {useI18n} from "vue-i18n"
    import {ElMessage, ElMessageBox} from "element-plus"
    import {
        Grid,
        Edit,
        Check,
        Close,
        Plus,
        Refresh,
        Delete,
        Setting,

    } from "@element-plus/icons-vue"
    import {GridLayout, GridItem} from "vue-grid-layout"
    import {useMonitoringStore} from "@/stores/monitoring"
    import {usePermissionStore} from "@/stores/permission"
    import PermissionGuard from "@/components/rbac/PermissionGuard.vue"
    import AccessDeniedMessage from "@/components/rbac/AccessDeniedMessage.vue"

    const {t} = useI18n()
    const monitoringStore = useMonitoringStore()
    const permissionStore = usePermissionStore()

    // State
    const loading = ref(false)
    const editMode = ref(false)
    const selectedDashboard = ref("")
    const showSettingsDialog = ref(false)
    const showWidgetDialog = ref(false)
    const selectedWidget = ref(null)
    const widgetConfig = ref({})

    // Data
    const dashboards = ref([])
    const currentDashboard = ref(null)
    const dashboardForm = ref({
        name: "",
        description: "",
        isDefault: false
    })

    // Available widgets
    const availableWidgets = [
        {type: "metric", icon: "DataAnalysis", title: "Metric Widget"},
        {type: "chart", icon: "TrendCharts", title: "Chart Widget"},
        {type: "pie", icon: "PieChart", title: "Pie Chart Widget"},
        {type: "status", icon: "Monitor", title: "Status Widget"}
    ]

    // Computed
    const hasPermission = (permission: string) => {
        return permissionStore.hasPermission(permission)
    }

    // Methods
    const loadDashboards = async () => {
        try {
            dashboards.value = await monitoringStore.fetchDashboards()
            if (dashboards.value.length > 0 && !selectedDashboard.value) {
                selectedDashboard.value = dashboards.value[0].id
                await loadDashboard()
            }
        } catch (error) {
            console.error("Failed to load dashboards:", error)
        }
    }

    const loadDashboard = async () => {
        if (!selectedDashboard.value) return

        const dashboard = dashboards.value.find(d => d.id === selectedDashboard.value)
        if (dashboard) {
            currentDashboard.value = JSON.parse(JSON.stringify(dashboard))
        }
    }

    const refreshDashboard = async () => {
        loading.value = true
        try {
            await loadDashboards()
        } catch {
            ElMessage.error(t("monitoring.custom.loadError"))
        } finally {
            loading.value = false
        }
    }

    const toggleEditMode = () => {
        editMode.value = !editMode.value
    }

    const saveDashboard = async () => {
        try {
            await monitoringStore.saveDashboard(currentDashboard.value)
            editMode.value = false
            ElMessage.success(t("monitoring.custom.saveSuccess"))
        } catch {
            ElMessage.error(t("monitoring.custom.saveError"))
        }
    }

    const cancelEdit = () => {
        editMode.value = false
        loadDashboard() // Reload original dashboard
    }

    const createDashboard = () => {
        dashboardForm.value = {
            name: "",
            description: "",
            isDefault: false
        }
        showSettingsDialog.value = true
    }

    const addWidget = (widget) => {
        if (!currentDashboard.value) return

        const newWidget = {
            i: `widget-${Date.now()}`,
            x: 0,
            y: 0,
            w: 6,
            h: 4,
            type: widget.type,
            config: {}
        }

        currentDashboard.value.layout.push(newWidget)
    }

    const removeWidget = (widget) => {
        if (!currentDashboard.value) return

        const index = currentDashboard.value.layout.findIndex(item => item.i === widget.i)
        if (index > -1) {
            currentDashboard.value.layout.splice(index, 1)
        }
    }

    const configureWidget = (widget) => {
        selectedWidget.value = widget
        widgetConfig.value = JSON.parse(JSON.stringify(widget.config || {}))
        showWidgetDialog.value = true
    }

    const saveWidgetConfig = () => {
        if (selectedWidget.value) {
            selectedWidget.value.config = widgetConfig.value
        }
        showWidgetDialog.value = false
    }

    const clearDashboard = async () => {
        try {
            await ElMessageBox.confirm(
                t("monitoring.custom.clearConfirm"),
                t("common.warning"),
                {type: "warning"}
            )
            if (currentDashboard.value) {
                currentDashboard.value.layout = []
            }
        } catch {
            // User cancelled
        }
    }

    const saveDashboardSettings = async () => {
        try {
            const newDashboard = {
                ...dashboardForm.value,
                layout: []
            }
            const result = await monitoringStore.saveDashboard(newDashboard)
            dashboards.value.push({...newDashboard, id: result.id})
            selectedDashboard.value = result.id
            await loadDashboard()
            showSettingsDialog.value = false
            ElMessage.success(t("monitoring.custom.createSuccess"))
        } catch {
            ElMessage.error(t("monitoring.custom.createError"))
        }
    }

    const getWidgetTitle = (widget) => {
        return widget.config?.title || t(`monitoring.custom.widgets.${widget.type}`)
    }

    const getWidgetComponent = (_type: string) => {
        // Return appropriate widget component based on type
        return "div" // Placeholder
    }

    const getWidgetConfigComponent = (_type: string) => {
        // Return appropriate widget config component based on type
        return "div" // Placeholder
    }

    const getWidgetData = (_widget: any) => {
        // Return data for the widget
        return {}
    }

    // Lifecycle
    onMounted(() => {
        refreshDashboard()
    })
</script>

<style scoped>
.custom-dashboard {
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

.header-left {
  display: flex;
  align-items: center;
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

.edit-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1rem;
  background: #f8f9fa;
  border-radius: 8px;
  border: 2px dashed #409eff;
}

.toolbar-section {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.toolbar-label {
  font-weight: 600;
  color: #303133;
}

.dashboard-grid {
  min-height: 400px;
}

.empty-dashboard {
  text-align: center;
  padding: 3rem 1rem;
}

.dashboard-widget {
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  overflow: hidden;
}

.widget-container {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.widget-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0.75rem 1rem;
  background: #f8f9fa;
  border-bottom: 1px solid #e4e7ed;
}

.widget-title {
  font-weight: 600;
  color: #303133;
}

.widget-actions {
  display: flex;
  gap: 0.25rem;
}

.widget-content {
  flex: 1;
  padding: 1rem;
  overflow: hidden;
}
</style>
