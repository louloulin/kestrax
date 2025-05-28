<template>
    <el-dialog
        v-model="visible"
        :title="dialogTitle"
        width="900px"
        @close="handleClose"
    >
        <div v-if="role" class="role-detail">
            <!-- Basic Information -->
            <el-card class="detail-section" shadow="never">
                <template #header>
                    <div class="section-header">
                        <el-icon><InfoFilled /></el-icon>
                        <span>{{ $t('rbac.roles.basicInfo') }}</span>
                    </div>
                </template>

                <el-descriptions :column="2" border>
                    <el-descriptions-item :label="$t('rbac.roles.name')">
                        <el-tag :type="role.systemRole ? 'info' : 'primary'" size="large">
                            {{ role.name }}
                        </el-tag>
                    </el-descriptions-item>
                    <el-descriptions-item :label="$t('rbac.roles.type')">
                        <el-tag :type="role.systemRole ? 'info' : 'primary'">
                            {{ role.systemRole ? $t('rbac.roles.system') : $t('rbac.roles.custom') }}
                        </el-tag>
                    </el-descriptions-item>
                    <el-descriptions-item :label="$t('rbac.roles.description')" :span="2">
                        {{ role.description || $t('rbac.roles.noDescription') }}
                    </el-descriptions-item>
                    <el-descriptions-item :label="$t('rbac.roles.createdAt')">
                        {{ formatDate(role.createdAt) }}
                    </el-descriptions-item>
                    <el-descriptions-item :label="$t('rbac.roles.updatedAt')">
                        {{ formatDate(role.updatedAt) }}
                    </el-descriptions-item>
                </el-descriptions>
            </el-card>

            <!-- Permissions -->
            <el-card class="detail-section" shadow="never">
                <template #header>
                    <div class="section-header">
                        <el-icon><Lock /></el-icon>
                        <span>{{ $t('rbac.roles.permissions') }}</span>
                        <el-tag type="info" class="permission-count">
                            {{ role.permissions?.length || 0 }} {{ $t('rbac.roles.permissionsCount') }}
                        </el-tag>
                    </div>
                </template>

                <div v-if="role.permissions && role.permissions.length > 0" class="permissions-content">
                    <!-- Permission Categories -->
                    <el-tabs v-model="activeTab" type="border-card">
                        <el-tab-pane
                            v-for="category in permissionsByCategory"
                            :key="category.name"
                            :label="`${category.label} (${category.permissions.length})`"
                            :name="category.name"
                        >
                            <div class="category-permissions">
                                <div class="permissions-grid">
                                    <div
                                        v-for="permission in category.permissions"
                                        :key="permission"
                                        class="permission-card"
                                    >
                                        <div class="permission-header">
                                            <el-icon class="permission-icon"><Check /></el-icon>
                                            <span class="permission-name">{{ permission }}</span>
                                        </div>
                                        <div class="permission-description">
                                            {{ getPermissionDescription(permission) }}
                                        </div>
                                    </div>
                                </div>
                            </div>
                        </el-tab-pane>
                    </el-tabs>
                </div>
                <div v-else class="no-permissions">
                    <el-empty
                        :description="$t('rbac.roles.noPermissions')"
                        :image-size="80"
                    />
                </div>
            </el-card>

            <!-- Users with this Role -->
            <el-card class="detail-section" shadow="never">
                <template #header>
                    <div class="section-header">
                        <el-icon><User /></el-icon>
                        <span>{{ $t('rbac.roles.usersWithRole') }}</span>
                        <el-tag type="info" class="user-count">
                            {{ roleUsers.length }} {{ $t('rbac.roles.users') }}
                        </el-tag>
                        <el-button
                            size="small"
                            :icon="Refresh"
                            @click="loadRoleUsers"
                            :loading="loadingUsers"
                        >
                            {{ $t('common.refresh') }}
                        </el-button>
                    </div>
                </template>

                <div v-if="roleUsers.length > 0" class="users-content">
                    <div class="users-grid">
                        <div
                            v-for="user in roleUsers"
                            :key="user.id"
                            class="user-card"
                        >
                            <div class="user-avatar">
                                <el-avatar :size="40">
                                    {{ user.firstName?.[0] || user.username[0] }}
                                </el-avatar>
                            </div>
                            <div class="user-info">
                                <div class="user-name">
                                    {{ user.firstName && user.lastName
                                        ? `${user.firstName} ${user.lastName}`
                                        : user.username }}
                                </div>
                                <div class="user-email">{{ user.email }}</div>
                                <div class="user-status">
                                    <el-tag
                                        :type="user.enabled ? 'success' : 'danger'"
                                        size="small"
                                    >
                                        {{ user.enabled ? $t('common.enabled') : $t('common.disabled') }}
                                    </el-tag>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
                <div v-else-if="!loadingUsers" class="no-users">
                    <el-empty
                        :description="$t('rbac.roles.noUsers')"
                        :image-size="80"
                    />
                </div>
                <div v-else class="loading-users">
                    <el-skeleton :rows="3" animated />
                </div>
            </el-card>

            <!-- Role Statistics -->
            <el-card class="detail-section" shadow="never">
                <template #header>
                    <div class="section-header">
                        <el-icon><DataAnalysis /></el-icon>
                        <span>{{ $t('rbac.roles.statistics') }}</span>
                    </div>
                </template>

                <el-row :gutter="16">
                    <el-col :span="6">
                        <el-statistic
                            :title="$t('rbac.roles.totalPermissions')"
                            :value="role.permissions?.length || 0"
                        />
                    </el-col>
                    <el-col :span="6">
                        <el-statistic
                            :title="$t('rbac.roles.assignedUsers')"
                            :value="roleUsers.length"
                        />
                    </el-col>
                    <el-col :span="6">
                        <el-statistic
                            :title="$t('rbac.roles.permissionCategories')"
                            :value="permissionsByCategory.filter(c => c.permissions.length > 0).length"
                        />
                    </el-col>
                    <el-col :span="6">
                        <el-statistic
                            :title="$t('rbac.roles.roleAge')"
                            :value="getRoleAge()"
                            suffix="days"
                        />
                    </el-col>
                </el-row>
            </el-card>
        </div>

        <template #footer>
            <div class="dialog-footer">
                <el-button @click="handleClose">
                    {{ $t('common.close') }}
                </el-button>
                <PermissionGuard permission="ROLE_UPDATE" v-if="role && !role.systemRole">
                    <el-button type="primary" @click="editRole">
                        {{ $t('common.edit') }}
                    </el-button>
                </PermissionGuard>
            </div>
        </template>
    </el-dialog>
