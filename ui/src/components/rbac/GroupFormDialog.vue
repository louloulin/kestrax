<template>
    <el-dialog
        v-model="visible"
        :title="dialogTitle"
        width="700px"
        :close-on-click-modal="false"
        @close="handleClose"
    >
        <ValidatedForm
            ref="formRef"
            :model="formData"
            :rules="formRules"
            label-width="120px"
            @submit="handleSubmit"
        >
            <el-row :gutter="16">
                <el-col :span="12">
                    <el-form-item
                        :label="$t('rbac.groups.name')"
                        prop="name"
                        required
                    >
                        <el-input
                            v-model="formData.name"
                            :placeholder="$t('rbac.groups.namePlaceholder')"
                            :disabled="mode === 'edit' && group?.systemGroup"
                        />
                    </el-form-item>
                </el-col>
                <el-col :span="12">
                    <el-form-item :label="$t('rbac.groups.type')">
                        <el-tag
                            :type="group?.systemGroup ? 'info' : 'primary'"
                            size="large"
                        >
                            {{ group?.systemGroup ? $t('rbac.groups.system') : $t('rbac.groups.custom') }}
                        </el-tag>
                    </el-form-item>
                </el-col>
            </el-row>

            <el-form-item
                :label="$t('rbac.groups.description')"
                prop="description"
            >
                <el-input
                    v-model="formData.description"
                    type="textarea"
                    :rows="3"
                    :placeholder="$t('rbac.groups.descriptionPlaceholder')"
                    :disabled="mode === 'edit' && group?.systemGroup"
                />
            </el-form-item>

            <el-form-item
                :label="$t('rbac.groups.roles')"
                prop="roles"
            >
                <div class="roles-container">
                    <el-select
                        v-model="formData.roles"
                        multiple
                        filterable
                        :placeholder="$t('rbac.groups.selectRoles')"
                        class="roles-select"
                        :disabled="mode === 'edit' && group?.systemGroup"
                    >
                        <el-option
                            v-for="role in availableRoles"
                            :key="role.id"
                            :label="role.name"
                            :value="role.id"
                        >
                            <div class="role-option">
                                <span class="role-name">{{ role.name }}</span>
                                <el-tag
                                    :type="role.systemRole ? 'info' : 'primary'"
                                    size="small"
                                >
                                    {{ role.systemRole ? $t('rbac.roles.system') : $t('rbac.roles.custom') }}
                                </el-tag>
                            </div>
                        </el-option>
                    </el-select>

                    <!-- Selected Roles Display -->
                    <div v-if="formData.roles.length > 0" class="selected-roles">
                        <div class="selected-roles-header">
                            <span>{{ $t('rbac.groups.selectedRoles') }}</span>
                            <el-tag type="info" size="small">
                                {{ formData.roles.length }} {{ $t('rbac.groups.rolesSelected') }}
                            </el-tag>
                        </div>
                        <div class="selected-roles-list">
                            <el-tag
                                v-for="roleId in formData.roles"
                                :key="roleId"
                                closable
                                @close="removeRole(roleId)"
                                class="selected-role-tag"
                            >
                                {{ getRoleName(roleId) }}
                            </el-tag>
                        </div>
                    </div>
                </div>
            </el-form-item>

            <el-row :gutter="16">
                <el-col :span="12">
                    <el-form-item :label="$t('rbac.groups.enabled')">
                        <el-switch
                            v-model="formData.enabled"
                            :disabled="mode === 'edit' && group?.systemGroup"
                        />
                    </el-form-item>
                </el-col>
                <el-col :span="12">
                    <el-form-item :label="$t('rbac.groups.autoAssign')">
                        <el-switch
                            v-model="formData.autoAssign"
                            :disabled="mode === 'edit' && group?.systemGroup"
                        />
                        <div class="form-help-text">
                            {{ $t('rbac.groups.autoAssignHelp') }}
                        </div>
                    </el-form-item>
                </el-col>
            </el-row>

            <!-- Group Attributes -->
            <el-form-item :label="$t('rbac.groups.attributes')">
                <div class="attributes-container">
                    <div
                        v-for="(attribute, index) in formData.attributes"
                        :key="index"
                        class="attribute-item"
                    >
                        <el-input
                            v-model="attribute.key"
                            :placeholder="$t('rbac.groups.attributeKey')"
                            class="attribute-key"
                        />
                        <el-input
                            v-model="attribute.value"
                            :placeholder="$t('rbac.groups.attributeValue')"
                            class="attribute-value"
                        />
                        <el-button
                            type="danger"
                            :icon="Delete"
                            size="small"
                            @click="removeAttribute(index)"
                        />
                    </div>
                    <el-button
                        type="primary"
                        :icon="Plus"
                        size="small"
                        @click="addAttribute"
                    >
                        {{ $t('rbac.groups.addAttribute') }}
                    </el-button>
                </div>
            </el-form-item>

            <!-- System Group Info -->
            <el-form-item v-if="mode === 'edit' && group?.systemGroup">
                <el-alert
                    :title="$t('rbac.groups.systemGroupInfo')"
                    type="info"
                    show-icon
                    :closable="false"
                />
            </el-form-item>
        </ValidatedForm>

        <template #footer>
            <div class="dialog-footer">
                <el-button @click="handleClose">
                    {{ $t('common.cancel') }}
                </el-button>
                <el-button
                    type="primary"
                    :loading="saving"
                    @click="handleSubmit"
                    :disabled="mode === 'edit' && group?.systemGroup"
                >
                    {{ mode === 'create' ? $t('common.create') : $t('common.update') }}
                </el-button>
            </div>
        </template>
    </el-dialog>
</template>

