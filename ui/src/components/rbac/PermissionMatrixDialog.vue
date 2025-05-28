<template>
    <el-dialog
        v-model="visible"
        :title="$t('rbac.permissions.matrixTitle')"
        width="1200px"
        :close-on-click-modal="false"
        @close="handleClose"
    >
        <div class="permission-matrix">
            <!-- Matrix Controls -->
            <div class="matrix-controls">
                <el-row :gutter="16" align="middle">
                    <el-col :span="8">
                        <el-input
                            v-model="searchQuery"
                            :placeholder="$t('rbac.permissions.searchRoles')"
                            :prefix-icon="Search"
                            clearable
                        />
                    </el-col>
                    <el-col :span="8">
                        <el-select
                            v-model="selectedCategory"
                            :placeholder="$t('rbac.permissions.filterCategory')"
                            clearable
                            @change="handleCategoryFilter"
                        >
                            <el-option
                                v-for="category in permissionCategories"
                                :key="category.name"
                                :label="category.label"
                                :value="category.name"
                            />
                        </el-select>
                    </el-col>
                    <el-col :span="8">
                        <div class="matrix-actions">
                            <el-button
                                :icon="Refresh"
                                @click="refreshMatrix"
                                :loading="loading"
                            >
                                {{ $t('common.refresh') }}
                            </el-button>
                            <el-button
                                type="primary"
                                :icon="Download"
                                @click="exportMatrix"
                            >
                                {{ $t('rbac.permissions.export') }}
                            </el-button>
                        </div>
                    </el-col>
                </el-row>
            </div>

            <!-- Matrix Table -->
            <div class="matrix-table-container" v-loading="loading">
                <el-table
                    :data="filteredRoles"
                    border
                    stripe
                    height="500"
                    :header-cell-style="{ backgroundColor: '#f5f7fa' }"
                >
                    <!-- Role Name Column -->
                    <el-table-column
                        :label="$t('rbac.roles.role')"
                        prop="name"
                        width="200"
                        fixed="left"
                        show-overflow-tooltip
                    >
                        <template #default="{ row }">
                            <div class="role-cell">
                                <el-tag
                                    :type="row.systemRole ? 'info' : 'primary'"
                                    size="small"
                                >
                                    {{ row.name }}
                                </el-tag>
                                <span v-if="row.systemRole" class="system-badge">
                                    {{ $t('rbac.roles.system') }}
                                </span>
                            </div>
                        </template>
                    </el-table-column>

                    <!-- Permission Columns -->
                    <el-table-column
                        v-for="permission in filteredPermissions"
                        :key="permission"
                        :label="permission"
                        :prop="permission"
                        width="120"
                        align="center"
                        class-name="permission-column"
                    >
                        <template #header>
                            <div class="permission-header">
                                <span class="permission-name">{{ permission }}</span>
                                <el-tooltip
                                    :content="getPermissionDescription(permission)"
                                    placement="top"
                                >
                                    <el-icon class="permission-info"><QuestionFilled /></el-icon>
                                </el-tooltip>
                            </div>
                        </template>
                        <template #default="{ row }">
                            <div class="permission-cell">
                                <el-checkbox
                                    :model-value="hasPermission(row, permission)"
                                    @change="togglePermission(row, permission, $event)"
                                    :disabled="row.systemRole"
                                />
                            </div>
                        </template>
                    </el-table-column>

                    <!-- Summary Column -->
                    <el-table-column
                        :label="$t('rbac.permissions.summary')"
                        width="120"
                        fixed="right"
                        align="center"
                    >
                        <template #default="{ row }">
                            <div class="summary-cell">
                                <el-tag type="info" size="small">
                                    {{ getPermissionCount(row) }} / {{ filteredPermissions.length }}
                                </el-tag>
                                <div class="permission-percentage">
                                    {{ getPermissionPercentage(row) }}%
                                </div>
                            </div>
                        </template>
                    </el-table-column>
                </el-table>
            </div>

            <!-- Matrix Statistics -->
            <div class="matrix-statistics">
                <el-row :gutter="16">
                    <el-col :span="6">
                        <el-statistic
                            :title="$t('rbac.permissions.totalRoles')"
                            :value="filteredRoles.length"
                        />
                    </el-col>
                    <el-col :span="6">
                        <el-statistic
                            :title="$t('rbac.permissions.totalPermissions')"
                            :value="filteredPermissions.length"
                        />
                    </el-col>
                    <el-col :span="6">
                        <el-statistic
                            :title="$t('rbac.permissions.averagePermissions')"
                            :value="getAveragePermissions()"
                            :precision="1"
                        />
                    </el-col>
                    <el-col :span="6">
                        <el-statistic
                            :title="$t('rbac.permissions.matrixCoverage')"
                            :value="getMatrixCoverage()"
                            :precision="1"
                            suffix="%"
                        />
                    </el-col>
                </el-row>
            </div>

            <!-- Permission Legend -->
            <div class="permission-legend">
                <h4>{{ $t('rbac.permissions.legend') }}</h4>
                <div class="legend-items">
                    <div class="legend-item">
                        <el-checkbox :model-value="true" disabled />
                        <span>{{ $t('rbac.permissions.hasPermission') }}</span>
                    </div>
                    <div class="legend-item">
                        <el-checkbox :model-value="false" disabled />
                        <span>{{ $t('rbac.permissions.noPermission') }}</span>
                    </div>
                    <div class="legend-item">
                        <el-tag type="info" size="small">{{ $t('rbac.roles.system') }}</el-tag>
                        <span>{{ $t('rbac.permissions.systemRoleNote') }}</span>
                    </div>
                </div>
            </div>
        </div>

        <template #footer>
            <div class="dialog-footer">
                <el-button @click="handleClose">
                    {{ $t('common.close') }}
                </el-button>
                <el-button
                    type="primary"
                    :loading="saving"
                    @click="saveChanges"
                    :disabled="!hasChanges"
                >
                    {{ $t('common.saveChanges') }}
                </el-button>
            </div>
        </template>
    </el-dialog>
