import {apiClient} from "./client"

/**
 * Tenant Configuration API client
 */
export const tenantConfigApi = {
  /**
   * Get all configurations for current tenant
   */
  getAllConfigs() {
    return apiClient.get("/api/v1/tenant/configs")
  },

  /**
   * Get configurations by category
   */
  getConfigsByCategory(category) {
    return apiClient.get(`/api/v1/tenant/configs/category/${category}`)
  },

  /**
   * Get configuration categories with counts
   */
  getCategoryCounts() {
    return apiClient.get("/api/v1/tenant/configs/categories")
  },

  /**
   * Get a specific configuration
   */
  getConfig(key) {
    return apiClient.get(`/api/v1/tenant/configs/${key}`)
  },

  /**
   * Search configurations by key pattern
   */
  searchConfigs(query) {
    return apiClient.get("/api/v1/tenant/configs/search", {
      params: {q: query}
    })
  },

  /**
   * Create or update a configuration
   */
  setConfig(key, config) {
    return apiClient.put(`/api/v1/tenant/configs/${key}`, config)
  },

  /**
   * Bulk update configurations
   */
  bulkUpdateConfigs(configs) {
    return apiClient.put("/api/v1/tenant/configs/bulk", {configs})
  },

  /**
   * Delete a configuration
   */
  deleteConfig(key) {
    return apiClient.delete(`/api/v1/tenant/configs/${key}`)
  },

  /**
   * Reset configurations to defaults
   */
  resetToDefaults() {
    return apiClient.post("/api/v1/tenant/configs/reset")
  },

  /**
   * Get configuration schema for UI rendering
   */
  getSchema() {
    return apiClient.get("/api/v1/tenant/configs/schema")
  }
}

/**
 * Configuration types for validation
 */
export const CONFIG_TYPES = {
  STRING: "STRING",
  INTEGER: "INTEGER",
  LONG: "LONG",
  DOUBLE: "DOUBLE",
  BOOLEAN: "BOOLEAN",
  JSON: "JSON",
  ARRAY: "ARRAY",
  OBJECT: "OBJECT",
  SECRET: "SECRET",
  URL: "URL",
  EMAIL: "EMAIL",
  DURATION: "DURATION",
  CRON: "CRON",
  REGEX: "REGEX"
}

/**
 * Configuration categories
 */
export const CONFIG_CATEGORIES = {
  GENERAL: "GENERAL",
  SECURITY: "SECURITY",
  STORAGE: "STORAGE",
  NOTIFICATION: "NOTIFICATION",
  INTEGRATION: "INTEGRATION",
  PERFORMANCE: "PERFORMANCE",
  UI: "UI",
  WORKFLOW: "WORKFLOW",
  MONITORING: "MONITORING",
  CUSTOM: "CUSTOM"
}

/**
 * Utility functions for configuration management
 */
