package io.kestra.webserver.controllers.api;

import io.kestra.core.services.TenantRoutingService;
import io.kestra.core.tenant.TenantRoutingConfig;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.*;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;
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
@Secured(SecurityRule.IS_AUTHENTICATED)
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
            // Create a mock request for testing
            MockHttpRequest mockRequest = new MockHttpRequest(
                testRequest.method,
                testRequest.path,
                testRequest.headers,
                testRequest.queryParams
            );
            
            Optional<String> tenantId = tenantRoutingService.resolveTenantId(mockRequest);
            TenantRoutingService.RoutingDecision decision = tenantRoutingService.getRoutingDecision(mockRequest);
            boolean required = tenantRoutingService.isTenantRoutingRequired(mockRequest);
            
            RoutingTestResponse response = new RoutingTestResponse(
                testRequest,
                tenantId.orElse(null),
                decision.allowed,
                decision.reason,
                required
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
    
    /**
     * Mock HTTP request for testing
     */
    private static class MockHttpRequest implements HttpRequest<Object> {
        private final String method;
        private final String path;
        private final Map<String, String> headers;
        private final Map<String, String> queryParams;
        
        public MockHttpRequest(String method, String path, Map<String, String> headers, Map<String, String> queryParams) {
            this.method = method;
            this.path = path;
            this.headers = headers != null ? headers : Map.of();
            this.queryParams = queryParams != null ? queryParams : Map.of();
        }
        
        @Override
        public io.micronaut.http.HttpMethod getMethod() {
            return io.micronaut.http.HttpMethod.valueOf(method);
        }
        
        @Override
        public String getPath() {
            return path;
        }
        
        @Override
        public io.micronaut.http.HttpHeaders getHeaders() {
            return new MockHttpHeaders(headers);
        }
        
        @Override
        public io.micronaut.http.HttpParameters getParameters() {
            return new MockHttpParameters(queryParams);
        }
        
        // Other HttpRequest methods would be implemented as needed
        @Override
        public java.net.URI getUri() { return null; }
        @Override
        public Optional<Object> getBody() { return Optional.empty(); }
        @Override
        public <T> Optional<T> getBody(Class<T> type) { return Optional.empty(); }
    }
    
    private static class MockHttpHeaders implements io.micronaut.http.HttpHeaders {
        private final Map<String, String> headers;
        
        public MockHttpHeaders(Map<String, String> headers) {
            this.headers = headers;
        }
        
        @Override
        public Optional<String> get(CharSequence name) {
            return Optional.ofNullable(headers.get(name.toString()));
        }
        
        // Other HttpHeaders methods would be implemented as needed
        @Override
        public java.util.List<String> getAll(CharSequence name) { return java.util.List.of(); }
        @Override
        public java.util.Set<String> names() { return headers.keySet(); }
        @Override
        public java.util.Collection<java.util.List<String>> values() { return java.util.List.of(); }
    }
    
    private static class MockHttpParameters implements io.micronaut.http.HttpParameters {
        private final Map<String, String> params;
        
        public MockHttpParameters(Map<String, String> params) {
            this.params = params;
        }
        
        @Override
        public Optional<String> get(CharSequence name) {
            return Optional.ofNullable(params.get(name.toString()));
        }
        
        // Other HttpParameters methods would be implemented as needed
        @Override
        public java.util.List<String> getAll(CharSequence name) { return java.util.List.of(); }
        @Override
        public java.util.Set<String> names() { return params.keySet(); }
        @Override
        public java.util.Collection<java.util.List<String>> values() { return java.util.List.of(); }
    }
}