</template>

<script setup lang="ts">
    import {ref, computed, watch, nextTick} from "vue"
    import {useI18n} from "vue-i18n"
    import {ElMessage} from "element-plus"
    import {
        Search,
        Refresh,
        Download,
        QuestionFilled
    } from "@element-plus/icons-vue"
    import {useRBACStore} from "@/stores/rbac"
    import type {Role} from "@/types/rbac"

    interface Props {
        modelValue: boolean
        roles: Role[]
        permissions: string[]
    }

    interface Emits {
        (e: 'update:modelValue', value: boolean): void
        (e: 'update'): void
    }

    const props = defineProps<Props>()
    const emit = defineEmits<Emits>()

    const {t} = useI18n()
    const rbacStore = useRBACStore()

    // State
    const loading = ref(false)
    const saving = ref(false)
    const searchQuery = ref('')
    const selectedCategory = ref('')
    const rolePermissions = ref<Map<string, Set<string>>>(new Map())
    const originalPermissions = ref<Map<string, Set<string>>>(new Map())

    // Computed
    const visible = computed({
        get: () => props.modelValue,
        set: (value) => emit('update:modelValue', value)
    })

    const permissionCategories = computed(() => [
        { name: 'flow', label: t('rbac.permissions.categories.flow') },
        { name: 'execution', label: t('rbac.permissions.categories.execution') },
        { name: 'template', label: t('rbac.permissions.categories.template') },
        { name: 'namespace', label: t('rbac.permissions.categories.namespace') },
        { name: 'user', label: t('rbac.permissions.categories.user') },
        { name: 'role', label: t('rbac.permissions.categories.role') },
        { name: 'group', label: t('rbac.permissions.categories.group') },
        { name: 'tenant', label: t('rbac.permissions.categories.tenant') },
        { name: 'audit', label: t('rbac.permissions.categories.audit') },
        { name: 'secret', label: t('rbac.permissions.categories.secret') }
    ])

    const filteredRoles = computed(() => {
        let filtered = props.roles

        if (searchQuery.value) {
            const query = searchQuery.value.toLowerCase()
            filtered = filtered.filter(role =>
                role.name.toLowerCase().includes(query) ||
                role.description?.toLowerCase().includes(query)
            )
        }

        return filtered
    })

    const filteredPermissions = computed(() => {
        if (!selectedCategory.value) {
            return props.permissions
        }

        const category = permissionCategories.value.find(c => c.name === selectedCategory.value)
        if (!category) return props.permissions

        const prefix = category.name.toUpperCase() + '_'
        return props.permissions.filter(p => p.startsWith(prefix))
    })

    const hasChanges = computed(() => {
        for (const [roleId, permissions] of rolePermissions.value) {
            const original = originalPermissions.value.get(roleId)
            if (!original) continue

            if (permissions.size !== original.size) return true
            for (const permission of permissions) {
                if (!original.has(permission)) return true
            }
        }
        return false
    })

    // Methods
    const initializeMatrix = () => {
        rolePermissions.value.clear()
        originalPermissions.value.clear()

        props.roles.forEach(role => {
            const permissions = new Set(role.permissions || [])
            rolePermissions.value.set(role.id, permissions)
            originalPermissions.value.set(role.id, new Set(permissions))
        })
    }

    const hasPermission = (role: Role, permission: string) => {
        const permissions = rolePermissions.value.get(role.id)
        return permissions?.has(permission) || false
    }

    const togglePermission = (role: Role, permission: string, checked: boolean) => {
        if (role.systemRole) return

        const permissions = rolePermissions.value.get(role.id)
        if (!permissions) return

        if (checked) {
            permissions.add(permission)
        } else {
            permissions.delete(permission)
        }
    }

    const getPermissionCount = (role: Role) => {
        const permissions = rolePermissions.value.get(role.id)
        if (!permissions) return 0
        return Array.from(permissions).filter(p => filteredPermissions.value.includes(p)).length
    }

    const getPermissionPercentage = (role: Role) => {
        const count = getPermissionCount(role)
        const total = filteredPermissions.value.length
        return total > 0 ? Math.round((count / total) * 100) : 0
    }

    const getAveragePermissions = () => {
        if (filteredRoles.value.length === 0) return 0
        const total = filteredRoles.value.reduce((sum, role) => sum + getPermissionCount(role), 0)
        return total / filteredRoles.value.length
    }

    const getMatrixCoverage = () => {
        const totalCells = filteredRoles.value.length * filteredPermissions.value.length
        if (totalCells === 0) return 0

        const filledCells = filteredRoles.value.reduce((sum, role) => sum + getPermissionCount(role), 0)
        return (filledCells / totalCells) * 100
    }

    const getPermissionDescription = (permission: string) => {
        return t(`rbac.permissions.descriptions.${permission}`, permission)
    }

    const handleCategoryFilter = () => {
        // Filter applied automatically through computed property
    }

    const refreshMatrix = () => {
        initializeMatrix()
    }

    const exportMatrix = () => {
        // Create CSV content
        const headers = ['Role', ...filteredPermissions.value]
        const rows = filteredRoles.value.map(role => [
            role.name,
            ...filteredPermissions.value.map(permission => hasPermission(role, permission) ? '✓' : '✗')
        ])

        const csvContent = [headers, ...rows]
            .map(row => row.map(cell => `"${cell}"`).join(','))
            .join('\n')

        // Download CSV
        const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' })
        const link = document.createElement('a')
        link.href = URL.createObjectURL(blob)
        link.download = `permission-matrix-${new Date().toISOString().split('T')[0]}.csv`
        link.click()
    }

    const saveChanges = async () => {
        saving.value = true
        try {
            const updates = []

            for (const [roleId, permissions] of rolePermissions.value) {
                const original = originalPermissions.value.get(roleId)
                if (!original) continue

                const role = props.roles.find(r => r.id === roleId)
                if (!role || role.systemRole) continue

                if (permissions.size !== original.size ||
                    Array.from(permissions).some(p => !original.has(p))) {
                    updates.push({
                        roleId,
                        permissions: Array.from(permissions)
                    })
                }
            }

            // Update roles with new permissions
            for (const update of updates) {
                await rbacStore.updateRolePermissions(update.roleId, update.permissions)
            }

            if (updates.length > 0) {
                ElMessage.success(t('rbac.permissions.updateSuccess', { count: updates.length }))
                emit('update')
                initializeMatrix() // Reset to new state
            }
        } catch (error) {
            ElMessage.error(t('rbac.permissions.updateError'))
        } finally {
            saving.value = false
        }
    }

    const handleClose = () => {
        visible.value = false
    }

    // Watchers
    watch(() => props.modelValue, (newValue) => {
        if (newValue) {
            nextTick(() => {
                initializeMatrix()
            })
        }
    })

    watch(() => props.roles, () => {
        if (props.modelValue) {
            initializeMatrix()
        }
    })
