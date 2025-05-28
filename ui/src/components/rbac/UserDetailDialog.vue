<template>
    <el-dialog
        v-model="visible"
        :title="$t('rbac.users.userDetails')"
        :width="900"
        @close="handleClose"
    >
        <div v-if="user" class="user-detail">
            <!-- 用户基本信息 -->
            <el-card class="detail-card">
                <template #header>
                    <div class="card-header">
                        <h3>{{ $t('rbac.users.basicInfo') }}</h3>
                    </div>
                </template>

                <el-descriptions :column="2" border>
                    <el-descriptions-item :label="$t('rbac.users.username')">
                        <el-tag type="primary">
                            {{ user.username }}
                        </el-tag>
                    </el-descriptions-item>

                    <el-descriptions-item :label="$t('rbac.users.email')">
                        {{ user.email }}
                    </el-descriptions-item>

                    <el-descriptions-item :label="$t('rbac.users.fullName')">
                        {{ `${user.firstName} ${user.lastName}` }}
                    </el-descriptions-item>

                    <el-descriptions-item :label="$t('rbac.users.status')">
                        <el-tag :type="user.enabled ? 'success' : 'danger'">
                            {{ user.enabled ? $t('common.enabled') : $t('common.disabled') }}
                        </el-tag>
                    </el-descriptions-item>

                    <el-descriptions-item :label="$t('common.createdAt')">
                        {{ formatDate(user.createdAt) }}
                    </el-descriptions-item>

                    <el-descriptions-item :label="$t('common.updatedAt')">
                        {{ formatDate(user.updatedAt) }}
                    </el-descriptions-item>

                    <el-descriptions-item :label="$t('rbac.users.lastLogin')">
                        <span v-if="user.lastLoginAt">
                            {{ formatDate(user.lastLoginAt) }}
                        </span>
                        <span v-else class="text-muted">
                            {{ $t('rbac.users.neverLoggedIn') }}
                        </span>
                    </el-descriptions-item>

                    <el-descriptions-item :label="$t('rbac.users.loginCount')">
                        {{ user.loginCount || 0 }}
                    </el-descriptions-item>
                </el-descriptions>

                <div v-if="user.description" class="description-section">
                    <h4>{{ $t('common.description') }}</h4>
                    <p>{{ user.description }}</p>
                </div>
            </el-card>

            <!-- 角色信息 -->
            <el-card class="detail-card">
                <template #header>
                    <div class="card-header">
                        <h3>{{ $t('rbac.users.roles') }}</h3>
                        <el-button
                            v-if="canManageRoles"
                            type="primary"
                            size="small"
                            @click="openRoleAssignment"
                        >
                            {{ $t('rbac.users.manageRoles') }}
                        </el-button>
                    </div>
                </template>

                <div v-if="user.roles && user.roles.length > 0" class="roles-grid">
                    <div
                        v-for="role in user.roles"
                        :key="role.id"
                        class="role-card"
                        :class="{'system-role': role.systemRole}"
                    >
                        <div class="role-header">
                            <span class="role-name">{{ role.name }}</span>
                            <el-tag v-if="role.systemRole" type="info" size="small">
                                {{ $t('rbac.roles.system') }}
                            </el-tag>
                        </div>
                        <div class="role-description">
                            {{ role.description || $t('common.noDescription') }}
                        </div>
                        <div class="role-permissions">
                            <el-tag
                                v-for="permission in role.permissions?.slice(0, 5)"
                                :key="permission.code"
                                size="small"
                                type="success"
                            >
                                {{ permission.name }}
                            </el-tag>
                            <span
                                v-if="role.permissions && role.permissions.length > 5"
                                class="more-permissions"
                            >
                                +{{ role.permissions.length - 5 }} {{ $t('common.more') }}
                            </span>
                        </div>
                    </div>
                </div>
                <el-empty v-else :description="$t('rbac.users.noRoles')" />
            </el-card>

            <!-- 组信息 -->
            <el-card class="detail-card">
                <template #header>
                    <div class="card-header">
                        <h3>{{ $t('rbac.users.groups') }}</h3>
                    </div>
                </template>

                <div v-if="user.groups && user.groups.length > 0" class="groups-grid">
                    <div
                        v-for="group in user.groups"
                        :key="group.id"
                        class="group-card"
                        :class="{'system-group': group.systemGroup}"
                    >
                        <div class="group-header">
                            <span class="group-name">{{ group.name }}</span>
                            <el-tag v-if="group.systemGroup" type="info" size="small">
                                {{ $t('rbac.groups.system') }}
                            </el-tag>
                        </div>
                        <div class="group-description">
                            {{ group.description || $t('common.noDescription') }}
                        </div>
                        <div class="group-members">
                            <el-icon><User /></el-icon>
                            <span>{{ group.memberCount || 0 }} {{ $t('rbac.groups.members') }}</span>
                        </div>
                    </div>
                </div>
                <el-empty v-else :description="$t('rbac.users.noGroups')" />
            </el-card>

            <!-- 权限总览 -->
            <el-card class="detail-card">
                <template #header>
                    <div class="card-header">
                        <h3>{{ $t('rbac.permissions.effectivePermissions') }}</h3>
                        <el-button
                            type="default"
                            size="small"
                            @click="refreshPermissions"
                            :loading="loadingPermissions"
                        >
                            {{ $t('common.refresh') }}
                        </el-button>
                    </div>
                </template>

                <div v-if="effectivePermissions.length > 0" class="permissions-section">
                    <div class="permissions-stats">
                        <el-statistic
                            :title="$t('rbac.permissions.totalPermissions')"
                            :value="effectivePermissions.length"
                        />
                    </div>

                    <div class="permissions-by-category">
                        <div
                            v-for="(perms, category) in permissionsByCategory"
                            :key="category"
                            class="permission-category"
                        >
                            <h4>{{ category }}</h4>
                            <div class="permission-tags">
                                <el-tag
                                    v-for="permission in perms"
                                    :key="permission.code"
                                    size="small"
                                    type="success"
                                >
                                    {{ permission.name }}
                                </el-tag>
                            </div>
                        </div>
                    </div>
                </div>
                <el-empty v-else :description="$t('rbac.permissions.noPermissions')" />
            </el-card>

            <!-- 活动日志 -->
            <el-card class="detail-card">
                <template #header>
                    <div class="card-header">
                        <h3>{{ $t('rbac.users.activityLog') }}</h3>
                        <el-button
                            type="default"
                            size="small"
                            @click="loadActivityLog"
                            :loading="loadingActivity"
                        >
                            {{ $t('common.refresh') }}
                        </el-button>
                    </div>
                </template>

                <div v-if="activityLog.length > 0" class="activity-timeline">
                    <el-timeline>
                        <el-timeline-item
                            v-for="activity in activityLog"
                            :key="activity.id"
                            :timestamp="formatDate(activity.timestamp)"
                            :type="getActivityType(activity.action)"
                        >
                            <div class="activity-content">
                                <div class="activity-header">
                                    <span class="activity-action">{{ activity.action }}</span>
                                    <el-tag
                                        :type="activity.result === 'SUCCESS' ? 'success' : 'danger'"
                                        size="small"
                                    >
                                        {{ activity.result }}
                                    </el-tag>
                                </div>
                                <div class="activity-details">
                                    {{ activity.description }}
                                </div>
                                <div v-if="activity.ipAddress" class="activity-meta">
                                    IP: {{ activity.ipAddress }}
                                </div>
                            </div>
                        </el-timeline-item>
                    </el-timeline>
                </div>
                <el-empty v-else :description="$t('rbac.users.noActivity')" />
            </el-card>
        </div>

        <template #footer>
            <div class="dialog-footer">
                <el-button @click="handleClose">
                    {{ $t('common.close') }}
                </el-button>
                <el-button
                    v-if="canEditUser"
                    type="primary"
                    @click="editUser"
                >
                    {{ $t('common.edit') }}
                </el-button>
            </div>
        </template>

        <!-- 角色分配对话框 -->
        <UserRoleAssignment
            v-model="showRoleDialog"
            :user="user"
            @save="handleRoleAssignment"
        />
    </el-dialog>