</template>

<script setup lang="ts">
    import {ref, computed, watch} from "vue"
    import {useI18n} from "vue-i18n"
    import {ElMessage} from "element-plus"
    import {
        InfoFilled,
        Lock,
        User,
        DataAnalysis,
        Check,
        Refresh
    } from "@element-plus/icons-vue"
    import {useRBACStore} from "@/stores/rbac"
    import PermissionGuard from "./PermissionGuard.vue"
    import {formatDate} from "@/utils/date"
    import type {Role, User as UserType} from "@/types/rbac"

    interface Props {
        modelValue: boolean
        role?: Role | null
    }

    interface Emits {
        (e: 'update:modelValue', value: boolean): void
        (e: 'edit', role: Role): void
    }

    const props = defineProps<Props>()
    const emit = defineEmits<Emits>()

    const {t} = useI18n()
    const rbacStore = useRBACStore()

    // State
    const activeTab = ref('flow')
    const roleUsers = ref<UserType[]>([])
    const loadingUsers = ref(false)

    // Computed
    const visible = computed({
        get: () => props.modelValue,
        set: (value) => emit('update:modelValue', value)
    })

    const dialogTitle = computed(() => {
        return props.role ? t('rbac.roles.roleDetails', {name: props.role.name}) : ''
    })

    const permissionsByCategory = computed(() => {
        if (!props.role?.permissions) return []

        const categories = [
            { name: 'flow', label: t('rbac.permissions.categories.flow'), prefix: 'FLOW_' },
            { name: 'execution', label: t('rbac.permissions.categories.execution'), prefix: 'EXECUTION_' },
            { name: 'template', label: t('rbac.permissions.categories.template'), prefix: 'TEMPLATE_' },
            { name: 'namespace', label: t('rbac.permissions.categories.namespace'), prefix: 'NAMESPACE_' },
            { name: 'user', label: t('rbac.permissions.categories.user'), prefix: 'USER_' },
            { name: 'role', label: t('rbac.permissions.categories.role'), prefix: 'ROLE_' },
            { name: 'group', label: t('rbac.permissions.categories.group'), prefix: 'GROUP_' },
            { name: 'tenant', label: t('rbac.permissions.categories.tenant'), prefix: 'TENANT_' },
            { name: 'audit', label: t('rbac.permissions.categories.audit'), prefix: 'AUDIT_' },
            { name: 'secret', label: t('rbac.permissions.categories.secret'), prefix: 'SECRET_' }
        ]

        return categories.map(category => ({
            ...category,
            permissions: props.role!.permissions!.filter(p => p.startsWith(category.prefix))
        })).filter(category => category.permissions.length > 0)
    })

    // Methods
    const loadRoleUsers = async () => {
        if (!props.role) return

        loadingUsers.value = true
        try {
            roleUsers.value = await rbacStore.fetchUsersByRole(props.role.id)
        } catch (error) {
            ElMessage.error(t('rbac.roles.loadUsersError'))
        } finally {
            loadingUsers.value = false
        }
    }

    const getPermissionDescription = (permission: string) => {
        return t(`rbac.permissions.descriptions.${permission}`, permission)
    }

    const getRoleAge = () => {
        if (!props.role?.createdAt) return 0
        const created = new Date(props.role.createdAt)
        const now = new Date()
        const diffTime = Math.abs(now.getTime() - created.getTime())
        return Math.ceil(diffTime / (1000 * 60 * 60 * 24))
    }

    const editRole = () => {
        if (props.role) {
            emit('edit', props.role)
            handleClose()
        }
    }

    const handleClose = () => {
        visible.value = false
    }

    // Watchers
    watch(() => props.modelValue, (newValue) => {
        if (newValue && props.role) {
            loadRoleUsers()
            // Set active tab to first category with permissions
            const firstCategory = permissionsByCategory.value[0]
            if (firstCategory) {
                activeTab.value = firstCategory.name
            }
        }
    })

    watch(() => props.role, () => {
        if (props.modelValue && props.role) {
            loadRoleUsers()
        }
    })