export const configUtils = {
  /**
   * Format category name for display
   */
  formatCategoryName(category) {
    return category.replace(/_/g, " ").replace(/\b\w/g, l => l.toUpperCase())
  },

  /**
   * Get category icon class
   */
  getCategoryIcon(category) {
    const icons = {
      GENERAL: "fas fa-cog",
      SECURITY: "fas fa-shield-alt",
      STORAGE: "fas fa-database",
      NOTIFICATION: "fas fa-bell",
      INTEGRATION: "fas fa-plug",
      PERFORMANCE: "fas fa-tachometer-alt",
      UI: "fas fa-palette",
      WORKFLOW: "fas fa-project-diagram",
      MONITORING: "fas fa-chart-line",
      CUSTOM: "fas fa-wrench"
    }
    return icons[category] || "fas fa-cog"
  },

  /**
   * Get type badge class
   */
  getTypeBadgeClass(type) {
    const classes = {
      STRING: "bg-primary",
      INTEGER: "bg-info",
      LONG: "bg-info",
      DOUBLE: "bg-info",
      BOOLEAN: "bg-success",
      JSON: "bg-warning",
      ARRAY: "bg-warning",
      OBJECT: "bg-warning",
      SECRET: "bg-danger",
      URL: "bg-secondary",
      EMAIL: "bg-secondary",
      DURATION: "bg-dark",
      CRON: "bg-dark",
      REGEX: "bg-dark"
    }
    return classes[type] || "bg-secondary"
  },

  /**
   * Validate configuration value based on type
   */
  validateConfigValue(value, type) {
    if (value === null || value === undefined || value === "") {
      return {valid: true, error: null}
    }

    switch (type) {
      case CONFIG_TYPES.STRING:
      case CONFIG_TYPES.SECRET:
        return {valid: typeof value === "string", error: "Value must be a string"}

      case CONFIG_TYPES.INTEGER: {
        const intValue = parseInt(value, 10)
        return {
          valid: !isNaN(intValue) && intValue.toString() === value.toString(),
          error: "Value must be a valid integer"
        }
      }

      case CONFIG_TYPES.LONG: {
        const longValue = parseInt(value, 10)
        return {
          valid: !isNaN(longValue),
          error: "Value must be a valid long integer"
        }
      }

      case CONFIG_TYPES.DOUBLE: {
        const doubleValue = parseFloat(value)
        return {
          valid: !isNaN(doubleValue),
          error: "Value must be a valid number"
        }
      }

      case CONFIG_TYPES.BOOLEAN:
        return {
          valid: typeof value === "boolean" || value === "true" || value === "false",
          error: "Value must be true or false"
        }

      case CONFIG_TYPES.URL:
        try {
          new URL(value)
          return {valid: true, error: null}
        } catch {
          return {valid: false, error: "Value must be a valid URL"}
        }

      case CONFIG_TYPES.EMAIL: {
        const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
        return {
          valid: emailRegex.test(value),
          error: "Value must be a valid email address"
        }
      }

      case CONFIG_TYPES.JSON:
      case CONFIG_TYPES.OBJECT:
      case CONFIG_TYPES.ARRAY:
        try {
          JSON.parse(value)
          return {valid: true, error: null}
        } catch {
          return {valid: false, error: "Value must be valid JSON"}
        }

      case CONFIG_TYPES.CRON: {
        // Basic cron validation (5 or 6 fields)
        const cronRegex = /^(\*|([0-9]|1[0-9]|2[0-9]|3[0-9]|4[0-9]|5[0-9])|\*\/([0-9]|1[0-9]|2[0-9]|3[0-9]|4[0-9]|5[0-9])) (\*|([0-9]|1[0-9]|2[0-3])|\*\/([0-9]|1[0-9]|2[0-3])) (\*|([1-9]|1[0-9]|2[0-9]|3[0-1])|\*\/([1-9]|1[0-9]|2[0-9]|3[0-1])) (\*|([1-9]|1[0-2])|\*\/([1-9]|1[0-2])) (\*|([0-6])|\*\/([0-6]))( (\*|([0-9]{4})|\*\/([0-9]{4})))?$/
        return {
          valid: cronRegex.test(value),
          error: "Value must be a valid cron expression"
        }
      }

      case CONFIG_TYPES.REGEX:
        try {
          new RegExp(value)
          return {valid: true, error: null}
        } catch {
          return {valid: false, error: "Value must be a valid regular expression"}
        }

      case CONFIG_TYPES.DURATION: {
        // Basic duration validation (e.g., 30s, 5m, 1h, 2d)
        const durationRegex = /^(\d+)(ms|s|m|h|d)$/
        return {
          valid: durationRegex.test(value),
          error: "Value must be a valid duration (e.g., 30s, 5m, 1h, 2d)"
        }
      }

      default:
        return {valid: true, error: null}
    }
  },

  /**
   * Format configuration value for display
   */
  formatConfigValue(value, type) {
    if (value === null || value === undefined) {
      return "null"
    }

    switch (type) {
      case CONFIG_TYPES.JSON:
      case CONFIG_TYPES.OBJECT:
      case CONFIG_TYPES.ARRAY:
        if (typeof value === "object") {
          return JSON.stringify(value, null, 2)
        }
        return String(value)

      case CONFIG_TYPES.SECRET:
        return "***"

      case CONFIG_TYPES.BOOLEAN:
        return value ? "true" : "false"

      default:
        return String(value)
    }
  },

  /**
   * Get default value for configuration type
   */
  getDefaultValueForType(type) {
    switch (type) {
      case CONFIG_TYPES.STRING:
      case CONFIG_TYPES.SECRET:
      case CONFIG_TYPES.URL:
      case CONFIG_TYPES.EMAIL:
      case CONFIG_TYPES.CRON:
      case CONFIG_TYPES.REGEX:
      case CONFIG_TYPES.DURATION:
        return ""

      case CONFIG_TYPES.INTEGER:
      case CONFIG_TYPES.LONG:
        return 0

      case CONFIG_TYPES.DOUBLE:
        return 0.0

      case CONFIG_TYPES.BOOLEAN:
        return false

      case CONFIG_TYPES.JSON:
      case CONFIG_TYPES.OBJECT:
        return "{}"

      case CONFIG_TYPES.ARRAY:
        return "[]"

      default:
        return ""
    }
  }
}
