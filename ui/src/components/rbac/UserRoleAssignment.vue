<template>
    <el-dialog
        v-model="visible"
        :title="$t('rbac.users.assignRoles')"
        :width="800"
        :close-on-click-modal="false"
        @close="handleClose"
    >
        <div v-if="user" class="role-assignment">
            <!-- 用户信息 -->
            <div class="user-info">
                <el-descriptions :column="2" border>
                    <el-descriptions-item :label="$t('rbac.users.username')">
                        {{ user.username }}
                    </el-descriptions-item>
                    <el-descriptions-item :label="$t('rbac.users.email')">
                        {{ user.email }}
                    </el-descriptions-item>
                    <el-descriptions-item :label="$t('rbac.users.fullName')">
                        {{ `${user.firstName} ${user.lastName}` }}
                    </el-descriptions-item>
                    <el-descriptions-item :label="$t('rbac.users.status')">
                        <el-tag :type="user.enabled ? 'success' : 'danger'" size="small">
                            {{ user.enabled ? $t('common.enabled') : $t('common.disabled') }}
                        </el-tag>
                    </el-descriptions-item>
                </el-descriptions>
            </div>

            <!-- 角色分配区域 -->
            <div class="role-assignment-content">
                <el-row :gutter="20">
                    <!-- 可用角色 -->
                    <el-col :span="11">
                        <div class="role-section">
                            <div class="section-header">
                                <h3>{{ $t('rbac.roles.availableRoles') }}</h3>
                                <el-input
                                    v-model="availableRolesSearch"
                                    :placeholder="$t('common.search')"
                                    size="small"
                                    clearable
                                    style="width: 200px"
                                />
                            </div>
              
                            <div class="role-list">
                                <div
                                    v-for="role in filteredAvailableRoles"
                                    :key="role.id"
                                    class="role-item"
                                    :class="{'system-role': role.systemRole}"
                                    @click="selectRole(role)"
                                >
                                    <div class="role-content">
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
                                                v-for="permission in role.permissions?.slice(0, 3)"
                                                :key="permission.code"
                                                size="small"
                                                type="info"
                                            >
                                                {{ permission.name }}
                                            </el-tag>
                                            <span
                                                v-if="role.permissions && role.permissions.length > 3"
                                                class="more-permissions"
                                            >
                                                +{{ role.permissions.length - 3 }} {{ $t('common.more') }}
                                            </span>
                                        </div>
                                    </div>
                                    <div class="role-actions">
                                        <el-button
                                            type="primary"
                                            size="small"
                                            :icon="ArrowRight"
                                            @click.stop="assignRole(role)"
                                        >
                                            {{ $t('rbac.roles.assign') }}
                                        </el-button>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </el-col>

                    <!-- 操作按钮 -->
                    <el-col :span="2">
                        <div class="transfer-actions">
                            <el-button
                                type="primary"
                                :icon="ArrowRight"
                                :disabled="selectedAvailableRoles.length === 0"
                                @click="assignSelectedRoles"
                            >
                                {{ $t('rbac.roles.assignSelected') }}
                            </el-button>
              
                            <el-button
                                type="default"
                                :icon="ArrowLeft"
                                :disabled="selectedAssignedRoles.length === 0"
                                @click="unassignSelectedRoles"
                            >
                                {{ $t('rbac.roles.unassignSelected') }}
                            </el-button>
                        </div>
                    </el-col>

                    <!-- 已分配角色 -->
                    <el-col :span="11">
                        <div class="role-section">
                            <div class="section-header">
                                <h3>{{ $t('rbac.roles.assignedRoles') }}</h3>
                                <el-input
                                    v-model="assignedRolesSearch"
                                    :placeholder="$t('common.search')"
                                    size="small"
                                    clearable
                                    style="width: 200px"
                                />
                            </div>
              
                            <div class="role-list">
                                <div
                                    v-for="role in filteredAssignedRoles"
                                    :key="role.id"
                                    class="role-item assigned"
                                    :class="{'system-role': role.systemRole}"
                                    @click="selectAssignedRole(role)"
                                >
                                    <div class="role-content">
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
                                                v-for="permission in role.permissions?.slice(0, 3)"
                                                :key="permission.code"
                                                size="small"
                                                type="success"
                                            >
                                                {{ permission.name }}
                                            </el-tag>
                                            <span
                                                v-if="role.permissions && role.permissions.length > 3"
                                                class="more-permissions"
                                            >
                                                +{{ role.permissions.length - 3 }} {{ $t('common.more') }}
                                            </span>
                                        </div>
                                    </div>
                                    <div class="role-actions">
                                        <el-button
                                            type="danger"
                                            size="small"
                                            :icon="ArrowLeft"
                                            :disabled="role.systemRole && isSystemRoleRequired(role)"
                                            @click.stop="unassignRole(role)"
                                        >
                                            {{ $t('rbac.roles.unassign') }}
                                        </el-button>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </el-col>
                </el-row>
            </div>

            <!-- 权限预览 -->
            <div class="permissions-preview">
                <h3>{{ $t('rbac.permissions.effectivePermissions') }}</h3>
                <div class="permissions-grid">
                    <el-tag
                        v-for="permission in effectivePermissions"
                        :key="permission.code"
                        type="success"
                        size="small"
                    >
                        {{ permission.name }}
                    </el-tag>
                </div>
            </div>
        </div>

        <template #footer>
            <div class="dialog-footer">
                <el-button @click="handleCancel">
                    {{ $t('common.cancel') }}
                </el-button>
                <el-button
                    type="primary"
                    :loading="saving"
                    @click="handleSave"
                >
                    {{ $t('common.save') }}
                </el-button>
            </div>
        </template>
    </el-dialog>