</template>

<script setup lang="ts">
    import {ref, computed, watch} from "vue"
    import {useI18n} from "vue-i18n"
    import {ElMessage} from "element-plus"
    import {User} from "@element-plus/icons-vue"
    import {useRBACStore} from "@/stores/rbac"
    import UserRoleAssignment from "./UserRoleAssignment.vue"
    import {formatDate} from "@/utils/date"
    import type {User as UserType, Permission, AuditEvent} from "@/types/rbac"

    interface Props {
        modelValue: boolean
        user?: UserType | null
    }

    const props = withDefaults(defineProps<Props>(), {
        user: null
    })

    const emit = defineEmits(["update:modelValue", "edit"])

    const {t} = useI18n()
    const rbacStore = useRBACStore()

    // 响应式状态
    const visible = computed({
        get: () => props.modelValue,
        set: (value) => emit("update:modelValue", value)
    })

    const effectivePermissions = ref<Permission[]>([])
    const activityLog = ref<AuditEvent[]>([])
    const loadingPermissions = ref(false)
    const loadingActivity = ref(false)
    const showRoleDialog = ref(false)

    // 计算属性
    const canEditUser = computed(() => true) // 简化权限检查

    const canManageRoles = computed(() => true) // 简化权限检查

    const permissionsByCategory = computed(() => {
        const grouped: Record<string, Permission[]> = {}
        effectivePermissions.value.forEach(permission => {
            const category = permission.category || "Other"
            if (!grouped[category]) {
                grouped[category] = []
            }
            grouped[category].push(permission)
        })
        return grouped
    })

    // 方法
    const refreshPermissions = async () => {
        if (!props.user) return

        loadingPermissions.value = true
        try {
            // 简化权限获取逻辑
            effectivePermissions.value = [
                {code: "USER_READ", name: "Read Users", category: "User Management"},
                {code: "USER_UPDATE", name: "Update Users", category: "User Management"},
                {code: "ROLE_READ", name: "Read Roles", category: "Role Management"}
            ]
        } catch (error) {
            ElMessage.error(t("rbac.permissions.loadError"))
        } finally {
            loadingPermissions.value = false
        }
    }

    const loadActivityLog = async () => {
        if (!props.user) return

        loadingActivity.value = true
        try {
            // 这里应该调用审计日志API
            // const response = await auditApi.getUserTimeline(props.user.id)
            // activityLog.value = response.data

            // 模拟数据
            activityLog.value = [
                {
                    id: "1",
                    action: "LOGIN",
                    result: "SUCCESS",
                    description: "User logged in successfully",
                    timestamp: new Date().toISOString(),
                    ipAddress: "192.168.1.100"
                },
                {
                    id: "2",
                    action: "ROLE_ASSIGNED",
                    result: "SUCCESS",
                    description: "Role \"Admin\" assigned to user",
                    timestamp: new Date(Date.now() - 86400000).toISOString(),
                    ipAddress: "192.168.1.100"
                }
            ]
        } catch (error) {
            ElMessage.error(t("rbac.users.activityLoadError"))
        } finally {
            loadingActivity.value = false
        }
    }

    const getActivityType = (action: string): string => {
        const typeMap: Record<string, string> = {
            "LOGIN": "success",
            "LOGOUT": "info",
            "ROLE_ASSIGNED": "primary",
            "ROLE_REMOVED": "warning",
            "PASSWORD_CHANGED": "warning",
            "ACCOUNT_DISABLED": "danger",
            "ACCOUNT_ENABLED": "success"
        }
        return typeMap[action] || "info"
    }

    const openRoleAssignment = () => {
        showRoleDialog.value = true
    }

    const handleRoleAssignment = async (roleIds: string[]) => {
        try {
            await rbacStore.assignUserRoles(props.user!.id, roleIds)
            ElMessage.success(t("rbac.users.roleAssignSuccess"))
            showRoleDialog.value = false

            // 刷新用户数据
            const updatedUser = await rbacStore.fetchUsers({
                query: props.user!.username
            })
            // 这里应该更新父组件的用户数据
        } catch (error) {
            ElMessage.error(t("rbac.users.roleAssignError"))
        }
    }

    const editUser = () => {
        emit("edit", props.user)
        visible.value = false
    }

    const handleClose = () => {
        effectivePermissions.value = []
        activityLog.value = []
    }

    // 监听器
    watch(
        () => props.modelValue,
        (newValue) => {
            if (newValue && props.user) {
                refreshPermissions()
                loadActivityLog()
            }
        }
    )
