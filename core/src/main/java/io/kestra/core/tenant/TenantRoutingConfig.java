package io.kestra.core.tenant;

import io.micronaut.context.annotation.ConfigurationProperties;
import io.micronaut.core.annotation.Introspected;
import lombok.Data;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Configuration properties for tenant routing and multi-tenancy settings
 */
@ConfigurationProperties("kestra.tenant.routing")
@Data
@Introspected
public class TenantRoutingConfig {

    /**
     * Enable or disable tenant routing
     */
    private boolean enabled = true;

    /**
     * Default tenant ID to use when none is specified
     */
    @Pattern(regexp = "^[a-z0-9][a-z0-9-_.]*[a-z0-9]$", message = "Invalid default tenant ID format")
    private String defaultTenant = "default";

    /**
     * Tenant extraction strategy
     */
    @NotNull
    private ExtractionStrategy strategy = ExtractionStrategy.HEADER_FIRST;

    /**
     * Tenant routing mode
     */
    @NotNull
    private RoutingMode mode = RoutingMode.STRICT;

    /**
     * Custom tenant header name
     */
    @Size(min = 1, max = 100)
    private String tenantHeader = "X-Tenant-ID";

    /**
     * Custom tenant query parameter name
     */
    @Size(min = 1, max = 100)
    private String tenantQueryParam = "tenant";

    /**
     * Paths that are exempt from tenant routing
     */
    private Set<String> exemptPaths = Set.of(
        "/health",
        "/metrics",
        "/api/v1/auth",
        "/api/v1/login",
        "/api/v1/logout",
        "/api/v1/system",
        "/static",
        "/assets",
        "/favicon.ico"
    );

    /**
     * Reserved subdomains that cannot be used as tenant IDs
     */
    private Set<String> reservedSubdomains = Set.of(
        "www", "api", "app", "admin", "dashboard", "portal",
        "mail", "email", "smtp", "pop", "imap",
        "ftp", "sftp", "ssh", "vpn",
        "dev", "test", "staging", "prod", "production",
        "cdn", "static", "assets", "media", "images",
        "docs", "help", "support", "status", "health"
    );

    /**
     * Tenant validation settings
     */
    private ValidationConfig validation = new ValidationConfig();

    /**
     * Load balancing configuration for tenant routing
     */
    private LoadBalancingConfig loadBalancing = new LoadBalancingConfig();

    /**
     * Caching configuration for tenant routing
     */
    private CachingConfig caching = new CachingConfig();

    /**
     * Tenant extraction strategy enumeration
     */
    public enum ExtractionStrategy {
        /**
         * Try header first, then path, then subdomain, then query parameter
         */
        HEADER_FIRST,

        /**
         * Try path first, then header, then subdomain, then query parameter
         */
        PATH_FIRST,

        /**
         * Try subdomain first, then header, then path, then query parameter
         */
        SUBDOMAIN_FIRST,

        /**
         * Only use header
         */
        HEADER_ONLY,

        /**
         * Only use path
         */
        PATH_ONLY,

        /**
         * Only use subdomain
         */
        SUBDOMAIN_ONLY
    }

    /**
     * Tenant routing mode enumeration
     */
    public enum RoutingMode {
        /**
         * Strict mode - tenant ID is required for all non-exempt requests
         */
        STRICT,

        /**
         * Lenient mode - use default tenant when none is specified
         */
        LENIENT,

        /**
         * Optional mode - tenant routing is optional
         */
        OPTIONAL
    }

    /**
     * Tenant validation configuration
     */
    @Data
    public static class ValidationConfig {
        /**
         * Enable tenant ID validation
         */
        private boolean enabled = true;

        /**
         * Minimum tenant ID length
         */
        private int minLength = 2;

        /**
         * Maximum tenant ID length
         */
        private int maxLength = 63;

        /**
         * Custom tenant ID pattern
         */
        private String pattern = "^[a-z][a-z0-9-_.]*[a-z0-9]$";

        /**
         * Case sensitivity for tenant IDs
         */
        private boolean caseSensitive = false;

        /**
         * Allow numeric-only tenant IDs
         */
        private boolean allowNumericOnly = false;
    }

    /**
     * Load balancing configuration
     */
    @Data
    public static class LoadBalancingConfig {
        /**
         * Enable load balancing for tenant requests
         */
        private boolean enabled = false;

        /**
         * Load balancing strategy
         */
        private Strategy strategy = Strategy.ROUND_ROBIN;

        /**
         * Health check configuration
         */
        private HealthCheckConfig healthCheck = new HealthCheckConfig();

        /**
         * Tenant-specific backend mappings
         */
        private Map<String, List<String>> tenantBackends = Map.of();

        public enum Strategy {
            ROUND_ROBIN,
            LEAST_CONNECTIONS,
            WEIGHTED_ROUND_ROBIN,
            IP_HASH,
            TENANT_HASH
        }

        @Data
        public static class HealthCheckConfig {
            private boolean enabled = true;
            private String path = "/health";
            private int intervalSeconds = 30;
            private int timeoutSeconds = 5;
            private int retries = 3;
        }
    }

    /**
     * Caching configuration
     */
    @Data
    public static class CachingConfig {
        /**
         * Enable tenant routing cache
         */
        private boolean enabled = true;

        /**
         * Cache TTL in seconds
         */
        private int ttlSeconds = 300;

        /**
         * Maximum cache size
         */
        private int maxSize = 1000;

        /**
         * Cache tenant resolution results
         */
        private boolean cacheTenantResolution = true;

        /**
         * Cache tenant validation results
         */
        private boolean cacheTenantValidation = true;

        /**
         * Cache backend routing decisions
         */
        private boolean cacheBackendRouting = false;
    }

    /**
     * Check if a path is exempt from tenant routing
     */
    public boolean isExemptPath(String path) {
        if (path == null) {
            return false;
        }

        return exemptPaths.stream().anyMatch(path::startsWith);
    }

    /**
     * Check if a subdomain is reserved
     */
    public boolean isReservedSubdomain(String subdomain) {
        if (subdomain == null) {
            return false;
        }

        return reservedSubdomains.contains(subdomain.toLowerCase());
    }

    /**
     * Validate tenant ID according to configuration
     */
    public boolean isValidTenantId(String tenantId) {
        if (tenantId == null || tenantId.trim().isEmpty()) {
            return false;
        }

        if (!validation.enabled) {
            return true;
        }

        String id = validation.caseSensitive ? tenantId : tenantId.toLowerCase();

        // Check length constraints
        if (id.length() < validation.minLength || id.length() > validation.maxLength) {
            return false;
        }

        // Check if it's numeric-only
        boolean isNumericOnly = id.matches("^\\d+$");
        if (isNumericOnly) {
            return validation.allowNumericOnly;
        }

        // Check pattern for non-numeric IDs
        return id.matches(validation.pattern);
    }

    /**
     * Normalize tenant ID according to configuration
     */
    public String normalizeTenantId(String tenantId) {
        if (tenantId == null) {
            return null;
        }

        String normalized = tenantId.trim();

        if (!validation.caseSensitive) {
            normalized = normalized.toLowerCase();
        }

        return normalized;
    }

    /**
     * Get effective tenant ID (with fallback to default)
     */
    public String getEffectiveTenantId(String tenantId) {
        if (tenantId != null && !tenantId.trim().isEmpty()) {
            return normalizeTenantId(tenantId);
        }

        if (mode == RoutingMode.LENIENT) {
            return defaultTenant;
        }

        return null;
    }
}
