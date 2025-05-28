<template>
    <div class="sso-configuration">
        <PermissionGuard permission="AUTH_SSO_READ">
            <!-- Header -->
            <div class="sso-header">
                <h2 class="sso-title">
                    <el-icon><Key /></el-icon>
                    {{ $t('auth.sso.title') }}
                </h2>
                <div class="sso-actions">
                    <PermissionGuard permission="AUTH_SSO_CREATE">
                        <el-button
                            type="primary"
                            :icon="Plus"
                            @click="createProvider"
                        >
                            {{ $t('auth.sso.addProvider') }}
                        </el-button>
                    </PermissionGuard>
                    <el-button
                        :icon="Refresh"
                        @click="loadProviders"
                        :loading="loading"
                    >
                        {{ $t('common.refresh') }}
                    </el-button>
                </div>
            </div>

            <!-- SSO Status Overview -->
            <el-card class="sso-overview" shadow="never">
                <template #header>
                    <div class="overview-header">
                        <el-icon><DataAnalysis /></el-icon>
                        <span>{{ $t('auth.sso.overview') }}</span>
                    </div>
                </template>
                
                <el-row :gutter="16">
                    <el-col :span="6">
                        <el-statistic
                            :title="$t('auth.sso.totalProviders')"
                            :value="providers.length"
                        />
                    </el-col>
                    <el-col :span="6">
                        <el-statistic
                            :title="$t('auth.sso.activeProviders')"
                            :value="activeProviders"
                        />
                    </el-col>
                    <el-col :span="6">
                        <el-statistic
                            :title="$t('auth.sso.ssoLogins')"
                            :value="ssoStats.totalLogins"
                        />
                    </el-col>
                    <el-col :span="6">
                        <el-statistic
                            :title="$t('auth.sso.successRate')"
                            :value="ssoStats.successRate"
                            :precision="1"
                            suffix="%"
                        />
                    </el-col>
                </el-row>
            </el-card>

            <!-- Fallback for no permission -->
            <template #fallback>
                <AccessDeniedMessage permission="AUTH_SSO_READ" />
            </template>
        </PermissionGuard>
    </div>
</template>

<script setup lang="ts">
    import {ref, computed, onMounted} from "vue"
    import {useI18n} from "vue-i18n"
    import {ElMessage, ElMessageBox} from "element-plus"
    import {
        Key,
        Plus,
        Edit,
        Delete,
        View,
        Refresh,
        DataAnalysis,
        Document,
        Connection,
        Link
    } from "@element-plus/icons-vue"
    import {useAuthStore} from "@/stores/auth"
    import {usePermissionStore} from "@/stores/permission"
    import PermissionGuard from "@/components/rbac/PermissionGuard.vue"
    import AccessDeniedMessage from "@/components/rbac/AccessDeniedMessage.vue"
    import {formatDate} from "@/utils/date"
    import type {SSOProvider} from "@/types/auth"

    const {t} = useI18n()
    const authStore = useAuthStore()
    const permissionStore = usePermissionStore()

    // State
    const loading = ref(false)
    const providers = ref<SSOProvider[]>([])
    const selectedProvider = ref<SSOProvider | null>(null)
    const showDialog = ref(false)
    const showDetailDialog = ref(false)
    const dialogMode = ref<'create' | 'edit'>('create')
    const ssoStats = ref({
        totalLogins: 0,
        successRate: 0
    })

    // Computed
    const activeProviders = computed(() => {
        return providers.value.filter(p => p.enabled).length
    })

    // Methods
    const hasPermission = (permission: string) => {
        return permissionStore.hasPermission(permission)
    }

    const loadProviders = async () => {
        loading.value = true
        try {
            const response = await authStore.fetchSSOProviders()
            providers.value = response.data
            await loadSSOStats()
        } catch (error) {
            ElMessage.error(t('auth.sso.loadError'))
        } finally {
            loading.value = false
        }
    }

    const loadSSOStats = async () => {
        try {
            ssoStats.value = await authStore.fetchSSOStats()
        } catch (error) {
            console.error('Failed to load SSO stats:', error)
        }
    }

    const createProvider = () => {
        selectedProvider.value = null
        dialogMode.value = 'create'
        showDialog.value = true
    }

    // Lifecycle
    onMounted(() => {
        loadProviders()
    })
</script>

<style scoped>
.sso-configuration {
  padding: 1.5rem;
}

.sso-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1.5rem;
  padding-bottom: 1rem;
  border-bottom: 1px solid #e4e7ed;
}

.sso-title {
  margin: 0;
  color: #303133;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.sso-actions {
  display: flex;
  gap: 0.5rem;
}

.sso-overview {
  margin-bottom: 1.5rem;
}

.overview-header {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-weight: 600;
  color: #303133;
}
</style>
