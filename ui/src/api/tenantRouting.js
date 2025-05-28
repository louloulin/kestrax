import {apiClient} from "./client"

/**
 * Tenant Routing API client
 */
export const tenantRoutingApi = {
  /**
   * Get current tenant routing configuration
   */
  getConfig() {
    return apiClient.get("/api/v1/tenant/routing/config")
  },

  /**
   * Resolve tenant ID for current request
   */
  resolveTenant() {
    return apiClient.get("/api/v1/tenant/routing/resolve")
  },

  /**
   * Validate tenant ID format
   */
  validateTenant(tenantId) {
    return apiClient.get(`/api/v1/tenant/routing/validate/${encodeURIComponent(tenantId)}`)
  },

  /**
   * Check if a path is exempt from tenant routing
   */
  checkExemptPath(path) {
    return apiClient.get("/api/v1/tenant/routing/exempt", {
      params: {path}
    })
  },

  /**
   * Get tenant routing statistics
   */
  getStats() {
    return apiClient.get("/api/v1/tenant/routing/stats")
  },

  /**
   * Clear tenant routing caches
   */
  clearCaches() {
    return apiClient.post("/api/v1/tenant/routing/cache/clear")
  },

  /**
   * Test tenant routing for a specific configuration
   */
  testRouting(testRequest) {
    return apiClient.post("/api/v1/tenant/routing/test", testRequest)
  }
}

/**
 * Tenant routing strategies
 */
export const ROUTING_STRATEGIES = {
  HEADER_FIRST: "HEADER_FIRST",
  PATH_FIRST: "PATH_FIRST",
  SUBDOMAIN_FIRST: "SUBDOMAIN_FIRST",
  HEADER_ONLY: "HEADER_ONLY",
  PATH_ONLY: "PATH_ONLY",
  SUBDOMAIN_ONLY: "SUBDOMAIN_ONLY"
}

/**
 * Tenant routing modes
 */
export const ROUTING_MODES = {
  STRICT: "STRICT",
  LENIENT: "LENIENT",
  OPTIONAL: "OPTIONAL"
}

/**
 * Utility functions for tenant routing
 */
