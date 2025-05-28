package io.kestra.core.tenant;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class TenantAwareCacheTest {

    private TenantAwareCache<String> tenantCache;

    @BeforeEach
    void setUp() {
        tenantCache = new TenantAwareCache<>("test-cache");
        TenantContext.clear();
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void testGetWithCurrentTenant() {
        TenantContext.setTenant("tenant1");

        // Initially empty
        Optional<String> result = tenantCache.get("key1");
        assertFalse(result.isPresent());

        // Put and get
        tenantCache.put("key1", "value1");
        result = tenantCache.get("key1");

        assertTrue(result.isPresent());
        assertEquals("value1", result.get());
    }

    @Test
    void testGetWithSpecificTenant() {
        TenantContext.setTenant("tenant1");

        // Put value for tenant2
        tenantCache.put("key1", "value1", "tenant2");

        Optional<String> result = tenantCache.get("key1", "tenant2");

        assertTrue(result.isPresent());
        assertEquals("value1", result.get());

        // Should not be accessible from tenant1
        Optional<String> tenant1Result = tenantCache.get("key1");
        assertFalse(tenant1Result.isPresent());
    }

    @Test
    void testGetCacheMiss() {
        TenantContext.setTenant("tenant1");

        Optional<String> result = tenantCache.get("key1");

        assertFalse(result.isPresent());
    }

    @Test
    void testGetWithSupplier() {
        TenantContext.setTenant("tenant1");

        AtomicInteger supplierCalls = new AtomicInteger(0);
        String result = tenantCache.get("key1", () -> {
            supplierCalls.incrementAndGet();
            return "computed-value";
        });

        assertEquals("computed-value", result);
        assertEquals(1, supplierCalls.get());

        // Should be cached now
        assertTrue(tenantCache.containsKey("key1"));
    }

    @Test
    void testGetWithSupplierCacheHit() {
        TenantContext.setTenant("tenant1");

        // First put a value
        tenantCache.put("key1", "cached-value");

        AtomicInteger supplierCalls = new AtomicInteger(0);
        String result = tenantCache.get("key1", () -> {
            supplierCalls.incrementAndGet();
            return "computed-value";
        });

        assertEquals("cached-value", result);
        assertEquals(0, supplierCalls.get()); // Supplier should not be called
    }

    @Test
    void testPutWithCurrentTenant() {
        TenantContext.setTenant("tenant1");

        tenantCache.put("key1", "value1");

        assertTrue(tenantCache.getKeys("tenant1").contains("key1"));
        assertEquals(Optional.of("value1"), tenantCache.get("key1"));
    }

    @Test
    void testPutWithSpecificTenant() {
        TenantContext.setTenant("tenant1");

        tenantCache.put("key1", "value1", "tenant2");

        assertTrue(tenantCache.getKeys("tenant2").contains("key1"));
        assertEquals(Optional.of("value1"), tenantCache.get("key1", "tenant2"));
    }

    @Test
    void testPutWithTTL() throws InterruptedException {
        TenantContext.setTenant("tenant1");
        Duration ttl = Duration.ofMillis(100);

        tenantCache.put("key1", "value1", ttl);

        assertTrue(tenantCache.getKeys("tenant1").contains("key1"));
        assertEquals(Optional.of("value1"), tenantCache.get("key1"));

        // Wait for expiration
        Thread.sleep(150);

        // Should be expired now
        assertEquals(Optional.empty(), tenantCache.get("key1"));
    }

    @Test
    void testRemoveWithCurrentTenant() {
        TenantContext.setTenant("tenant1");

        // First put a value
        tenantCache.put("key1", "value1");
        assertTrue(tenantCache.containsKey("key1"));

        boolean removed = tenantCache.remove("key1");

        assertTrue(removed);
        assertFalse(tenantCache.getKeys("tenant1").contains("key1"));
        assertFalse(tenantCache.containsKey("key1"));
    }

    @Test
    void testRemoveWithSpecificTenant() {
        TenantContext.setTenant("tenant1");

        // First put a value for tenant2
        tenantCache.put("key1", "value1", "tenant2");
        assertTrue(tenantCache.containsKey("key1", "tenant2"));

        boolean removed = tenantCache.remove("key1", "tenant2");

        assertTrue(removed);
        assertFalse(tenantCache.getKeys("tenant2").contains("key1"));
        assertFalse(tenantCache.containsKey("key1", "tenant2"));
    }

    @Test
    void testContainsKey() {
        TenantContext.setTenant("tenant1");

        tenantCache.put("key1", "value1");
        assertTrue(tenantCache.containsKey("key1"));
        assertFalse(tenantCache.containsKey("key2"));
    }

    @Test
    void testClearTenant() {
        TenantContext.setTenant("tenant1");

        // Put some values
        tenantCache.put("key1", "value1");
        tenantCache.put("key2", "value2");
        assertEquals(2, tenantCache.size("tenant1"));

        tenantCache.clearTenant("tenant1");

        assertTrue(tenantCache.getKeys("tenant1").isEmpty());
        assertEquals(0, tenantCache.size("tenant1"));
        assertFalse(tenantCache.containsKey("key1"));
        assertFalse(tenantCache.containsKey("key2"));
    }

    @Test
    void testSize() {
        TenantContext.setTenant("tenant1");

        assertEquals(0, tenantCache.size());

        tenantCache.put("key1", "value1");
        assertEquals(1, tenantCache.size());

        tenantCache.put("key2", "value2");
        assertEquals(2, tenantCache.size());

        // Different tenant should have different size
        assertEquals(0, tenantCache.size("tenant2"));
    }

    @Test
    void testIsEmpty() {
        TenantContext.setTenant("tenant1");

        assertTrue(tenantCache.isEmpty());

        tenantCache.put("key1", "value1");
        assertFalse(tenantCache.isEmpty());

        // Different tenant should be empty
        assertTrue(tenantCache.isEmpty("tenant2"));
    }

    @Test
    void testGetStats() {
        TenantContext.setTenant("tenant1");

        tenantCache.put("key1", "value1");
        tenantCache.put("key2", "value2");

        TenantAwareCache.CacheStats stats = tenantCache.getStats("tenant1");

        assertEquals("tenant1", stats.getTenantId());
        assertEquals("test-cache", stats.getCacheName());
        assertEquals(2, stats.getTotalKeys());
        assertEquals(2, stats.getExistingEntries());
        assertEquals(0, stats.getExpiredEntries());
    }

    @Test
    void testGetStatsWithExpiredEntries() throws InterruptedException {
        TenantContext.setTenant("tenant1");

        tenantCache.put("key1", "value1");
        tenantCache.put("key2", "value2", Duration.ofMillis(50));

        // Wait for key2 to expire
        Thread.sleep(100);

        TenantAwareCache.CacheStats stats = tenantCache.getStats("tenant1");

        assertEquals(2, stats.getTotalKeys());
        assertEquals(1, stats.getExistingEntries());
        assertEquals(1, stats.getExpiredEntries());
    }

    @Test
    void testGetTenantIds() {
        TenantContext.setTenant("tenant1");
        tenantCache.put("key1", "value1");

        TenantContext.setTenant("tenant2");
        tenantCache.put("key2", "value2");

        Set<String> tenantIds = tenantCache.getTenantIds();
        assertEquals(2, tenantIds.size());
        assertTrue(tenantIds.contains("tenant1"));
        assertTrue(tenantIds.contains("tenant2"));
    }

    @Test
    void testClearAll() {
        TenantContext.setTenant("tenant1");
        tenantCache.put("key1", "value1");

        TenantContext.setTenant("tenant2");
        tenantCache.put("key2", "value2");

        assertEquals(2, tenantCache.getTenantIds().size());

        tenantCache.clearAll();

        assertTrue(tenantCache.getTenantIds().isEmpty());
        assertEquals(0, tenantCache.size("tenant1"));
        assertEquals(0, tenantCache.size("tenant2"));
    }

    @Test
    void testRequiresTenantContext() {
        // Operations should fail without tenant context
        assertThrows(IllegalStateException.class, () -> tenantCache.get("key1"));
        assertThrows(IllegalStateException.class, () -> tenantCache.put("key1", "value1"));
        assertThrows(IllegalStateException.class, () -> tenantCache.remove("key1"));
        assertThrows(IllegalStateException.class, () -> tenantCache.containsKey("key1"));
        assertThrows(IllegalStateException.class, () -> tenantCache.size());
        assertThrows(IllegalStateException.class, () -> tenantCache.isEmpty());
        assertThrows(IllegalStateException.class, () -> tenantCache.getKeys());
        assertThrows(IllegalStateException.class, () -> tenantCache.getStats());
        assertThrows(IllegalStateException.class, () -> tenantCache.clearTenant());
    }

    @Test
    void testCacheStatsToString() {
        TenantAwareCache.CacheStats stats = new TenantAwareCache.CacheStats("tenant1", "test-cache", 5, 3);
        String toString = stats.toString();

        assertTrue(toString.contains("tenant1"));
        assertTrue(toString.contains("test-cache"));
        assertTrue(toString.contains("total=5"));
        assertTrue(toString.contains("existing=3"));
        assertTrue(toString.contains("expired=2"));
    }
}
