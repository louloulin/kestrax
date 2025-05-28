package io.kestra.core.services;

import io.kestra.core.models.tenants.Tenant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

class TenantServiceTest {

    private TenantService tenantService;

    @BeforeEach
    void setUp() {
        tenantService = new TenantService();
    }

    @Test
    void testCreateTenant() {
        Tenant tenant = Tenant.builder()
            .id("test-tenant")
            .name("Test Tenant")
            .description("A test tenant")
            .status(Tenant.TenantStatus.ACTIVE)
            .plan(Tenant.TenantPlan.BASIC)
            .build();

        Tenant created = tenantService.createTenant(tenant);

        assertNotNull(created);
        assertEquals("test-tenant", created.getId());
        assertEquals("Test Tenant", created.getName());
        assertEquals(Tenant.TenantStatus.ACTIVE, created.getStatus());
        assertNotNull(created.getUpdatedAt());
    }

    @Test
    void testCreateDuplicateTenant() {
        Tenant tenant = Tenant.builder()
            .id("duplicate")
            .name("Duplicate Tenant")
            .status(Tenant.TenantStatus.ACTIVE)
            .plan(Tenant.TenantPlan.BASIC)
            .build();

        tenantService.createTenant(tenant);

        // Try to create another tenant with same ID
        assertThrows(IllegalArgumentException.class, () -> {
            tenantService.createTenant(tenant);
        });
    }

    @Test
    void testGetTenant() {
        Tenant tenant = Tenant.builder()
            .id("get-test")
            .name("Get Test")
            .status(Tenant.TenantStatus.ACTIVE)
            .plan(Tenant.TenantPlan.BASIC)
            .build();

        tenantService.createTenant(tenant);

        Optional<Tenant> retrieved = tenantService.getTenant("get-test");
        assertTrue(retrieved.isPresent());
        assertEquals("get-test", retrieved.get().getId());

        // Test non-existent tenant
        Optional<Tenant> notFound = tenantService.getTenant("non-existent");
        assertFalse(notFound.isPresent());
    }

    @Test
    void testGetActiveTenant() {
        // Create active tenant
        Tenant activeTenant = Tenant.builder()
            .id("active-tenant")
            .name("Active Tenant")
            .status(Tenant.TenantStatus.ACTIVE)
            .plan(Tenant.TenantPlan.BASIC)
            .build();
        tenantService.createTenant(activeTenant);

        // Create suspended tenant
        Tenant suspendedTenant = Tenant.builder()
            .id("suspended-tenant")
            .name("Suspended Tenant")
            .status(Tenant.TenantStatus.SUSPENDED)
            .plan(Tenant.TenantPlan.BASIC)
            .build();
        tenantService.createTenant(suspendedTenant);

        // Test active tenant retrieval
        Optional<Tenant> active = tenantService.getActiveTenant("active-tenant");
        assertTrue(active.isPresent());
        assertTrue(active.get().isActive());

        // Test suspended tenant should not be returned as active
        Optional<Tenant> suspended = tenantService.getActiveTenant("suspended-tenant");
        assertFalse(suspended.isPresent());
    }

    @Test
    void testUpdateTenant() {
        Tenant original = Tenant.builder()
            .id("update-test")
            .name("Original Name")
            .status(Tenant.TenantStatus.ACTIVE)
            .plan(Tenant.TenantPlan.BASIC)
            .build();

        tenantService.createTenant(original);

        Tenant updated = original.withName("Updated Name")
                                .withDescription("Updated description");

        Tenant result = tenantService.updateTenant("update-test", updated);

        assertEquals("Updated Name", result.getName());
        assertEquals("Updated description", result.getDescription());
        assertTrue(result.getUpdatedAt().isAfter(original.getUpdatedAt()));
    }

    @Test
    void testUpdateNonExistentTenant() {
        Tenant tenant = Tenant.builder()
            .id("non-existent")
            .name("Non Existent")
            .status(Tenant.TenantStatus.ACTIVE)
            .plan(Tenant.TenantPlan.BASIC)
            .build();

        assertThrows(IllegalArgumentException.class, () -> {
            tenantService.updateTenant("non-existent", tenant);
        });
    }

    @Test
    void testUpdateTenantIdChange() {
        Tenant original = Tenant.builder()
            .id("original-id")
            .name("Original")
            .status(Tenant.TenantStatus.ACTIVE)
            .plan(Tenant.TenantPlan.BASIC)
            .build();

        tenantService.createTenant(original);

        Tenant withChangedId = original.withId("changed-id");

        assertThrows(IllegalArgumentException.class, () -> {
            tenantService.updateTenant("original-id", withChangedId);
        });
    }

