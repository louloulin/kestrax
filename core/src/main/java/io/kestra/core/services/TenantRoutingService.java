package io.kestra.core.services;

import io.kestra.core.tenant.TenantRoutingConfig;
import io.micronaut.http.HttpRequest;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service for tenant routing and resolution logic
 */
@Singleton
@Slf4j
public class TenantRoutingService {

    private final TenantRoutingConfig config;

    // Caches for performance optimization
    private final ConcurrentMap<String, String> tenantResolutionCache = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Boolean> tenantValidationCache = new ConcurrentHashMap<>();

    // Patterns for tenant extraction
    private static final Pattern TENANT_PATH_PATTERN = Pattern.compile("^/api/v\\d+/tenant/([a-z0-9][a-z0-9-_.]*[a-z0-9])/.*");
    private static final Pattern TENANT_SUBDOMAIN_PATTERN = Pattern.compile("^([a-z0-9][a-z0-9-_.]*[a-z0-9])\\..*");

    @Inject
    public TenantRoutingService(TenantRoutingConfig config) {
        this.config = config;
    }

    /**
     * Resolve tenant ID from HTTP request
     */
    public Optional<String> resolveTenantId(HttpRequest<?> request) {
        if (!config.isEnabled()) {
            return Optional.empty();
        }

        String path = request.getPath();

        // Skip exempt paths
        if (config.isExemptPath(path)) {
            return Optional.empty();
        }

        // Try to get from cache first
        String cacheKey = buildCacheKey(request);
        if (config.getCaching().isCacheTenantResolution()) {
            String cached = tenantResolutionCache.get(cacheKey);
            if (cached != null) {
                log.debug("Tenant ID resolved from cache: {}", cached);
                return Optional.of(cached);
            }
        }

        // Extract tenant ID based on strategy
        Optional<String> tenantId = extractTenantId(request);

        if (tenantId.isPresent()) {
            String normalized = config.normalizeTenantId(tenantId.get());

            // Validate tenant ID
            if (isValidTenantId(normalized)) {
                // Cache the result
                if (config.getCaching().isCacheTenantResolution()) {
                    tenantResolutionCache.put(cacheKey, normalized);
                }

                log.debug("Tenant ID resolved: {}", normalized);
                return Optional.of(normalized);
            } else {
                log.warn("Invalid tenant ID: {}", normalized);
                return Optional.empty();
            }
        }

        // Handle fallback based on routing mode
        return handleTenantFallback();
    }

    /**
     * Extract tenant ID from request based on configured strategy
     */
    private Optional<String> extractTenantId(HttpRequest<?> request) {
        return switch (config.getStrategy()) {
            case HEADER_FIRST -> extractTenantHeaderFirst(request);
            case PATH_FIRST -> extractTenantPathFirst(request);
            case SUBDOMAIN_FIRST -> extractTenantSubdomainFirst(request);
            case HEADER_ONLY -> extractTenantFromHeader(request);
            case PATH_ONLY -> extractTenantFromPath(request);
            case SUBDOMAIN_ONLY -> extractTenantFromSubdomain(request);
        };
    }

    /**
     * Extract tenant ID with header-first strategy
     */
    private Optional<String> extractTenantHeaderFirst(HttpRequest<?> request) {
        return extractTenantFromHeader(request)
            .or(() -> extractTenantFromPath(request))
            .or(() -> extractTenantFromSubdomain(request))
            .or(() -> extractTenantFromQuery(request));
    }

    /**
     * Extract tenant ID with path-first strategy
     */
    private Optional<String> extractTenantPathFirst(HttpRequest<?> request) {
        return extractTenantFromPath(request)
            .or(() -> extractTenantFromHeader(request))
            .or(() -> extractTenantFromSubdomain(request))
            .or(() -> extractTenantFromQuery(request));
    }

    /**
     * Extract tenant ID with subdomain-first strategy
     */
    private Optional<String> extractTenantSubdomainFirst(HttpRequest<?> request) {
        return extractTenantFromSubdomain(request)
            .or(() -> extractTenantFromHeader(request))
            .or(() -> extractTenantFromPath(request))
            .or(() -> extractTenantFromQuery(request));
    }

    /**
     * Extract tenant ID from HTTP header
     */
    private Optional<String> extractTenantFromHeader(HttpRequest<?> request) {
        String header = request.getHeaders().get(config.getTenantHeader());
        if (header != null && !header.trim().isEmpty()) {
            return Optional.of(header.trim());
        }
        return Optional.empty();
    }

    /**
     * Extract tenant ID from URL path
     */
    private Optional<String> extractTenantFromPath(HttpRequest<?> request) {
        String path = request.getPath();
        Matcher matcher = TENANT_PATH_PATTERN.matcher(path);
        if (matcher.matches()) {
            return Optional.of(matcher.group(1));
        }
        return Optional.empty();
    }