</script>

<style scoped>
.permission-matrix {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.matrix-controls {
  padding: 1rem;
  background: #f8f9fa;
  border-radius: 8px;
  border: 1px solid #e4e7ed;
}

.matrix-actions {
  display: flex;
  gap: 0.5rem;
  justify-content: flex-end;
}

.matrix-table-container {
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  overflow: hidden;
}

.role-cell {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.system-badge {
  font-size: 0.7rem;
  color: #909399;
  font-style: italic;
}

.permission-header {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.25rem;
  text-align: center;
}

.permission-name {
  font-size: 0.8rem;
  font-weight: 600;
  word-break: break-word;
  line-height: 1.2;
}

.permission-info {
  color: #909399;
  cursor: help;
}

.permission-cell {
  display: flex;
  justify-content: center;
  align-items: center;
}

.summary-cell {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.25rem;
}

.permission-percentage {
  font-size: 0.8rem;
  color: #606266;
  font-weight: 500;
}

.matrix-statistics {
  padding: 1rem;
  background: #f8f9fa;
  border-radius: 8px;
  border: 1px solid #e4e7ed;
}

.permission-legend {
  padding: 1rem;
  background: #fafafa;
  border-radius: 8px;
  border: 1px solid #e4e7ed;
}

.permission-legend h4 {
  margin: 0 0 1rem 0;
  color: #303133;
  font-size: 1rem;
}

.legend-items {
  display: flex;
  flex-wrap: wrap;
  gap: 1rem;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.9rem;
  color: #606266;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 0.5rem;
}

:deep(.el-table) {
  font-size: 0.9rem;
}

:deep(.el-table th) {
  background-color: #f5f7fa !important;
  font-weight: 600;
  color: #303133;
}

:deep(.el-table .permission-column) {
  padding: 8px 4px;
}

:deep(.el-table .permission-column .cell) {
  padding: 0;
  text-align: center;
}

:deep(.el-table__header-wrapper) {
  background: #f5f7fa;
}

:deep(.el-checkbox) {
  margin: 0;
}

:deep(.el-checkbox__input) {
  margin: 0;
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
</style>
