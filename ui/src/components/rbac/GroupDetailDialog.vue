<template>
    <el-dialog
        v-model="visible"
        :title="dialogTitle"
        width="900px"
        @close="handleClose"
    >
        <div v-if="group" class="group-detail">
            <!-- Basic Information -->
            <el-card class="detail-section" shadow="never">
                <template #header>
                    <div class="section-header">
                        <el-icon><InfoFilled /></el-icon>
                        <span>{{ $t('rbac.groups.basicInfo') }}</span>
                    </div>
                </template>

                <el-descriptions :column="2" border>
                    <el-descriptions-item :label="$t('rbac.groups.name')">
                        <el-tag :type="group.systemGroup ? 'info' : 'primary'" size="large">
                            {{ group.name }}
                        </el-tag>
                    </el-descriptions-item>
                    <el-descriptions-item :label="$t('rbac.groups.type')">
                        <el-tag :type="group.systemGroup ? 'info' : 'primary'">
                            {{ group.systemGroup ? $t('rbac.groups.system') : $t('rbac.groups.custom') }}
                        </el-tag>
                    </el-descriptions-item>
                    <el-descriptions-item :label="$t('rbac.groups.description')" :span="2">
                        {{ group.description || $t('rbac.groups.noDescription') }}
                    </el-descriptions-item>
                    <el-descriptions-item :label="$t('common.status')">
                        <el-tag :type="group.enabled ? 'success' : 'danger'">
                            {{ group.enabled ? $t('common.enabled') : $t('common.disabled') }}
                        </el-tag>
                    </el-descriptions-item>
                    <el-descriptions-item :label="$t('rbac.groups.autoAssign')">
                        <el-tag :type="group.autoAssign ? 'success' : 'info'">
                            {{ group.autoAssign ? $t('common.yes') : $t('common.no') }}
                        </el-tag>
                    </el-descriptions-item>
                    <el-descriptions-item :label="$t('rbac.groups.createdAt')">
                        {{ formatDate(group.createdAt) }}
                    </el-descriptions-item>
                    <el-descriptions-item :label="$t('rbac.groups.updatedAt')">
                        {{ formatDate(group.updatedAt) }}
                    </el-descriptions-item>
                </el-descriptions>
            </el-card>

            <!-- Roles -->
            <el-card class="detail-section" shadow="never">
                <template #header>
                    <div class="section-header">
                        <el-icon><UserFilled /></el-icon>
                        <span>{{ $t('rbac.groups.roles') }}</span>
                        <el-tag type="info" class="role-count">
                            {{ group.roles?.length || 0 }} {{ $t('rbac.groups.rolesCount') }}
                        </el-tag>
                    </div>
                </template>

                <div v-if="group.roles && group.roles.length > 0" class="roles-content">
                    <div class="roles-grid">
                        <div
                            v-for="role in groupRoles"
                            :key="role.id"
                            class="role-card"
                        >
                            <div class="role-header">
                                <el-icon class="role-icon"><Lock /></el-icon>
                                <span class="role-name">{{ role.name }}</span>
                                <el-tag
                                    :type="role.systemRole ? 'info' : 'primary'"
                                    size="small"
                                >
                                    {{ role.systemRole ? $t('rbac.roles.system') : $t('rbac.roles.custom') }}
                                </el-tag>
                            </div>
                            <div class="role-description">
                                {{ role.description || $t('rbac.roles.noDescription') }}
                            </div>
                            <div class="role-permissions">
                                <span class="permissions-label">{{ $t('rbac.roles.permissions') }}:</span>
                                <el-tag type="info" size="small">
                                    {{ role.permissions?.length || 0 }}
                                </el-tag>
                            </div>
                        </div>
                    </div>
                </div>
                <div v-else class="no-roles">
                    <el-empty
                        :description="$t('rbac.groups.noRoles')"
                        :image-size="80"
                    />
                </div>
            </el-card>

            <!-- Members -->
            <el-card class="detail-section" shadow="never">
                <template #header>
                    <div class="section-header">
                        <el-icon><User /></el-icon>
                        <span>{{ $t('rbac.groups.members') }}</span>
                        <el-tag type="info" class="member-count">
                            {{ groupMembers.length }} {{ $t('rbac.groups.membersCount') }}
                        </el-tag>
                        <el-button
                            size="small"
                            :icon="Refresh"
                            @click="loadGroupMembers"
                            :loading="loadingMembers"
                        >
                            {{ $t('common.refresh') }}
                        </el-button>
                        <PermissionGuard permission="GROUP_UPDATE" v-if="!group.systemGroup">
                            <el-button
                                size="small"
                                type="primary"
                                :icon="Edit"
                                @click="manageMembers"
                            >
                                {{ $t('rbac.groups.manageMembers') }}
                            </el-button>
                        </PermissionGuard>
                    </div>
                </template>

                <div v-if="groupMembers.length > 0" class="members-content">
                    <div class="members-grid">
                        <div
                            v-for="member in groupMembers"
                            :key="member.id"
                            class="member-card"
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
                                <div class="member-status">
                                    <el-tag
                                        :type="member.enabled ? 'success' : 'danger'"
                                        size="small"
                                    >
                                        {{ member.enabled ? $t('common.enabled') : $t('common.disabled') }}
                                    </el-tag>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
                <div v-else-if="!loadingMembers" class="no-members">
                    <el-empty
                        :description="$t('rbac.groups.noMembers')"
                        :image-size="80"
                    />
                </div>
                <div v-else class="loading-members">
                    <el-skeleton :rows="3" animated />
                </div>
            </el-card>

            <!-- Attributes -->
            <el-card v-if="group.attributes && Object.keys(group.attributes).length > 0" class="detail-section" shadow="never">
                <template #header>
                    <div class="section-header">
                        <el-icon><Setting /></el-icon>
                        <span>{{ $t('rbac.groups.attributes') }}</span>
                    </div>
                </template>

                <div class="attributes-content">
                    <el-descriptions border>
                        <el-descriptions-item
                            v-for="[key, value] in Object.entries(group.attributes)"
                            :key="key"
                            :label="key"
                        >
                            {{ value }}
                        </el-descriptions-item>
                    </el-descriptions>
                </div>
            </el-card>

            <!-- Statistics -->
            <el-card class="detail-section" shadow="never">
                <template #header>
                    <div class="section-header">
                        <el-icon><DataAnalysis /></el-icon>
                        <span>{{ $t('rbac.groups.statistics') }}</span>
                    </div>
                </template>

                <el-row :gutter="16">
                    <el-col :span="6">
                        <el-statistic
                            :title="$t('rbac.groups.totalRoles')"
                            :value="group.roles?.length || 0"
                        />
                    </el-col>
                    <el-col :span="6">
                        <el-statistic
                            :title="$t('rbac.groups.totalMembers')"
                            :value="groupMembers.length"
                        />
                    </el-col>
                    <el-col :span="6">
                        <el-statistic
                            :title="$t('rbac.groups.totalPermissions')"
                            :value="getTotalPermissions()"
                        />
                    </el-col>
                    <el-col :span="6">
                        <el-statistic
                            :title="$t('rbac.groups.groupAge')"
                            :value="getGroupAge()"
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
                <PermissionGuard permission="GROUP_UPDATE" v-if="group && !group.systemGroup">
                    <el-button type="primary" @click="editGroup">
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
        UserFilled,
        User,
        Lock,
        Setting,
        DataAnalysis,
        Edit,
        Refresh
    } from "@element-plus/icons-vue"
    import {useRBACStore} from "@/stores/rbac"
    import PermissionGuard from "./PermissionGuard.vue"
    import {formatDate} from "@/utils/date"
    import type {Group, User as UserType, Role} from "@/types/rbac"

    interface Props {
        modelValue: boolean
        group?: Group | null
    }

    interface Emits {
        (e: 'update:modelValue', value: boolean): void
        (e: 'edit', group: Group): void
        (e: 'manageMembers', group: Group): void
    }

    const props = defineProps<Props>()
    const emit = defineEmits<Emits>()

    const {t} = useI18n()
    const rbacStore = useRBACStore()

    // State
    const groupMembers = ref<UserType[]>([])
    const groupRoles = ref<Role[]>([])
    const loadingMembers = ref(false)

    // Computed
    const visible = computed({
        get: () => props.modelValue,
        set: (value) => emit('update:modelValue', value)
    })

    const dialogTitle = computed(() => {
        return props.group ? t('rbac.groups.groupDetails', {name: props.group.name}) : ''
    })

    // Methods
    const loadGroupMembers = async () => {
        if (!props.group) return

        loadingMembers.value = true
        try {
            groupMembers.value = await rbacStore.fetchGroupMembers(props.group.id)
        } catch (error) {
            ElMessage.error(t('rbac.groups.loadMembersError'))
        } finally {
            loadingMembers.value = false
        }
    }

    const loadGroupRoles = async () => {
        if (!props.group?.roles) return

        try {
            const roles = await Promise.all(
                props.group.roles.map(roleId => rbacStore.fetchRole(roleId))
            )
            groupRoles.value = roles.filter(Boolean)
        } catch (error) {
            console.error('Failed to load group roles:', error)
        }
    }

    const getTotalPermissions = () => {
        return groupRoles.value.reduce((total, role) => {
            return total + (role.permissions?.length || 0)
        }, 0)
    }

    const getGroupAge = () => {
        if (!props.group?.createdAt) return 0
        const created = new Date(props.group.createdAt)
        const now = new Date()
        const diffTime = Math.abs(now.getTime() - created.getTime())
        return Math.ceil(diffTime / (1000 * 60 * 60 * 24))
    }

    const editGroup = () => {
        if (props.group) {
            emit('edit', props.group)
            handleClose()
        }
    }

    const manageMembers = () => {
        if (props.group) {
            emit('manageMembers', props.group)
        }
    }

    const handleClose = () => {
        visible.value = false
    }

    // Watchers
    watch(() => props.modelValue, (newValue) => {
        if (newValue && props.group) {
            loadGroupMembers()
            loadGroupRoles()
        }
    })

    watch(() => props.group, () => {
        if (props.modelValue && props.group) {
            loadGroupMembers()
            loadGroupRoles()
        }
    })
