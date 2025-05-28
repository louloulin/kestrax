<template>
    <div class="tenant-management">
        <PermissionGuard permission="TENANT_READ">
            <!-- Header -->
            <div class="tenant-header">
                <h2 class="tenant-title">
                    <el-icon><OfficeBuilding /></el-icon>
                    {{ $t('tenants.management.title') }}
                </h2>
                <div class="tenant-actions">
                    <PermissionGuard permission="TENANT_CREATE">
                        <el-button
                            type="primary"
                            :icon="Plus"
                            @click="createTenant"
                        >
                            {{ $t('tenants.management.create') }}
                        </el-button>
                    </PermissionGuard>
                    <el-button
                        :icon="Refresh"
                        @click="loadTenants"
                        :loading="loading"
                    >
                        {{ $t('common.refresh') }}
                    </el-button>
                </div>
            </div>

            <!-- Tenant Overview -->
            <el-card class="tenant-overview" shadow="never">
                <template #header>
                    <div class="overview-header">
                        <el-icon><DataAnalysis /></el-icon>
                        <span>{{ $t('tenants.management.overview') }}</span>
                    </div>
                </template>

                <el-row :gutter="16">
                    <el-col :span="6">
                        <el-statistic
                            :title="$t('tenants.management.totalTenants')"
                            :value="tenantStats.totalTenants"
                        />
                    </el-col>
                    <el-col :span="6">
                        <el-statistic
                            :title="$t('tenants.management.activeTenants')"
                            :value="tenantStats.activeTenants"
                        />
                    </el-col>
                    <el-col :span="6">
                        <el-statistic
                            :title="$t('tenants.management.totalUsers')"
                            :value="tenantStats.totalUsers"
                        />
                    </el-col>
                    <el-col :span="6">
                        <el-statistic
                            :title="$t('tenants.management.totalFlows')"
                            :value="tenantStats.totalFlows"
                        />
                    </el-col>
                </el-row>
            </el-card>

            <!-- Search and Filters -->
            <div class="tenant-filters">
                <el-row :gutter="16">
                    <el-col :span="8">
                        <el-input
                            v-model="searchQuery"
                            :placeholder="$t('tenants.management.search')"
                            :prefix-icon="Search"
                            clearable
                            @input="handleSearch"
                        />
                    </el-col>
                    <el-col :span="6">
                        <el-select
                            v-model="filterStatus"
                            :placeholder="$t('tenants.management.filterStatus')"
                            clearable
                            @change="handleFilter"
                        >
                            <el-option
                                :label="$t('common.enabled')"
                                value="enabled"
                            />
                            <el-option
                                :label="$t('common.disabled')"
                                value="disabled"
                            />
                        </el-select>
                    </el-col>
                    <el-col :span="6">
                        <el-select
                            v-model="filterPlan"
                            :placeholder="$t('tenants.management.filterPlan')"
                            clearable
                            @change="handleFilter"
                        >
                            <el-option
                                v-for="plan in subscriptionPlans"
                                :key="plan"
                                :label="$t(`tenants.plans.${plan}`)"
                                :value="plan"
                            />
                        </el-select>
                    </el-col>
                    <el-col :span="4">
                        <el-button @click="clearFilters">
                            {{ $t('common.clearFilters') }}
                        </el-button>
                    </el-col>
                </el-row>
            </div>

            <!-- Tenants Grid -->
            <div class="tenants-grid" v-loading="loading">
                <div
                    v-for="tenant in filteredTenants"
                    :key="tenant.id"
                    class="tenant-card"
                    :class="{ 'tenant-disabled': !tenant.enabled }"
                >
                    <div class="tenant-card-header">
                        <div class="tenant-info">
                            <div class="tenant-avatar">
                                <el-avatar :size="48" :src="tenant.logo">
                                    {{ tenant.name[0] }}
                                </el-avatar>
                            </div>
                            <div class="tenant-details">
                                <h3 class="tenant-name">{{ tenant.name }}</h3>
                                <div class="tenant-meta">
                                    <el-tag
                                        :type="tenant.enabled ? 'success' : 'danger'"
                                        size="small"
                                    >
                                        {{ tenant.enabled ? $t('common.enabled') : $t('common.disabled') }}
                                    </el-tag>
                                    <el-tag
                                        :type="getPlanType(tenant.subscriptionPlan)"
                                        size="small"
                                    >
                                        {{ $t(`tenants.plans.${tenant.subscriptionPlan}`) }}
                                    </el-tag>
                                </div>
                            </div>
                        </div>
                        <div class="tenant-actions">
                            <el-switch
                                v-model="tenant.enabled"
                                @change="toggleTenant(tenant)"
                                :disabled="!hasPermission('TENANT_UPDATE')"
                            />
                            <PermissionGuard permission="TENANT_READ">
                                <el-button
                                    size="small"
                                    :icon="View"
                                    @click="viewTenant(tenant)"
                                >
                                    {{ $t('common.view') }}
                                </el-button>
                            </PermissionGuard>
                            <PermissionGuard permission="TENANT_UPDATE">
                                <el-button
                                    size="small"
                                    type="primary"
                                    :icon="Edit"
                                    @click="editTenant(tenant)"
                                >
                                    {{ $t('common.edit') }}
                                </el-button>
                            </PermissionGuard>
                            <PermissionGuard permission="TENANT_DELETE">
                                <el-button
                                    size="small"
                                    type="danger"
                                    :icon="Delete"
                                    @click="deleteTenant(tenant)"
                                >
                                    {{ $t('common.delete') }}
                                </el-button>
                            </PermissionGuard>
                        </div>
                    </div>

                    <div class="tenant-card-body">
                        <div class="tenant-description">
                            {{ tenant.description || $t('tenants.management.noDescription') }}
                        </div>

                        <div class="tenant-metrics">
                            <div class="metric-item">
                                <span class="metric-label">{{ $t('tenants.management.users') }}:</span>
                                <span class="metric-value">{{ tenant.userCount || 0 }}</span>
                            </div>
                            <div class="metric-item">
                                <span class="metric-label">{{ $t('tenants.management.flows') }}:</span>
                                <span class="metric-value">{{ tenant.flowCount || 0 }}</span>
                            </div>
                            <div class="metric-item">
                                <span class="metric-label">{{ $t('tenants.management.executions') }}:</span>
                                <span class="metric-value">{{ tenant.executionCount || 0 }}</span>
                            </div>
                        </div>

                        <div class="tenant-resources">
                            <div class="resource-item">
                                <span class="resource-label">{{ $t('tenants.management.storage') }}:</span>
                                <div class="resource-usage">
                                    <span class="usage-text">{{ formatBytes(tenant.storageUsed) }} / {{ formatBytes(tenant.storageLimit) }}</span>
                                    <el-progress
                                        :percentage="getStoragePercentage(tenant)"
                                        :color="getUsageColor(getStoragePercentage(tenant))"
                                        :show-text="false"
                                        :stroke-width="4"
                                    />
                                </div>
                            </div>
                            <div class="resource-item">
                                <span class="resource-label">{{ $t('tenants.management.bandwidth') }}:</span>
                                <div class="resource-usage">
                                    <span class="usage-text">{{ formatBytes(tenant.bandwidthUsed) }} / {{ formatBytes(tenant.bandwidthLimit) }}</span>
                                    <el-progress
                                        :percentage="getBandwidthPercentage(tenant)"
                                        :color="getUsageColor(getBandwidthPercentage(tenant))"
                                        :show-text="false"
                                        :stroke-width="4"
                                    />
                                </div>
                            </div>
                        </div>

                        <div class="tenant-footer">
                            <div class="tenant-dates">
                                <span class="date-item">
                                    {{ $t('tenants.management.created') }}: {{ formatDate(tenant.createdAt) }}
                                </span>
                                <span class="date-item">
                                    {{ $t('tenants.management.lastActive') }}: {{ formatDate(tenant.lastActiveAt) }}
                                </span>
                            </div>
                        </div>
                    </div>
                </div>
            </div>

            <!-- Empty State -->
            <div v-if="!loading && filteredTenants.length === 0" class="empty-state">
                <el-empty
                    :description="$t('tenants.management.noTenants')"
                    :image-size="120"
                >
                    <PermissionGuard permission="TENANT_CREATE">
                        <el-button type="primary" @click="createTenant">
                            {{ $t('tenants.management.createFirst') }}
                        </el-button>
                    </PermissionGuard>
                </el-empty>
            </div>

            <!-- Pagination -->
            <div v-if="total > pageSize" class="tenant-pagination">
                <el-pagination
                    v-model:current-page="currentPage"
                    v-model:page-size="pageSize"
                    :total="total"
                    :page-sizes="[12, 24, 48, 96]"
                    layout="total, sizes, prev, pager, next, jumper"
                    @current-change="handlePageChange"
                    @size-change="handleSizeChange"
                />
            </div>

            <!-- Tenant Form Dialog -->
            <TenantFormDialog
                v-model="showDialog"
                :tenant="selectedTenant"
                :mode="dialogMode"
                @save="handleTenantSave"
            />

            <!-- Tenant Detail Dialog -->
            <TenantDetailDialog
                v-model="showDetailDialog"
                :tenant="selectedTenant"
                @edit="editTenant"
            />

            <!-- Fallback for no permission -->
            <template #fallback>
                <AccessDeniedMessage permission="TENANT_READ" />
            </template>
        </PermissionGuard>
    </div>