    /**
     * Extract tenant ID from subdomain
     */
    private Optional<String> extractTenantFromSubdomain(HttpRequest<?> request) {
        String host = request.getHeaders().get("Host");
        if (host == null || host.trim().isEmpty()) {
            return Optional.empty();
        }

        String hostname = host.toLowerCase();

        // Skip localhost and IP addresses
        if (hostname.startsWith("localhost") || hostname.matches("^\\d+\\.\\d+\\.\\d+\\.\\d+.*")) {
            return Optional.empty();
        }

        Matcher matcher = TENANT_SUBDOMAIN_PATTERN.matcher(hostname);
        if (matcher.matches()) {
            String subdomain = matcher.group(1);

            // Check if subdomain is reserved
            if (config.isReservedSubdomain(subdomain)) {
                return Optional.empty();
            }

            return Optional.of(subdomain);
        }

        return Optional.empty();
    }

    /**
     * Extract tenant ID from query parameter
     */
    private Optional<String> extractTenantFromQuery(HttpRequest<?> request) {
        String param = request.getParameters().get(config.getTenantQueryParam());
        if (param != null && !param.trim().isEmpty()) {
            return Optional.of(param.trim());
        }
        return Optional.empty();
    }

    /**
     * Handle tenant fallback based on routing mode
     */
    private Optional<String> handleTenantFallback() {
        return switch (config.getMode()) {
            case STRICT -> Optional.empty();
            case LENIENT -> Optional.of(config.getDefaultTenant());
            case OPTIONAL -> Optional.of(config.getDefaultTenant());
        };
    }

    /**
     * Validate tenant ID
     */
    public boolean isValidTenantId(String tenantId) {
        if (tenantId == null) {
            return false;
        }

        // Check cache first
        if (config.getCaching().isCacheTenantValidation()) {
            Boolean cached = tenantValidationCache.get(tenantId);
            if (cached != null) {
                return cached;
            }
        }

        boolean valid = config.isValidTenantId(tenantId);

        // Cache the result
        if (config.getCaching().isCacheTenantValidation()) {
            tenantValidationCache.put(tenantId, valid);
        }

        return valid;
    }

    /**
     * Check if tenant routing is required for the request
     */
    public boolean isTenantRoutingRequired(HttpRequest<?> request) {
        if (!config.isEnabled()) {
            return false;
        }

        String path = request.getPath();

        // Exempt paths don't require tenant routing
        if (config.isExemptPath(path)) {
            return false;
        }

        // In strict mode, tenant routing is always required
        if (config.getMode() == TenantRoutingConfig.RoutingMode.STRICT) {
            return true;
        }

        // In lenient and optional modes, tenant routing is not strictly required
        return false;
    }

    /**
     * Get routing decision for a request
     */
    public RoutingDecision getRoutingDecision(HttpRequest<?> request) {
        Optional<String> tenantId = resolveTenantId(request);
        boolean required = isTenantRoutingRequired(request);

        if (tenantId.isPresent()) {
            return new RoutingDecision(true, tenantId.get(), "Tenant resolved successfully");
        } else if (required) {
            return new RoutingDecision(false, null, "Tenant ID is required but not found");
        } else {
            return new RoutingDecision(true, config.getDefaultTenant(), "Using default tenant");
        }
    }

    /**
     * Clear caches
     */
    public void clearCaches() {
        tenantResolutionCache.clear();
        tenantValidationCache.clear();
        log.info("Tenant routing caches cleared");
    }

    /**
     * Get cache statistics
     */
    public CacheStatistics getCacheStatistics() {
        return new CacheStatistics(
            tenantResolutionCache.size(),
            tenantValidationCache.size()
        );
    }

    /**
     * Build cache key for tenant resolution
     */
    private String buildCacheKey(HttpRequest<?> request) {
        StringBuilder key = new StringBuilder();
        key.append(request.getMethod().name()).append(":");
        key.append(request.getPath()).append(":");

        // Include relevant headers
        String tenantHeader = request.getHeaders().get(config.getTenantHeader());
        if (tenantHeader != null) {
            key.append("h:").append(tenantHeader).append(":");
        }

        String hostHeader = request.getHeaders().get("Host");
        if (hostHeader != null) {
            key.append("host:").append(hostHeader).append(":");
        }

        // Include query parameter
        String queryParam = request.getParameters().get(config.getTenantQueryParam());
        if (queryParam != null) {
            key.append("q:").append(queryParam);
        }

        return key.toString();
    }

    /**
     * Routing decision result
     */
    public static class RoutingDecision {
        public final boolean allowed;
        public final String tenantId;
        public final String reason;

        public RoutingDecision(boolean allowed, String tenantId, String reason) {
            this.allowed = allowed;
            this.tenantId = tenantId;
            this.reason = reason;
        }

        @Override
        public String toString() {
            return String.format("RoutingDecision{allowed=%s, tenantId='%s', reason='%s'}",
                allowed, tenantId, reason);
        }
    }

    /**
     * Cache statistics
     */
    public static class CacheStatistics {
        public final int resolutionCacheSize;
        public final int validationCacheSize;

        public CacheStatistics(int resolutionCacheSize, int validationCacheSize) {
            this.resolutionCacheSize = resolutionCacheSize;
            this.validationCacheSize = validationCacheSize;
        }

        @Override
        public String toString() {
            return String.format("CacheStatistics{resolutionCache=%d, validationCache=%d}",
                resolutionCacheSize, validationCacheSize);
        }
    }
}