</template>

<script setup lang="ts">
    import {ref, computed, watch, onMounted} from "vue"
    import {useI18n} from "vue-i18n"
    import {ElMessage} from "element-plus"
    import {ArrowRight, ArrowLeft} from "@element-plus/icons-vue"
    import {useRBACStore} from "@/stores/rbac"
    import type {User, Role, Permission} from "@/types/rbac"

    interface Props {
        modelValue: boolean
        user?: User | null
    }

    const props = withDefaults(defineProps<Props>(), {
        user: null
    })

    const emit = defineEmits(["update:modelValue", "save"])

    const {t} = useI18n()
    const rbacStore = useRBACStore()

    // 响应式状态
    const visible = computed({
        get: () => props.modelValue,
        set: (value) => emit("update:modelValue", value)
    })

    const saving = ref(false)
    const availableRoles = ref<Role[]>([])
    const assignedRoles = ref<Role[]>([])
    const selectedAvailableRoles = ref<Role[]>([])
    const selectedAssignedRoles = ref<Role[]>([])
    const availableRolesSearch = ref("")
    const assignedRolesSearch = ref("")

    // 计算属性
    const filteredAvailableRoles = computed(() => {
        const search = availableRolesSearch.value.toLowerCase()
        return availableRoles.value.filter(role =>
            role.name.toLowerCase().includes(search) ||
            (role.description && role.description.toLowerCase().includes(search))
        )
    })

    const filteredAssignedRoles = computed(() => {
        const search = assignedRolesSearch.value.toLowerCase()
        return assignedRoles.value.filter(role =>
            role.name.toLowerCase().includes(search) ||
            (role.description && role.description.toLowerCase().includes(search))
        )
    })

    const effectivePermissions = computed(() => {
        const permissions = new Map<string, Permission>()
  
        assignedRoles.value.forEach(role => {
            role.permissions?.forEach(permission => {
                permissions.set(permission.code, permission)
            })
        })
  
        return Array.from(permissions.values())
    })

    // 方法
    const loadRoles = async () => {
        try {
            const response = await rbacStore.fetchRoles({includeSystem: true})
            const allRoles = response.content || []
    
            // 分离已分配和可用角色
            const userRoleIds = props.user?.roles?.map(role => role.id) || []
            assignedRoles.value = allRoles.filter(role => userRoleIds.includes(role.id))
            availableRoles.value = allRoles.filter(role => !userRoleIds.includes(role.id))
        } catch (error) {
            ElMessage.error(t("rbac.roles.loadError"))
        }
    }

    const selectRole = (role: Role) => {
        const index = selectedAvailableRoles.value.findIndex(r => r.id === role.id)
        if (index > -1) {
            selectedAvailableRoles.value.splice(index, 1)
        } else {
            selectedAvailableRoles.value.push(role)
        }
    }

    const selectAssignedRole = (role: Role) => {
        const index = selectedAssignedRoles.value.findIndex(r => r.id === role.id)
        if (index > -1) {
            selectedAssignedRoles.value.splice(index, 1)
        } else {
            selectedAssignedRoles.value.push(role)
        }
    }

    const assignRole = (role: Role) => {
        const index = availableRoles.value.findIndex(r => r.id === role.id)
        if (index > -1) {
            availableRoles.value.splice(index, 1)
            assignedRoles.value.push(role)
        }
    }

    const unassignRole = (role: Role) => {
        if (role.systemRole && isSystemRoleRequired(role)) {
            ElMessage.warning(t("rbac.roles.systemRoleRequired"))
            return
        }
  
        const index = assignedRoles.value.findIndex(r => r.id === role.id)
        if (index > -1) {
            assignedRoles.value.splice(index, 1)
            availableRoles.value.push(role)
        }
    }

    const assignSelectedRoles = () => {
        selectedAvailableRoles.value.forEach(role => {
            assignRole(role)
        })
        selectedAvailableRoles.value = []
    }

    const unassignSelectedRoles = () => {
        selectedAssignedRoles.value.forEach(role => {
            unassignRole(role)
        })
        selectedAssignedRoles.value = []
    }

    const isSystemRoleRequired = (role: Role): boolean => {
        // 检查是否是必需的系统角色
        return role.systemRole && ["ADMIN", "USER"].includes(role.name)
    }

    const handleSave = async () => {
        saving.value = true
        try {
            const roleIds = assignedRoles.value.map(role => role.id)
            emit("save", roleIds)
        } finally {
            saving.value = false
        }
    }

    const handleCancel = () => {
        visible.value = false
    }

    const handleClose = () => {
        selectedAvailableRoles.value = []
        selectedAssignedRoles.value = []
        availableRolesSearch.value = ""
        assignedRolesSearch.value = ""
    }

    // 监听器
    watch(
        () => props.modelValue,
        (newValue) => {
            if (newValue && props.user) {
                loadRoles()
            }
        }
    )

    // 生命周期
    onMounted(() => {
        if (visible.value && props.user) {
            loadRoles()
        }
    })
