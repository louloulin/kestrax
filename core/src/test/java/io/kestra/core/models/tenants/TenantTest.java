package io.kestra.core.models.tenants;

import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

class TenantTest {

    @Test
    void testTenantCreation() {
        Tenant tenant = Tenant.builder()
            .id("test-tenant")
            .name("Test Tenant")
            .description("A test tenant")
            .status(Tenant.TenantStatus.ACTIVE)
            .plan(Tenant.TenantPlan.BASIC)
            .adminEmail("admin@test.com")
            .billingEmail("billing@test.com")
            .settings(Map.of("theme", "dark", "timezone", "UTC"))
            .enabledFeatures(Set.of("feature1", "feature2"))
            .properties(Map.of("custom", "value"))
            .limits(Tenant.TenantLimits.builder()
                .maxFlows(50)
                .maxUsers(5)
                .build())
            .build();

        assertEquals("test-tenant", tenant.getId());
        assertEquals("Test Tenant", tenant.getName());
        assertEquals("A test tenant", tenant.getDescription());
        assertEquals(Tenant.TenantStatus.ACTIVE, tenant.getStatus());
        assertEquals(Tenant.TenantPlan.BASIC, tenant.getPlan());
        assertEquals("admin@test.com", tenant.getAdminEmail());
        assertEquals("billing@test.com", tenant.getBillingEmail());
        assertNotNull(tenant.getCreatedAt());
        assertNotNull(tenant.getUpdatedAt());
        assertNull(tenant.getDeletedAt());
        assertEquals(1L, tenant.getVersion());
        assertEquals(50, tenant.getLimits().getMaxFlows());
        assertEquals(5, tenant.getLimits().getMaxUsers());
    }

    @Test
    void testTenantInterface() {
        Tenant tenant = Tenant.builder()
            .id("tenant-123")
            .name("Test")
            .status(Tenant.TenantStatus.ACTIVE)
            .plan(Tenant.TenantPlan.BASIC)
            .build();

        assertEquals("tenant-123", tenant.getTenantId());
    }

    @Test
    void testTenantStatusChecks() {
        // Active tenant
        Tenant activeTenant = Tenant.builder()
            .id("active")
            .name("Active Tenant")
            .status(Tenant.TenantStatus.ACTIVE)
            .plan(Tenant.TenantPlan.BASIC)
            .build();

        assertTrue(activeTenant.isActive());
        assertFalse(activeTenant.isSuspended());
        assertFalse(activeTenant.isDeleted());

        // Suspended tenant
        Tenant suspendedTenant = activeTenant.withStatus(Tenant.TenantStatus.SUSPENDED);
        assertFalse(suspendedTenant.isActive());
        assertTrue(suspendedTenant.isSuspended());
        assertFalse(suspendedTenant.isDeleted());

        // Deleted tenant
        Tenant deletedTenant = activeTenant.withDeletedAt(Instant.now());
        assertFalse(deletedTenant.isActive());
        assertFalse(deletedTenant.isSuspended());
        assertTrue(deletedTenant.isDeleted());
    }

    @Test
    void testFeatureChecks() {
        Tenant tenant = Tenant.builder()
            .id("test")
            .name("Test")
            .status(Tenant.TenantStatus.ACTIVE)
            .plan(Tenant.TenantPlan.BASIC)
            .enabledFeatures(Set.of("feature1", "feature2"))
            .build();

        assertTrue(tenant.hasFeature("feature1"));
        assertTrue(tenant.hasFeature("feature2"));
        assertFalse(tenant.hasFeature("feature3"));

        // Test with null features
        Tenant tenantWithoutFeatures = tenant.withEnabledFeatures(null);
        assertFalse(tenantWithoutFeatures.hasFeature("feature1"));
    }

