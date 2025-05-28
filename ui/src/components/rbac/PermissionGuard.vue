<template>
    <div v-if="hasPermission" class="permission-guard">
        <slot />
    </div>
    <div v-else-if="showFallback" class="permission-guard-fallback">
        <slot name="fallback">
            <AccessDeniedMessage
                :permission="permission"
                :action="action"
                :resource="resource"
            />
        </slot>
    </div>
</template>

<script setup lang="ts">
    import {computed} from "vue"
    import {usePermissionStore} from "@/stores/permission"
    import AccessDeniedMessage from "./AccessDeniedMessage.vue"

    interface Props {
        permission?: string
        action?: string
        resource?: string
        namespace?: string
        requireAll?: boolean
        showFallback?: boolean
    }

    const props = withDefaults(defineProps<Props>(), {
        requireAll: false,
        showFallback: true
    })

    const permissionStore = usePermissionStore()

    // 计算是否有权限
    const hasPermission = computed(() => {
        // 如果没有指定任何权限要求，默认允许访问
        if (!props.permission && !props.action) {
            return true
        }

        // 检查具体权限
        if (props.permission) {
            return permissionStore.hasPermission(props.permission, props.namespace)
        }

        // 检查操作权限
        if (props.action && props.resource) {
            return permissionStore.hasActionPermission(props.action, props.resource, props.namespace)
        }

        return false
    })

    // 暴露权限检查方法给父组件
    defineExpose({
        hasPermission
    })
</script>

<script lang="ts">
    export default {
        name: "PermissionGuard"
    }
</script>

<style scoped>
.permission-guard {
  /* 权限通过时的样式 */
}

.permission-guard-fallback {
  /* 权限不足时的样式 */
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 200px;
  background-color: var(--el-fill-color-light);
  border-radius: var(--el-border-radius-base);
  border: 1px dashed var(--el-border-color);
}
</style>