<script setup lang="ts">
    import {ref, computed, watch, nextTick} from "vue"
    import {useI18n} from "vue-i18n"
    import {ElMessage} from "element-plus"
    import {Plus, Delete} from "@element-plus/icons-vue"
    import {useRBACStore} from "@/stores/rbac"
    import ValidatedForm from "@/components/common/ValidatedForm.vue"
    import type {Group, Role} from "@/types/rbac"

    interface Props {
        modelValue: boolean
        group?: Group | null
        mode: 'create' | 'edit'
    }

    interface Emits {
        (e: 'update:modelValue', value: boolean): void
        (e: 'save'): void
    }

    const props = defineProps<Props>()
    const emit = defineEmits<Emits>()

    const {t} = useI18n()
    const rbacStore = useRBACStore()

    // State
    const formRef = ref()
    const saving = ref(false)
    const availableRoles = ref<Role[]>([])

    // Form data
    const formData = ref({
        name: '',
        description: '',
        roles: [] as string[],
        enabled: true,
        autoAssign: false,
        attributes: [] as Array<{key: string, value: string}>
    })

    // Computed
    const visible = computed({
        get: () => props.modelValue,
        set: (value) => emit('update:modelValue', value)
    })

    const dialogTitle = computed(() => {
        if (props.mode === 'create') {
            return t('rbac.groups.createGroup')
        }
        return t('rbac.groups.editGroup', {name: props.group?.name})
    })

    // Form validation rules
    const formRules = computed(() => ({
        name: [
            {
                required: true,
                message: t('rbac.groups.nameRequired'),
                trigger: 'blur'
            },
            {
                min: 2,
                max: 50,
                message: t('rbac.groups.nameLength'),
                trigger: 'blur'
            },
            {
                pattern: /^[a-zA-Z0-9_-]+$/,
                message: t('rbac.groups.namePattern'),
                trigger: 'blur'
            }
        ],
        description: [
            {
                max: 500,
                message: t('rbac.groups.descriptionLength'),
                trigger: 'blur'
            }
        ]
    }))

    // Methods
    const loadAvailableRoles = async () => {
        try {
            const response = await rbacStore.fetchRoles({includeSystem: true})
            availableRoles.value = response.data
        } catch (error) {
            console.error('Failed to load available roles:', error)
        }
    }

    const getRoleName = (roleId: string) => {
        const role = availableRoles.value.find(r => r.id === roleId)
        return role?.name || roleId
    }

    const removeRole = (roleId: string) => {
        const index = formData.value.roles.indexOf(roleId)
        if (index > -1) {
            formData.value.roles.splice(index, 1)
        }
    }

    const addAttribute = () => {
        formData.value.attributes.push({key: '', value: ''})
    }

    const removeAttribute = (index: number) => {
        formData.value.attributes.splice(index, 1)
    }

    const resetForm = () => {
        formData.value = {
            name: '',
            description: '',
            roles: [],
            enabled: true,
            autoAssign: false,
            attributes: []
        }
        nextTick(() => {
            formRef.value?.clearValidate()
        })
    }

    const populateForm = () => {
        if (props.group) {
            formData.value = {
                name: props.group.name,
                description: props.group.description || '',
                roles: [...(props.group.roles || [])],
                enabled: props.group.enabled ?? true,
                autoAssign: props.group.autoAssign ?? false,
                attributes: props.group.attributes ?
                    Object.entries(props.group.attributes).map(([key, value]) => ({key, value})) :
                    []
            }
        } else {
            resetForm()
        }
    }

    const handleSubmit = async () => {
        try {
            await formRef.value?.validate()
            saving.value = true

            // Convert attributes array to object
            const attributes = formData.value.attributes.reduce((acc, attr) => {
                if (attr.key && attr.value) {
                    acc[attr.key] = attr.value
                }
                return acc
            }, {} as Record<string, string>)

            const groupData = {
                ...formData.value,
                attributes
            }

            if (props.mode === 'create') {
                await rbacStore.createGroup(groupData)
                ElMessage.success(t('rbac.groups.createSuccess'))
            } else if (props.group) {
                await rbacStore.updateGroup(props.group.id, groupData)
                ElMessage.success(t('rbac.groups.updateSuccess'))
            }

            emit('save')
        } catch (error) {
            if (error !== false) { // Not validation error
                ElMessage.error(
                    props.mode === 'create'
                        ? t('rbac.groups.createError')
                        : t('rbac.groups.updateError')
                )
            }
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
            loadAvailableRoles()
            populateForm()
        }
    })

    watch(() => props.group, () => {
        if (props.modelValue) {
            populateForm()
        }
    })
</script>

<style scoped>
.roles-container {
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  padding: 1rem;
  background: #fafafa;
}

.roles-select {
  width: 100%;
  margin-bottom: 1rem;
}

.role-option {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}

.role-name {
  flex: 1;
  font-weight: 500;
}

.selected-roles {
  margin-top: 1rem;
}

.selected-roles-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 0.5rem;
  font-weight: 600;
  color: #303133;
}

.selected-roles-list {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
}

.selected-role-tag {
  margin: 0;
}

.attributes-container {
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  padding: 1rem;
  background: #fafafa;
}

.attribute-item {
  display: flex;
  gap: 0.5rem;
  margin-bottom: 0.5rem;
  align-items: center;
}

.attribute-key {
  flex: 1;
}

.attribute-value {
  flex: 2;
}

.form-help-text {
  font-size: 0.8rem;
  color: #909399;
  margin-top: 0.25rem;
  line-height: 1.3;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 0.5rem;
}

:deep(.el-form-item__label) {
  font-weight: 600;
}

:deep(.el-select .el-input__inner) {
  cursor: pointer;
}
</style>