</script>

<style scoped>
.role-detail {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.detail-section {
  border: 1px solid #e4e7ed;
  border-radius: 8px;
}

.section-header {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-weight: 600;
  color: #303133;
}

.permission-count,
.user-count {
  margin-left: auto;
}

.permissions-content {
  padding: 0;
}

.category-permissions {
  padding: 1rem;
}

.permissions-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 1rem;
}

.permission-card {
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  padding: 0.75rem;
  background: #fafafa;
  transition: all 0.2s ease;
}

.permission-card:hover {
  border-color: #409eff;
  background: #f0f9ff;
}

.permission-header {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin-bottom: 0.5rem;
}

.permission-icon {
  color: #67c23a;
  font-size: 1rem;
}

.permission-name {
  font-weight: 600;
  color: #303133;
  font-size: 0.9rem;
}

.permission-description {
  color: #606266;
  font-size: 0.8rem;
  line-height: 1.3;
}

.no-permissions,
.no-users {
  padding: 2rem;
  text-align: center;
}

.users-content {
  padding: 1rem;
}

.users-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(250px, 1fr));
  gap: 1rem;
}

.user-card {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.75rem;
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  background: #fafafa;
  transition: all 0.2s ease;
}

.user-card:hover {
  border-color: #409eff;
  background: #f0f9ff;
}

.user-avatar {
  flex-shrink: 0;
}

.user-info {
  flex: 1;
  min-width: 0;
}

.user-name {
  font-weight: 600;
  color: #303133;
  font-size: 0.9rem;
  margin-bottom: 0.25rem;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.user-email {
  color: #606266;
  font-size: 0.8rem;
  margin-bottom: 0.25rem;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.user-status {
  display: flex;
  justify-content: flex-start;
}

.loading-users {
  padding: 1rem;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 0.5rem;
}

:deep(.el-descriptions__label) {
  font-weight: 600;
}

:deep(.el-tabs__content) {
  padding: 0;
}

:deep(.el-card__header) {
  padding: 1rem;
  background: #f8f9fa;
  border-bottom: 1px solid #e4e7ed;
}

:deep(.el-card__body) {
  padding: 0;
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
