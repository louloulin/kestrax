import {defineStore} from "pinia"
import {ref, computed} from "vue"
import type {User} from "@/types/rbac"

export const useUserStore = defineStore("user", () => {
  // 状态
  const currentUser = ref<User | null>(null)
  const loading = ref(false)
  const error = ref<string | null>(null)

  // 计算属性
  const isAuthenticated = computed(() => !!currentUser.value)
  
  const isSuperAdmin = computed(() => {
    if (!currentUser.value) return false
    return currentUser.value.roles?.some(role => 
      role.name === "SUPER_ADMIN" || role.name === "SYSTEM_ADMIN"
    ) || false
  })

  const userRoles = computed(() => currentUser.value?.roles || [])
  
  const userGroups = computed(() => currentUser.value?.groups || [])

  // Actions
  const setCurrentUser = (user: User | null) => {
    currentUser.value = user
  }

  const updateCurrentUser = (userData: Partial<User>) => {
    if (currentUser.value) {
      Object.assign(currentUser.value, userData)
    }
  }

  const clearUser = () => {
    currentUser.value = null
    error.value = null
  }

  const setError = (errorMessage: string) => {
    error.value = errorMessage
  }

  const clearError = () => {
    error.value = null
  }

  return {
    // 状态
    currentUser,
    loading,
    error,

    // 计算属性
    isAuthenticated,
    isSuperAdmin,
    userRoles,
    userGroups,

    // Actions
    setCurrentUser,
    updateCurrentUser,
    clearUser,
    setError,
    clearError
  }
})