</script>

<script lang="ts">
    export default {
        name: "UserRoleAssignment"
    }
</script>

<style scoped>
.role-assignment {
  padding: 16px 0;
}

.user-info {
  margin-bottom: 24px;
}

.role-assignment-content {
  margin-bottom: 24px;
}

.role-section {
  height: 400px;
  border: 1px solid var(--el-border-color);
  border-radius: var(--el-border-radius-base);
  overflow: hidden;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: var(--el-bg-color-page);
  border-bottom: 1px solid var(--el-border-color);
}

.section-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
}

.role-list {
  height: calc(100% - 60px);
  overflow-y: auto;
  padding: 8px;
}

.role-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px;
  margin-bottom: 8px;
  border: 1px solid var(--el-border-color-light);
  border-radius: var(--el-border-radius-base);
  cursor: pointer;
  transition: all 0.3s;
}

.role-item:hover {
  border-color: var(--el-color-primary);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.role-item.assigned {
  background: var(--el-color-success-light-9);
  border-color: var(--el-color-success-light-5);
}

.role-item.system-role {
  background: var(--el-color-info-light-9);
}

.role-content {
  flex: 1;
  min-width: 0;
}

.role-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 4px;
}

.role-name {
  font-weight: 500;
  color: var(--el-text-color-primary);
}

.role-description {
  font-size: 12px;
  color: var(--el-text-color-regular);
  margin-bottom: 8px;
  line-height: 1.4;
}

.role-permissions {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  align-items: center;
}

.more-permissions {
  font-size: 12px;
  color: var(--el-text-color-placeholder);
}

.role-actions {
  margin-left: 12px;
}

.transfer-actions {
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  height: 400px;
  gap: 16px;
}

.permissions-preview {
  padding: 16px;
  background: var(--el-bg-color-page);
  border-radius: var(--el-border-radius-base);
}

.permissions-preview h3 {
  margin: 0 0 12px 0;
  font-size: 16px;
  font-weight: 600;
}

.permissions-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

@media (max-width: 768px) {
  .role-assignment-content .el-row {
    flex-direction: column;
  }
  
  .transfer-actions {
    flex-direction: row;
    height: auto;
    padding: 16px 0;
  }
  
  .role-section {
    height: 300px;
    margin-bottom: 16px;
  }
}
</style>
