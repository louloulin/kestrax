package io.kestra.core.security.cache;

import io.kestra.core.models.rbac.Permission;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

class PermissionCacheTest {

    private PermissionCache permissionCache;

    @BeforeEach
    void setUp() {
        permissionCache = new PermissionCache();
    }

    @Test
    void testCacheAndRetrievePermissions() {
        String userId = "user1";
        String tenantId = "tenant1";
        String namespace = "test-namespace";
        Set<Permission> permissions = Set.of(Permission.FLOW_READ, Permission.FLOW_UPDATE);

        // Initially should return null (cache miss)
        Set<Permission> cached = permissionCache.getUserPermissions(userId, tenantId, namespace);
        assertNull(cached);

        // Cache the permissions
        permissionCache.cacheUserPermissions(userId, tenantId, namespace, permissions);

        // Should now return the cached permissions
        cached = permissionCache.getUserPermissions(userId, tenantId, namespace);
        assertNotNull(cached);
        assertEquals(permissions, cached);
    }

    @Test
    void testCacheWithNullNamespace() {
        String userId = "user1";
        String tenantId = "tenant1";
        Set<Permission> permissions = Set.of(Permission.FLOW_READ);

        // Cache with null namespace (global)
        permissionCache.cacheUserPermissions(userId, tenantId, null, permissions);

        // Should retrieve with null namespace
        Set<Permission> cached = permissionCache.getUserPermissions(userId, tenantId, null);
        assertNotNull(cached);
        assertEquals(permissions, cached);
    }

    @Test
    void testInvalidateUserPermissions() {
        String userId = "user1";
        String tenantId = "tenant1";
        String namespace1 = "namespace1";
        String namespace2 = "namespace2";
        Set<Permission> permissions = Set.of(Permission.FLOW_READ);

        // Cache permissions for multiple namespaces
        permissionCache.cacheUserPermissions(userId, tenantId, namespace1, permissions);
        permissionCache.cacheUserPermissions(userId, tenantId, namespace2, permissions);

        // Verify both are cached
        assertNotNull(permissionCache.getUserPermissions(userId, tenantId, namespace1));
        assertNotNull(permissionCache.getUserPermissions(userId, tenantId, namespace2));

        // Invalidate user permissions
        permissionCache.invalidateUserPermissions(userId, tenantId);

        // Both should now be null
        assertNull(permissionCache.getUserPermissions(userId, tenantId, namespace1));
        assertNull(permissionCache.getUserPermissions(userId, tenantId, namespace2));
    }

    @Test
    void testInvalidateNamespacePermissions() {
        String userId1 = "user1";
        String userId2 = "user2";
        String tenantId = "tenant1";
        String namespace = "test-namespace";
        Set<Permission> permissions = Set.of(Permission.FLOW_READ);

        // Cache permissions for multiple users in same namespace
        permissionCache.cacheUserPermissions(userId1, tenantId, namespace, permissions);
        permissionCache.cacheUserPermissions(userId2, tenantId, namespace, permissions);

        // Verify both are cached
        assertNotNull(permissionCache.getUserPermissions(userId1, tenantId, namespace));
        assertNotNull(permissionCache.getUserPermissions(userId2, tenantId, namespace));

        // Invalidate namespace permissions
        permissionCache.invalidateNamespacePermissions(tenantId, namespace);

        // Both should now be null
        assertNull(permissionCache.getUserPermissions(userId1, tenantId, namespace));
        assertNull(permissionCache.getUserPermissions(userId2, tenantId, namespace));
    }

    @Test
    void testInvalidateTenantPermissions() {
        String userId = "user1";
        String tenantId = "tenant1";
        String namespace1 = "namespace1";
        String namespace2 = "namespace2";
        Set<Permission> permissions = Set.of(Permission.FLOW_READ);

        // Cache permissions for multiple namespaces
        permissionCache.cacheUserPermissions(userId, tenantId, namespace1, permissions);
        permissionCache.cacheUserPermissions(userId, tenantId, namespace2, permissions);

        // Verify both are cached
        assertNotNull(permissionCache.getUserPermissions(userId, tenantId, namespace1));
        assertNotNull(permissionCache.getUserPermissions(userId, tenantId, namespace2));

        // Invalidate tenant permissions
        permissionCache.invalidateTenantPermissions(tenantId);

        // Both should now be null
        assertNull(permissionCache.getUserPermissions(userId, tenantId, namespace1));
        assertNull(permissionCache.getUserPermissions(userId, tenantId, namespace2));
    }

