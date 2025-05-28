package io.kestra.core.tenant;

import io.kestra.core.models.tenants.Tenant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;

class TenantContextTest {

    @BeforeEach
    @AfterEach
    void clearContext() {
        TenantContext.clear();
    }

    @Test
    void testSetAndGetTenantId() {
        // Initially no context
        assertFalse(TenantContext.hasTenantContext());
        assertEquals(Optional.empty(), TenantContext.getCurrentTenantId());

        // Set tenant context
        TenantContext.setTenant("test-tenant");
        
        assertTrue(TenantContext.hasTenantContext());
        assertEquals(Optional.of("test-tenant"), TenantContext.getCurrentTenantId());
        assertEquals("test-tenant", TenantContext.requireCurrentTenantId());
    }

    @Test
    void testSetTenantWithFullInfo() {
        Tenant tenant = Tenant.builder()
            .id("test-tenant")
            .name("Test Tenant")
            .status(Tenant.TenantStatus.ACTIVE)
            .plan(Tenant.TenantPlan.BASIC)
            .build();

        TenantContext.setTenant("test-tenant", tenant, "user123", "session456");

        assertEquals(Optional.of("test-tenant"), TenantContext.getCurrentTenantId());
        assertEquals(Optional.of(tenant), TenantContext.getCurrentTenant());
        assertEquals(Optional.of("user123"), TenantContext.getCurrentUserId());
        assertEquals(Optional.of("session456"), TenantContext.getCurrentSessionId());

        TenantContext.TenantInfo info = TenantContext.getCurrentTenantInfo().orElse(null);
        assertNotNull(info);
        assertEquals("test-tenant", info.getTenantId());
        assertEquals(Optional.of(tenant), info.getTenant());
        assertEquals(Optional.of("user123"), info.getUserId());
        assertEquals(Optional.of("session456"), info.getSessionId());
    }

    @Test
    void testRequireCurrentTenantIdThrowsWhenNotSet() {
        assertThrows(IllegalStateException.class, () -> {
            TenantContext.requireCurrentTenantId();
        });
    }

    @Test
    void testSetTenantWithNullId() {
        assertThrows(IllegalArgumentException.class, () -> {
            TenantContext.setTenant(null);
        });

        assertThrows(IllegalArgumentException.class, () -> {
            TenantContext.setTenant("");
        });

        assertThrows(IllegalArgumentException.class, () -> {
            TenantContext.setTenant("  ");
        });
    }

    @Test
    void testIsCurrentTenant() {
        TenantContext.setTenant("test-tenant");

        assertTrue(TenantContext.isCurrentTenant("test-tenant"));
        assertFalse(TenantContext.isCurrentTenant("other-tenant"));
        assertFalse(TenantContext.isCurrentTenant(null));
    }

    @Test
    void testClearContext() {
        TenantContext.setTenant("test-tenant");
        assertTrue(TenantContext.hasTenantContext());

        TenantContext.clear();
        assertFalse(TenantContext.hasTenantContext());
        assertEquals(Optional.empty(), TenantContext.getCurrentTenantId());
    }

    @Test
    void testWithTenantSupplier() {
        // Set initial context
        TenantContext.setTenant("initial-tenant");

        String result = TenantContext.withTenant("temp-tenant", () -> {
            assertEquals("temp-tenant", TenantContext.requireCurrentTenantId());
            return "success";
        });

        assertEquals("success", result);
        assertEquals("initial-tenant", TenantContext.requireCurrentTenantId());
    }

    @Test
    void testWithTenantRunnable() {
        TenantContext.setTenant("initial-tenant");

        TenantContext.withTenant("temp-tenant", () -> {
            assertEquals("temp-tenant", TenantContext.requireCurrentTenantId());
        });

        assertEquals("initial-tenant", TenantContext.requireCurrentTenantId());
    }

    @Test
    void testWithTenantFullInfo() {
        Tenant tenant = Tenant.builder()
            .id("temp-tenant")
            .name("Temp Tenant")
            .status(Tenant.TenantStatus.ACTIVE)
            .plan(Tenant.TenantPlan.BASIC)
            .build();

        String result = TenantContext.withTenant("temp-tenant", tenant, "user123", "session456", () -> {
            assertEquals("temp-tenant", TenantContext.requireCurrentTenantId());
            assertEquals(Optional.of(tenant), TenantContext.getCurrentTenant());
            assertEquals(Optional.of("user123"), TenantContext.getCurrentUserId());
            assertEquals(Optional.of("session456"), TenantContext.getCurrentSessionId());
            return "success";
        });

        assertEquals("success", result);
        assertFalse(TenantContext.hasTenantContext());
    }

