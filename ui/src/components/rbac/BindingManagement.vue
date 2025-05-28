<template>
    <div class="binding-management">
        <PermissionGuard permission="BINDING_READ">
            <!-- Header -->
            <div class="binding-header">
                <h2 class="binding-title">
                    <el-icon><Link /></el-icon>
                    {{ $t('rbac.bindings.title') }}
                </h2>
                <div class="binding-actions">
                    <PermissionGuard permission="BINDING_CREATE">
                        <el-button
                            type="primary"
                            :icon="Plus"
                            @click="createBinding"
                        >
                            {{ $t('rbac.bindings.create') }}
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
            <div class="binding-filters">
                <el-row :gutter="16">
                    <el-col :span="6">
                        <el-input
                            v-model="searchQuery"
                            :placeholder="$t('rbac.bindings.search')"
                            :prefix-icon="Search"
                            clearable
                            @input="handleSearch"
                        />
                    </el-col>
                    <el-col :span="5">
                        <el-select
                            v-model="filterScope"
                            :placeholder="$t('rbac.bindings.filterScope')"
                            clearable
                            @change="handleFilter"
                        >
                            <el-option
                                :label="$t('rbac.bindings.globalScope')"
                                value="global"
                            />
                            <el-option
                                :label="$t('rbac.bindings.namespaceScope')"
                                value="namespace"
                            />
                        </el-select>
                    </el-col>
                    <el-col :span="5">
                        <el-select
                            v-model="filterSubjectType"
                            :placeholder="$t('rbac.bindings.filterSubjectType')"
                            clearable
                            @change="handleFilter"
                        >
                            <el-option
                                :label="$t('rbac.bindings.userSubject')"
                                value="user"
                            />
                            <el-option
                                :label="$t('rbac.bindings.groupSubject')"
                                value="group"
                            />
                        </el-select>
                    </el-col>
                    <el-col :span="5">
                        <el-select
                            v-model="filterNamespace"
                            :placeholder="$t('rbac.bindings.filterNamespace')"
                            clearable
                            @change="handleFilter"
                        >
                            <el-option
                                v-for="namespace in availableNamespaces"
                                :key="namespace"
                                :label="namespace"
                                :value="namespace"
                            />
                        </el-select>
                    </el-col>
                    <el-col :span="3">
                        <el-button @click="clearFilters">
                            {{ $t('common.clearFilters') }}
                        </el-button>
                    </el-col>
                </el-row>
            </div>

            <!-- Bindings Table -->
            <EnterpriseDataTable
                :data="filteredBindings"
                :loading="loading"
                :total="total"
                :page-size="pageSize"
                :current-page="currentPage"
                @page-change="handlePageChange"
                @size-change="handleSizeChange"
                class="bindings-table"
            >
                <el-table-column
                    :label="$t('rbac.bindings.name')"
                    prop="name"
                    min-width="200"
                    show-overflow-tooltip
                >
                    <template #default="{ row }">
                        <div class="binding-name-cell">
                            <el-tag
                                :type="row.systemBinding ? 'info' : 'primary'"
                                size="small"
                                class="binding-type-tag"
                            >
                                {{ row.systemBinding ? $t('rbac.bindings.system') : $t('rbac.bindings.custom') }}
                            </el-tag>
                            <span class="binding-name">{{ row.name }}</span>
                        </div>
                    </template>
                </el-table-column>

                <el-table-column
                    :label="$t('rbac.bindings.scope')"
                    prop="scope"
                    width="120"
                    align="center"
                >
                    <template #default="{ row }">
                        <el-tag
                            :type="row.scope === 'global' ? 'success' : 'warning'"
                            size="small"
                        >
                            {{ row.scope === 'global' ? $t('rbac.bindings.global') : $t('rbac.bindings.namespace') }}
                        </el-tag>
                    </template>
                </el-table-column>

                <el-table-column
                    :label="$t('rbac.bindings.namespace')"
                    prop="namespace"
                    width="150"
                    align="center"
                >
                    <template #default="{ row }">
                        <span v-if="row.namespace">{{ row.namespace }}</span>
                        <el-tag v-else type="info" size="small">{{ $t('rbac.bindings.allNamespaces') }}</el-tag>
                    </template>
                </el-table-column>

                <el-table-column
                    :label="$t('rbac.bindings.role')"
                    prop="roleName"
                    min-width="150"
                    show-overflow-tooltip
                >
                    <template #default="{ row }">
                        <el-tag type="primary" size="small">{{ row.roleName }}</el-tag>
                    </template>
                </el-table-column>

                <el-table-column
                    :label="$t('rbac.bindings.subjects')"
                    prop="subjects"
                    min-width="250"
                >
                    <template #default="{ row }">
                        <div class="binding-subjects">
                            <div
                                v-for="subject in (row.subjects || []).slice(0, 2)"
                                :key="`${subject.type}-${subject.name}`"
                                class="subject-item"
                            >
                                <el-icon v-if="subject.type === 'user'"><User /></el-icon>
                                <el-icon v-else><UserFilled /></el-icon>
                                <span class="subject-name">{{ subject.name }}</span>
                                <el-tag
                                    :type="subject.type === 'user' ? 'primary' : 'success'"
                                    size="small"
                                >
                                    {{ subject.type }}
                                </el-tag>
                            </div>
                            <el-tag
                                v-if="(row.subjects || []).length > 2"
                                size="small"
                                type="info"
                                class="more-subjects"
                            >
                                +{{ (row.subjects || []).length - 2 }} {{ $t('common.more') }}
                            </el-tag>
                        </div>
                    </template>
                </el-table-column>

                <el-table-column
                    :label="$t('rbac.bindings.createdAt')"
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
                            <PermissionGuard permission="BINDING_READ">
                                <el-button
                                    size="small"
                                    :icon="View"
                                    @click="viewBinding(row)"
                                >
                                    {{ $t('common.view') }}
                                </el-button>
                            </PermissionGuard>
                            <PermissionGuard permission="BINDING_UPDATE" v-if="!row.systemBinding">
                                <el-button
                                    size="small"
                                    type="primary"
                                    :icon="Edit"
                                    @click="editBinding(row)"
                                >
                                    {{ $t('common.edit') }}
                                </el-button>
                            </PermissionGuard>
                            <PermissionGuard permission="BINDING_DELETE" v-if="!row.systemBinding">
                                <el-button
                                    size="small"
                                    type="danger"
                                    :icon="Delete"
                                    @click="deleteBinding(row)"
                                >
                                    {{ $t('common.delete') }}
                                </el-button>
                            </PermissionGuard>
                        </div>
                    </template>
                </el-table-column>
            </EnterpriseDataTable>

            <!-- Binding Form Dialog -->
            <BindingFormDialog
                v-model="showDialog"
                :binding="selectedBinding"
                :mode="dialogMode"
                @save="handleBindingSave"
            />

            <!-- Binding Detail Dialog -->
            <BindingDetailDialog
                v-model="showDetailDialog"
                :binding="selectedBinding"
                @edit="editBinding"
            />

            <!-- Fallback for no permission -->
            <template #fallback>
                <AccessDeniedMessage permission="BINDING_READ" />
            </template>
        </PermissionGuard>
    </div>