    @Test
    void testDeleteTenant() {
        Tenant tenant = Tenant.builder()
            .id("delete-test")
            .name("Delete Test")
            .status(Tenant.TenantStatus.ACTIVE)
            .plan(Tenant.TenantPlan.BASIC)
            .build();

        tenantService.createTenant(tenant);

        // Verify tenant exists
        assertTrue(tenantService.getTenant("delete-test").isPresent());

        // Delete tenant
        tenantService.deleteTenant("delete-test");

        // Verify tenant is marked as deleted
        Optional<Tenant> deleted = tenantService.getTenant("delete-test");
        assertFalse(deleted.isPresent()); // Should not be returned by getTenant
    }

    @Test
    void testDeleteDefaultTenant() {
        assertThrows(IllegalArgumentException.class, () -> {
            tenantService.deleteTenant("default");
        });
    }

    @Test
    void testDeleteNonExistentTenant() {
        assertThrows(IllegalArgumentException.class, () -> {
            tenantService.deleteTenant("non-existent");
        });
    }

    @Test
    void testActivateTenant() {
        Tenant tenant = Tenant.builder()
            .id("activate-test")
            .name("Activate Test")
            .status(Tenant.TenantStatus.PENDING_ACTIVATION)
            .plan(Tenant.TenantPlan.BASIC)
            .build();

        tenantService.createTenant(tenant);

        Tenant activated = tenantService.activateTenant("activate-test");

        assertEquals(Tenant.TenantStatus.ACTIVE, activated.getStatus());
        assertTrue(activated.isActive());
    }

    @Test
    void testSuspendTenant() {
        Tenant tenant = Tenant.builder()
            .id("suspend-test")
            .name("Suspend Test")
            .status(Tenant.TenantStatus.ACTIVE)
            .plan(Tenant.TenantPlan.BASIC)
            .build();

        tenantService.createTenant(tenant);

        Tenant suspended = tenantService.suspendTenant("suspend-test");

        assertEquals(Tenant.TenantStatus.SUSPENDED, suspended.getStatus());
        assertTrue(suspended.isSuspended());
    }

    @Test
    void testSuspendDefaultTenant() {
        assertThrows(IllegalArgumentException.class, () -> {
            tenantService.suspendTenant("default");
        });
    }

    @Test
    void testListTenants() {
        // Create test tenants
        tenantService.createTenant(Tenant.builder()
            .id("tenant1")
            .name("Tenant 1")
            .status(Tenant.TenantStatus.ACTIVE)
            .plan(Tenant.TenantPlan.BASIC)
            .build());

        tenantService.createTenant(Tenant.builder()
            .id("tenant2")
            .name("Tenant 2")
            .status(Tenant.TenantStatus.SUSPENDED)
            .plan(Tenant.TenantPlan.BASIC)
            .build());

        List<Tenant> tenants = tenantService.listTenants();

        // Should include default tenant + 2 created tenants
        assertEquals(3, tenants.size());
        assertTrue(tenants.stream().anyMatch(t -> t.getId().equals("default")));
        assertTrue(tenants.stream().anyMatch(t -> t.getId().equals("tenant1")));
        assertTrue(tenants.stream().anyMatch(t -> t.getId().equals("tenant2")));
    }

    @Test
    void testListActiveTenants() {
        // Create test tenants
        tenantService.createTenant(Tenant.builder()
            .id("active1")
            .name("Active 1")
            .status(Tenant.TenantStatus.ACTIVE)
            .plan(Tenant.TenantPlan.BASIC)
            .build());

        tenantService.createTenant(Tenant.builder()
            .id("suspended1")
            .name("Suspended 1")
            .status(Tenant.TenantStatus.SUSPENDED)
            .plan(Tenant.TenantPlan.BASIC)
            .build());

        List<Tenant> activeTenants = tenantService.listActiveTenants();

        // Should include default tenant + 1 active tenant
        assertEquals(2, activeTenants.size());
        assertTrue(activeTenants.stream().allMatch(Tenant::isActive));
        assertTrue(activeTenants.stream().anyMatch(t -> t.getId().equals("default")));
        assertTrue(activeTenants.stream().anyMatch(t -> t.getId().equals("active1")));
    }

