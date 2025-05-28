<template>
    <div class="audit-log-viewer">
        <PermissionGuard permission="AUDIT_READ">
            <!-- Header -->
            <div class="audit-header">
                <h2 class="audit-title">
                    <el-icon><Document /></el-icon>
                    {{ $t('audit.logs.title') }}
                </h2>
                <div class="audit-actions">
                    <el-button
                        :icon="Download"
                        @click="exportLogs"
                        :loading="exporting"
                    >
                        {{ $t('audit.logs.export') }}
                    </el-button>
                    <el-button
                        :icon="Refresh"
                        @click="loadAuditLogs"
                        :loading="loading"
                    >
                        {{ $t('common.refresh') }}
                    </el-button>
                </div>
            </div>

            <!-- Filters -->
            <el-card class="audit-filters" shadow="never">
                <template #header>
                    <div class="filters-header">
                        <el-icon><Filter /></el-icon>
                        <span>{{ $t('audit.logs.filters') }}</span>
                        <el-button
                            size="small"
                            @click="clearFilters"
                            class="clear-filters"
                        >
                            {{ $t('common.clearFilters') }}
                        </el-button>
                    </div>
                </template>

                <el-row :gutter="16">
                    <el-col :span="6">
                        <el-form-item :label="$t('audit.logs.dateRange')">
                            <el-date-picker
                                v-model="dateRange"
                                type="datetimerange"
                                :range-separator="$t('common.to')"
                                :start-placeholder="$t('common.startDate')"
                                :end-placeholder="$t('common.endDate')"
                                format="YYYY-MM-DD HH:mm:ss"
                                value-format="YYYY-MM-DD HH:mm:ss"
                                @change="handleDateRangeChange"
                                style="width: 100%"
                            />
                        </el-form-item>
                    </el-col>
                    <el-col :span="4">
                        <el-form-item :label="$t('audit.logs.eventType')">
                            <el-select
                                v-model="filters.eventType"
                                :placeholder="$t('audit.logs.allEvents')"
                                clearable
                                @change="handleFilterChange"
                            >
                                <el-option
                                    v-for="type in eventTypes"
                                    :key="type"
                                    :label="$t(`audit.events.${type}`)"
                                    :value="type"
                                />
                            </el-select>
                        </el-form-item>
                    </el-col>
                    <el-col :span="4">
                        <el-form-item :label="$t('audit.logs.severity')">
                            <el-select
                                v-model="filters.severity"
                                :placeholder="$t('audit.logs.allSeverities')"
                                clearable
                                @change="handleFilterChange"
                            >
                                <el-option
                                    v-for="severity in severityLevels"
                                    :key="severity"
                                    :label="$t(`audit.severity.${severity}`)"
                                    :value="severity"
                                />
                            </el-select>
                        </el-form-item>
                    </el-col>
                    <el-col :span="4">
                        <el-form-item :label="$t('audit.logs.user')">
                            <el-input
                                v-model="filters.userId"
                                :placeholder="$t('audit.logs.searchUser')"
                                clearable
                                @input="handleFilterChange"
                            />
                        </el-form-item>
                    </el-col>
                    <el-col :span="6">
                        <el-form-item :label="$t('audit.logs.resource')">
                            <el-input
                                v-model="filters.resource"
                                :placeholder="$t('audit.logs.searchResource')"
                                clearable
                                @input="handleFilterChange"
                            />
                        </el-form-item>
                    </el-col>
                </el-row>
            </el-card>

            <!-- Statistics -->
            <el-card class="audit-stats" shadow="never">
                <template #header>
                    <div class="stats-header">
                        <el-icon><DataAnalysis /></el-icon>
                        <span>{{ $t('audit.logs.statistics') }}</span>
                    </div>
                </template>
                
                <el-row :gutter="16">
                    <el-col :span="6">
                        <el-statistic
                            :title="$t('audit.logs.totalEvents')"
                            :value="auditStats.totalEvents"
                        />
                    </el-col>
                    <el-col :span="6">
                        <el-statistic
                            :title="$t('audit.logs.criticalEvents')"
                            :value="auditStats.criticalEvents"
                        />
                    </el-col>
                    <el-col :span="6">
                        <el-statistic
                            :title="$t('audit.logs.failedActions')"
                            :value="auditStats.failedActions"
                        />
                    </el-col>
                    <el-col :span="6">
                        <el-statistic
                            :title="$t('audit.logs.uniqueUsers')"
                            :value="auditStats.uniqueUsers"
                        />
                    </el-col>
                </el-row>
            </el-card>

            <!-- Fallback for no permission -->
            <template #fallback>
                <AccessDeniedMessage permission="AUDIT_READ" />
            </template>
        </PermissionGuard>
    </div>
</template>

