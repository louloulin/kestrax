<template>
    <div class="role-management">
        <PermissionGuard permission="ROLE_READ">
            <!-- Header -->
            <div class="role-header">
                <h2 class="role-title">
                    <el-icon><UserFilled /></el-icon>
                    {{ $t('rbac.roles.title') }}
                </h2>
                <div class="role-actions">
                    <PermissionGuard permission="ROLE_CREATE">
                        <el-button
                            type="primary"
                            :icon="Plus"
                            @click="createRole"
                        >
                            {{ $t('rbac.roles.create') }}
                        </el-button>
                    </PermissionGuard>
                    <el-button
                        :icon="Refresh"
                        @click="handleRefresh"
                        :loading="loading"
                    >
                        {{ $t('common.refresh') }}
                    </el-button>
                </div>
            </div>

            <!-- Search and Filters -->
            <div class="role-filters">
                <el-row :gutter="16">
                    <el-col :span="8">
                        <el-input
                            v-model="searchQuery"
                            :placeholder="$t('rbac.roles.search')"
                            :prefix-icon="Search"
                            clearable
                            @input="handleSearch"
                        />
                    </el-col>
                    <el-col :span="6">
                        <el-select
                            v-model="filterType"
                            :placeholder="$t('rbac.roles.filterType')"
                            clearable
                            @change="handleFilter"
                        >
                            <el-option
                                :label="$t('rbac.roles.allRoles')"
                                value=""
                            />
                            <el-option
                                :label="$t('rbac.roles.systemRoles')"
                                value="system"
                            />
                            <el-option
                                :label="$t('rbac.roles.customRoles')"
                                value="custom"
                            />
                        </el-select>
                    </el-col>
                    <el-col :span="6">
                        <el-select
                            v-model="filterPermissions"
                            :placeholder="$t('rbac.roles.filterPermissions')"
                            multiple
                            clearable
                            @change="handleFilter"
                        >
                            <el-option
                                v-for="permission in availablePermissions"
                                :key="permission"
                                :label="permission"
                                :value="permission"
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

            <!-- Role Cards Grid -->
            <div class="role-grid" v-loading="loading">
                <div
                    v-for="role in filteredRoles"
                    :key="role.id"
                    class="role-card"
                    :class="{ 'system-role': role.systemRole }"
                >
                    <div class="role-card-header">
                        <div class="role-info">
                            <h3 class="role-name">{{ role.name }}</h3>
                            <el-tag
                                :type="role.systemRole ? 'info' : 'primary'"
                                size="small"
                            >
                                {{ role.systemRole ? $t('rbac.roles.system') : $t('rbac.roles.custom') }}
                            </el-tag>
                        </div>
                        <div class="role-actions">
                            <PermissionGuard permission="ROLE_READ">
                                <el-button
                                    size="small"
                                    :icon="View"
                                    @click="viewRole(role)"
                                >
                                    {{ $t('common.view') }}
                                </el-button>
                            </PermissionGuard>
                            <PermissionGuard permission="ROLE_UPDATE" v-if="!role.systemRole">
                                <el-button
                                    size="small"
                                    type="primary"
                                    :icon="Edit"
                                    @click="editRole(role)"
                                >
                                    {{ $t('common.edit') }}
                                </el-button>
                            </PermissionGuard>
                            <PermissionGuard permission="ROLE_DELETE" v-if="!role.systemRole">
                                <el-button
                                    size="small"
                                    type="danger"
                                    :icon="Delete"
                                    @click="deleteRole(role)"
                                >
                                    {{ $t('common.delete') }}
                                </el-button>
                            </PermissionGuard>
                        </div>
                    </div>

                    <div class="role-card-body">
                        <p class="role-description">
                            {{ role.description || $t('rbac.roles.noDescription') }}
                        </p>

                        <div class="role-permissions">
                            <div class="permissions-header">
                                <span class="permissions-label">{{ $t('rbac.roles.permissions') }}:</span>
                                <el-tag size="small" type="info">
                                    {{ role.permissions?.length || 0 }} {{ $t('rbac.roles.permissionsCount') }}
                                </el-tag>
                            </div>
                            <div class="permissions-list">
                                <el-tag
                                    v-for="permission in (role.permissions || []).slice(0, 3)"
                                    :key="permission"
                                    size="small"
                                    class="permission-tag"
                                >
                                    {{ permission }}
                                </el-tag>
                                <el-tag
                                    v-if="(role.permissions || []).length > 3"
                                    size="small"
                                    type="info"
                                    class="more-permissions"
                                >
                                    +{{ (role.permissions || []).length - 3 }} {{ $t('common.more') }}
                                </el-tag>
                            </div>
                        </div>

                        <div class="role-stats">
                            <div class="stat-item">
                                <span class="stat-label">{{ $t('rbac.roles.userCount') }}:</span>
                                <span class="stat-value">{{ role.userCount || 0 }}</span>
                            </div>
                            <div class="stat-item">
                                <span class="stat-label">{{ $t('rbac.roles.createdAt') }}:</span>
                                <span class="stat-value">{{ formatDate(role.createdAt) }}</span>
                            </div>
                        </div>
                    </div>
                </div>
            </div>

            <!-- Empty State -->
            <div v-if="!loading && filteredRoles.length === 0" class="empty-state">
                <el-empty
                    :description="$t('rbac.roles.noRoles')"
                    :image-size="120"
                >
                    <PermissionGuard permission="ROLE_CREATE">
                        <el-button type="primary" @click="createRole">
                            {{ $t('rbac.roles.createFirst') }}
                        </el-button>
                    </PermissionGuard>
                </el-empty>
            </div>

            <!-- Pagination -->
            <div v-if="total > pageSize" class="role-pagination">
                <el-pagination
                    v-model:current-page="currentPage"
                    v-model:page-size="pageSize"
                    :total="total"
                    :page-sizes="[10, 20, 50, 100]"
                    layout="total, sizes, prev, pager, next, jumper"
                    @current-change="handlePageChange"
                    @size-change="handleSizeChange"
                />
            </div>

            <!-- Role Form Dialog -->
            <RoleFormDialog
                v-model="showDialog"
                :role="selectedRole"
                :mode="dialogMode"
                @save="handleRoleSave"
            />

            <!-- Role Detail Dialog -->
            <RoleDetailDialog
                v-model="showDetailDialog"
                :role="selectedRole"
            />

            <!-- Permission Matrix Dialog -->
            <PermissionMatrixDialog
                v-model="showMatrixDialog"
                :roles="roles"
                :permissions="availablePermissions"
                @update="handlePermissionUpdate"
            />

            <!-- Fallback for no permission -->
            <template #fallback>
                <AccessDeniedMessage permission="ROLE_READ" />
            </template>
        </PermissionGuard>
    </div>
