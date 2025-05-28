<template>
    <div class="tenant-switcher">
        <PermissionGuard permission="TENANT_READ">
            <el-dropdown
                trigger="click"
                placement="bottom-end"
                @command="handleTenantSwitch"
                class="tenant-dropdown"
            >
                <div class="current-tenant">
                    <div class="tenant-info">
                        <el-icon class="tenant-icon"><OfficeBuilding /></el-icon>
                        <div class="tenant-details">
                            <span class="tenant-name">{{ currentTenant?.name || $t('tenants.noTenant') }}</span>
                            <span class="tenant-id">{{ currentTenant?.id || '-' }}</span>
                        </div>
                    </div>
                    <el-icon class="dropdown-arrow"><ArrowDown /></el-icon>
                </div>

                <template #dropdown>
                    <el-dropdown-menu class="tenant-menu">
                        <!-- Search -->
                        <div class="tenant-search">
                            <el-input
                                v-model="searchQuery"
                                :placeholder="$t('tenants.searchTenants')"
                                :prefix-icon="Search"
                                size="small"
                                clearable
                                @click.stop
                            />
                        </div>

                        <!-- Current Tenant -->
                        <div class="menu-section">
                            <div class="section-title">{{ $t('tenants.currentTenant') }}</div>
                            <el-dropdown-item
                                v-if="currentTenant"
                                :command="currentTenant.id"
                                :disabled="true"
                                class="current-tenant-item"
                            >
                                <div class="tenant-item">
                                    <div class="tenant-item-info">
                                        <span class="tenant-item-name">{{ currentTenant.name }}</span>
                                        <span class="tenant-item-id">{{ currentTenant.id }}</span>
                                    </div>
                                    <el-tag type="success" size="small">{{ $t('tenants.current') }}</el-tag>
                                </div>
                            </el-dropdown-item>
                        </div>

                        <!-- Available Tenants -->
                        <div class="menu-section" v-if="filteredTenants.length > 0">
                            <div class="section-title">{{ $t('tenants.availableTenants') }}</div>
                            <el-dropdown-item
                                v-for="tenant in filteredTenants"
                                :key="tenant.id"
                                :command="tenant.id"
                                class="tenant-menu-item"
                            >
                                <div class="tenant-item">
                                    <div class="tenant-item-info">
                                        <span class="tenant-item-name">{{ tenant.name }}</span>
                                        <span class="tenant-item-id">{{ tenant.id }}</span>
                                    </div>
                                    <div class="tenant-item-status">
                                        <el-tag
                                            :type="tenant.enabled ? 'success' : 'danger'"
                                            size="small"
                                        >
                                            {{ tenant.enabled ? $t('common.enabled') : $t('common.disabled') }}
                                        </el-tag>
                                    </div>
                                </div>
                            </el-dropdown-item>
                        </div>

                        <!-- No Tenants -->
                        <div v-if="filteredTenants.length === 0 && !loading" class="no-tenants">
                            <el-empty
                                :description="$t('tenants.noTenantsAvailable')"
                                :image-size="60"
                            />
                        </div>

                        <!-- Loading -->
                        <div v-if="loading" class="tenant-loading">
                            <el-skeleton :rows="3" animated />
                        </div>

                        <!-- Actions -->
                        <div class="menu-actions">
                            <el-divider />
                            <PermissionGuard permission="TENANT_CREATE">
                                <el-dropdown-item command="create" class="action-item">
                                    <el-icon><Plus /></el-icon>
                                    {{ $t('tenants.createTenant') }}
                                </el-dropdown-item>
                            </PermissionGuard>
                            <PermissionGuard permission="TENANT_READ">
                                <el-dropdown-item command="manage" class="action-item">
                                    <el-icon><Setting /></el-icon>
                                    {{ $t('tenants.manageTenants') }}
                                </el-dropdown-item>
                            </PermissionGuard>
                            <el-dropdown-item command="refresh" class="action-item">
                                <el-icon><Refresh /></el-icon>
                                {{ $t('common.refresh') }}
                            </el-dropdown-item>
                        </div>
                    </el-dropdown-menu>
                </template>
            </el-dropdown>

            <!-- Tenant Context Indicator -->
            <div v-if="currentTenant" class="tenant-context-indicator">
                <el-tooltip
                    :content="$t('tenants.currentContext', { tenant: currentTenant.name })"
                    placement="bottom"
                >
                    <div class="context-badge">
                        <el-icon><OfficeBuilding /></el-icon>
                        <span>{{ currentTenant.name }}</span>
                    </div>
                </el-tooltip>
            </div>

            <!-- Fallback for no permission -->
            <template #fallback>
                <div class="no-permission-tenant">
                    <el-tooltip
                        :content="$t('tenants.noPermission')"
                        placement="bottom"
                    >
                        <el-icon class="disabled-icon"><OfficeBuilding /></el-icon>
                    </el-tooltip>
                </div>
            </template>
        </PermissionGuard>
    </div>
</template>