<script setup lang="ts">
    import {ref, computed, onMounted} from "vue"
    import {useI18n} from "vue-i18n"
    import {ElMessage} from "element-plus"
    import {
        Document,
        Download,
        Refresh,
        Filter,
        DataAnalysis,
        Clock,
        View
    } from "@element-plus/icons-vue"
    import {useAuditStore} from "@/stores/audit"
    import PermissionGuard from "@/components/rbac/PermissionGuard.vue"
    import AccessDeniedMessage from "@/components/rbac/AccessDeniedMessage.vue"
    import {formatDateTime} from "@/utils/date"
    import type {AuditEvent} from "@/types/audit"

    const {t} = useI18n()
    const auditStore = useAuditStore()

    // State
    const loading = ref(false)
    const exporting = ref(false)
    const auditLogs = ref<AuditEvent[]>([])
    const selectedAuditEvent = ref<AuditEvent | null>(null)
    const showDetailDialog = ref(false)
    const dateRange = ref<[string, string] | null>(null)

    // Pagination
    const currentPage = ref(1)
    const pageSize = ref(20)
    const total = ref(0)

    // Filters
    const filters = ref({
        eventType: '',
        severity: '',
        userId: '',
        resource: ''
    })

    // Statistics
    const auditStats = ref({
        totalEvents: 0,
        criticalEvents: 0,
        failedActions: 0,
        uniqueUsers: 0
    })

    // Constants
    const eventTypes = [
        'USER_LOGIN', 'USER_LOGOUT', 'USER_CREATE', 'USER_UPDATE', 'USER_DELETE',
        'ROLE_CREATE', 'ROLE_UPDATE', 'ROLE_DELETE',
        'GROUP_CREATE', 'GROUP_UPDATE', 'GROUP_DELETE',
        'PERMISSION_GRANT', 'PERMISSION_REVOKE',
        'FLOW_CREATE', 'FLOW_UPDATE', 'FLOW_DELETE', 'FLOW_EXECUTE',
        'SYSTEM_CONFIG', 'SECURITY_EVENT'
    ]

    const severityLevels = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL']

    // Methods
    const loadAuditLogs = async () => {
        loading.value = true
        try {
            const params = {
                page: currentPage.value,
                size: pageSize.value,
                startDate: dateRange.value?.[0],
                endDate: dateRange.value?.[1],
                ...filters.value
            }

            const response = await auditStore.fetchAuditLogs(params)
            auditLogs.value = response.data
            total.value = response.total
            await loadAuditStats()
        } catch (error) {
            ElMessage.error(t('audit.logs.loadError'))
        } finally {
            loading.value = false
        }
    }

    const loadAuditStats = async () => {
        try {
            const params = {
                startDate: dateRange.value?.[0],
                endDate: dateRange.value?.[1],
                ...filters.value
            }
            auditStats.value = await auditStore.fetchAuditStats(params)
        } catch (error) {
            console.error('Failed to load audit stats:', error)
        }
    }

    const exportLogs = async () => {
        exporting.value = true
        try {
            const params = {
                startDate: dateRange.value?.[0],
                endDate: dateRange.value?.[1],
                ...filters.value
            }

            const blob = await auditStore.exportAuditLogs(params)
            
            // Create download link
            const url = window.URL.createObjectURL(blob)
            const link = document.createElement('a')
            link.href = url
            link.download = `audit-logs-${new Date().toISOString().split('T')[0]}.csv`
            link.click()
            window.URL.revokeObjectURL(url)

            ElMessage.success(t('audit.logs.exportSuccess'))
        } catch (error) {
            ElMessage.error(t('audit.logs.exportError'))
        } finally {
            exporting.value = false
        }
    }

    const handleDateRangeChange = () => {
        currentPage.value = 1
        loadAuditLogs()
    }

    const handleFilterChange = () => {
        currentPage.value = 1
        loadAuditLogs()
    }

    const clearFilters = () => {
        dateRange.value = null
        filters.value = {
            eventType: '',
            severity: '',
            userId: '',
            resource: ''
        }
        currentPage.value = 1
        loadAuditLogs()
    }

    // Lifecycle
    onMounted(() => {
        // Set default date range to last 7 days
        const endDate = new Date()
        const startDate = new Date()
        startDate.setDate(startDate.getDate() - 7)
        
        dateRange.value = [
            startDate.toISOString().slice(0, 19).replace('T', ' '),
            endDate.toISOString().slice(0, 19).replace('T', ' ')
        ]
        
        loadAuditLogs()
    })
</script>

<style scoped>
.audit-log-viewer {
  padding: 1.5rem;
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.audit-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-bottom: 1rem;
  border-bottom: 1px solid #e4e7ed;
}

.audit-title {
  margin: 0;
  color: #303133;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.audit-actions {
  display: flex;
  gap: 0.5rem;
}

.filters-header,
.stats-header {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-weight: 600;
  color: #303133;
}

.clear-filters {
  margin-left: auto;
}
</style>