</script>

<script lang="ts">
    export default {
        name: "UserDetailDialog"
    }
</script>

<style scoped>
.user-detail {
  max-height: 70vh;
  overflow-y: auto;
}

.detail-card {
  margin-bottom: 20px;
}

.detail-card:last-child {
  margin-bottom: 0;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
}

.description-section {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid var(--el-border-color-light);
}

.description-section h4 {
  margin: 0 0 8px 0;
  font-size: 14px;
  font-weight: 500;
}

.description-section p {
  margin: 0;
  color: var(--el-text-color-regular);
  line-height: 1.6;
}

.roles-grid,
.groups-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 16px;
}

.role-card,
.group-card {
  padding: 16px;
  border: 1px solid var(--el-border-color-light);
  border-radius: var(--el-border-radius-base);
  background: var(--el-bg-color-page);
}

.role-card.system-role,
.group-card.system-group {
  background: var(--el-color-info-light-9);
  border-color: var(--el-color-info-light-5);
}

.role-header,
.group-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.role-name,
.group-name {
  font-weight: 500;
  color: var(--el-text-color-primary);
}

.role-description,
.group-description {
  font-size: 12px;
  color: var(--el-text-color-regular);
  margin-bottom: 12px;
  line-height: 1.4;
}

.role-permissions {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  align-items: center;
}

.group-members {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: var(--el-text-color-regular);
}

.more-permissions {
  font-size: 12px;
  color: var(--el-text-color-placeholder);
}

.permissions-section {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.permissions-stats {
  display: flex;
  justify-content: center;
}

.permissions-by-category {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.permission-category h4 {
  margin: 0 0 8px 0;
  font-size: 14px;
  font-weight: 500;
  color: var(--el-text-color-primary);
}

.permission-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.activity-timeline {
  max-height: 400px;
  overflow-y: auto;
}

.activity-content {
  padding: 8px 0;
}

.activity-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 4px;
}

.activity-action {
  font-weight: 500;
  color: var(--el-text-color-primary);
}

.activity-details {
  font-size: 12px;
  color: var(--el-text-color-regular);
  margin-bottom: 4px;
}

.activity-meta {
  font-size: 11px;
  color: var(--el-text-color-placeholder);
}

.text-muted {
  color: var(--el-text-color-placeholder);
  font-style: italic;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

@media (max-width: 768px) {
  .roles-grid,
  .groups-grid {
    grid-template-columns: 1fr;
  }

  .permissions-by-category {
    gap: 12px;
  }

  .card-header {
    flex-direction: column;
    gap: 8px;
    align-items: stretch;
  }
}
</style>