<script setup lang="ts">
    import {ref, computed, onMounted, watch} from "vue"
    import {useI18n} from "vue-i18n"
    import {useRouter} from "vue-router"
    import {ElMessage, ElMessageBox} from "element-plus"
    import {
        OfficeBuilding,
        ArrowDown,
        Search,
        Plus,
        Setting,
        Refresh
    } from "@element-plus/icons-vue"
    import {useTenantStore} from "@/stores/tenant"
    import PermissionGuard from "@/components/rbac/PermissionGuard.vue"
    import type {Tenant} from "@/types/tenant"

    const {t} = useI18n()
    const router = useRouter()
    const tenantStore = useTenantStore()

    // State
    const loading = ref(false)
    const searchQuery = ref('')
    const availableTenants = ref<Tenant[]>([])

    // Computed
    const currentTenant = computed(() => tenantStore.currentTenant)

    const filteredTenants = computed(() => {
        let filtered = availableTenants.value.filter(
            tenant => tenant.id !== currentTenant.value?.id
        )

        if (searchQuery.value) {
            const query = searchQuery.value.toLowerCase()
            filtered = filtered.filter(tenant =>
                tenant.name.toLowerCase().includes(query) ||
                tenant.id.toLowerCase().includes(query) ||
                tenant.description?.toLowerCase().includes(query)
            )
        }

        return filtered.slice(0, 10) // Limit to 10 items for performance
    })

    // Methods
    const loadAvailableTenants = async () => {
        loading.value = true
        try {
            const response = await tenantStore.fetchAvailableTenants()
            availableTenants.value = response.data
        } catch (error) {
            console.error('Failed to load available tenants:', error)
        } finally {
            loading.value = false
        }
    }

    const handleTenantSwitch = async (command: string) => {
        if (command === 'create') {
            // Navigate to tenant creation
            router.push('/admin/tenants/create')
            return
        }

        if (command === 'manage') {
            // Navigate to tenant management
            router.push('/admin/tenants')
            return
        }

        if (command === 'refresh') {
            await loadAvailableTenants()
            ElMessage.success(t('tenants.refreshSuccess'))
            return
        }

        // Handle tenant switch
        const targetTenant = availableTenants.value.find(t => t.id === command)
        if (!targetTenant) return

        try {
            await ElMessageBox.confirm(
                t('tenants.switchConfirm', {
                    from: currentTenant.value?.name || t('tenants.noTenant'),
                    to: targetTenant.name
                }),
                t('tenants.switchTenant'),
                {
                    confirmButtonText: t('common.confirm'),
                    cancelButtonText: t('common.cancel'),
                    type: 'info'
                }
            )

            // Switch tenant context
            await tenantStore.switchTenant(targetTenant.id)
            
            ElMessage.success(
                t('tenants.switchSuccess', { tenant: targetTenant.name })
            )

            // Reload current page to apply new tenant context
            window.location.reload()
        } catch (error) {
            if (error !== 'cancel') {
                ElMessage.error(t('tenants.switchError'))
            }
        }
    }

    // Watchers
    watch(() => currentTenant.value, () => {
        // Reload available tenants when current tenant changes
        loadAvailableTenants()
    })

    // Lifecycle
    onMounted(() => {
        loadAvailableTenants()
    })
</script>

<style scoped>
.tenant-switcher {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.tenant-dropdown {
  cursor: pointer;
}

.current-tenant {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem 0.75rem;
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  background: white;
  transition: all 0.2s ease;
  min-width: 200px;
}

.current-tenant:hover {
  border-color: #409eff;
  background: #f0f9ff;
}

.tenant-info {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  flex: 1;
}

.tenant-icon {
  color: #409eff;
  font-size: 1.1rem;
}

.tenant-details {
  display: flex;
  flex-direction: column;
  gap: 0.1rem;
  flex: 1;
  min-width: 0;
}

.tenant-name {
  font-weight: 600;
  color: #303133;
  font-size: 0.9rem;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.tenant-id {
  color: #909399;
  font-size: 0.75rem;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.dropdown-arrow {
  color: #909399;
  font-size: 0.8rem;
  transition: transform 0.2s ease;
}

.tenant-menu {
  min-width: 300px;
  max-height: 400px;
  overflow-y: auto;
}

.tenant-search {
  padding: 0.5rem;
  border-bottom: 1px solid #f0f0f0;
}

.menu-section {
  padding: 0.5rem 0;
}

.section-title {
  padding: 0.25rem 1rem;
  font-size: 0.8rem;
  color: #909399;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.tenant-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
  gap: 0.5rem;
}

.tenant-item-info {
  display: flex;
  flex-direction: column;
  gap: 0.1rem;
  flex: 1;
  min-width: 0;
}

.tenant-item-name {
  font-weight: 600;
  color: #303133;
  font-size: 0.9rem;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.tenant-item-id {
  color: #909399;
  font-size: 0.75rem;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.tenant-item-status {
  flex-shrink: 0;
}

.current-tenant-item {
  background: #f0f9ff;
  border-left: 3px solid #409eff;
}

.tenant-menu-item:hover {
  background: #f5f7fa;
}

.no-tenants {
  padding: 1rem;
  text-align: center;
}

.tenant-loading {
  padding: 1rem;
}

.menu-actions {
  padding: 0.5rem 0;
}

.action-item {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  color: #606266;
}

.action-item:hover {
  color: #409eff;
  background: #f0f9ff;
}

.tenant-context-indicator {
  margin-left: 0.5rem;
}

.context-badge {
  display: flex;
  align-items: center;
  gap: 0.25rem;
  padding: 0.25rem 0.5rem;
  background: #e1f3d8;
  border: 1px solid #b3d8a4;
  border-radius: 4px;
  font-size: 0.8rem;
  color: #529b2e;
}

.no-permission-tenant {
  padding: 0.5rem;
}

.disabled-icon {
  color: #c0c4cc;
  font-size: 1.1rem;
}

:deep(.el-dropdown-menu__item) {
  padding: 0.5rem 1rem;
  line-height: 1.4;
}

:deep(.el-dropdown-menu__item:not(.is-disabled):hover) {
  background: #f5f7fa;
  color: #303133;
}

:deep(.el-divider--horizontal) {
  margin: 0.5rem 0;
}
</style>
