<template>
    <div class="access-denied">
        <div class="access-denied-icon">
            <el-icon :size="48" color="var(--el-color-warning)">
                <Lock />
            </el-icon>
        </div>

        <div class="access-denied-content">
            <h3 class="access-denied-title">
                {{ $t('rbac.accessDenied.title') }}
            </h3>
            <p class="access-denied-message">
                {{ message }}
            </p>

            <div v-if="showDetails" class="access-denied-details">
                <el-descriptions :column="1" size="small" border>
                    <el-descriptions-item v-if="permission" :label="$t('rbac.accessDenied.requiredPermission')">
                        <el-tag type="warning" size="small">
                            {{ permission }}
                        </el-tag>
                    </el-descriptions-item>
                    <el-descriptions-item v-if="action" :label="$t('rbac.accessDenied.requiredAction')">
                        <el-tag type="warning" size="small">
                            {{ action }}
                        </el-tag>
                    </el-descriptions-item>
                    <el-descriptions-item v-if="resource" :label="$t('rbac.accessDenied.resource')">
                        <el-tag type="info" size="small">
                            {{ resource }}
                        </el-tag>
                    </el-descriptions-item>
                    <el-descriptions-item :label="$t('rbac.accessDenied.currentUser')">
                        <el-tag type="primary" size="small">
                            {{ currentUser?.username || 'Unknown' }}
                        </el-tag>
                    </el-descriptions-item>
                </el-descriptions>
            </div>

            <div class="access-denied-actions">
                <el-button
                    v-if="showContactAdmin"
                    type="primary"
                    size="small"
                    @click="contactAdmin"
                >
                    {{ $t('rbac.accessDenied.contactAdmin') }}
                </el-button>

                <el-button
                    v-if="showRequestAccess"
                    type="default"
                    size="small"
                    @click="requestAccess"
                >
                    {{ $t('rbac.accessDenied.requestAccess') }}
                </el-button>

                <el-button
                    size="small"
                    @click="goBack"
                >
                    {{ $t('rbac.accessDenied.goBack') }}
                </el-button>
            </div>
        </div>
    </div>
</template>

<script setup lang="ts">
    import {computed} from "vue"
    import {useRouter} from "vue-router"
    import {useI18n} from "vue-i18n"
    import {ElMessage} from "element-plus"
    import {Lock} from "@element-plus/icons-vue"
    import {useUserStore} from "@/stores/user"

    interface Props {
        permission?: string
        action?: string
        resource?: string
        showDetails?: boolean
        showContactAdmin?: boolean
        showRequestAccess?: boolean
    }

    const props = withDefaults(defineProps<Props>(), {
        showDetails: true,
        showContactAdmin: true,
        showRequestAccess: true
    })

    const router = useRouter()
    const {t} = useI18n()
    const userStore = useUserStore()

    const currentUser = computed(() => userStore.currentUser)

    const message = computed(() => {
        if (props.permission) {
            return t("rbac.accessDenied.permissionMessage", {permission: props.permission})
        }
        if (props.action && props.resource) {
            return t("rbac.accessDenied.actionMessage", {action: props.action, resource: props.resource})
        }
        return t("rbac.accessDenied.defaultMessage")
    })

    const contactAdmin = () => {
        // 这里可以集成邮件系统或工单系统
        ElMessage.info(t("rbac.accessDenied.adminContactedMessage"))
    }

    const requestAccess = () => {
        // 这里可以集成权限申请流程
        ElMessage.info(t("rbac.accessDenied.accessRequestedMessage"))
    }

    const goBack = () => {
        if (window.history.length > 1) {
            router.go(-1)
        } else {
            router.push("/")
        }
    }
</script>

<script lang="ts">
    export default {
        name: "AccessDeniedMessage"
    }
</script>

<style scoped>
.access-denied {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 40px 20px;
  text-align: center;
  max-width: 500px;
  margin: 0 auto;
}

.access-denied-icon {
  margin-bottom: 20px;
}

.access-denied-content {
  width: 100%;
}

.access-denied-title {
  margin: 0 0 12px 0;
  font-size: 20px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.access-denied-message {
  margin: 0 0 20px 0;
  color: var(--el-text-color-regular);
  line-height: 1.6;
}

.access-denied-details {
  margin: 20px 0;
  text-align: left;
}

.access-denied-actions {
  display: flex;
  gap: 12px;
  justify-content: center;
  flex-wrap: wrap;
  margin-top: 24px;
}

@media (max-width: 768px) {
  .access-denied {
    padding: 20px 16px;
  }

  .access-denied-actions {
    flex-direction: column;
    align-items: center;
  }

  .access-denied-actions .el-button {
    width: 100%;
    max-width: 200px;
  }
}
</style>