    @Test
    void testSettingsAndProperties() {
        Tenant tenant = Tenant.builder()
            .id("test")
            .name("Test")
            .status(Tenant.TenantStatus.ACTIVE)
            .plan(Tenant.TenantPlan.BASIC)
            .settings(Map.of("timeout", 30, "retries", 3, "enabled", true))
            .properties(Map.of("region", "us-east-1", "env", "prod"))
            .build();

        // Test settings
        assertEquals(30, tenant.getSetting("timeout", 60));
        assertEquals(3, tenant.getSetting("retries", 1));
        assertEquals(true, tenant.getSetting("enabled", false));
        assertEquals(60, tenant.getSetting("missing", 60)); // default value

        // Test properties
        assertEquals("us-east-1", tenant.getProperty("region", "us-west-1"));
        assertEquals("prod", tenant.getProperty("env", "dev"));
        assertEquals("us-west-1", tenant.getProperty("missing", "us-west-1")); // default value

        // Test with null settings/properties
        Tenant emptyTenant = tenant.withSettings(null).withProperties(null);
        assertEquals(60, emptyTenant.getSetting("timeout", 60));
        assertEquals("us-west-1", emptyTenant.getProperty("region", "us-west-1"));
    }

    @Test
    void testLimitChecks() {
        Tenant.TenantLimits limits = Tenant.TenantLimits.builder()
            .maxFlows(10)
            .maxConcurrentExecutions(5)
            .maxUsers(3)
            .maxStorageBytes(1000L)
            .build();

        Tenant tenant = Tenant.builder()
            .id("test")
            .name("Test")
            .status(Tenant.TenantStatus.ACTIVE)
            .plan(Tenant.TenantPlan.BASIC)
            .limits(limits)
            .build();

        // Test execution limits
        assertTrue(tenant.canExecute(3)); // under limit
        assertTrue(tenant.canExecute(4)); // at limit
        assertFalse(tenant.canExecute(5)); // over limit

        // Test flow limits
        assertTrue(tenant.canCreateFlow(9)); // under limit
        assertFalse(tenant.canCreateFlow(10)); // at limit

        // Test user limits
        assertTrue(tenant.canAddUser(2)); // under limit
        assertFalse(tenant.canAddUser(3)); // at limit

        // Test storage limits
        assertTrue(tenant.hasStorageCapacity(500L, 400L)); // 900 total, under limit
        assertTrue(tenant.hasStorageCapacity(500L, 500L)); // 1000 total, at limit
        assertFalse(tenant.hasStorageCapacity(500L, 600L)); // 1100 total, over limit

        // Test with null limits
        Tenant unlimitedTenant = tenant.withLimits(null);
        assertTrue(unlimitedTenant.canExecute(1000));
        assertTrue(unlimitedTenant.canCreateFlow(1000));
        assertTrue(unlimitedTenant.canAddUser(1000));
        assertTrue(unlimitedTenant.hasStorageCapacity(1000L, 1000L));
    }

    @Test
    void testDefaultTenant() {
        Tenant defaultTenant = Tenant.createDefault();

        assertEquals("default", defaultTenant.getId());
        assertEquals("Default Tenant", defaultTenant.getName());
        assertEquals(Tenant.TenantStatus.ACTIVE, defaultTenant.getStatus());
        assertEquals(Tenant.TenantPlan.ENTERPRISE, defaultTenant.getPlan());
        assertTrue(defaultTenant.isActive());

        // Default tenant should have unlimited resources
        assertTrue(defaultTenant.canExecute(Integer.MAX_VALUE - 1));
        assertTrue(defaultTenant.canCreateFlow(Integer.MAX_VALUE - 1));
        assertTrue(defaultTenant.canAddUser(Integer.MAX_VALUE - 1));
    }

    @Test
    void testBasicTenant() {
        Tenant basicTenant = Tenant.createBasic("basic-tenant", "Basic Tenant");

        assertEquals("basic-tenant", basicTenant.getId());
        assertEquals("Basic Tenant", basicTenant.getName());
        assertEquals(Tenant.TenantStatus.PENDING_ACTIVATION, basicTenant.getStatus());
        assertEquals(Tenant.TenantPlan.BASIC, basicTenant.getPlan());
        assertNotNull(basicTenant.getLimits());
        assertFalse(basicTenant.isActive()); // pending activation
    }

