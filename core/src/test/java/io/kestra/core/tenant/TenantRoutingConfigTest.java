package io.kestra.core.tenant;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TenantRoutingConfigTest {

    @Test
    void testDefaultConfiguration() {
        TenantRoutingConfig config = new TenantRoutingConfig();
        
        assertTrue(config.isEnabled());
        assertEquals("default", config.getDefaultTenant());
        assertEquals(TenantRoutingConfig.ExtractionStrategy.HEADER_FIRST, config.getStrategy());
        assertEquals(TenantRoutingConfig.RoutingMode.STRICT, config.getMode());
        assertEquals("X-Tenant-ID", config.getTenantHeader());
        assertEquals("tenant", config.getTenantQueryParam());
        assertFalse(config.getExemptPaths().isEmpty());
        assertFalse(config.getReservedSubdomains().isEmpty());
    }

    @Test
    void testExemptPathChecking() {
        TenantRoutingConfig config = new TenantRoutingConfig();
        
        // Test exempt paths
        assertTrue(config.isExemptPath("/health"));
        assertTrue(config.isExemptPath("/health/check"));
        assertTrue(config.isExemptPath("/metrics"));
        assertTrue(config.isExemptPath("/api/v1/auth/login"));
        assertTrue(config.isExemptPath("/static/css/style.css"));
        
        // Test non-exempt paths
        assertFalse(config.isExemptPath("/api/v1/workflows"));
        assertFalse(config.isExemptPath("/api/v1/tenant/test/workflows"));
        assertFalse(config.isExemptPath("/dashboard"));
    }

    @Test
    void testReservedSubdomainChecking() {
        TenantRoutingConfig config = new TenantRoutingConfig();
        
        // Test reserved subdomains
        assertTrue(config.isReservedSubdomain("www"));
        assertTrue(config.isReservedSubdomain("api"));
        assertTrue(config.isReservedSubdomain("admin"));
        assertTrue(config.isReservedSubdomain("mail"));
        assertTrue(config.isReservedSubdomain("dev"));
        
        // Test non-reserved subdomains
        assertFalse(config.isReservedSubdomain("tenant1"));
        assertFalse(config.isReservedSubdomain("company-a"));
        assertFalse(config.isReservedSubdomain("test-tenant"));
    }

    @Test
    void testTenantIdValidation() {
        TenantRoutingConfig config = new TenantRoutingConfig();
        
        // Valid tenant IDs
        assertTrue(config.isValidTenantId("tenant1"));
        assertTrue(config.isValidTenantId("company-a"));
        assertTrue(config.isValidTenantId("test.tenant"));
        assertTrue(config.isValidTenantId("tenant_123"));
        assertTrue(config.isValidTenantId("a1"));
        
        // Invalid tenant IDs
        assertFalse(config.isValidTenantId(null));
        assertFalse(config.isValidTenantId(""));
        assertFalse(config.isValidTenantId(" "));
        assertFalse(config.isValidTenantId("a")); // too short
        assertFalse(config.isValidTenantId("A")); // uppercase
        assertFalse(config.isValidTenantId("1tenant")); // starts with number
        assertFalse(config.isValidTenantId("tenant-")); // ends with hyphen
        assertFalse(config.isValidTenantId("-tenant")); // starts with hyphen
        assertFalse(config.isValidTenantId("tenant!")); // invalid character
        assertFalse(config.isValidTenantId("tenant space")); // contains space
    }

    @Test
    void testTenantIdNormalization() {
        TenantRoutingConfig config = new TenantRoutingConfig();
        
        assertEquals("tenant1", config.normalizeTenantId("tenant1"));
        assertEquals("tenant1", config.normalizeTenantId("TENANT1"));
        assertEquals("tenant1", config.normalizeTenantId(" tenant1 "));
        assertEquals("tenant1", config.normalizeTenantId("Tenant1"));
        assertNull(config.normalizeTenantId(null));
    }

    @Test
    void testCaseSensitiveValidation() {
        TenantRoutingConfig config = new TenantRoutingConfig();
        config.getValidation().setCaseSensitive(true);
        
        assertTrue(config.isValidTenantId("tenant1"));
        assertFalse(config.isValidTenantId("TENANT1")); // uppercase not allowed
        
        // Normalization should preserve case when case sensitive
        assertEquals("Tenant1", config.normalizeTenantId("Tenant1"));
        assertEquals("tenant1", config.normalizeTenantId(" tenant1 "));
    }

    @Test
    void testNumericOnlyValidation() {
        TenantRoutingConfig config = new TenantRoutingConfig();
        
        // By default, numeric-only is not allowed
        assertFalse(config.isValidTenantId("123"));
        assertFalse(config.isValidTenantId("456789"));
        
        // Allow numeric-only
        config.getValidation().setAllowNumericOnly(true);
        assertTrue(config.isValidTenantId("123"));
        assertTrue(config.isValidTenantId("456789"));
    }

    @Test
    void testCustomValidationPattern() {
        TenantRoutingConfig config = new TenantRoutingConfig();
        
        // Set custom pattern that only allows letters
        config.getValidation().setPattern("^[a-z]+$");
        
        assertTrue(config.isValidTenantId("tenant"));
        assertTrue(config.isValidTenantId("company"));
        assertFalse(config.isValidTenantId("tenant1")); // contains number
        assertFalse(config.isValidTenantId("tenant-a")); // contains hyphen
    }

    @Test
    void testLengthValidation() {
        TenantRoutingConfig config = new TenantRoutingConfig();
        
        // Set custom length constraints
        config.getValidation().setMinLength(5);
        config.getValidation().setMaxLength(10);
        
        assertFalse(config.isValidTenantId("ab")); // too short
        assertFalse(config.isValidTenantId("abcd")); // too short
        assertTrue(config.isValidTenantId("abcde")); // minimum length
        assertTrue(config.isValidTenantId("abcdefghij")); // maximum length
        assertFalse(config.isValidTenantId("abcdefghijk")); // too long
    }

    @Test
    void testDisabledValidation() {
        TenantRoutingConfig config = new TenantRoutingConfig();
        config.getValidation().setEnabled(false);
        
        // When validation is disabled, all non-null strings should be valid
        assertTrue(config.isValidTenantId("tenant1"));
        assertTrue(config.isValidTenantId("INVALID!"));
        assertTrue(config.isValidTenantId("123"));
        assertTrue(config.isValidTenantId("a"));
        assertFalse(config.isValidTenantId(null));
        assertFalse(config.isValidTenantId(""));
    }

    @Test
    void testEffectiveTenantId() {
        TenantRoutingConfig config = new TenantRoutingConfig();
        
        // In strict mode, should return normalized tenant or null
        config.setMode(TenantRoutingConfig.RoutingMode.STRICT);
        assertEquals("tenant1", config.getEffectiveTenantId("tenant1"));
        assertEquals("tenant1", config.getEffectiveTenantId("TENANT1"));
        assertNull(config.getEffectiveTenantId(null));
        assertNull(config.getEffectiveTenantId(""));
        
        // In lenient mode, should return normalized tenant or default
        config.setMode(TenantRoutingConfig.RoutingMode.LENIENT);
        assertEquals("tenant1", config.getEffectiveTenantId("tenant1"));
        assertEquals("default", config.getEffectiveTenantId(null));
        assertEquals("default", config.getEffectiveTenantId(""));
        assertEquals("default", config.getEffectiveTenantId(" "));
    }

    @Test
    void testExtractionStrategies() {
        TenantRoutingConfig config = new TenantRoutingConfig();
        
        // Test all extraction strategies
        for (TenantRoutingConfig.ExtractionStrategy strategy : TenantRoutingConfig.ExtractionStrategy.values()) {
            config.setStrategy(strategy);
            assertEquals(strategy, config.getStrategy());
        }
    }

    @Test
    void testRoutingModes() {
        TenantRoutingConfig config = new TenantRoutingConfig();
        
        // Test all routing modes
        for (TenantRoutingConfig.RoutingMode mode : TenantRoutingConfig.RoutingMode.values()) {
            config.setMode(mode);
            assertEquals(mode, config.getMode());
        }
    }

    @Test
    void testLoadBalancingConfig() {
        TenantRoutingConfig config = new TenantRoutingConfig();
        TenantRoutingConfig.LoadBalancingConfig lbConfig = config.getLoadBalancing();
        
        assertNotNull(lbConfig);
        assertFalse(lbConfig.isEnabled());
        assertEquals(TenantRoutingConfig.LoadBalancingConfig.Strategy.ROUND_ROBIN, lbConfig.getStrategy());
        assertNotNull(lbConfig.getHealthCheck());
        assertNotNull(lbConfig.getTenantBackends());
    }

    @Test
    void testCachingConfig() {
        TenantRoutingConfig config = new TenantRoutingConfig();
        TenantRoutingConfig.CachingConfig cachingConfig = config.getCaching();
        
        assertNotNull(cachingConfig);
        assertTrue(cachingConfig.isEnabled());
        assertTrue(cachingConfig.getTtlSeconds() > 0);
        assertTrue(cachingConfig.getMaxSize() > 0);
        assertTrue(cachingConfig.isCacheTenantResolution());
        assertTrue(cachingConfig.isCacheTenantValidation());
    }

    @Test
    void testHealthCheckConfig() {
        TenantRoutingConfig config = new TenantRoutingConfig();
        TenantRoutingConfig.LoadBalancingConfig.HealthCheckConfig healthCheck = 
            config.getLoadBalancing().getHealthCheck();
        
        assertNotNull(healthCheck);
        assertTrue(healthCheck.isEnabled());
        assertEquals("/health", healthCheck.getPath());
        assertTrue(healthCheck.getIntervalSeconds() > 0);
        assertTrue(healthCheck.getTimeoutSeconds() > 0);
        assertTrue(healthCheck.getRetries() > 0);
    }

    @Test
    void testConfigurationModification() {
        TenantRoutingConfig config = new TenantRoutingConfig();
        
        // Test modifying configuration
        config.setEnabled(false);
        config.setDefaultTenant("custom-default");
        config.setStrategy(TenantRoutingConfig.ExtractionStrategy.PATH_FIRST);
        config.setMode(TenantRoutingConfig.RoutingMode.LENIENT);
        config.setTenantHeader("X-Custom-Tenant");
        config.setTenantQueryParam("customTenant");
        
        assertFalse(config.isEnabled());
        assertEquals("custom-default", config.getDefaultTenant());
        assertEquals(TenantRoutingConfig.ExtractionStrategy.PATH_FIRST, config.getStrategy());
        assertEquals(TenantRoutingConfig.RoutingMode.LENIENT, config.getMode());
        assertEquals("X-Custom-Tenant", config.getTenantHeader());
        assertEquals("customTenant", config.getTenantQueryParam());
    }
}
