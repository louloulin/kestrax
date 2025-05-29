package io.kestra.webserver.filters;

import io.kestra.core.tenant.TenantContext;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.http.annotation.Filter;
import io.micronaut.http.filter.HttpServerFilter;
import io.micronaut.http.filter.ServerFilterChain;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * HTTP filter for tenant-aware API routing and request isolation
 */
@Filter("/**")
@Slf4j
public class TenantRoutingFilter implements HttpServerFilter {

    private static final int FILTER_ORDER = -100; // Execute early in the filter chain

    // Pattern to extract tenant ID from various URL formats
    private static final Pattern TENANT_PATH_PATTERN = Pattern.compile("^/api/v\\d+/tenant/([a-z0-9][a-z0-9-_.]*[a-z0-9])/.*");
    private static final Pattern TENANT_SUBDOMAIN_PATTERN = Pattern.compile("^([a-z0-9][a-z0-9-_.]*[a-z0-9])\\..*");

    // Tenant header names
    private static final String TENANT_HEADER = "X-Tenant-ID";
    private static final String TENANT_CONTEXT_HEADER = "X-Tenant-Context";

    // Paths that don't require tenant context
    private static final String[] TENANT_EXEMPT_PATHS = {
        "/health",
        "/metrics",
        "/api/v1/auth",
        "/api/v1/login",
        "/api/v1/logout",
        "/api/v1/system",
        "/static",
        "/assets",
        "/favicon.ico"
    };

    @Override
    public int getOrder() {
        return FILTER_ORDER;
    }

    @Override
    public Publisher<MutableHttpResponse<?>> doFilter(HttpRequest<?> request, ServerFilterChain chain) {
        String path = request.getPath();
        String method = request.getMethod().name();

        log.debug("Processing request: {} {}", method, path);

        // Skip tenant processing for exempt paths
        if (isExemptPath(path)) {
            log.debug("Skipping tenant processing for exempt path: {}", path);
            return chain.proceed(request);
        }

        try {
            // Extract tenant ID from request
            Optional<String> tenantId = extractTenantId(request);

            if (tenantId.isEmpty()) {
                log.warn("No tenant ID found in request: {} {}", method, path);
                return createErrorResponse(HttpStatus.BAD_REQUEST, "Tenant ID is required");
            }

            String tenant = tenantId.get();
            log.debug("Extracted tenant ID: {}", tenant);

            // Validate tenant ID format
            if (!isValidTenantId(tenant)) {
                log.warn("Invalid tenant ID format: {}", tenant);
                return createErrorResponse(HttpStatus.BAD_REQUEST, "Invalid tenant ID format");
            }

            // Set tenant context for the request
            TenantContext.setTenant(tenant);

            // Add tenant information to response headers
            return Mono.from(chain.proceed(request))
                .map(response -> {
                    response.header(TENANT_CONTEXT_HEADER, tenant);
                    return response;
                });

        } catch (Exception e) {
            log.error("Error processing tenant routing for request: {} {}", method, path, e);
            return createErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error");
        } finally {
            // Always clear tenant context after request processing
            TenantContext.clear();
        }
    }

    /**
     * Extract tenant ID from various sources in the request
     */
    private Optional<String> extractTenantId(HttpRequest<?> request) {
        // 1. Try to extract from URL path (e.g., /api/v1/tenant/{tenantId}/...)
        Optional<String> pathTenant = extractTenantFromPath(request.getPath());
        if (pathTenant.isPresent()) {
            return pathTenant;
        }

        // 2. Try to extract from HTTP header
        String headerTenant = request.getHeaders().get(TENANT_HEADER);
        if (headerTenant != null && !headerTenant.trim().isEmpty()) {
            return Optional.of(headerTenant);
        }

        // 3. Try to extract from subdomain (e.g., tenant1.dataflare.io)
        Optional<String> subdomainTenant = extractTenantFromSubdomain(request);
        if (subdomainTenant.isPresent()) {
            return subdomainTenant;
        }

        // 4. Try to extract from query parameter
        String queryTenant = request.getParameters().get("tenant");
        if (queryTenant != null && !queryTenant.trim().isEmpty()) {
            return Optional.of(queryTenant);
        }

        return Optional.empty();
    }

    /**
     * Extract tenant ID from URL path
     */
    private Optional<String> extractTenantFromPath(String path) {
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
        if (host == null) {
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

            // Skip common subdomains that are not tenant IDs
            if (isReservedSubdomain(subdomain)) {
                return Optional.empty();
            }

            return Optional.of(subdomain);
        }

        return Optional.empty();
    }

    /**
     * Check if the path is exempt from tenant processing
     */
    private boolean isExemptPath(String path) {
        for (String exemptPath : TENANT_EXEMPT_PATHS) {
            if (path.startsWith(exemptPath)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Validate tenant ID format
     */
    private boolean isValidTenantId(String tenantId) {
        if (tenantId == null || tenantId.trim().isEmpty()) {
            return false;
        }

        // Check length constraints
        if (tenantId.length() < 2 || tenantId.length() > 63) {
            return false;
        }

        // Check format: lowercase alphanumeric with hyphens, dots, and underscores
        // Must start and end with alphanumeric character
        return tenantId.matches("^[a-z0-9][a-z0-9-_.]*[a-z0-9]$");
    }

    /**
     * Check if subdomain is reserved and should not be treated as tenant ID
     */
    private boolean isReservedSubdomain(String subdomain) {
        String[] reserved = {
            "www", "api", "app", "admin", "dashboard", "portal",
            "mail", "email", "smtp", "pop", "imap",
            "ftp", "sftp", "ssh", "vpn",
            "dev", "test", "staging", "prod", "production",
            "cdn", "static", "assets", "media", "images",
            "docs", "help", "support", "status", "health"
        };

        for (String reservedName : reserved) {
            if (reservedName.equals(subdomain)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Create error response
     */
    private Publisher<MutableHttpResponse<?>> createErrorResponse(HttpStatus status, String message) {
        MutableHttpResponse<Object> response = HttpResponse.status(status);
        response.body(new ErrorResponse(status.getCode(), message));
        response.contentType("application/json");
        return Mono.just(response);
    }

    /**
     * Error response model
     */
    public static class ErrorResponse {
        public final int code;
        public final String message;
        public final long timestamp;

        public ErrorResponse(int code, String message) {
            this.code = code;
            this.message = message;
            this.timestamp = System.currentTimeMillis();
        }
    }
}
