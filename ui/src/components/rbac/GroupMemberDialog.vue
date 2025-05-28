<template>
    <el-dialog
        v-model="visible"
        :title="dialogTitle"
        width="800px"
        :close-on-click-modal="false"
        @close="handleClose"
    >
        <div v-if="group" class="member-management">
            <!-- Search and Add Members -->
            <div class="member-actions">
                <el-row :gutter="16">
                    <el-col :span="16">
                        <el-input
                            v-model="searchQuery"
                            :placeholder="$t('rbac.groups.searchMembers')"
                            :prefix-icon="Search"
                            clearable
                            @input="handleSearch"
                        />
                    </el-col>
                    <el-col :span="8">
                        <PermissionGuard permission="GROUP_UPDATE">
                            <el-button
                                type="primary"
                                :icon="Plus"
                                @click="showAddMemberDialog"
                                :disabled="group.systemGroup"
                            >
                                {{ $t('rbac.groups.addMember') }}
                            </el-button>
                        </PermissionGuard>
                    </el-col>
                </el-row>
            </div>

            <!-- Current Members -->
            <div class="current-members">
                <div class="section-header">
                    <h3>{{ $t('rbac.groups.currentMembers') }}</h3>
                    <el-tag type="info">
                        {{ filteredMembers.length }} {{ $t('rbac.groups.membersCount') }}
                    </el-tag>
                </div>

                <div v-if="loading" class="loading-state">
                    <el-skeleton :rows="5" animated />
                </div>

                <div v-else-if="filteredMembers.length > 0" class="members-list">
                    <div
                        v-for="member in filteredMembers"
                        :key="member.id"
                        class="member-item"
                    >
                        <div class="member-avatar">
                            <el-avatar :size="40">
                                {{ member.firstName?.[0] || member.username[0] }}
                            </el-avatar>
                        </div>
                        <div class="member-info">
                            <div class="member-name">
                                {{ member.firstName && member.lastName
                                    ? `${member.firstName} ${member.lastName}`
                                    : member.username }}
                            </div>
                            <div class="member-email">{{ member.email }}</div>
                            <div class="member-roles">
                                <el-tag
                                    v-for="role in member.roles?.slice(0, 2)"
                                    :key="role"
                                    size="small"
                                    type="primary"
                                >
                                    {{ role }}
                                </el-tag>
                                <el-tag
                                    v-if="(member.roles?.length || 0) > 2"
                                    size="small"
                                    type="info"
                                >
                                    +{{ (member.roles?.length || 0) - 2 }} {{ $t('common.more') }}
                                </el-tag>
                            </div>
                        </div>
                        <div class="member-status">
                            <el-tag
                                :type="member.enabled ? 'success' : 'danger'"
                                size="small"
                            >
                                {{ member.enabled ? $t('common.enabled') : $t('common.disabled') }}
                            </el-tag>
                        </div>
                        <div class="member-actions">
                            <PermissionGuard permission="GROUP_UPDATE" v-if="!group.systemGroup">
                                <el-button
                                    size="small"
                                    type="danger"
                                    :icon="Delete"
                                    @click="removeMember(member)"
                                >
                                    {{ $t('rbac.groups.removeMember') }}
                                </el-button>
                            </PermissionGuard>
                        </div>
                    </div>
                </div>

                <div v-else class="no-members">
                    <el-empty
                        :description="$t('rbac.groups.noMembers')"
                        :image-size="100"
                    >
                        <PermissionGuard permission="GROUP_UPDATE" v-if="!group.systemGroup">
                            <el-button type="primary" @click="showAddMemberDialog">
                                {{ $t('rbac.groups.addFirstMember') }}
                            </el-button>
                        </PermissionGuard>
                    </el-empty>
                </div>
            </div>
        </div>

        <!-- Add Member Dialog -->
        <el-dialog
            v-model="showAddDialog"
            :title="$t('rbac.groups.addMemberToGroup')"
            width="600px"
            append-to-body
        >
            <div class="add-member-content">
                <el-input
                    v-model="userSearchQuery"
                    :placeholder="$t('rbac.groups.searchUsers')"
                    :prefix-icon="Search"
                    clearable
                    @input="searchUsers"
                    class="user-search"
                />

                <div v-if="searchingUsers" class="searching-state">
                    <el-skeleton :rows="3" animated />
                </div>

                <div v-else-if="availableUsers.length > 0" class="available-users">
                    <div
                        v-for="user in availableUsers"
                        :key="user.id"
                        class="user-item"
                        :class="{ selected: selectedUsers.includes(user.id) }"
                        @click="toggleUserSelection(user.id)"
                    >
                        <el-checkbox
                            :model-value="selectedUsers.includes(user.id)"
                            @change="toggleUserSelection(user.id)"
                        />
                        <div class="user-avatar">
                            <el-avatar :size="32">
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
                        </div>
                    </div>
                </div>

                <div v-else-if="userSearchQuery" class="no-users">
                    <el-empty
                        :description="$t('rbac.groups.noUsersFound')"
                        :image-size="80"
                    />
                </div>
            </div>

            <template #footer>
                <div class="add-dialog-footer">
                    <el-button @click="showAddDialog = false">
                        {{ $t('common.cancel') }}
                    </el-button>
                    <el-button
                        type="primary"
                        :loading="adding"
                        :disabled="selectedUsers.length === 0"
                        @click="addSelectedUsers"
                    >
                        {{ $t('rbac.groups.addSelected') }} ({{ selectedUsers.length }})
                    </el-button>
                </div>
            </template>
        </el-dialog>

        <template #footer>
            <div class="dialog-footer">
                <el-button @click="handleClose">
                    {{ $t('common.close') }}
                </el-button>
                <el-button
                    :icon="Refresh"
                    @click="loadMembers"
                    :loading="loading"
                >
                    {{ $t('common.refresh') }}
                </el-button>
            </div>
        </template>
    </el-dialog>
