<template>
    <div class="token-management">
        <PermissionGuard permission="AUTH_TOKEN_READ">
            <!-- Header -->
            <div class="token-header">
                <h2 class="token-title">
                    <el-icon><Key /></el-icon>
                    {{ $t('auth.tokens.title') }}
                </h2>
                <div class="token-actions">
                    <PermissionGuard permission="AUTH_TOKEN_CREATE">
                        <el-button
                            type="primary"
                            :icon="Plus"
                            @click="createToken"
                        >
                            {{ $t('auth.tokens.create') }}
                        </el-button>
                    </PermissionGuard>
                    <el-button
                        :icon="Refresh"
                        @click="loadTokens"
                        :loading="loading"
                    >
                        {{ $t('common.refresh') }}
                    </el-button>
                </div>
            </div>

            <!-- Token Overview -->
            <el-card class="token-overview" shadow="never">
                <template #header>
                    <div class="overview-header">
                        <el-icon><DataAnalysis /></el-icon>
                        <span>{{ $t('auth.tokens.overview') }}</span>
                    </div>
                </template>
                
                <el-row :gutter="16">
                    <el-col :span="6">
                        <el-statistic
                            :title="$t('auth.tokens.totalTokens')"
                            :value="tokenStats.totalTokens"
                        />
                    </el-col>
                    <el-col :span="6">
                        <el-statistic
                            :title="$t('auth.tokens.activeTokens')"
                            :value="tokenStats.activeTokens"
                        />
                    </el-col>
                    <el-col :span="6">
                        <el-statistic
                            :title="$t('auth.tokens.expiredTokens')"
                            :value="tokenStats.expiredTokens"
                        />
                    </el-col>
                    <el-col :span="6">
                        <el-statistic
                            :title="$t('auth.tokens.revokedTokens')"
                            :value="tokenStats.revokedTokens"
                        />
                    </el-col>
                </el-row>
            </el-card>

            <!-- Fallback for no permission -->
            <template #fallback>
                <AccessDeniedMessage permission="AUTH_TOKEN_READ" />
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
        Search,
        Refresh,
        DataAnalysis,
        Lock
    } from "@element-plus/icons-vue"
    import {useAuthStore} from "@/stores/auth"
    import {usePermissionStore} from "@/stores/permission"
    import PermissionGuard from "@/components/rbac/PermissionGuard.vue"
    import AccessDeniedMessage from "@/components/rbac/AccessDeniedMessage.vue"
    import {formatDate} from "@/utils/date"
    import type {Token} from "@/types/auth"

    const {t} = useI18n()
    const authStore = useAuthStore()
    const permissionStore = usePermissionStore()

    // State
    const loading = ref(false)
    const tokens = ref<Token[]>([])
    const selectedToken = ref<Token | null>(null)
    const showDialog = ref(false)
    const showDetailDialog = ref(false)
    const dialogMode = ref<'create' | 'edit'>('create')

    // Statistics
    const tokenStats = ref({
        totalTokens: 0,
        activeTokens: 0,
        expiredTokens: 0,
        revokedTokens: 0
    })

    // Constants
    const tokenTypes = ['ACCESS', 'REFRESH', 'API', 'SERVICE']

    // Methods
    const hasPermission = (permission: string) => {
        return permissionStore.hasPermission(permission)
    }

    const loadTokens = async () => {
        loading.value = true
        try {
            const response = await authStore.fetchTokens({
                page: 1,
                size: 20
            })
            tokens.value = response.data
            await loadTokenStats()
        } catch (error) {
            ElMessage.error(t('auth.tokens.loadError'))
        } finally {
            loading.value = false
        }
    }

    const loadTokenStats = async () => {
        try {
            tokenStats.value = await authStore.fetchTokenStats()
        } catch (error) {
            console.error('Failed to load token stats:', error)
        }
    }

    const createToken = () => {
        selectedToken.value = null
        dialogMode.value = 'create'
        showDialog.value = true
    }

    // Lifecycle
    onMounted(() => {
        loadTokens()
    })
</script>

<style scoped>
.token-management {
  padding: 1.5rem;
}

.token-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1.5rem;
  padding-bottom: 1rem;
  border-bottom: 1px solid #e4e7ed;
}

.token-title {
  margin: 0;
  color: #303133;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.token-actions {
  display: flex;
  gap: 0.5rem;
}

.token-overview {
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
