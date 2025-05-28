import {defineStore} from "pinia"
import {ref, computed, watch} from "vue"
import type {Tenant, TenantConfig} from "@/types/tenant"
import {tenantApi} from "@/api/tenant"
import {useUserStore} from "./user"

export const useTenantStore = defineStore("tenant", () => {
  // 状态
  const tenants = ref<Tenant[]>([])
  const currentTenant = ref<Tenant | null>(null)
  const tenantConfigs = ref<TenantConfig[]>([])
  const accessibleTenants = ref<Tenant[]>([])
  const loading = ref(false)
  const error = ref<string | null>(null)

  // 依赖的其他store
  const userStore = useUserStore()

  // 计算属性
  const activeTenants = computed(() =>
    tenants.value.filter((tenant: Tenant) => tenant.status === "ACTIVE")
  )

  const inactiveTenants = computed(() =>
    tenants.value.filter((tenant: Tenant) => tenant.status !== "ACTIVE")
  )

  const currentTenantConfig = computed(() => {
    if (!currentTenant.value) return null
    return tenantConfigs.value.find((config: TenantConfig) => config.tenantId === currentTenant.value!.id) || null
  })

  const isMultiTenant = computed(() => tenants.value.length > 1)

  const canSwitchTenant = computed(() => accessibleTenants.value.length > 1)

  const tenantsByStatus = computed(() => {
    const grouped: Record<string, Tenant[]> = {}
    tenants.value.forEach((tenant: Tenant) => {
      const status = tenant.status || "UNKNOWN"
      if (!grouped[status]) {
        grouped[status] = []
      }
      grouped[status].push(tenant)
    })
    return grouped
  })

  // Actions
  const fetchTenants = async (params?: {
    page?: number
    size?: number
    query?: string
    status?: string
  }) => {
    loading.value = true
    error.value = null
    try {
      const response = await tenantApi.getTenants(params)
      tenants.value = response.data.content || response.data
      return response.data
    } catch (err: any) {
      error.value = err.message
      throw err
    } finally {
      loading.value = false
    }
  }

  const fetchAccessibleTenants = async () => {
    if (!userStore.currentUser) return []

    loading.value = true
    error.value = null
    try {
      const response = await tenantApi.getAccessibleTenants(userStore.currentUser.id)
      accessibleTenants.value = response.data
      return response.data
    } catch (err: any) {
      error.value = err.message
      throw err
    } finally {
      loading.value = false
    }
  }

  const createTenant = async (tenantData: Partial<Tenant>) => {
    loading.value = true
    error.value = null
    try {
      const response = await tenantApi.createTenant(tenantData)
      tenants.value.push(response.data)
      return response.data
    } catch (err: any) {
      error.value = err.message
      throw err
    } finally {
      loading.value = false
    }
  }

  const updateTenant = async (tenantId: string, tenantData: Partial<Tenant>) => {
    loading.value = true
    error.value = null
    try {
      const response = await tenantApi.updateTenant(tenantId, tenantData)
      const index = tenants.value.findIndex((t: Tenant) => t.id === tenantId)
      if (index !== -1) {
        tenants.value[index] = response.data
      }

      // 如果更新的是当前租户，同步更新
      if (currentTenant.value?.id === tenantId) {
        currentTenant.value = response.data
      }

      return response.data
    } catch (err: any) {
      error.value = err.message
      throw err
    } finally {
      loading.value = false
    }
  }

  const deleteTenant = async (tenantId: string) => {
    loading.value = true
    error.value = null
    try {
      await tenantApi.deleteTenant(tenantId)
      const index = tenants.value.findIndex((t: Tenant) => t.id === tenantId)
      if (index !== -1) {
        tenants.value.splice(index, 1)
      }

      // 如果删除的是当前租户，切换到默认租户
      if (currentTenant.value?.id === tenantId) {
        await switchToDefaultTenant()
      }
    } catch (err: any) {
      error.value = err.message
      throw err
    } finally {
      loading.value = false
    }
  }

  const switchTenant = async (tenantId: string) => {
    const tenant = accessibleTenants.value.find((t: Tenant) => t.id === tenantId)
    if (!tenant) {
      throw new Error("Tenant not accessible or not found")
    }

    loading.value = true
    error.value = null
    try {
      // 调用API切换租户上下文
      await tenantApi.switchTenant(tenantId)

      // 更新当前租户
      currentTenant.value = tenant

      // 保存到本地存储
      localStorage.setItem("currentTenantId", tenantId)

      // 触发租户切换事件
      window.dispatchEvent(new CustomEvent("tenant-switched", {
        detail: {tenant, previousTenant: currentTenant.value}
      }))

      return tenant
    } catch (err: any) {
      error.value = err.message
      throw err
    } finally {
      loading.value = false
    }
  }

  const switchToDefaultTenant = async () => {
    const defaultTenant = accessibleTenants.value.find((t: Tenant) => t.isDefault) ||
                          accessibleTenants.value[0]

    if (defaultTenant) {
      await switchTenant(defaultTenant.id)
    }
  }

  const initializeTenant = async () => {
    try {
      // 获取用户可访问的租户
      await fetchAccessibleTenants()

      // 尝试从本地存储恢复租户
      const savedTenantId = localStorage.getItem("currentTenantId")
      if (savedTenantId && accessibleTenants.value.some((t: Tenant) => t.id === savedTenantId)) {
        await switchTenant(savedTenantId)
      } else {
        // 切换到默认租户
        await switchToDefaultTenant()
      }
    } catch (err: any) {
      error.value = err.message
      // 如果初始化失败，尝试使用第一个可用租户
      if (accessibleTenants.value.length > 0) {
        currentTenant.value = accessibleTenants.value[0]
      }
    }
  }

  // 租户配置管理
  const fetchTenantConfigs = async (tenantId?: string) => {
    loading.value = true
    error.value = null
    try {
      const response = await tenantApi.getTenantConfigs(tenantId)
      if (tenantId) {
        // 更新特定租户的配置
        const configs = Array.isArray(response.data) ? response.data : [response.data]
        configs.forEach((config: TenantConfig) => {
          const index = tenantConfigs.value.findIndex((c: TenantConfig) => c.id === config.id)
          if (index !== -1) {
            tenantConfigs.value[index] = config
          } else {
            tenantConfigs.value.push(config)
          }
        })
      } else {
        tenantConfigs.value = response.data
      }
      return response.data
    } catch (err: any) {
      error.value = err.message
      throw err
    } finally {
      loading.value = false
    }
  }

  const updateTenantConfig = async (configId: string, configData: Partial<TenantConfig>) => {
    loading.value = true
    error.value = null
    try {
      const response = await tenantApi.updateTenantConfig(configId, configData)
      const index = tenantConfigs.value.findIndex((c: TenantConfig) => c.id === configId)
      if (index !== -1) {
        tenantConfigs.value[index] = response.data
      }
      return response.data
    } catch (err: any) {
      error.value = err.message
      throw err
    } finally {
      loading.value = false
    }
  }

  // 租户统计
  const getTenantStatistics = async (tenantId?: string) => {
    const targetTenantId = tenantId || currentTenant.value?.id
    if (!targetTenantId) return null

    try {
      const response = await tenantApi.getTenantStatistics(targetTenantId)
      return response.data
    } catch (err: any) {
      error.value = err.message
      throw err
    }
  }

  // 租户健康检查
  const checkTenantHealth = async (tenantId?: string) => {
    const targetTenantId = tenantId || currentTenant.value?.id
    if (!targetTenantId) return null

    try {
      const response = await tenantApi.checkTenantHealth(targetTenantId)
      return response.data
    } catch (err: any) {
      error.value = err.message
      throw err
    }
  }

  // 工具方法
  const isTenantAccessible = (tenantId: string): boolean => {
    return accessibleTenants.value.some((t: Tenant) => t.id === tenantId)
  }

  const getTenantById = (tenantId: string): Tenant | undefined => {
    return tenants.value.find((t: Tenant) => t.id === tenantId)
  }

  const getCurrentTenantId = (): string | null => {
    return currentTenant.value?.id || null
  }

  const isCurrentTenant = (tenantId: string): boolean => {
    return currentTenant.value?.id === tenantId
  }

  // 清除状态
  const clearError = () => {
    error.value = null
  }

  const reset = () => {
    tenants.value = []
    currentTenant.value = null
    tenantConfigs.value = []
    accessibleTenants.value = []
    loading.value = false
    error.value = null
    localStorage.removeItem("currentTenantId")
  }

  // 监听用户变化，重新初始化租户
  watch(
    () => userStore.currentUser,
    async (newUser) => {
      if (newUser) {
        await initializeTenant()
      } else {
        reset()
      }
    }
  )

  return {
    // 状态
    tenants,
    currentTenant,
    tenantConfigs,
    accessibleTenants,
    loading,
    error,

    // 计算属性
    activeTenants,
    inactiveTenants,
    currentTenantConfig,
    isMultiTenant,
    canSwitchTenant,
    tenantsByStatus,

    // Actions
    fetchTenants,
    fetchAccessibleTenants,
    createTenant,
    updateTenant,
    deleteTenant,
    switchTenant,
    switchToDefaultTenant,
    initializeTenant,
    fetchTenantConfigs,
    updateTenantConfig,
    getTenantStatistics,
    checkTenantHealth,

    // 工具方法
    isTenantAccessible,
    getTenantById,
    getCurrentTenantId,
    isCurrentTenant,
    clearError,
    reset
  }
})
