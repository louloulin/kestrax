<template>
    <el-dialog
        v-model="visible"
        :title="dialogTitle"
        :width="600"
        :close-on-click-modal="false"
        :close-on-press-escape="false"
        @close="handleClose"
    >
        <ValidatedForm
            v-model="formData"
            :rules="formRules"
            :submitting="submitting"
            @submit="handleSubmit"
            @cancel="handleCancel"
        >
            <el-row :gutter="20">
                <el-col :span="12">
                    <el-form-item
                        prop="username"
                        :label="$t('rbac.users.username')"
                    >
                        <el-input
                            v-model="formData.username"
                            :placeholder="$t('rbac.users.usernamePlaceholder')"
                            :disabled="mode === 'edit'"
                            clearable
                        />
                    </el-form-item>
                </el-col>
        
                <el-col :span="12">
                    <el-form-item
                        prop="email"
                        :label="$t('rbac.users.email')"
                    >
                        <el-input
                            v-model="formData.email"
                            type="email"
                            :placeholder="$t('rbac.users.emailPlaceholder')"
                            clearable
                        />
                    </el-form-item>
                </el-col>
            </el-row>

            <el-row :gutter="20">
                <el-col :span="12">
                    <el-form-item
                        prop="firstName"
                        :label="$t('rbac.users.firstName')"
                    >
                        <el-input
                            v-model="formData.firstName"
                            :placeholder="$t('rbac.users.firstNamePlaceholder')"
                            clearable
                        />
                    </el-form-item>
                </el-col>
        
                <el-col :span="12">
                    <el-form-item
                        prop="lastName"
                        :label="$t('rbac.users.lastName')"
                    >
                        <el-input
                            v-model="formData.lastName"
                            :placeholder="$t('rbac.users.lastNamePlaceholder')"
                            clearable
                        />
                    </el-form-item>
                </el-col>
            </el-row>

            <el-row v-if="mode === 'create'" :gutter="20">
                <el-col :span="12">
                    <el-form-item
                        prop="password"
                        :label="$t('rbac.users.password')"
                    >
                        <el-input
                            v-model="formData.password"
                            type="password"
                            :placeholder="$t('rbac.users.passwordPlaceholder')"
                            show-password
                            clearable
                        />
                    </el-form-item>
                </el-col>
        
                <el-col :span="12">
                    <el-form-item
                        prop="confirmPassword"
                        :label="$t('rbac.users.confirmPassword')"
                    >
                        <el-input
                            v-model="formData.confirmPassword"
                            type="password"
                            :placeholder="$t('rbac.users.confirmPasswordPlaceholder')"
                            show-password
                            clearable
                        />
                    </el-form-item>
                </el-col>
            </el-row>

            <el-form-item
                prop="enabled"
                :label="$t('rbac.users.status')"
            >
                <el-switch
                    v-model="formData.enabled"
                    :active-text="$t('common.enabled')"
                    :inactive-text="$t('common.disabled')"
                />
            </el-form-item>

            <el-form-item
                prop="roleIds"
                :label="$t('rbac.users.roles')"
            >
                <el-select
                    v-model="formData.roleIds"
                    multiple
                    :placeholder="$t('rbac.users.selectRoles')"
                    style="width: 100%"
                    clearable
                    filterable
                >
                    <el-option
                        v-for="role in availableRoles"
                        :key="role.id"
                        :label="role.name"
                        :value="role.id"
                    >
                        <div class="role-option">
                            <span class="role-name">{{ role.name }}</span>
                            <span class="role-description">{{ role.description }}</span>
                        </div>
                    </el-option>
                </el-select>
            </el-form-item>

            <el-form-item
                prop="groupIds"
                :label="$t('rbac.users.groups')"
            >
                <el-select
                    v-model="formData.groupIds"
                    multiple
                    :placeholder="$t('rbac.users.selectGroups')"
                    style="width: 100%"
                    clearable
                    filterable
                >
                    <el-option
                        v-for="group in availableGroups"
                        :key="group.id"
                        :label="group.name"
                        :value="group.id"
                    >
                        <div class="group-option">
                            <span class="group-name">{{ group.name }}</span>
                            <span class="group-description">{{ group.description }}</span>
                        </div>
                    </el-option>
                </el-select>
            </el-form-item>

            <el-form-item
                prop="description"
                :label="$t('common.description')"
            >
                <el-input
                    v-model="formData.description"
                    type="textarea"
                    :rows="3"
                    :placeholder="$t('rbac.users.descriptionPlaceholder')"
                    maxlength="500"
                    show-word-limit
                />
            </el-form-item>
        </ValidatedForm>
    </el-dialog>
</template>