</template>

<script setup lang="ts">
    import {ref, computed, watch} from "vue"
    import {useI18n} from "vue-i18n"
    import {ElMessage, ElMessageBox} from "element-plus"
    import {
        Search,
        Plus,
        Delete,
        Refresh
    } from "@element-plus/icons-vue"
    import {useRBACStore} from "@/stores/rbac"
    import PermissionGuard from "./PermissionGuard.vue"
    import type {Group, User} from "@/types/rbac"

    interface Props {
        modelValue: boolean
        group?: Group | null
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
    const adding = ref(false)
    const searchingUsers = ref(false)
    const searchQuery = ref('')
    const userSearchQuery = ref('')
    const showAddDialog = ref(false)
    const members = ref<User[]>([])
    const availableUsers = ref<User[]>([])
    const selectedUsers = ref<string[]>([])

    // Computed
    const visible = computed({
        get: () => props.modelValue,
        set: (value) => emit('update:modelValue', value)
    })

    const dialogTitle = computed(() => {
        return props.group ? t('rbac.groups.manageGroupMembers', {name: props.group.name}) : ''
    })

    const filteredMembers = computed(() => {
        if (!searchQuery.value) return members.value

        const query = searchQuery.value.toLowerCase()
        return members.value.filter(member =>
            member.username.toLowerCase().includes(query) ||
            member.email.toLowerCase().includes(query) ||
            (member.firstName && member.firstName.toLowerCase().includes(query)) ||
            (member.lastName && member.lastName.toLowerCase().includes(query))
        )
    })

    // Methods
    const loadMembers = async () => {
        if (!props.group) return

        loading.value = true
        try {
            members.value = await rbacStore.fetchGroupMembers(props.group.id)
        } catch (error) {
            ElMessage.error(t('rbac.groups.loadMembersError'))
        } finally {
            loading.value = false
        }
    }

    const searchUsers = async () => {
        if (!userSearchQuery.value || userSearchQuery.value.length < 2) {
            availableUsers.value = []
            return
        }

        searchingUsers.value = true
        try {
            const response = await rbacStore.searchUsers({
                query: userSearchQuery.value,
                excludeGroupId: props.group?.id
            })
            availableUsers.value = response.data
        } catch (error) {
            console.error('Failed to search users:', error)
        } finally {
            searchingUsers.value = false
        }
    }

    const toggleUserSelection = (userId: string) => {
        const index = selectedUsers.value.indexOf(userId)
        if (index > -1) {
            selectedUsers.value.splice(index, 1)
        } else {
            selectedUsers.value.push(userId)
        }
    }

    const showAddMemberDialog = () => {
        selectedUsers.value = []
        userSearchQuery.value = ''
        availableUsers.value = []
        showAddDialog.value = true
    }

    const addSelectedUsers = async () => {
        if (!props.group || selectedUsers.value.length === 0) return

        adding.value = true
        try {
            await rbacStore.addGroupMembers(props.group.id, selectedUsers.value)
            ElMessage.success(
                t('rbac.groups.addMembersSuccess', {count: selectedUsers.value.length})
            )
            showAddDialog.value = false
            await loadMembers()
            emit('update')
        } catch (error) {
            ElMessage.error(t('rbac.groups.addMembersError'))
        } finally {
            adding.value = false
        }
    }

    const removeMember = async (member: User) => {
        if (!props.group) return

        try {
            await ElMessageBox.confirm(
                t('rbac.groups.removeMemberConfirm', {
                    member: member.firstName && member.lastName
                        ? `${member.firstName} ${member.lastName}`
                        : member.username,
                    group: props.group.name
                }),
                t('common.warning'),
                {
                    confirmButtonText: t('common.confirm'),
                    cancelButtonText: t('common.cancel'),
                    type: 'warning'
                }
            )

            await rbacStore.removeGroupMember(props.group.id, member.id)
            ElMessage.success(t('rbac.groups.removeMemberSuccess'))
            await loadMembers()
            emit('update')
        } catch (error) {
            if (error !== 'cancel') {
                ElMessage.error(t('rbac.groups.removeMemberError'))
            }
        }
    }

    const handleSearch = () => {
        // Filtering is handled by computed property
    }

    const handleClose = () => {
        visible.value = false
    }

    // Watchers
    watch(() => props.modelValue, (newValue) => {
        if (newValue && props.group) {
            loadMembers()
        }
    })

    watch(() => props.group, () => {
        if (props.modelValue && props.group) {
            loadMembers()
        }
    })
</script>

<style scoped>
.member-management {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.member-actions {
  padding: 1rem;
  background: #f8f9fa;
  border-radius: 8px;
  border: 1px solid #e4e7ed;
}

.current-members {
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  overflow: hidden;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1rem;
  background: #f8f9fa;
  border-bottom: 1px solid #e4e7ed;
}

.section-header h3 {
  margin: 0;
  color: #303133;
  font-size: 1rem;
}

.loading-state {
  padding: 1rem;
}

.members-list {
  max-height: 400px;
  overflow-y: auto;
}

.member-item {
  display: flex;
  align-items: center;
  gap: 1rem;
  padding: 1rem;
  border-bottom: 1px solid #f0f0f0;
  transition: background-color 0.2s ease;
}

.member-item:hover {
  background: #f8f9fa;
}

.member-item:last-child {
  border-bottom: none;
}

.member-avatar {
  flex-shrink: 0;
}

.member-info {
  flex: 1;
  min-width: 0;
}

.member-name {
  font-weight: 600;
  color: #303133;
  margin-bottom: 0.25rem;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.member-email {
  color: #606266;
  font-size: 0.9rem;
  margin-bottom: 0.25rem;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.member-roles {
  display: flex;
  flex-wrap: wrap;
  gap: 0.25rem;
}

.member-status {
  flex-shrink: 0;
}

.member-actions {
  flex-shrink: 0;
}

.no-members {
  padding: 2rem;
  text-align: center;
}

.add-member-content {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.user-search {
  margin-bottom: 1rem;
}

.searching-state {
  padding: 1rem;
}

.available-users {
  max-height: 300px;
  overflow-y: auto;
  border: 1px solid #e4e7ed;
  border-radius: 6px;
}

.user-item {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.75rem;
  border-bottom: 1px solid #f0f0f0;
  cursor: pointer;
  transition: all 0.2s ease;
}

.user-item:hover {
  background: #f8f9fa;
}

.user-item.selected {
  background: #e1f3d8;
  border-color: #b3d8a4;
}

.user-item:last-child {
  border-bottom: none;
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
  margin-bottom: 0.25rem;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.user-email {
  color: #606266;
  font-size: 0.9rem;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.no-users {
  padding: 2rem;
  text-align: center;
}

.add-dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 0.5rem;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 0.5rem;
}

:deep(.el-checkbox) {
  margin-right: 0;
}
</style>