export const routingUtils = {
  /**
   * Format strategy name for display
   */
  formatStrategy(strategy) {
    const strategies = {
      HEADER_FIRST: "Header First",
      PATH_FIRST: "Path First",
      SUBDOMAIN_FIRST: "Subdomain First",
      HEADER_ONLY: "Header Only",
      PATH_ONLY: "Path Only",
      SUBDOMAIN_ONLY: "Subdomain Only"
    }
    return strategies[strategy] || strategy
  },

  /**
   * Format mode name for display
   */
  formatMode(mode) {
    const modes = {
      STRICT: "Strict",
      LENIENT: "Lenient",
      OPTIONAL: "Optional"
    }
    return modes[mode] || mode
  },

  /**
   * Get strategy description
   */
  getStrategyDescription(strategy) {
    const descriptions = {
      HEADER_FIRST: "Try header first, then path, then subdomain, then query parameter",
      PATH_FIRST: "Try path first, then header, then subdomain, then query parameter",
      SUBDOMAIN_FIRST: "Try subdomain first, then header, then path, then query parameter",
      HEADER_ONLY: "Only use HTTP header for tenant extraction",
      PATH_ONLY: "Only use URL path for tenant extraction",
      SUBDOMAIN_ONLY: "Only use subdomain for tenant extraction"
    }
    return descriptions[strategy] || "Unknown strategy"
  },

  /**
   * Get mode description
   */
  getModeDescription(mode) {
    const descriptions = {
      STRICT: "Tenant ID is required for all non-exempt requests",
      LENIENT: "Use default tenant when none is specified",
      OPTIONAL: "Tenant routing is optional"
    }
    return descriptions[mode] || "Unknown mode"
  },

  /**
   * Validate tenant ID format
   */
  validateTenantIdFormat(tenantId) {
    if (!tenantId || typeof tenantId !== "string") {
      return {valid: false, error: "Tenant ID must be a non-empty string"}
    }

    const trimmed = tenantId.trim()
    
    if (trimmed.length < 2) {
      return {valid: false, error: "Tenant ID must be at least 2 characters long"}
    }

    if (trimmed.length > 63) {
      return {valid: false, error: "Tenant ID must not exceed 63 characters"}
    }

    // Check format: lowercase alphanumeric with hyphens, dots, and underscores
    // Must start and end with alphanumeric character
    const pattern = /^[a-z0-9][a-z0-9-_.]*[a-z0-9]$/
    if (!pattern.test(trimmed)) {
      return { 
        valid: false, 
        error: "Tenant ID must start and end with alphanumeric characters and contain only lowercase letters, numbers, hyphens, dots, and underscores" 
      }
    }

    return {valid: true, error: null}
  },

  /**
   * Extract tenant ID from URL path
   */
  extractTenantFromPath(path) {
    if (!path) return null
    
    const match = path.match(/^\/api\/v\d+\/tenant\/([a-z0-9][a-z0-9-_.]*[a-z0-9])\/.*/)
    return match ? match[1] : null
  },

  /**
   * Extract tenant ID from subdomain
   */
  extractTenantFromSubdomain(hostname) {
    if (!hostname) return null
    
    const host = hostname.toLowerCase()
    
    // Skip localhost and IP addresses
    if (host.startsWith("localhost") || host.match(/^\d+\.\d+\.\d+\.\d+/)) {
      return null
    }
    
    const match = host.match(/^([a-z0-9][a-z0-9-_.]*[a-z0-9])\..*/)
    if (!match) return null
    
    const subdomain = match[1]
    
    // Check if subdomain is reserved
    if (this.isReservedSubdomain(subdomain)) {
      return null
    }
    
    return subdomain
  },

  /**
   * Check if subdomain is reserved
   */
  isReservedSubdomain(subdomain) {
    const reserved = [
      "www", "api", "app", "admin", "dashboard", "portal",
      "mail", "email", "smtp", "pop", "imap",
      "ftp", "sftp", "ssh", "vpn",
      "dev", "test", "staging", "prod", "production",
      "cdn", "static", "assets", "media", "images",
      "docs", "help", "support", "status", "health"
    ]
    
    return reserved.includes(subdomain.toLowerCase())
  },

  /**
   * Check if path is likely exempt from tenant routing
   */
  isLikelyExemptPath(path) {
    const exemptPatterns = [
      "/health",
      "/metrics",
      "/api/v1/auth",
      "/api/v1/login",
      "/api/v1/logout",
      "/api/v1/system",
      "/static",
      "/assets",
      "/favicon.ico"
    ]
    
    return exemptPatterns.some(pattern => path.startsWith(pattern))
  },

  /**
   * Build tenant-aware URL
   */
  buildTenantUrl(basePath, tenantId, resourcePath) {
    if (!tenantId || !resourcePath) {
      return resourcePath || basePath
    }
    
    // Remove leading slash from resourcePath if present
    const cleanResourcePath = resourcePath.startsWith("/") ? resourcePath.slice(1) : resourcePath
    
    return `${basePath}/tenant/${tenantId}/${cleanResourcePath}`
  },

  /**
   * Parse tenant routing test result
   */
  parseTestResult(result) {
    if (!result) return null
    
    return {
      success: result.allowed,
      tenantId: result.resolvedTenantId,
      required: result.required,
      reason: result.reason,
      status: result.allowed ? "success" : "error",
      message: result.reason
    }
  },

  /**
   * Generate routing test scenarios
   */
  generateTestScenarios() {
    return [
      {
        name: "Header-based routing",
        method: "GET",
        path: "/api/v1/workflows",
        headers: {"X-Tenant-ID": "example-tenant"},
        queryParams: {},
        description: "Test tenant extraction from HTTP header"
      },
      {
        name: "Path-based routing",
        method: "GET",
        path: "/api/v1/tenant/example-tenant/workflows",
        headers: {},
        queryParams: {},
        description: "Test tenant extraction from URL path"
      },
      {
        name: "Query parameter routing",
        method: "GET",
        path: "/api/v1/workflows",
        headers: {},
        queryParams: {tenant: "example-tenant"},
        description: "Test tenant extraction from query parameter"
      },
      {
        name: "Exempt path test",
        method: "GET",
        path: "/health",
        headers: {},
        queryParams: {},
        description: "Test exempt path that should not require tenant"
      },
      {
        name: "Missing tenant test",
        method: "GET",
        path: "/api/v1/workflows",
        headers: {},
        queryParams: {},
        description: "Test request without tenant information"
      }
    ]
  }
}