</template>

<script setup lang="ts">
    import {ref, computed, onMounted} from "vue"
    import {useI18n} from "vue-i18n"
    import {ElMessage, ElMessageBox} from "element-plus"
    import {
        OfficeBuilding,
        Plus,
        Edit,
        Delete,
        View,
        Search,
        Refresh,
        DataAnalysis
    } from "@element-plus/icons-vue"
    import {useTenantStore} from "@/stores/tenant"
    import {usePermissionStore} from "@/stores/permission"
    import PermissionGuard from "@/components/rbac/PermissionGuard.vue"
    import AccessDeniedMessage from "@/components/rbac/AccessDeniedMessage.vue"
    import TenantFormDialog from "./TenantFormDialog.vue"
    import TenantDetailDialog from "./TenantDetailDialog.vue"
    import {formatDate, formatBytes} from "@/utils/format"
    import type {Tenant} from "@/types/tenant"

    const {t} = useI18n()
    const tenantStore = useTenantStore()
    const permissionStore = usePermissionStore()

    // State
    const loading = ref(false)
    const tenants = ref<Tenant[]>([])
    const selectedTenant = ref<Tenant | null>(null)
    const showDialog = ref(false)
    const showDetailDialog = ref(false)
    const dialogMode = ref<'create' | 'edit'>('create')

    // Pagination
    const currentPage = ref(1)
    const pageSize = ref(12)
    const total = ref(0)

    // Filters
    const searchQuery = ref('')
    const filterStatus = ref('')
    const filterPlan = ref('')

    // Statistics
    const tenantStats = ref({
        totalTenants: 0,
        activeTenants: 0,
        totalUsers: 0,
        totalFlows: 0
    })

    // Constants
    const subscriptionPlans = ['FREE', 'BASIC', 'PROFESSIONAL', 'ENTERPRISE']

    // Computed
    const filteredTenants = computed(() => {
        let filtered = tenants.value

        // Search filter
        if (searchQuery.value) {
            const query = searchQuery.value.toLowerCase()
            filtered = filtered.filter(tenant =>
                tenant.name.toLowerCase().includes(query) ||
                tenant.description?.toLowerCase().includes(query) ||
                tenant.id.toLowerCase().includes(query)
            )
        }

        // Status filter
        if (filterStatus.value) {
            filtered = filtered.filter(tenant => {
                if (filterStatus.value === 'enabled') return tenant.enabled
                if (filterStatus.value === 'disabled') return !tenant.enabled
                return true
            })
        }

        // Plan filter
        if (filterPlan.value) {
            filtered = filtered.filter(tenant => tenant.subscriptionPlan === filterPlan.value)
        }

        return filtered
    })

    // Methods
    const hasPermission = (permission: string) => {
        return permissionStore.hasPermission(permission)
    }

    const loadTenants = async () => {
        loading.value = true
        try {
            const response = await tenantStore.fetchTenants({
                page: currentPage.value,
                size: pageSize.value,
                query: searchQuery.value
            })
            tenants.value = response.data
            total.value = response.total
            await loadTenantStats()
        } catch (error) {
            ElMessage.error(t('tenants.management.loadError'))
        } finally {
            loading.value = false
        }
    }

    const loadTenantStats = async () => {
        try {
            tenantStats.value = await tenantStore.fetchTenantStats()
        } catch (error) {
            console.error('Failed to load tenant stats:', error)
        }
    }

    const getPlanType = (plan: string) => {
        switch (plan) {
            case 'FREE': return 'info'
            case 'BASIC': return 'primary'
            case 'PROFESSIONAL': return 'warning'
            case 'ENTERPRISE': return 'success'
            default: return 'info'
        }
    }

    const getStoragePercentage = (tenant: Tenant) => {
        if (!tenant.storageLimit) return 0
        return Math.round((tenant.storageUsed / tenant.storageLimit) * 100)
    }

    const getBandwidthPercentage = (tenant: Tenant) => {
        if (!tenant.bandwidthLimit) return 0
        return Math.round((tenant.bandwidthUsed / tenant.bandwidthLimit) * 100)
    }

    const getUsageColor = (percentage: number) => {
        if (percentage < 60) return '#67c23a'
        if (percentage < 80) return '#e6a23c'
        return '#f56c6c'
    }

    const createTenant = () => {
        selectedTenant.value = null
        dialogMode.value = 'create'
        showDialog.value = true
    }

    const editTenant = (tenant: Tenant) => {
        selectedTenant.value = tenant
        dialogMode.value = 'edit'
        showDialog.value = true
    }

    const viewTenant = (tenant: Tenant) => {
        selectedTenant.value = tenant
        showDetailDialog.value = true
    }

    const toggleTenant = async (tenant: Tenant) => {
        try {
            await tenantStore.updateTenant(tenant.id, {
                enabled: tenant.enabled
            })
            ElMessage.success(
                tenant.enabled
                    ? t('tenants.management.enableSuccess')
                    : t('tenants.management.disableSuccess')
            )
        } catch (error) {
            // Revert the change
            tenant.enabled = !tenant.enabled
            ElMessage.error(t('tenants.management.toggleError'))
        }
    }

    const deleteTenant = async (tenant: Tenant) => {
        try {
            await ElMessageBox.confirm(
                t('tenants.management.deleteConfirm', {name: tenant.name}),
                t('common.warning'),
                {
                    confirmButtonText: t('common.confirm'),
                    cancelButtonText: t('common.cancel'),
                    type: 'warning'
                }
            )

            await tenantStore.deleteTenant(tenant.id)
            ElMessage.success(t('tenants.management.deleteSuccess'))
            await loadTenants()
        } catch (error) {
            if (error !== 'cancel') {
                ElMessage.error(t('tenants.management.deleteError'))
            }
        }
    }

    const handleTenantSave = async () => {
        await loadTenants()
        showDialog.value = false
    }

    const handleSearch = () => {
        currentPage.value = 1
        loadTenants()
    }

    const handleFilter = () => {
        currentPage.value = 1
        loadTenants()
    }

    const clearFilters = () => {
        searchQuery.value = ''
        filterStatus.value = ''
        filterPlan.value = ''
        currentPage.value = 1
        loadTenants()
    }

    const handlePageChange = (page: number) => {
        currentPage.value = page
        loadTenants()
    }

    const handleSizeChange = (size: number) => {
        pageSize.value = size
        currentPage.value = 1
        loadTenants()
    }

    // Lifecycle
    onMounted(() => {
        loadTenants()
    })
