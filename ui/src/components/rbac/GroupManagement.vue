<template>
    <div class="group-management">
        <PermissionGuard permission="GROUP_READ">
            <!-- Header -->
            <div class="group-header">
                <h2 class="group-title">
                    <el-icon><UserFilled /></el-icon>
                    {{ $t('rbac.groups.title') }}
                </h2>
                <div class="group-actions">
                    <PermissionGuard permission="GROUP_CREATE">
                        <el-button
                            type="primary"
                            :icon="Plus"
                            @click="createGroup"
                        >
                            {{ $t('rbac.groups.create') }}
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
            <div class="group-filters">
                <el-row :gutter="16">
                    <el-col :span="8">
                        <el-input
                            v-model="searchQuery"
                            :placeholder="$t('rbac.groups.search')"
                            :prefix-icon="Search"
                            clearable
                            @input="handleSearch"
                        />
                    </el-col>
                    <el-col :span="6">
                        <el-select
                            v-model="filterType"
                            :placeholder="$t('rbac.groups.filterType')"
                            clearable
                            @change="handleFilter"
                        >
                            <el-option
                                :label="$t('rbac.groups.allGroups')"
                                value=""
                            />
                            <el-option
                                :label="$t('rbac.groups.systemGroups')"
                                value="system"
                            />
                            <el-option
                                :label="$t('rbac.groups.customGroups')"
                                value="custom"
                            />
                        </el-select>
                    </el-col>
                    <el-col :span="6">
                        <el-select
                            v-model="filterStatus"
                            :placeholder="$t('rbac.groups.filterStatus')"
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
                    <el-col :span="4">
                        <el-button @click="clearFilters">
                            {{ $t('common.clearFilters') }}
                        </el-button>
                    </el-col>
                </el-row>
            </div>

            <!-- Groups Table -->
            <EnterpriseDataTable
                :data="filteredGroups"
                :loading="loading"
                :total="total"
                :page-size="pageSize"
                :current-page="currentPage"
                @page-change="handlePageChange"
                @size-change="handleSizeChange"
                class="groups-table"
            >
                <el-table-column
                    :label="$t('rbac.groups.name')"
                    prop="name"
                    min-width="200"
                    show-overflow-tooltip
                >
                    <template #default="{ row }">
                        <div class="group-name-cell">
                            <el-tag
                                :type="row.systemGroup ? 'info' : 'primary'"
                                size="small"
                                class="group-type-tag"
                            >
                                {{ row.systemGroup ? $t('rbac.groups.system') : $t('rbac.groups.custom') }}
                            </el-tag>
                            <span class="group-name">{{ row.name }}</span>
                        </div>
                    </template>
                </el-table-column>

                <el-table-column
                    :label="$t('rbac.groups.description')"
                    prop="description"
                    min-width="250"
                    show-overflow-tooltip
                >
                    <template #default="{ row }">
                        {{ row.description || $t('rbac.groups.noDescription') }}
                    </template>
                </el-table-column>

                <el-table-column
                    :label="$t('rbac.groups.memberCount')"
                    prop="memberCount"
                    width="120"
                    align="center"
                >
                    <template #default="{ row }">
                        <el-tag type="info" size="small">
                            {{ row.memberCount || 0 }}
                        </el-tag>
                    </template>
                </el-table-column>

                <el-table-column
                    :label="$t('rbac.groups.roles')"
                    prop="roles"
                    min-width="200"
                >
                    <template #default="{ row }">
                        <div class="group-roles">
                            <el-tag
                                v-for="role in (row.roles || []).slice(0, 2)"
                                :key="role"
                                size="small"
                                class="role-tag"
                            >
                                {{ role }}
                            </el-tag>
                            <el-tag
                                v-if="(row.roles || []).length > 2"
                                size="small"
                                type="info"
                                class="more-roles"
                            >
                                +{{ (row.roles || []).length - 2 }} {{ $t('common.more') }}
                            </el-tag>
                        </div>
                    </template>
                </el-table-column>

                <el-table-column
                    :label="$t('common.status')"
                    prop="enabled"
                    width="100"
                    align="center"
                >
                    <template #default="{ row }">
                        <el-tag
                            :type="row.enabled ? 'success' : 'danger'"
                            size="small"
                        >
                            {{ row.enabled ? $t('common.enabled') : $t('common.disabled') }}
                        </el-tag>
                    </template>
                </el-table-column>

                <el-table-column
                    :label="$t('rbac.groups.createdAt')"
                    prop="createdAt"
                    width="150"
                    align="center"
                >
                    <template #default="{ row }">
                        {{ formatDate(row.createdAt) }}
                    </template>
                </el-table-column>

                <el-table-column
                    :label="$t('common.actions')"
                    width="200"
                    align="center"
                    fixed="right"
                >
                    <template #default="{ row }">
                        <div class="action-buttons">
                            <PermissionGuard permission="GROUP_READ">
                                <el-button
                                    size="small"
                                    :icon="View"
                                    @click="viewGroup(row)"
                                >
                                    {{ $t('common.view') }}
                                </el-button>
                            </PermissionGuard>
                            <PermissionGuard permission="GROUP_UPDATE" v-if="!row.systemGroup">
                                <el-button
                                    size="small"
                                    type="primary"
                                    :icon="Edit"
                                    @click="editGroup(row)"
                                >
                                    {{ $t('common.edit') }}
                                </el-button>
                            </PermissionGuard>
                            <PermissionGuard permission="GROUP_DELETE" v-if="!row.systemGroup">
                                <el-button
                                    size="small"
                                    type="danger"
                                    :icon="Delete"
                                    @click="deleteGroup(row)"
                                >
                                    {{ $t('common.delete') }}
                                </el-button>
                            </PermissionGuard>
                        </div>
                    </template>
                </el-table-column>
            </EnterpriseDataTable>

            <!-- Group Form Dialog -->
            <GroupFormDialog
                v-model="showDialog"
                :group="selectedGroup"
                :mode="dialogMode"
                @save="handleGroupSave"
            />

            <!-- Group Detail Dialog -->
            <GroupDetailDialog
                v-model="showDetailDialog"
                :group="selectedGroup"
                @edit="editGroup"
            />

            <!-- Group Member Management Dialog -->
            <GroupMemberDialog
                v-model="showMemberDialog"
                :group="selectedGroup"
                @update="handleMemberUpdate"
            />

            <!-- Fallback for no permission -->
            <template #fallback>
                <AccessDeniedMessage permission="GROUP_READ" />
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
    import EnterpriseDataTable from "@/components/common/EnterpriseDataTable.vue"
    import GroupFormDialog from "./GroupFormDialog.vue"
    import GroupDetailDialog from "./GroupDetailDialog.vue"
    import GroupMemberDialog from "./GroupMemberDialog.vue"
    import {formatDate} from "@/utils/date"
    import type {Group} from "@/types/rbac"

    const {t} = useI18n()
    const rbacStore = useRBACStore()

    // State
    const loading = ref(false)
    const groups = ref<Group[]>([])
    const selectedGroup = ref<Group | null>(null)
    const showDialog = ref(false)
    const showDetailDialog = ref(false)
    const showMemberDialog = ref(false)
    const dialogMode = ref<'create' | 'edit'>('create')

    // Pagination
    const currentPage = ref(1)
    const pageSize = ref(20)
    const total = ref(0)

    // Filters
    const searchQuery = ref('')
    const filterType = ref('')
    const filterStatus = ref('')

    // Computed
    const filteredGroups = computed(() => {
        let filtered = groups.value

        // Search filter
        if (searchQuery.value) {
            const query = searchQuery.value.toLowerCase()
            filtered = filtered.filter(group =>
                group.name.toLowerCase().includes(query) ||
                group.description?.toLowerCase().includes(query)
            )
        }

        // Type filter
        if (filterType.value) {
            filtered = filtered.filter(group => {
                if (filterType.value === 'system') return group.systemGroup
                if (filterType.value === 'custom') return !group.systemGroup
                return true
            })
        }

        // Status filter
        if (filterStatus.value) {
            filtered = filtered.filter(group => {
                if (filterStatus.value === 'enabled') return group.enabled
                if (filterStatus.value === 'disabled') return !group.enabled
                return true
            })
        }

        return filtered
    })

    // Methods
    const loadGroups = async () => {
        loading.value = true
        try {
            const response = await rbacStore.fetchGroups({
                page: currentPage.value,
                size: pageSize.value,
                query: searchQuery.value,
                includeSystem: true
            })
            groups.value = response.data
            total.value = response.total
        } catch (error) {
            ElMessage.error(t('rbac.groups.loadError'))
        } finally {
            loading.value = false
        }
    }

    const createGroup = () => {
        selectedGroup.value = null
        dialogMode.value = 'create'
        showDialog.value = true
    }

    const editGroup = (group: Group) => {
        selectedGroup.value = group
        dialogMode.value = 'edit'
        showDialog.value = true
    }

    const viewGroup = (group: Group) => {
        selectedGroup.value = group
        showDetailDialog.value = true
    }

    const deleteGroup = async (group: Group) => {
        try {
            await ElMessageBox.confirm(
                t('rbac.groups.deleteConfirm', {name: group.name}),
                t('common.warning'),
                {
                    confirmButtonText: t('common.confirm'),
                    cancelButtonText: t('common.cancel'),
                    type: 'warning'
                }
            )

            await rbacStore.deleteGroup(group.id)
            ElMessage.success(t('rbac.groups.deleteSuccess'))
            await loadGroups()
        } catch (error) {
            if (error !== 'cancel') {
                ElMessage.error(t('rbac.groups.deleteError'))
            }
        }
    }

    const handleGroupSave = async () => {
        await loadGroups()
        showDialog.value = false
    }

    const handleMemberUpdate = async () => {
        await loadGroups()
        showMemberDialog.value = false
    }

    const handleRefresh = () => {
        loadGroups()
    }

    const handleSearch = () => {
        currentPage.value = 1
        loadGroups()
    }

    const handleFilter = () => {
        currentPage.value = 1
        loadGroups()
    }

    const clearFilters = () => {
        searchQuery.value = ''
        filterType.value = ''
        filterStatus.value = ''
        currentPage.value = 1
        loadGroups()
    }

    const handlePageChange = (page: number) => {
        currentPage.value = page
        loadGroups()
    }

    const handleSizeChange = (size: number) => {
        pageSize.value = size
        currentPage.value = 1
        loadGroups()
    }

    // Lifecycle
    onMounted(() => {
        loadGroups()
    })
</script>

<style scoped>
.group-management {
  padding: 1.5rem;
}

.group-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1.5rem;
  padding-bottom: 1rem;
  border-bottom: 1px solid #e4e7ed;
}

.group-title {
  margin: 0;
  color: #303133;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.group-actions {
  display: flex;
  gap: 0.5rem;
}

.group-filters {
  margin-bottom: 1.5rem;
  padding: 1rem;
  background: #f8f9fa;
  border-radius: 8px;
}

.groups-table {
  margin-bottom: 2rem;
}

.group-name-cell {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.group-type-tag {
  flex-shrink: 0;
}

.group-name {
  font-weight: 600;
  color: #303133;
}

.group-roles {
  display: flex;
  flex-wrap: wrap;
  gap: 0.25rem;
}

.role-tag {
  font-size: 0.8rem;
}

.more-roles {
  cursor: pointer;
}

.action-buttons {
  display: flex;
  gap: 0.25rem;
  flex-wrap: wrap;
  justify-content: center;
}
</style>