</template>

<script setup lang="ts">
    import {ref, computed, onMounted} from "vue"
    import {useI18n} from "vue-i18n"
    import {ElMessage, ElMessageBox} from "element-plus"
    import {
        Plus,
        Edit,
        Delete,
        View,
        Search,
        Refresh,
        UserFilled
    } from "@element-plus/icons-vue"
    import {useRBACStore} from "@/stores/rbac"
    import PermissionGuard from "./PermissionGuard.vue"
    import AccessDeniedMessage from "./AccessDeniedMessage.vue"
    import RoleFormDialog from "./RoleFormDialog.vue"
    import RoleDetailDialog from "./RoleDetailDialog.vue"
    import PermissionMatrixDialog from "./PermissionMatrixDialog.vue"
    import {formatDate} from "@/utils/date"
    import type {Role} from "@/types/rbac"

    const {t} = useI18n()
    const rbacStore = useRBACStore()

    // State
    const loading = ref(false)
    const roles = ref<Role[]>([])
    const selectedRole = ref<Role | null>(null)
    const showDialog = ref(false)
    const showDetailDialog = ref(false)
    const showMatrixDialog = ref(false)
    const dialogMode = ref<'create' | 'edit'>('create')

    // Pagination
    const currentPage = ref(1)
    const pageSize = ref(20)
    const total = ref(0)

    // Filters
    const searchQuery = ref('')
    const filterType = ref('')
    const filterPermissions = ref<string[]>([])
    const availablePermissions = ref<string[]>([])

    // Computed
    const filteredRoles = computed(() => {
        let filtered = roles.value

        // Search filter
        if (searchQuery.value) {
            const query = searchQuery.value.toLowerCase()
            filtered = filtered.filter(role =>
                role.name.toLowerCase().includes(query) ||
                role.description?.toLowerCase().includes(query)
            )
        }

        // Type filter
        if (filterType.value) {
            filtered = filtered.filter(role => {
                if (filterType.value === 'system') return role.systemRole
                if (filterType.value === 'custom') return !role.systemRole
                return true
            })
        }

        // Permission filter
        if (filterPermissions.value.length > 0) {
            filtered = filtered.filter(role =>
                filterPermissions.value.some(permission =>
                    role.permissions?.includes(permission)
                )
            )
        }

        return filtered
    })

    // Methods
    const loadRoles = async () => {
        loading.value = true
        try {
            const response = await rbacStore.fetchRoles({
                page: currentPage.value,
                size: pageSize.value,
                query: searchQuery.value,
                includeSystem: true
            })
            roles.value = response.data
            total.value = response.total
        } catch (error) {
            ElMessage.error(t('rbac.roles.loadError'))
        } finally {
            loading.value = false
        }
    }

    const loadAvailablePermissions = async () => {
        try {
            availablePermissions.value = await rbacStore.fetchAvailablePermissions()
        } catch (error) {
            console.error('Failed to load available permissions:', error)
        }
    }

    const createRole = () => {
        selectedRole.value = null
        dialogMode.value = 'create'
        showDialog.value = true
    }

    const editRole = (role: Role) => {
        selectedRole.value = role
        dialogMode.value = 'edit'
        showDialog.value = true
    }

    const viewRole = (role: Role) => {
        selectedRole.value = role
        showDetailDialog.value = true
    }

    const deleteRole = async (role: Role) => {
        try {
            await ElMessageBox.confirm(
                t('rbac.roles.deleteConfirm', {name: role.name}),
                t('common.warning'),
                {
                    confirmButtonText: t('common.confirm'),
                    cancelButtonText: t('common.cancel'),
                    type: 'warning'
                }
            )

            await rbacStore.deleteRole(role.id)
            ElMessage.success(t('rbac.roles.deleteSuccess'))
            await loadRoles()
        } catch (error) {
            if (error !== 'cancel') {
                ElMessage.error(t('rbac.roles.deleteError'))
            }
        }
    }

    const handleRoleSave = async () => {
        await loadRoles()
        showDialog.value = false
    }

    const handlePermissionUpdate = async () => {
        await loadRoles()
        showMatrixDialog.value = false
    }

    const handleRefresh = () => {
        loadRoles()
    }

    const handleSearch = () => {
        currentPage.value = 1
        loadRoles()
    }

    const handleFilter = () => {
        currentPage.value = 1
        loadRoles()
    }

    const clearFilters = () => {
        searchQuery.value = ''
        filterType.value = ''
        filterPermissions.value = []
        currentPage.value = 1
        loadRoles()
    }

    const handlePageChange = (page: number) => {
        currentPage.value = page
        loadRoles()
    }

    const handleSizeChange = (size: number) => {
        pageSize.value = size
        currentPage.value = 1
        loadRoles()
    }

    // Lifecycle
    onMounted(() => {
        loadRoles()
        loadAvailablePermissions()
    })
