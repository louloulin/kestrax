<template>
    <el-dialog
        v-model="visible"
        :title="dialogTitle"
        width="800px"
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
                        :label="$t('rbac.roles.name')"
                        prop="name"
                        required
                    >
                        <el-input
                            v-model="formData.name"
                            :placeholder="$t('rbac.roles.namePlaceholder')"
                            :disabled="mode === 'edit' && role?.systemRole"
                        />
                    </el-form-item>
                </el-col>
                <el-col :span="12">
                    <el-form-item :label="$t('rbac.roles.type')">
                        <el-tag
                            :type="role?.systemRole ? 'info' : 'primary'"
                            size="large"
                        >
                            {{ role?.systemRole ? $t('rbac.roles.system') : $t('rbac.roles.custom') }}
                        </el-tag>
                    </el-form-item>
                </el-col>
            </el-row>

            <el-form-item
                :label="$t('rbac.roles.description')"
                prop="description"
            >
                <el-input
                    v-model="formData.description"
                    type="textarea"
                    :rows="3"
                    :placeholder="$t('rbac.roles.descriptionPlaceholder')"
                    :disabled="mode === 'edit' && role?.systemRole"
                />
            </el-form-item>

            <el-form-item
                :label="$t('rbac.roles.permissions')"
                prop="permissions"
                required
            >
                <div class="permissions-container">
                    <!-- Permission Categories -->
                    <div class="permission-categories">
                        <el-tabs v-model="activeCategory" type="border-card">
                            <el-tab-pane
                                v-for="category in permissionCategories"
                                :key="category.name"
                                :label="category.label"
                                :name="category.name"
                            >
                                <div class="category-permissions">
                                    <div class="category-header">
                                        <el-checkbox
                                            :model-value="isCategorySelected(category.name)"
                                            :indeterminate="isCategoryIndeterminate(category.name)"
                                            @change="toggleCategory(category.name, $event)"
                                        >
                                            {{ $t('rbac.permissions.selectAll') }}
                                        </el-checkbox>
                                        <span class="category-count">
                                            {{ getSelectedCategoryCount(category.name) }} / {{ category.permissions.length }}
                                        </span>
                                    </div>

                                    <el-checkbox-group
                                        v-model="formData.permissions"
                                        class="permission-grid"
                                    >
                                        <el-checkbox
                                            v-for="permission in category.permissions"
                                            :key="permission"
                                            :label="permission"
                                            :disabled="mode === 'edit' && role?.systemRole"
                                        >
                                            <div class="permission-item">
                                                <span class="permission-name">{{ permission }}</span>
                                                <span class="permission-desc">
                                                    {{ getPermissionDescription(permission) }}
                                                </span>
                                            </div>
                                        </el-checkbox>
                                    </el-checkbox-group>
                                </div>
                            </el-tab-pane>
                        </el-tabs>
                    </div>

                    <!-- Selected Permissions Summary -->
                    <div class="selected-permissions-summary">
                        <div class="summary-header">
                            <h4>{{ $t('rbac.roles.selectedPermissions') }}</h4>
                            <el-tag type="info">
                                {{ formData.permissions.length }} {{ $t('rbac.roles.permissionsSelected') }}
                            </el-tag>
                        </div>
                        <div class="selected-permissions-list">
                            <el-tag
                                v-for="permission in formData.permissions"
                                :key="permission"
                                closable
                                @close="removePermission(permission)"
                                :disable-transitions="false"
                                class="selected-permission-tag"
                            >
                                {{ permission }}
                            </el-tag>
                        </div>
                    </div>
                </div>
            </el-form-item>

            <!-- Permission Inheritance (for future use) -->
            <el-form-item
                v-if="mode === 'edit'"
                :label="$t('rbac.roles.inheritance')"
            >
                <el-alert
                    :title="$t('rbac.roles.inheritanceInfo')"
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
                    :disabled="mode === 'edit' && role?.systemRole"
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
    import {useRBACStore} from "@/stores/rbac"
    import ValidatedForm from "@/components/common/ValidatedForm.vue"
    import type {Role, Permission} from "@/types/rbac"

    interface Props {
        modelValue: boolean
        role?: Role | null
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
    const activeCategory = ref('flow')
    const availablePermissions = ref<string[]>([])

    // Form data
    const formData = ref({
        name: '',
        description: '',
        permissions: [] as string[]
    })

    // Computed
    const visible = computed({
        get: () => props.modelValue,
        set: (value) => emit('update:modelValue', value)
    })

    const dialogTitle = computed(() => {
        if (props.mode === 'create') {
            return t('rbac.roles.createRole')
        }
        return t('rbac.roles.editRole', {name: props.role?.name})
    })

    // Permission categories
    const permissionCategories = computed(() => [
        {
            name: 'flow',
            label: t('rbac.permissions.categories.flow'),
            permissions: availablePermissions.value.filter(p => p.startsWith('FLOW_'))
        },
        {
            name: 'execution',
            label: t('rbac.permissions.categories.execution'),
            permissions: availablePermissions.value.filter(p => p.startsWith('EXECUTION_'))
        },
        {
            name: 'template',
            label: t('rbac.permissions.categories.template'),
            permissions: availablePermissions.value.filter(p => p.startsWith('TEMPLATE_'))
        },
        {
            name: 'namespace',
            label: t('rbac.permissions.categories.namespace'),
            permissions: availablePermissions.value.filter(p => p.startsWith('NAMESPACE_'))
        },
        {
            name: 'user',
            label: t('rbac.permissions.categories.user'),
            permissions: availablePermissions.value.filter(p => p.startsWith('USER_'))
        },
        {
            name: 'role',
            label: t('rbac.permissions.categories.role'),
            permissions: availablePermissions.value.filter(p => p.startsWith('ROLE_'))
        },
        {
            name: 'group',
            label: t('rbac.permissions.categories.group'),
            permissions: availablePermissions.value.filter(p => p.startsWith('GROUP_'))
        },
        {
            name: 'tenant',
            label: t('rbac.permissions.categories.tenant'),
            permissions: availablePermissions.value.filter(p => p.startsWith('TENANT_'))
        },
        {
            name: 'audit',
            label: t('rbac.permissions.categories.audit'),
            permissions: availablePermissions.value.filter(p => p.startsWith('AUDIT_'))
        },
        {
            name: 'secret',
            label: t('rbac.permissions.categories.secret'),
            permissions: availablePermissions.value.filter(p => p.startsWith('SECRET_'))
        }
    ])

    // Form validation rules
    const formRules = computed(() => ({
        name: [
            {
                required: true,
                message: t('rbac.roles.nameRequired'),
                trigger: 'blur'
            },
            {
                min: 2,
                max: 50,
                message: t('rbac.roles.nameLength'),
                trigger: 'blur'
            },
            {
                pattern: /^[a-zA-Z0-9_-]+$/,
                message: t('rbac.roles.namePattern'),
                trigger: 'blur'
            }
        ],
        description: [
            {
                max: 500,
                message: t('rbac.roles.descriptionLength'),
                trigger: 'blur'
            }
        ],
        permissions: [
            {
                type: 'array',
                min: 1,
                message: t('rbac.roles.permissionsRequired'),
                trigger: 'change'
            }
        ]
    }))

    // Methods
    const loadAvailablePermissions = async () => {
        try {
            availablePermissions.value = await rbacStore.fetchAvailablePermissions()
        } catch (error) {
            console.error('Failed to load available permissions:', error)
        }
    }

    const isCategorySelected = (categoryName: string) => {
        const category = permissionCategories.value.find(c => c.name === categoryName)
        if (!category) return false
        return category.permissions.every(p => formData.value.permissions.includes(p))
    }

    const isCategoryIndeterminate = (categoryName: string) => {
        const category = permissionCategories.value.find(c => c.name === categoryName)
        if (!category) return false
        const selectedCount = category.permissions.filter(p => formData.value.permissions.includes(p)).length
        return selectedCount > 0 && selectedCount < category.permissions.length
    }

    const getSelectedCategoryCount = (categoryName: string) => {
        const category = permissionCategories.value.find(c => c.name === categoryName)
        if (!category) return 0
        return category.permissions.filter(p => formData.value.permissions.includes(p)).length
    }

    const toggleCategory = (categoryName: string, selected: boolean) => {
        const category = permissionCategories.value.find(c => c.name === categoryName)
        if (!category) return

        if (selected) {
            // Add all category permissions
            category.permissions.forEach(permission => {
                if (!formData.value.permissions.includes(permission)) {
                    formData.value.permissions.push(permission)
                }
            })
        } else {
            // Remove all category permissions
            formData.value.permissions = formData.value.permissions.filter(
                permission => !category.permissions.includes(permission)
            )
        }
    }

    const removePermission = (permission: string) => {
        const index = formData.value.permissions.indexOf(permission)
        if (index > -1) {
            formData.value.permissions.splice(index, 1)
        }
    }

    const getPermissionDescription = (permission: string) => {
        // Return localized permission description
        return t(`rbac.permissions.descriptions.${permission}`, permission)
    }

    const resetForm = () => {
        formData.value = {
            name: '',
            description: '',
            permissions: []
        }
        nextTick(() => {
            formRef.value?.clearValidate()
        })
    }

    const populateForm = () => {
        if (props.role) {
            formData.value = {
                name: props.role.name,
                description: props.role.description || '',
                permissions: [...(props.role.permissions || [])]
            }
        } else {
            resetForm()
        }
    }

    const handleSubmit = async () => {
        try {
            await formRef.value?.validate()
            saving.value = true

            if (props.mode === 'create') {
                await rbacStore.createRole(formData.value)
                ElMessage.success(t('rbac.roles.createSuccess'))
            } else if (props.role) {
                await rbacStore.updateRole(props.role.id, formData.value)
                ElMessage.success(t('rbac.roles.updateSuccess'))
            }

            emit('save')
        } catch (error) {
            if (error !== false) { // Not validation error
                ElMessage.error(
                    props.mode === 'create'
                        ? t('rbac.roles.createError')
                        : t('rbac.roles.updateError')
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
            loadAvailablePermissions()
            populateForm()
        }
    })

    watch(() => props.role, () => {
        if (props.modelValue) {
            populateForm()
        }
    })
</script>

<style scoped>
.permissions-container {
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  overflow: hidden;
}

.permission-categories {
  min-height: 400px;
}

.category-permissions {
  padding: 1rem;
}

.category-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1rem;
  padding-bottom: 0.5rem;
  border-bottom: 1px solid #f0f0f0;
}

.category-count {
  color: #909399;
  font-size: 0.9rem;
}

.permission-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 0.75rem;
}

.permission-item {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.permission-name {
  font-weight: 600;
  color: #303133;
}

.permission-desc {
  font-size: 0.8rem;
  color: #909399;
  line-height: 1.3;
}

.selected-permissions-summary {
  border-top: 1px solid #e4e7ed;
  background: #f8f9fa;
  padding: 1rem;
}

.summary-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1rem;
}

.summary-header h4 {
  margin: 0;
  color: #303133;
  font-size: 1rem;
}

.selected-permissions-list {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
  max-height: 120px;
  overflow-y: auto;
}

.selected-permission-tag {
  margin: 0;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 0.5rem;
}

:deep(.el-tabs__content) {
  padding: 0;
}

:deep(.el-checkbox-group) {
  width: 100%;
}

:deep(.el-checkbox) {
  width: 100%;
  margin-right: 0;
  margin-bottom: 0.5rem;
}

:deep(.el-checkbox__label) {
  width: 100%;
  padding-left: 0.5rem;
}
</style>
