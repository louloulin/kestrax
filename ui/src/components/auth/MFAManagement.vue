<template>
    <div class="mfa-management">
        <PermissionGuard permission="AUTH_MFA_READ">
            <!-- Header -->
            <div class="mfa-header">
                <h2 class="mfa-title">
                    <el-icon><Shield /></el-icon>
                    {{ $t('auth.mfa.title') }}
                </h2>
                <div class="mfa-actions">
                    <el-button
                        :icon="Refresh"
                        @click="loadMFASettings"
                        :loading="loading"
                    >
                        {{ $t('common.refresh') }}
                    </el-button>
                </div>
            </div>

            <!-- MFA Overview -->
            <el-card class="mfa-overview" shadow="never">
                <template #header>
                    <div class="overview-header">
                        <el-icon><DataAnalysis /></el-icon>
                        <span>{{ $t('auth.mfa.overview') }}</span>
                    </div>
                </template>
                
                <el-row :gutter="16">
                    <el-col :span="6">
                        <el-statistic
                            :title="$t('auth.mfa.totalUsers')"
                            :value="mfaStats.totalUsers"
                        />
                    </el-col>
                    <el-col :span="6">
                        <el-statistic
                            :title="$t('auth.mfa.enabledUsers')"
                            :value="mfaStats.enabledUsers"
                        />
                    </el-col>
                    <el-col :span="6">
                        <el-statistic
                            :title="$t('auth.mfa.adoptionRate')"
                            :value="mfaStats.adoptionRate"
                            :precision="1"
                            suffix="%"
                        />
                    </el-col>
                    <el-col :span="6">
                        <el-statistic
                            :title="$t('auth.mfa.successRate')"
                            :value="mfaStats.successRate"
                            :precision="1"
                            suffix="%"
                        />
                    </el-col>
                </el-row>
            </el-card>

            <!-- Fallback for no permission -->
            <template #fallback>
                <AccessDeniedMessage permission="AUTH_MFA_READ" />
            </template>
        </PermissionGuard>
    </div>
</template>

<script setup lang="ts">
    import {ref, computed, onMounted} from "vue"
    import {useI18n} from "vue-i18n"
    import {ElMessage, ElMessageBox} from "element-plus"
    import {
        Shield,
        Setting,
        DataAnalysis,
        User,
        Search,
        Refresh,
        Smartphone,
        Message,
        Key
    } from "@element-plus/icons-vue"
    import {useAuthStore} from "@/stores/auth"
    import {usePermissionStore} from "@/stores/permission"
    import PermissionGuard from "@/components/rbac/PermissionGuard.vue"
    import AccessDeniedMessage from "@/components/rbac/AccessDeniedMessage.vue"
    import {formatDate} from "@/utils/date"
    import type {MFASettings, User} from "@/types/auth"

    const {t} = useI18n()
    const authStore = useAuthStore()
    const permissionStore = usePermissionStore()

    // State
    const loading = ref(false)
    const loadingUsers = ref(false)

    // MFA Settings
    const mfaSettings = ref<MFASettings>({
        enabled: false,
        enforced: false,
        allowedMethods: ['TOTP'],
        gracePeriodDays: 7,
        maxAttempts: 3
    })

    // MFA Statistics
    const mfaStats = ref({
        totalUsers: 0,
        enabledUsers: 0,
        adoptionRate: 0,
        successRate: 0
    })

    // Methods
    const hasPermission = (permission: string) => {
        return permissionStore.hasPermission(permission)
    }

    const loadMFASettings = async () => {
        loading.value = true
        try {
            mfaSettings.value = await authStore.fetchMFASettings()
            await loadMFAStats()
        } catch (error) {
            ElMessage.error(t('auth.mfa.loadError'))
        } finally {
            loading.value = false
        }
    }

    const loadMFAStats = async () => {
        try {
            mfaStats.value = await authStore.fetchMFAStats()
        } catch (error) {
            console.error('Failed to load MFA stats:', error)
        }
    }

    // Lifecycle
    onMounted(() => {
        loadMFASettings()
    })
</script>

<style scoped>
.mfa-management {
  padding: 1.5rem;
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.mfa-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-bottom: 1rem;
  border-bottom: 1px solid #e4e7ed;
}

.mfa-title {
  margin: 0;
  color: #303133;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.mfa-actions {
  display: flex;
  gap: 0.5rem;
}

.overview-header {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-weight: 600;
  color: #303133;
}
</style>