</template>

<script setup lang="ts">
    import {ref, computed, onMounted} from "vue"
    import {useI18n} from "vue-i18n"
    import {ElMessage, ElMessageBox} from "element-plus"
    import {
        Link,
        Plus,
        Edit,
        Delete,
        View,
        Search,
        Refresh,
        User,
        UserFilled
    } from "@element-plus/icons-vue"
    import {useRBACStore} from "@/stores/rbac"
    import PermissionGuard from "./PermissionGuard.vue"
    import AccessDeniedMessage from "./AccessDeniedMessage.vue"
    import EnterpriseDataTable from "@/components/common/EnterpriseDataTable.vue"
    import BindingFormDialog from "./BindingFormDialog.vue"
    import BindingDetailDialog from "./BindingDetailDialog.vue"
    import {formatDate} from "@/utils/date"
    import type {Binding} from "@/types/rbac"

    const {t} = useI18n()
    const rbacStore = useRBACStore()

    // State
    const loading = ref(false)
    const bindings = ref<Binding[]>([])
    const selectedBinding = ref<Binding | null>(null)
    const showDialog = ref(false)
    const showDetailDialog = ref(false)
    const dialogMode = ref<'create' | 'edit'>('create')
    const availableNamespaces = ref<string[]>([])

    // Pagination
    const currentPage = ref(1)
    const pageSize = ref(20)
    const total = ref(0)

    // Filters
    const searchQuery = ref('')
    const filterScope = ref('')
    const filterSubjectType = ref('')
    const filterNamespace = ref('')

    // Computed
    const filteredBindings = computed(() => {
        let filtered = bindings.value

        // Search filter
        if (searchQuery.value) {
            const query = searchQuery.value.toLowerCase()
            filtered = filtered.filter(binding =>
                binding.name.toLowerCase().includes(query) ||
                binding.roleName.toLowerCase().includes(query) ||
                binding.namespace?.toLowerCase().includes(query) ||
                binding.subjects?.some(subject =>
                    subject.name.toLowerCase().includes(query)
                )
            )
        }

        // Scope filter
        if (filterScope.value) {
            filtered = filtered.filter(binding => binding.scope === filterScope.value)
        }

        // Subject type filter
        if (filterSubjectType.value) {
            filtered = filtered.filter(binding =>
                binding.subjects?.some(subject => subject.type === filterSubjectType.value)
            )
        }

        // Namespace filter
        if (filterNamespace.value) {
            filtered = filtered.filter(binding => binding.namespace === filterNamespace.value)
        }

        return filtered
    })

    // Methods
    const loadBindings = async () => {
        loading.value = true
        try {
            const response = await rbacStore.fetchBindings({
                page: currentPage.value,
                size: pageSize.value,
                query: searchQuery.value,
                includeSystem: true
            })
            bindings.value = response.data
            total.value = response.total
        } catch (error) {
            ElMessage.error(t('rbac.bindings.loadError'))
        } finally {
            loading.value = false
        }
    }

    const loadNamespaces = async () => {
        try {
            availableNamespaces.value = await rbacStore.fetchAvailableNamespaces()
        } catch (error) {
            console.error('Failed to load namespaces:', error)
        }
    }

    const createBinding = () => {
        selectedBinding.value = null
        dialogMode.value = 'create'
        showDialog.value = true
    }

    const editBinding = (binding: Binding) => {
        selectedBinding.value = binding
        dialogMode.value = 'edit'
        showDialog.value = true
    }

    const viewBinding = (binding: Binding) => {
        selectedBinding.value = binding
        showDetailDialog.value = true
    }

    const deleteBinding = async (binding: Binding) => {
        try {
            await ElMessageBox.confirm(
                t('rbac.bindings.deleteConfirm', {name: binding.name}),
                t('common.warning'),
                {
                    confirmButtonText: t('common.confirm'),
                    cancelButtonText: t('common.cancel'),
                    type: 'warning'
                }
            )

            await rbacStore.deleteBinding(binding.id)
            ElMessage.success(t('rbac.bindings.deleteSuccess'))
            await loadBindings()
        } catch (error) {
            if (error !== 'cancel') {
                ElMessage.error(t('rbac.bindings.deleteError'))
            }
        }
    }

    const handleBindingSave = async () => {
        await loadBindings()
        showDialog.value = false
    }

    const handleRefresh = () => {
        loadBindings()
    }

    const handleSearch = () => {
        currentPage.value = 1
        loadBindings()
    }

    const handleFilter = () => {
        currentPage.value = 1
        loadBindings()
    }

    const clearFilters = () => {
        searchQuery.value = ''
        filterScope.value = ''
        filterSubjectType.value = ''
        filterNamespace.value = ''
        currentPage.value = 1
        loadBindings()
    }

    const handlePageChange = (page: number) => {
        currentPage.value = page
        loadBindings()
    }

    const handleSizeChange = (size: number) => {
        pageSize.value = size
        currentPage.value = 1
        loadBindings()
    }

    // Lifecycle
    onMounted(() => {
        loadBindings()
        loadNamespaces()
    })
</script>

<style scoped>
.binding-management {
  padding: 1.5rem;
}

.binding-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1.5rem;
  padding-bottom: 1rem;
  border-bottom: 1px solid #e4e7ed;
}

.binding-title {
  margin: 0;
  color: #303133;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.binding-actions {
  display: flex;
  gap: 0.5rem;
}

.binding-filters {
  margin-bottom: 1.5rem;
  padding: 1rem;
  background: #f8f9fa;
  border-radius: 8px;
}

.bindings-table {
  margin-bottom: 2rem;
}

.binding-name-cell {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.binding-type-tag {
  flex-shrink: 0;
}

.binding-name {
  font-weight: 600;
  color: #303133;
}

.binding-subjects {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.subject-item {
  display: flex;
  align-items: center;
  gap: 0.25rem;
  font-size: 0.85rem;
}

.subject-name {
  font-weight: 500;
  color: #303133;
}

.more-subjects {
  cursor: pointer;
  align-self: flex-start;
}

.action-buttons {
  display: flex;
  gap: 0.25rem;
  flex-wrap: wrap;
  justify-content: center;
}
</style>