</script>

<style scoped>
.group-detail {
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

.role-count,
.member-count {
  margin-left: auto;
}

.roles-content {
  padding: 1rem;
}

.roles-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 1rem;
}

.role-card {
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  padding: 0.75rem;
  background: #fafafa;
  transition: all 0.2s ease;
}

.role-card:hover {
  border-color: #409eff;
  background: #f0f9ff;
}

.role-header {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin-bottom: 0.5rem;
}

.role-icon {
  color: #409eff;
  font-size: 1rem;
}

.role-name {
  font-weight: 600;
  color: #303133;
  font-size: 0.9rem;
  flex: 1;
}

.role-description {
  color: #606266;
  font-size: 0.8rem;
  line-height: 1.3;
  margin-bottom: 0.5rem;
}

.role-permissions {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.permissions-label {
  font-size: 0.8rem;
  color: #909399;
}

.no-roles,
.no-members {
  padding: 2rem;
  text-align: center;
}

.members-content {
  padding: 1rem;
}

.members-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(250px, 1fr));
  gap: 1rem;
}

.member-card {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.75rem;
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  background: #fafafa;
  transition: all 0.2s ease;
}

.member-card:hover {
  border-color: #409eff;
  background: #f0f9ff;
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
  font-size: 0.9rem;
  margin-bottom: 0.25rem;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.member-email {
  color: #606266;
  font-size: 0.8rem;
  margin-bottom: 0.25rem;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.member-status {
  display: flex;
  justify-content: flex-start;
}

.loading-members {
  padding: 1rem;
}

.attributes-content {
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