</script>

<style scoped>
.tenant-management {
  padding: 1.5rem;
}

.tenant-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1.5rem;
  padding-bottom: 1rem;
  border-bottom: 1px solid #e4e7ed;
}

.tenant-title {
  margin: 0;
  color: #303133;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.tenant-actions {
  display: flex;
  gap: 0.5rem;
}

.tenant-overview {
  margin-bottom: 1.5rem;
}

.overview-header {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-weight: 600;
  color: #303133;
}

.tenant-filters {
  margin-bottom: 1.5rem;
  padding: 1rem;
  background: #f8f9fa;
  border-radius: 8px;
}

.tenants-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(400px, 1fr));
  gap: 1.5rem;
  margin-bottom: 2rem;
}

.tenant-card {
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  background: white;
  transition: all 0.3s ease;
  overflow: hidden;
}

.tenant-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  border-color: #409eff;
}

.tenant-card.tenant-disabled {
  opacity: 0.6;
  background: #f8f9fa;
}

.tenant-card-header {
  padding: 1rem;
  background: #fafafa;
  border-bottom: 1px solid #e4e7ed;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.tenant-info {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  flex: 1;
}

.tenant-avatar {
  flex-shrink: 0;
}

.tenant-details {
  flex: 1;
  min-width: 0;
}

