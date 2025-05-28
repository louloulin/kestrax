<template>
    <el-form
        ref="formRef"
        :model="formData"
        :rules="computedRules"
        :label-width="labelWidth"
        :label-position="labelPosition"
        :size="size"
        :disabled="disabled"
        :validate-on-rule-change="validateOnRuleChange"
        :hide-required-asterisk="hideRequiredAsterisk"
        :show-message="showMessage"
        :inline-message="inlineMessage"
        :status-icon="statusIcon"
        @validate="handleValidate"
    >
        <slot :form-data="formData" :validate="validate" :reset="resetForm" />

        <!-- 表单操作按钮 -->
        <div v-if="showActions" class="form-actions">
            <slot name="actions" :form-data="formData" :validate="validate" :reset="resetForm">
                <el-button
                    v-if="showReset"
                    @click="resetForm"
                >
                    {{ resetText }}
                </el-button>

                <el-button
                    v-if="showCancel"
                    @click="handleCancel"
                >
                    {{ cancelText }}
                </el-button>

                <el-button
                    type="primary"
                    :loading="submitting"
                    @click="handleSubmit"
                >
                    {{ submitText }}
                </el-button>
            </slot>
        </div>
    </el-form>
</template>

<script setup lang="ts">
    import {ref, computed, watch, nextTick} from "vue"
    import {useI18n} from "vue-i18n"
    import {ElMessage} from "element-plus"
    import type {FormInstance, FormRules} from "element-plus"

    interface Props {
        modelValue: Record<string, any>
        rules?: FormRules
        labelWidth?: string | number
        labelPosition?: "left" | "right" | "top"
        size?: "large" | "default" | "small"
        disabled?: boolean
        validateOnRuleChange?: boolean
        hideRequiredAsterisk?: boolean
        showMessage?: boolean
        inlineMessage?: boolean
        statusIcon?: boolean
        showActions?: boolean
        showReset?: boolean
        showCancel?: boolean
        submitText?: string
        resetText?: string
        cancelText?: string
        submitting?: boolean
        autoValidate?: boolean
        validateOnMount?: boolean
    }

    const props = withDefaults(defineProps<Props>(), {
        rules: () => ({}),
        labelWidth: "120px",
        labelPosition: "right",
        size: "default",
        disabled: false,
        validateOnRuleChange: true,
        hideRequiredAsterisk: false,
        showMessage: true,
        inlineMessage: false,
        statusIcon: false,
        showActions: true,
        showReset: true,
        showCancel: false,
        submitText: "提交",
        resetText: "重置",
        cancelText: "取消",
        submitting: false,
        autoValidate: true,
        validateOnMount: false
    })

    const emit = defineEmits([
        "update:modelValue",
        "submit",
        "cancel",
        "validate",
        "reset"
    ])

    const {t} = useI18n()
    const formRef = ref<FormInstance>()

    // 表单数据
    const formData = computed({
        get: () => props.modelValue,
        set: (value) => emit("update:modelValue", value)
    })

    // 计算规则
    const computedRules = computed(() => {
        if (!props.rules) return {}

        // 可以在这里添加通用的验证规则处理逻辑
        const processedRules: FormRules = {}

        Object.keys(props.rules).forEach(key => {
            const rule = props.rules![key]
            const ruleArray = Array.isArray(rule) ? rule.filter(r => r) : (rule ? [rule] : [])
            processedRules[key] = ruleArray

            // 添加国际化支持
            ruleArray.forEach((r: any) => {
                if (r && r.message && r.message.startsWith("validation.")) {
                    r.message = t(r.message)
                }
            })
        })

        return processedRules
    })

    // 验证表单
    const validate = async (): Promise<boolean> => {
        if (!formRef.value) return false

        try {
            await formRef.value.validate()
            return true
        } catch (error) {
            return false
        }
    }

    // 验证指定字段
    const validateField = async (prop: string): Promise<boolean> => {
        if (!formRef.value) return false

        try {
            await formRef.value.validateField(prop)
            return true
        } catch (error) {
            return false
        }
    }

    // 重置表单
    const resetForm = () => {
        formRef.value?.resetFields()
        emit("reset")
    }

    // 清除验证
    const clearValidate = (props?: string | string[]) => {
        formRef.value?.clearValidate(props)
    }

    // 处理提交
    const handleSubmit = async () => {
        if (props.autoValidate) {
            const isValid = await validate()
            if (!isValid) {
                ElMessage.warning(t("form.validationFailed"))
                return
            }
        }

        emit("submit", formData.value)
    }

    // 处理取消
    const handleCancel = () => {
        emit("cancel")
    }

    // 处理验证事件
    const handleValidate = (prop: string, isValid: boolean, message: string) => {
        emit("validate", {prop, isValid, message})
    }

    // 监听表单数据变化
    watch(
        () => props.modelValue,
        () => {
            if (props.autoValidate && formRef.value) {
                nextTick(() => {
                    // 延迟验证，避免初始化时的验证错误
                    setTimeout(() => {
                        formRef.value?.validate(() => {})
                    }, 100)
                })
            }
        },
        {deep: true}
    )

    // 组件挂载时验证
    if (props.validateOnMount) {
        nextTick(() => {
            validate()
        })
    }

    // 暴露方法给父组件
    defineExpose({
        validate,
        validateField,
        resetForm,
        clearValidate,
        formRef
    })
</script>

<script lang="ts">
    export default {
        name: "ValidatedForm"
    }
</script>

<style scoped>
.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 24px;
  padding-top: 16px;
  border-top: 1px solid var(--el-border-color-light);
}

@media (max-width: 768px) {
  .form-actions {
    flex-direction: column-reverse;
    align-items: stretch;
  }

  .form-actions .el-button {
    width: 100%;
  }
}
</style>
