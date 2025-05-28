package io.kestra.core.services;

import io.kestra.core.tenant.TenantRoutingConfig;
import io.micronaut.http.HttpHeaders;
import io.micronaut.http.HttpMethod;
import io.micronaut.http.HttpParameters;
import io.micronaut.http.HttpRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.net.URI;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TenantRoutingServiceTest {

    private TenantRoutingService tenantRoutingService;
    private TenantRoutingConfig config;

    @Mock
    private HttpRequest<?> mockRequest;

    @Mock
    private HttpHeaders mockHeaders;

    @Mock
    private HttpParameters mockParameters;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        config = new TenantRoutingConfig();
        config.setEnabled(true);
        config.setStrategy(TenantRoutingConfig.ExtractionStrategy.HEADER_FIRST);
        config.setMode(TenantRoutingConfig.RoutingMode.STRICT);
        config.setDefaultTenant("default");
        config.setTenantHeader("X-Tenant-ID");
        config.setTenantQueryParam("tenant");

        tenantRoutingService = new TenantRoutingService(config);
    }

    @Test
    void testResolveTenantFromHeader() {
        // Setup mock request
        when(mockRequest.getPath()).thenReturn("/api/v1/workflows");
        when(mockRequest.getMethod()).thenReturn(io.micronaut.http.HttpMethod.GET);
        when(mockRequest.getHeaders()).thenReturn(mockHeaders);
        when(mockRequest.getParameters()).thenReturn(mockParameters);
        when(mockHeaders.get("X-Tenant-ID")).thenReturn("test-tenant");
        when(mockHeaders.get("Host")).thenReturn(null);
        when(mockParameters.get("tenant")).thenReturn(null);

        Optional<String> result = tenantRoutingService.resolveTenantId(mockRequest);

        assertTrue(result.isPresent());
        assertEquals("test-tenant", result.get());
    }

    @Test
    void testResolveTenantFromPath() {
        // Setup mock request with tenant in path
        when(mockRequest.getPath()).thenReturn("/api/v1/tenant/path-tenant/workflows");
        when(mockRequest.getMethod()).thenReturn(io.micronaut.http.HttpMethod.GET);
        when(mockRequest.getHeaders()).thenReturn(mockHeaders);
        when(mockRequest.getParameters()).thenReturn(mockParameters);
        when(mockHeaders.get("X-Tenant-ID")).thenReturn(null);
        when(mockHeaders.get("Host")).thenReturn(null);
        when(mockParameters.get("tenant")).thenReturn(null);

        Optional<String> result = tenantRoutingService.resolveTenantId(mockRequest);

        assertTrue(result.isPresent());
        assertEquals("path-tenant", result.get());
    }

    @Test
    void testValidTenantId() {
        assertTrue(tenantRoutingService.isValidTenantId("valid-tenant"));
        assertTrue(tenantRoutingService.isValidTenantId("tenant123"));
        assertTrue(tenantRoutingService.isValidTenantId("test.tenant"));
        assertTrue(tenantRoutingService.isValidTenantId("tenant_name"));

        assertFalse(tenantRoutingService.isValidTenantId(null));
        assertFalse(tenantRoutingService.isValidTenantId(""));
        assertFalse(tenantRoutingService.isValidTenantId("a"));
        assertFalse(tenantRoutingService.isValidTenantId("1tenant")); // starts with number
        assertFalse(tenantRoutingService.isValidTenantId("invalid!"));
    }

    @Test
    void testCacheOperations() {
        // Test cache statistics
        TenantRoutingService.CacheStatistics stats = tenantRoutingService.getCacheStatistics();
        assertNotNull(stats);
        assertTrue(stats.resolutionCacheSize >= 0);
        assertTrue(stats.validationCacheSize >= 0);

        // Test cache clearing
        tenantRoutingService.clearCaches();

        // After clearing, cache sizes should be 0
        stats = tenantRoutingService.getCacheStatistics();
        assertEquals(0, stats.resolutionCacheSize);
        assertEquals(0, stats.validationCacheSize);
    }
}