    @Test
    void testWithTenantNested() {
        TenantContext.setTenant("outer-tenant");

        String result = TenantContext.withTenant("middle-tenant", () -> {
            assertEquals("middle-tenant", TenantContext.requireCurrentTenantId());
            
            return TenantContext.withTenant("inner-tenant", () -> {
                assertEquals("inner-tenant", TenantContext.requireCurrentTenantId());
                return "nested-success";
            });
        });

        assertEquals("nested-success", result);
        assertEquals("outer-tenant", TenantContext.requireCurrentTenantId());
    }

    @Test
    void testValidateTenantAccess() {
        TenantContext.setTenant("allowed-tenant");

        // Should not throw for same tenant
        assertDoesNotThrow(() -> {
            TenantContext.validateTenantAccess("allowed-tenant");
        });

        // Should throw for different tenant
        assertThrows(TenantContext.TenantAccessException.class, () -> {
            TenantContext.validateTenantAccess("forbidden-tenant");
        });
    }

    @Test
    void testGetTenantKey() {
        TenantContext.setTenant("test-tenant");

        assertEquals("test-tenant:my-key", TenantContext.getTenantKey("my-key"));
        assertEquals("other-tenant:my-key", TenantContext.getTenantKey("other-tenant", "my-key"));
    }

    @Test
    void testGetTenantKeyWithInvalidInput() {
        TenantContext.setTenant("test-tenant");

        assertThrows(IllegalArgumentException.class, () -> {
            TenantContext.getTenantKey(null);
        });

        assertThrows(IllegalArgumentException.class, () -> {
            TenantContext.getTenantKey("", "key");
        });

        assertThrows(IllegalArgumentException.class, () -> {
            TenantContext.getTenantKey("tenant", null);
        });
    }

    @Test
    void testExtractTenantId() {
        assertEquals(Optional.of("test-tenant"), TenantContext.extractTenantId("test-tenant:my-key"));
        assertEquals(Optional.of(""), TenantContext.extractTenantId(":my-key"));
        assertEquals(Optional.empty(), TenantContext.extractTenantId("no-separator"));
        assertEquals(Optional.empty(), TenantContext.extractTenantId(null));
    }

    @Test
    void testExtractKey() {
        assertEquals(Optional.of("my-key"), TenantContext.extractKey("test-tenant:my-key"));
        assertEquals(Optional.of("my-key"), TenantContext.extractKey(":my-key"));
        assertEquals(Optional.of(""), TenantContext.extractKey("test-tenant:"));
        assertEquals(Optional.empty(), TenantContext.extractKey("no-separator"));
        assertEquals(Optional.empty(), TenantContext.extractKey(null));
    }

    @Test
    void testThreadIsolation() throws ExecutionException, InterruptedException {
        TenantContext.setTenant("main-tenant");

        CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
            // Different thread should not have tenant context
            assertFalse(TenantContext.hasTenantContext());
            
            TenantContext.setTenant("thread-tenant");
            return TenantContext.requireCurrentTenantId();
        });

        String threadTenantId = future.get();
        assertEquals("thread-tenant", threadTenantId);

        // Main thread should still have original context
        assertEquals("main-tenant", TenantContext.requireCurrentTenantId());
    }

    @Test
    void testTenantInfoToString() {
        TenantContext.TenantInfo info = new TenantContext.TenantInfo("test-tenant", null, "user123", "session456");
        String toString = info.toString();
        
        assertTrue(toString.contains("test-tenant"));
        assertTrue(toString.contains("user123"));
        assertTrue(toString.contains("session456"));
    }

    @Test
    void testTenantAccessException() {
        TenantContext.TenantAccessException exception = new TenantContext.TenantAccessException("Access denied");
        assertEquals("Access denied", exception.getMessage());

        RuntimeException cause = new RuntimeException("Root cause");
        TenantContext.TenantAccessException exceptionWithCause = new TenantContext.TenantAccessException("Access denied", cause);
        assertEquals("Access denied", exceptionWithCause.getMessage());
        assertEquals(cause, exceptionWithCause.getCause());
    }

    @Test
    void testWithTenantExceptionHandling() {
        TenantContext.setTenant("initial-tenant");

        assertThrows(RuntimeException.class, () -> {
            TenantContext.withTenant("temp-tenant", () -> {
                assertEquals("temp-tenant", TenantContext.requireCurrentTenantId());
                throw new RuntimeException("Test exception");
            });
        });

        // Context should be restored even after exception
        assertEquals("initial-tenant", TenantContext.requireCurrentTenantId());
    }

    @Test
    void testWithTenantRunnableExceptionHandling() {
        TenantContext.setTenant("initial-tenant");

        assertThrows(RuntimeException.class, () -> {
            TenantContext.withTenant("temp-tenant", () -> {
                assertEquals("temp-tenant", TenantContext.requireCurrentTenantId());
                throw new RuntimeException("Test exception");
            });
        });

        // Context should be restored even after exception
        assertEquals("initial-tenant", TenantContext.requireCurrentTenantId());
    }
}