    @Test
    void testTenantOperations() {
        Tenant tenant = Tenant.builder()
            .id("test")
            .name("Test")
            .status(Tenant.TenantStatus.PENDING_ACTIVATION)
            .plan(Tenant.TenantPlan.BASIC)
            .version(1L)
            .build();

        // Test update
        Tenant updatedTenant = tenant.withUpdate();
        assertEquals(2L, updatedTenant.getVersion());
        assertTrue(updatedTenant.getUpdatedAt().isAfter(tenant.getUpdatedAt()));

        // Test activation
        Tenant activatedTenant = tenant.activate();
        assertEquals(Tenant.TenantStatus.ACTIVE, activatedTenant.getStatus());
        assertTrue(activatedTenant.isActive());
        assertEquals(2L, activatedTenant.getVersion());

        // Test suspension
        Tenant suspendedTenant = activatedTenant.suspend();
        assertEquals(Tenant.TenantStatus.SUSPENDED, suspendedTenant.getStatus());
        assertTrue(suspendedTenant.isSuspended());
        assertEquals(3L, suspendedTenant.getVersion());

        // Test deletion
        Tenant deletedTenant = tenant.markAsDeleted();
        assertEquals(Tenant.TenantStatus.PENDING_DELETION, deletedTenant.getStatus());
        assertNotNull(deletedTenant.getDeletedAt());
        assertTrue(deletedTenant.isDeleted());
        assertEquals(2L, deletedTenant.getVersion());
    }

    @Test
    void testTenantLimitsDefaults() {
        Tenant.TenantLimits limits = Tenant.TenantLimits.builder().build();

        assertEquals(100, limits.getMaxFlows());
        assertEquals(1000, limits.getMaxExecutionsPerDay());
        assertEquals(10, limits.getMaxConcurrentExecutions());
        assertEquals(10, limits.getMaxUsers());
        assertEquals(1024L * 1024L * 1024L, limits.getMaxStorageBytes()); // 1GB
        assertEquals(3600, limits.getMaxExecutionDurationSeconds()); // 1 hour
        assertEquals(1000, limits.getApiCallsPerMinute());
        assertEquals(10, limits.getMaxWebhooks());
        assertEquals(50, limits.getMaxTriggers());
    }

    @Test
    void testTenantLimitsCustom() {
        Tenant.TenantLimits limits = Tenant.TenantLimits.builder()
            .maxFlows(200)
            .maxUsers(50)
            .maxStorageBytes(5L * 1024L * 1024L * 1024L) // 5GB
            .build();

        assertEquals(200, limits.getMaxFlows());
        assertEquals(50, limits.getMaxUsers());
        assertEquals(5L * 1024L * 1024L * 1024L, limits.getMaxStorageBytes());
        // Other values should use defaults
        assertEquals(1000, limits.getMaxExecutionsPerDay());
        assertEquals(10, limits.getMaxConcurrentExecutions());
    }

    @Test
    void testImmutability() {
        Tenant originalTenant = Tenant.builder()
            .id("test")
            .name("Original")
            .status(Tenant.TenantStatus.ACTIVE)
            .plan(Tenant.TenantPlan.BASIC)
            .version(1L)
            .build();

        Tenant modifiedTenant = originalTenant
            .withName("Modified")
            .withStatus(Tenant.TenantStatus.SUSPENDED)
            .withVersion(2L);

        // Original should be unchanged
        assertEquals("Original", originalTenant.getName());
        assertEquals(Tenant.TenantStatus.ACTIVE, originalTenant.getStatus());
        assertEquals(1L, originalTenant.getVersion());

        // Modified should have changes
        assertEquals("Modified", modifiedTenant.getName());
        assertEquals(Tenant.TenantStatus.SUSPENDED, modifiedTenant.getStatus());
        assertEquals(2L, modifiedTenant.getVersion());
    }
}