    @Test
    void testSearchTenants() {
        // Create test tenants
        tenantService.createTenant(Tenant.builder()
            .id("search1")
            .name("Search Test 1")
            .status(Tenant.TenantStatus.ACTIVE)
            .plan(Tenant.TenantPlan.BASIC)
            .build());

        tenantService.createTenant(Tenant.builder()
            .id("search2")
            .name("Another Search")
            .status(Tenant.TenantStatus.ACTIVE)
            .plan(Tenant.TenantPlan.BASIC)
            .build());

        tenantService.createTenant(Tenant.builder()
            .id("other")
            .name("Other Tenant")
            .status(Tenant.TenantStatus.ACTIVE)
            .plan(Tenant.TenantPlan.BASIC)
            .build());

        List<Tenant> searchResults = tenantService.searchTenants("search");

        assertEquals(2, searchResults.size());
        assertTrue(searchResults.stream().anyMatch(t -> t.getId().equals("search1")));
        assertTrue(searchResults.stream().anyMatch(t -> t.getId().equals("search2")));
    }

    @Test
    void testIsActiveTenant() {
        tenantService.createTenant(Tenant.builder()
            .id("active-check")
            .name("Active Check")
            .status(Tenant.TenantStatus.ACTIVE)
            .plan(Tenant.TenantPlan.BASIC)
            .build());

        assertTrue(tenantService.isActiveTenant("active-check"));
        assertTrue(tenantService.isActiveTenant("default")); // default is active
        assertFalse(tenantService.isActiveTenant("non-existent"));
    }

    @Test
    void testCanExecute() {
        Tenant tenant = Tenant.builder()
            .id("exec-test")
            .name("Execution Test")
            .status(Tenant.TenantStatus.ACTIVE)
            .plan(Tenant.TenantPlan.BASIC)
            .limits(Tenant.TenantLimits.builder()
                .maxConcurrentExecutions(5)
                .build())
            .build();

        tenantService.createTenant(tenant);

        assertTrue(tenantService.canExecute("exec-test"));
        assertFalse(tenantService.canExecute("non-existent"));
    }

    @Test
    void testExecutionTracking() {
        Tenant tenant = Tenant.builder()
            .id("tracking-test")
            .name("Tracking Test")
            .status(Tenant.TenantStatus.ACTIVE)
            .plan(Tenant.TenantPlan.BASIC)
            .build();

        tenantService.createTenant(tenant);

        TenantService.TenantMetrics metrics = tenantService.getTenantMetrics("tracking-test");
        assertNotNull(metrics);
        assertEquals(0, metrics.getCurrentExecutions());

        // Record execution start
        tenantService.recordExecutionStart("tracking-test");
        assertEquals(1, metrics.getCurrentExecutions());
        assertEquals(1, metrics.getTotalExecutions());

        // Record execution end
        tenantService.recordExecutionEnd("tracking-test");
        assertEquals(0, metrics.getCurrentExecutions());
        assertEquals(1, metrics.getTotalExecutions());
    }

    @Test
    void testTenantStatistics() {
        // Create some test tenants
        tenantService.createTenant(Tenant.builder()
            .id("stats1")
            .name("Stats 1")
            .status(Tenant.TenantStatus.ACTIVE)
            .plan(Tenant.TenantPlan.BASIC)
            .build());

        tenantService.createTenant(Tenant.builder()
            .id("stats2")
            .name("Stats 2")
            .status(Tenant.TenantStatus.SUSPENDED)
            .plan(Tenant.TenantPlan.BASIC)
            .build());

        TenantService.TenantStatistics stats = tenantService.getTenantStatistics();

        assertEquals(3, stats.getTotalTenants()); // default + 2 created
        assertEquals(2, stats.getActiveTenants()); // default + stats1
        assertEquals(1, stats.getSuspendedTenants()); // stats2
    }

    @Test
    void testValidation() {
        // Test null ID
        assertThrows(IllegalArgumentException.class, () -> {
            tenantService.createTenant(Tenant.builder()
                .name("Test")
                .status(Tenant.TenantStatus.ACTIVE)
                .plan(Tenant.TenantPlan.BASIC)
                .build());
        });

        // Test empty name
        assertThrows(IllegalArgumentException.class, () -> {
            tenantService.createTenant(Tenant.builder()
                .id("test")
                .name("")
                .status(Tenant.TenantStatus.ACTIVE)
                .plan(Tenant.TenantPlan.BASIC)
                .build());
        });

        // Test invalid ID format
        assertThrows(IllegalArgumentException.class, () -> {
            tenantService.createTenant(Tenant.builder()
                .id("Invalid_ID")
                .name("Test")
                .status(Tenant.TenantStatus.ACTIVE)
                .plan(Tenant.TenantPlan.BASIC)
                .build());
        });
    }

    @Test
    void testDefaultTenantExists() {
        // Default tenant should exist
        Optional<Tenant> defaultTenant = tenantService.getTenant("default");
        assertTrue(defaultTenant.isPresent());
        assertEquals("default", defaultTenant.get().getId());
        assertTrue(defaultTenant.get().isActive());
    }
}