.tenant-name {
  margin: 0 0 0.25rem 0;
  color: #303133;
  font-size: 1.1rem;
  font-weight: 600;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.tenant-meta {
  display: flex;
  gap: 0.5rem;
}

.tenant-actions {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.tenant-card-body {
  padding: 1rem;
}

.tenant-description {
  margin-bottom: 1rem;
  color: #606266;
  font-size: 0.9rem;
  line-height: 1.4;
}

.tenant-metrics {
  display: flex;
  justify-content: space-between;
  margin-bottom: 1rem;
  padding: 0.75rem;
  background: #f8f9fa;
  border-radius: 6px;
}

.metric-item {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
  text-align: center;
}

.metric-label {
  font-size: 0.8rem;
  color: #909399;
  font-weight: 500;
}

.metric-value {
  font-size: 1.1rem;
  color: #303133;
  font-weight: 600;
}

.tenant-resources {
  margin-bottom: 1rem;
}

.resource-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 0.5rem;
}

.resource-item:last-child {
  margin-bottom: 0;
}

.resource-label {
  font-size: 0.85rem;
  color: #606266;
  font-weight: 500;
  min-width: 80px;
}

.resource-usage {
  flex: 1;
  margin-left: 1rem;
}

.usage-text {
  font-size: 0.8rem;
  color: #909399;
  margin-bottom: 0.25rem;
  display: block;
}

.tenant-footer {
  padding-top: 0.75rem;
  border-top: 1px solid #f0f0f0;
}

.tenant-dates {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.date-item {
  font-size: 0.8rem;
  color: #909399;
}

.empty-state {
  text-align: center;
  padding: 3rem 1rem;
}

.tenant-pagination {
  display: flex;
  justify-content: center;
  margin-top: 2rem;
}

:deep(.el-statistic__head) {
  font-size: 0.9rem;
  color: #606266;
}

:deep(.el-statistic__content) {
  font-size: 1.5rem;
  font-weight: 600;
  color: #303133;
}

:deep(.el-card__header) {
  padding: 1rem;
  background: #f8f9fa;
  border-bottom: 1px solid #e4e7ed;
}

:deep(.el-progress-bar__outer) {
  border-radius: 2px;
}

:deep(.el-progress-bar__inner) {
  border-radius: 2px;
}
</style>
