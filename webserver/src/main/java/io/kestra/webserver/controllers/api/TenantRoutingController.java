package io.kestra.webserver.controllers.api;

import io.kestra.core.services.TenantRoutingService;
import io.kestra.core.tenant.TenantRoutingConfig;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.*;
// import io.micronaut.security.annotation.Secured; // TODO: Enable when security is configured
// import io.micronaut.security.rules.SecurityRule; // TODO: Enable when security is configured
import io.micronaut.validation.Validated;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

import jakarta.validation.constraints.NotBlank;
import java.util.Map;
import java.util.Optional;

/**
 * REST API controller for tenant routing management and diagnostics
 */
@Controller("/api/v1/tenant/routing")
// @Secured(SecurityRule.IS_AUTHENTICATED) // TODO: Enable when security is configured
@Validated
@Slf4j
public class TenantRoutingController {

    @Inject
    private TenantRoutingService tenantRoutingService;

    @Inject
    private TenantRoutingConfig tenantRoutingConfig;

    /**
     * Get current tenant routing configuration
     */
    @Get("/config")
    public HttpResponse<TenantRoutingConfig> getRoutingConfig() {
        try {
            return HttpResponse.ok(tenantRoutingConfig);
        } catch (Exception e) {
            log.error("Error getting tenant routing configuration", e);
            return HttpResponse.serverError();
        }
    }

    /**
     * Resolve tenant ID for current request
     */
    @Get("/resolve")
    public HttpResponse<TenantResolutionResponse> resolveTenant(HttpRequest<?> request) {
        try {
            Optional<String> tenantId = tenantRoutingService.resolveTenantId(request);
            TenantRoutingService.RoutingDecision decision = tenantRoutingService.getRoutingDecision(request);

            TenantResolutionResponse response = new TenantResolutionResponse(
                tenantId.orElse(null),
                decision.allowed,
                decision.reason,
                tenantRoutingService.isTenantRoutingRequired(request)
            );

            return HttpResponse.ok(response);
        } catch (Exception e) {
            log.error("Error resolving tenant for request", e);
            return HttpResponse.serverError();
        }
    }

    /**
     * Validate tenant ID format
     */
    @Get("/validate/{tenantId}")
    public HttpResponse<TenantValidationResponse> validateTenant(@PathVariable @NotBlank String tenantId) {
        try {
            boolean valid = tenantRoutingService.isValidTenantId(tenantId);
            String normalized = tenantRoutingConfig.normalizeTenantId(tenantId);

            TenantValidationResponse response = new TenantValidationResponse(
                tenantId,
                normalized,
                valid,
                valid ? "Tenant ID is valid" : "Tenant ID format is invalid"
            );

            return HttpResponse.ok(response);
        } catch (Exception e) {
            log.error("Error validating tenant ID: " + tenantId, e);
            return HttpResponse.serverError();
        }
    }

    /**
     * Check if a path is exempt from tenant routing
     */
    @Get("/exempt")
    public HttpResponse<ExemptPathResponse> checkExemptPath(@QueryValue @NotBlank String path) {
        try {
            boolean exempt = tenantRoutingConfig.isExemptPath(path);

            ExemptPathResponse response = new ExemptPathResponse(
                path,
                exempt,
                exempt ? "Path is exempt from tenant routing" : "Path requires tenant routing"
            );

            return HttpResponse.ok(response);
        } catch (Exception e) {
            log.error("Error checking exempt path: " + path, e);
            return HttpResponse.serverError();
        }
    }

    /**
     * Get tenant routing statistics
     */
    @Get("/stats")
    public HttpResponse<RoutingStatistics> getRoutingStatistics() {
        try {
            TenantRoutingService.CacheStatistics cacheStats = tenantRoutingService.getCacheStatistics();

            RoutingStatistics stats = new RoutingStatistics(
                tenantRoutingConfig.isEnabled(),
                tenantRoutingConfig.getStrategy().name(),
                tenantRoutingConfig.getMode().name(),
                cacheStats.resolutionCacheSize,
                cacheStats.validationCacheSize,
                tenantRoutingConfig.getExemptPaths().size(),
                tenantRoutingConfig.getReservedSubdomains().size()
            );

            return HttpResponse.ok(stats);
        } catch (Exception e) {
            log.error("Error getting tenant routing statistics", e);
            return HttpResponse.serverError();
        }
    }

