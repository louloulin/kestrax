<template>
    <div class="user-management">
        <PermissionGuard permission="USER_READ">
            <!-- 页面标题 -->
            <div class="page-header">
                <div class="header-content">
                    <h1 class="page-title">
                        {{ $t('rbac.users.title') }}
                    </h1>
                    <p class="page-description">
                        {{ $t('rbac.users.description') }}
                    </p>
                </div>
                <div class="header-actions">
                    <PermissionGuard permission="USER_CREATE">
                        <el-button
                            type="primary"
                            :icon="Plus"
                            @click="showCreateDialog"
                        >
                            {{ $t('rbac.users.createUser') }}
                        </el-button>
                    </PermissionGuard>
                </div>
            </div>

            <!-- 数据表格 -->
            <EnterpriseDataTable
                :data="users"
                :columns="tableColumns"
                :loading="loading"
                :total="total"
                :current-page="currentPage"
                :page-size="pageSize"
                :selectable="true"
                :show-batch-actions="true"
                :show-batch-delete="true"
                @refresh="handleRefresh"
                @search="handleSearch"
                @selection-change="handleSelectionChange"
                @current-change="handlePageChange"
                @size-change="handleSizeChange"
                @batch-delete="handleBatchDelete"
            >
                <!-- 自定义列插槽 -->
                <template #status="{row}">
                    <el-tag
                        :type="row.enabled ? 'success' : 'danger'"
                        size="small"
                    >
                        {{ row.enabled ? $t('common.enabled') : $t('common.disabled') }}
                    </el-tag>
                </template>

                <template #roles="{row}">
                    <div class="user-roles">
                        <el-tag
                            v-for="role in row.roles?.slice(0, 2)"
                            :key="role.id"
                            size="small"
                            class="role-tag"
                        >
                            {{ role.name }}
                        </el-tag>
                        <el-tag
                            v-if="row.roles && row.roles.length > 2"
                            size="small"
                            type="info"
                        >
                            +{{ row.roles.length - 2 }}
                        </el-tag>
                    </div>
                </template>

                <template #lastLogin="{row}">
                    <span v-if="row.lastLoginAt">
                        {{ formatDate(row.lastLoginAt) }}
                    </span>
                    <span v-else class="text-muted">
                        {{ $t('rbac.users.neverLoggedIn') }}
                    </span>
                </template>

                <template #actions="{row}">
                    <div class="table-actions">
                        <PermissionGuard permission="USER_READ">
                            <el-button
                                size="small"
                                :icon="View"
                                @click="viewUser(row)"
                            >
                                {{ $t('common.view') }}
                            </el-button>
                        </PermissionGuard>

                        <PermissionGuard permission="USER_UPDATE">
                            <el-button
                                size="small"
                                type="primary"
                                :icon="Edit"
                                @click="editUser(row)"
                            >
                                {{ $t('common.edit') }}
                            </el-button>
                        </PermissionGuard>

                        <PermissionGuard permission="USER_UPDATE">
                            <el-button
                                size="small"
                                :type="row.enabled ? 'warning' : 'success'"
                                @click="toggleUserStatus(row)"
                            >
                                {{ row.enabled ? $t('common.disable') : $t('common.enable') }}
                            </el-button>
                        </PermissionGuard>

                        <PermissionGuard permission="USER_DELETE">
                            <el-button
                                size="small"
                                type="danger"
                                :icon="Delete"
                                @click="deleteUser(row)"
                            >
                                {{ $t('common.delete') }}
                            </el-button>
                        </PermissionGuard>
                    </div>
                </template>
            </EnterpriseDataTable>

            <!-- 用户表单对话框 -->
            <UserFormDialog
                v-model="showDialog"
                :user="selectedUser"
                :mode="dialogMode"
                @save="handleUserSave"
            />

            <!-- 用户详情对话框 -->
            <UserDetailDialog
                v-model="showDetailDialog"
                :user="selectedUser"
            />

            <!-- 角色分配对话框 -->
            <UserRoleAssignment
                v-model="showRoleDialog"
                :user="selectedUser"
                @save="handleRoleAssignment"
            />

            <!-- 无权限访问 -->
            <template #fallback>
                <AccessDeniedMessage permission="USER_READ" />
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
        View
    } from "@element-plus/icons-vue"
    import {useRBACStore} from "@/stores/rbac"
    import PermissionGuard from "./PermissionGuard.vue"
    import AccessDeniedMessage from "./AccessDeniedMessage.vue"
    import EnterpriseDataTable from "@/components/common/EnterpriseDataTable.vue"
    import UserFormDialog from "./UserFormDialog.vue"
    import UserDetailDialog from "./UserDetailDialog.vue"
    import UserRoleAssignment from "./UserRoleAssignment.vue"
    import {formatDate} from "@/utils/date"
    import type {User} from "@/types/rbac"

    const {t} = useI18n()
    const rbacStore = useRBACStore()

    // 响应式状态
    const users = computed(() => rbacStore.users)
    const loading = computed(() => rbacStore.loading)
    const total = ref(0)
    const currentPage = ref(1)
    const pageSize = ref(20)
    const searchQuery = ref("")
    const selectedUsers = ref<User[]>([])
    const selectedUser = ref<User | null>(null)

    // 对话框状态
    const showDialog = ref(false)
    const showDetailDialog = ref(false)
    const showRoleDialog = ref(false)
    const dialogMode = ref<"create" | "edit" | "view">("create")

    // 表格列配置
    const tableColumns = computed(() => [
        {
            prop: "username",
            label: t("rbac.users.username"),
            minWidth: 120,
            sortable: true
        },
        {
            prop: "email",
            label: t("rbac.users.email"),
            minWidth: 180,
            sortable: true
        },
        {
            prop: "firstName",
            label: t("rbac.users.firstName"),
            minWidth: 100
        },
        {
            prop: "lastName",
            label: t("rbac.users.lastName"),
            minWidth: 100
        },
        {
            prop: "enabled",
            label: t("rbac.users.status"),
            width: 100,
            slot: "status"
        },
        {
            prop: "roles",
            label: t("rbac.users.roles"),
            minWidth: 150,
            slot: "roles"
        },
        {
            prop: "lastLoginAt",
            label: t("rbac.users.lastLogin"),
            width: 160,
            slot: "lastLogin"
        },
        {
            prop: "createdAt",
            label: t("common.createdAt"),
            width: 160,
            type: "date"
        },
        {
            prop: "actions",
            label: t("common.actions"),
            width: 300,
            fixed: "right",
            type: "actions",
            slot: "actions"
        }
    ])

    // 方法
    const fetchUsers = async () => {
        try {
            const response = await rbacStore.fetchUsers({
                page: currentPage.value,
                size: pageSize.value,
                query: searchQuery.value
            })
            total.value = response.total
        } catch (error) {
            ElMessage.error(t("rbac.users.fetchError"))
        }
    }

    const handleRefresh = () => {
        fetchUsers()
    }

    const handleSearch = (query: string) => {
        searchQuery.value = query
        currentPage.value = 1
        fetchUsers()
    }

    const handlePageChange = (page: number) => {
        currentPage.value = page
        fetchUsers()
    }

    const handleSizeChange = (size: number) => {
        pageSize.value = size
        currentPage.value = 1
        fetchUsers()
    }

    const handleSelectionChange = (selection: User[]) => {
        selectedUsers.value = selection
    }

    const showCreateDialog = () => {
        selectedUser.value = null
        dialogMode.value = "create"
        showDialog.value = true
    }

    const viewUser = (user: User) => {
        selectedUser.value = user
        showDetailDialog.value = true
    }

    const editUser = (user: User) => {
        selectedUser.value = user
        dialogMode.value = "edit"
        showDialog.value = true
    }

    const deleteUser = async (user: User) => {
        try {
            await ElMessageBox.confirm(
                t("rbac.users.deleteConfirm", {username: user.username}),
                t("common.warning"),
                {
                    confirmButtonText: t("common.confirm"),
                    cancelButtonText: t("common.cancel"),
                    type: "warning"
                }
            )

            await rbacStore.deleteUser(user.id)
            ElMessage.success(t("rbac.users.deleteSuccess"))
            await fetchUsers()
        } catch (error) {
            if (error !== "cancel") {
                ElMessage.error(t("rbac.users.deleteError"))
            }
        }
    }

    const toggleUserStatus = async (user: User) => {
        try {
            await rbacStore.updateUser(user.id, {enabled: !user.enabled})
            ElMessage.success(
                user.enabled
                    ? t("rbac.users.disableSuccess")
                    : t("rbac.users.enableSuccess")
            )
            await fetchUsers()
        } catch (error) {
            ElMessage.error(t("rbac.users.updateError"))
        }
    }

    const handleBatchDelete = async (users: User[]) => {
        try {
            await Promise.all(users.map(user => rbacStore.deleteUser(user.id)))
            ElMessage.success(t("rbac.users.batchDeleteSuccess"))
            await fetchUsers()
        } catch (error) {
            ElMessage.error(t("rbac.users.batchDeleteError"))
        }
    }

    const handleUserSave = async (userData: Partial<User>) => {
        try {
            if (dialogMode.value === "create") {
                await rbacStore.createUser(userData)
                ElMessage.success(t("rbac.users.createSuccess"))
            } else {
                await rbacStore.updateUser(selectedUser.value!.id, userData)
                ElMessage.success(t("rbac.users.updateSuccess"))
            }

            showDialog.value = false
            await fetchUsers()
        } catch (error) {
            ElMessage.error(
                dialogMode.value === "create"
                    ? t("rbac.users.createError")
                    : t("rbac.users.updateError")
            )
        }
    }

    const handleRoleAssignment = async (roleIds: string[]) => {
        try {
            await rbacStore.assignUserRoles(selectedUser.value!.id, roleIds)
            ElMessage.success(t("rbac.users.roleAssignSuccess"))
            showRoleDialog.value = false
            await fetchUsers()
        } catch (error) {
            ElMessage.error(t("rbac.users.roleAssignError"))
        }
    }

    // 生命周期
    onMounted(() => {
        fetchUsers()
    })
</script>

<script lang="ts">
    export default {
        name: "UserManagement"
    }
</script>

<style scoped>
.user-management {
  padding: 24px;
  background: var(--el-bg-color-page);
  min-height: 100vh;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 24px;
  padding: 24px;
  background: var(--el-bg-color);
  border-radius: var(--el-border-radius-base);
  box-shadow: var(--el-box-shadow-light);
}

.header-content h1 {
  margin: 0 0 8px 0;
  font-size: 24px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.header-content p {
  margin: 0;
  color: var(--el-text-color-regular);
  font-size: 14px;
}

.user-roles {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.role-tag {
  margin: 0;
}

.table-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.text-muted {
  color: var(--el-text-color-placeholder);
  font-style: italic;
}

@media (max-width: 768px) {
  .user-management {
    padding: 16px;
  }

  .page-header {
    flex-direction: column;
    gap: 16px;
    align-items: stretch;
  }

  .table-actions {
    flex-direction: column;
  }

  .table-actions .el-button {
    width: 100%;
  }
}
</style>