</script>

<style scoped>
.role-management {
  padding: 1.5rem;
}

.role-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1.5rem;
  padding-bottom: 1rem;
  border-bottom: 1px solid #e4e7ed;
}

.role-title {
  margin: 0;
  color: #303133;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.role-actions {
  display: flex;
  gap: 0.5rem;
}

.role-filters {
  margin-bottom: 1.5rem;
  padding: 1rem;
  background: #f8f9fa;
  border-radius: 8px;
}

.role-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(400px, 1fr));
  gap: 1.5rem;
  margin-bottom: 2rem;
}

.role-card {
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  background: white;
  transition: all 0.3s ease;
  overflow: hidden;
}

.role-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  border-color: #409eff;
}

.role-card.system-role {
  border-left: 4px solid #909399;
}

.role-card-header {
  padding: 1rem;
  background: #fafafa;
  border-bottom: 1px solid #e4e7ed;
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

.role-info {
  flex: 1;
}

.role-name {
  margin: 0 0 0.5rem 0;
  color: #303133;
  font-size: 1.1rem;
  font-weight: 600;
}

.role-actions {
  display: flex;
  gap: 0.25rem;
  flex-wrap: wrap;
}

.role-card-body {
  padding: 1rem;
}

.role-description {
  margin: 0 0 1rem 0;
  color: #606266;
  font-size: 0.9rem;
  line-height: 1.4;
}

.role-permissions {
  margin-bottom: 1rem;
}

.permissions-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 0.5rem;
}

.permissions-label {
  font-weight: 600;
  color: #303133;
  font-size: 0.9rem;
}

.permissions-list {
  display: flex;
  flex-wrap: wrap;
  gap: 0.25rem;
}

.permission-tag {
  font-size: 0.8rem;
}

.more-permissions {
  cursor: pointer;
}

.role-stats {
  display: flex;
  justify-content: space-between;
  padding-top: 0.5rem;
  border-top: 1px solid #f0f0f0;
  font-size: 0.85rem;
}

.stat-item {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.stat-label {
  color: #909399;
  font-weight: 500;
}

.stat-value {
  color: #303133;
  font-weight: 600;
}

.empty-state {
  text-align: center;
  padding: 3rem 1rem;
}

.role-pagination {
  display: flex;
  justify-content: center;
  margin-top: 2rem;
}
</style>