<script setup lang="ts">
    import {ref, computed, watch, onMounted} from "vue"
    import {useI18n} from "vue-i18n"
    import {ElMessage} from "element-plus"
    import ValidatedForm from "@/components/common/ValidatedForm.vue"
    import {useRBACStore} from "@/stores/rbac"
    import type {User, Role, Group} from "@/types/rbac"
    import type {FormRules} from "element-plus"

    interface Props {
        modelValue: boolean
        user?: User | null
        mode: "create" | "edit" | "view"
    }

    const props = withDefaults(defineProps<Props>(), {
        user: null,
        mode: "create"
    })

    const emit = defineEmits(["update:modelValue", "save"])

    const {t} = useI18n()
    const rbacStore = useRBACStore()

    // 响应式状态
    const visible = computed({
        get: () => props.modelValue,
        set: (value) => emit("update:modelValue", value)
    })

    const submitting = ref(false)
    const availableRoles = ref<Role[]>([])
    const availableGroups = ref<Group[]>([])

    // 表单数据
    const defaultFormData = () => ({
        username: "",
        email: "",
        firstName: "",
        lastName: "",
        password: "",
        confirmPassword: "",
        enabled: true,
        roleIds: [] as string[],
        groupIds: [] as string[],
        description: ""
    })

    const formData = ref(defaultFormData())

    // 计算属性
    const dialogTitle = computed(() => {
        switch (props.mode) {
        case "create":
            return t("rbac.users.createUser")
        case "edit":
            return t("rbac.users.editUser")
        case "view":
            return t("rbac.users.viewUser")
        default:
            return ""
        }
    })

    // 表单验证规则
    const formRules = computed((): FormRules => {
        const rules: FormRules = {
            username: [
                {required: true, message: t("rbac.users.usernameRequired"), trigger: "blur"},
                {min: 3, max: 50, message: t("rbac.users.usernameLength"), trigger: "blur"},
                {pattern: /^[a-zA-Z0-9_-]+$/, message: t("rbac.users.usernamePattern"), trigger: "blur"}
            ],
            email: [
                {required: true, message: t("rbac.users.emailRequired"), trigger: "blur"},
                {type: "email", message: t("rbac.users.emailInvalid"), trigger: "blur"}
            ],
            firstName: [
                {required: true, message: t("rbac.users.firstNameRequired"), trigger: "blur"},
                {max: 50, message: t("rbac.users.firstNameLength"), trigger: "blur"}
            ],
            lastName: [
                {required: true, message: t("rbac.users.lastNameRequired"), trigger: "blur"},
                {max: 50, message: t("rbac.users.lastNameLength"), trigger: "blur"}
            ]
        }

        // 创建模式下需要密码验证
        if (props.mode === "create") {
            rules.password = [
                {required: true, message: t("rbac.users.passwordRequired"), trigger: "blur"},
                {min: 8, message: t("rbac.users.passwordMinLength"), trigger: "blur"},
                { 
                    pattern: /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&])[A-Za-z\d@$!%*?&]/,
                    message: t("rbac.users.passwordPattern"),
                    trigger: "blur"
                }
            ]

            rules.confirmPassword = [
                {required: true, message: t("rbac.users.confirmPasswordRequired"), trigger: "blur"},
                {
                    validator: (rule, value, callback) => {
                        if (value !== formData.value.password) {
                            callback(new Error(t("rbac.users.passwordMismatch")))
                        } else {
                            callback()
                        }
                    },
                    trigger: "blur"
                }
            ]
        }

        return rules
    })

    // 方法
    const loadRolesAndGroups = async () => {
        try {
            const [rolesResponse, groupsResponse] = await Promise.all([
                rbacStore.fetchRoles({includeSystem: true}),
                rbacStore.fetchGroups({includeSystem: true})
            ])
    
            availableRoles.value = rolesResponse.content || []
            availableGroups.value = groupsResponse.content || []
        } catch (error) {
            ElMessage.error(t("rbac.users.loadDataError"))
        }
    }

    const initializeForm = () => {
        if (props.user && props.mode !== "create") {
            formData.value = {
                username: props.user.username || "",
                email: props.user.email || "",
                firstName: props.user.firstName || "",
                lastName: props.user.lastName || "",
                password: "",
                confirmPassword: "",
                enabled: props.user.enabled ?? true,
                roleIds: props.user.roles?.map(role => role.id) || [],
                groupIds: props.user.groups?.map(group => group.id) || [],
                description: props.user.description || ""
            }
        } else {
            formData.value = defaultFormData()
        }
    }

    const handleSubmit = async (data: any) => {
        submitting.value = true
        try {
            const submitData = {...data}
    
            // 移除确认密码字段
            delete submitData.confirmPassword
    
            // 如果是编辑模式且密码为空，则不提交密码
            if (props.mode === "edit" && !submitData.password) {
                delete submitData.password
            }

            emit("save", submitData)
        } finally {
            submitting.value = false
        }
    }

    const handleCancel = () => {
        visible.value = false
    }

    const handleClose = () => {
        formData.value = defaultFormData()
    }

    // 监听器
    watch(
        () => props.modelValue,
        (newValue) => {
            if (newValue) {
                initializeForm()
                loadRolesAndGroups()
            }
        }
    )

    // 生命周期
    onMounted(() => {
        if (visible.value) {
            loadRolesAndGroups()
        }
    })
</script>

<script lang="ts">
    export default {
        name: "UserFormDialog"
    }
</script>

<style scoped>
.role-option,
.group-option {
  display: flex;
  flex-direction: column;
  gap: 2px;
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
}

:deep(.el-dialog__body) {
  padding: 20px 24px;
}

:deep(.el-form-item__label) {
  font-weight: 500;
}

@media (max-width: 768px) {
  :deep(.el-dialog) {
    width: 95% !important;
    margin: 0 auto;
  }
  
  .el-col {
    margin-bottom: 16px;
  }
}
</style>