    @Test
    void testClearAll() {
        String userId = "user1";
        String tenantId = "tenant1";
        String namespace = "namespace";
        Set<Permission> permissions = Set.of(Permission.FLOW_READ);

        // Cache some permissions
        permissionCache.cacheUserPermissions(userId, tenantId, namespace, permissions);
        assertNotNull(permissionCache.getUserPermissions(userId, tenantId, namespace));

        // Clear all
        permissionCache.clearAll();

        // Should now be null
        assertNull(permissionCache.getUserPermissions(userId, tenantId, namespace));
    }

    @Test
    void testGetStats() {
        String userId = "user1";
        String tenantId = "tenant1";
        Set<Permission> permissions = Set.of(Permission.FLOW_READ);

        // Initially should have no entries
        PermissionCache.CacheStats stats = permissionCache.getStats();
        assertEquals(0, stats.getTotalEntries());
        assertEquals(0, stats.getActiveEntries());
        assertEquals(0, stats.getExpiredEntries());

        // Cache some permissions
        permissionCache.cacheUserPermissions(userId, tenantId, "namespace1", permissions);
        permissionCache.cacheUserPermissions(userId, tenantId, "namespace2", permissions);

        // Should now have entries
        stats = permissionCache.getStats();
        assertEquals(2, stats.getTotalEntries());
        assertEquals(2, stats.getActiveEntries());
        assertEquals(0, stats.getExpiredEntries());
    }

    @Test
    void testCacheStatsToString() {
        PermissionCache.CacheStats stats = new PermissionCache.CacheStats(10, 3);
        String str = stats.toString();
        
        assertTrue(str.contains("total=10"));
        assertTrue(str.contains("active=7"));
        assertTrue(str.contains("expired=3"));
    }

    @Test
    void testCleanupExpired() {
        // This test verifies the method doesn't throw
        // In a real scenario with expired entries, it would remove them
        assertDoesNotThrow(() -> permissionCache.cleanupExpired());
    }

    @Test
    void testCacheKeyGeneration() {
        String userId = "user1";
        String tenantId = "tenant1";
        Set<Permission> permissions = Set.of(Permission.FLOW_READ);

        // Cache with different namespace combinations
        permissionCache.cacheUserPermissions(userId, tenantId, "namespace", permissions);
        permissionCache.cacheUserPermissions(userId, tenantId, null, permissions);

        // Should be able to retrieve both independently
        assertNotNull(permissionCache.getUserPermissions(userId, tenantId, "namespace"));
        assertNotNull(permissionCache.getUserPermissions(userId, tenantId, null));
        
        // Different namespace should return null
        assertNull(permissionCache.getUserPermissions(userId, tenantId, "other-namespace"));
    }

    @Test
    void testMultipleTenants() {
        String userId = "user1";
        String namespace = "namespace";
        Set<Permission> permissions = Set.of(Permission.FLOW_READ);

        // Cache for different tenants
        permissionCache.cacheUserPermissions(userId, "tenant1", namespace, permissions);
        permissionCache.cacheUserPermissions(userId, "tenant2", namespace, permissions);

        // Should be able to retrieve both independently
        assertNotNull(permissionCache.getUserPermissions(userId, "tenant1", namespace));
        assertNotNull(permissionCache.getUserPermissions(userId, "tenant2", namespace));

        // Invalidating one tenant shouldn't affect the other
        permissionCache.invalidateTenantPermissions("tenant1");
        assertNull(permissionCache.getUserPermissions(userId, "tenant1", namespace));
        assertNotNull(permissionCache.getUserPermissions(userId, "tenant2", namespace));
    }
}