    /**
     * Clear tenant routing caches
     */
    @Post("/cache/clear")
    public HttpResponse<Map<String, String>> clearCaches() {
        try {
            tenantRoutingService.clearCaches();

            Map<String, String> response = Map.of(
                "status", "success",
                "message", "Tenant routing caches cleared successfully"
            );

            return HttpResponse.ok(response);
        } catch (Exception e) {
            log.error("Error clearing tenant routing caches", e);
            return HttpResponse.serverError();
        }
    }

    /**
     * Test tenant routing for a specific configuration
     */
    @Post("/test")
    public HttpResponse<RoutingTestResponse> testRouting(@Body RoutingTestRequest testRequest) {
        try {
            // TODO: Implement proper mock request testing when Mock classes are fixed
            // For now, return a placeholder response

            RoutingTestResponse response = new RoutingTestResponse(
                testRequest,
                "default", // Mock tenant
                true,      // Mock success
                "Test endpoint temporarily disabled due to API compatibility issues",
                false      // Mock required
            );

            return HttpResponse.ok(response);
        } catch (Exception e) {
            log.error("Error testing tenant routing", e);
            return HttpResponse.serverError();
        }
    }

    /**
     * Response models
     */
    public static class TenantResolutionResponse {
        public final String tenantId;
        public final boolean allowed;
        public final String reason;
        public final boolean required;

        public TenantResolutionResponse(String tenantId, boolean allowed, String reason, boolean required) {
            this.tenantId = tenantId;
            this.allowed = allowed;
            this.reason = reason;
            this.required = required;
        }
    }

    public static class TenantValidationResponse {
        public final String originalTenantId;
        public final String normalizedTenantId;
        public final boolean valid;
        public final String message;

        public TenantValidationResponse(String originalTenantId, String normalizedTenantId, boolean valid, String message) {
            this.originalTenantId = originalTenantId;
            this.normalizedTenantId = normalizedTenantId;
            this.valid = valid;
            this.message = message;
        }
    }

    public static class ExemptPathResponse {
        public final String path;
        public final boolean exempt;
        public final String message;

        public ExemptPathResponse(String path, boolean exempt, String message) {
            this.path = path;
            this.exempt = exempt;
            this.message = message;
        }
    }

    public static class RoutingStatistics {
        public final boolean enabled;
        public final String strategy;
        public final String mode;
        public final int resolutionCacheSize;
        public final int validationCacheSize;
        public final int exemptPathsCount;
        public final int reservedSubdomainsCount;

        public RoutingStatistics(boolean enabled, String strategy, String mode,
                               int resolutionCacheSize, int validationCacheSize,
                               int exemptPathsCount, int reservedSubdomainsCount) {
            this.enabled = enabled;
            this.strategy = strategy;
            this.mode = mode;
            this.resolutionCacheSize = resolutionCacheSize;
            this.validationCacheSize = validationCacheSize;
            this.exemptPathsCount = exemptPathsCount;
            this.reservedSubdomainsCount = reservedSubdomainsCount;
        }
    }

    public static class RoutingTestRequest {
        public String method = "GET";
        public String path;
        public Map<String, String> headers = Map.of();
        public Map<String, String> queryParams = Map.of();
    }

    public static class RoutingTestResponse {
        public final RoutingTestRequest request;
        public final String resolvedTenantId;
        public final boolean allowed;
        public final String reason;
        public final boolean required;

        public RoutingTestResponse(RoutingTestRequest request, String resolvedTenantId,
                                 boolean allowed, String reason, boolean required) {
            this.request = request;
            this.resolvedTenantId = resolvedTenantId;
            this.allowed = allowed;
            this.reason = reason;
            this.required = required;
        }
    }

    // TODO: Mock classes removed due to Micronaut API compatibility issues
    // These would need to be properly implemented with all required interface methods
}
