package io.kestra.core.tenant;

import io.micronaut.core.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;


/**
 * Tenant-aware cache implementation that provides automatic tenant isolation
 */
@Slf4j
public class TenantAwareCache<T> {

    /**
     * Simple cache entry with expiration support
     */
    private static class CacheEntry<T> {
        private final T value;
        private final Instant expiresAt;

        public CacheEntry(T value, @Nullable Duration ttl) {
            this.value = value;
            this.expiresAt = ttl != null ? Instant.now().plus(ttl) : null;
        }

        public T getValue() { return value; }

        public boolean isExpired() {
            return expiresAt != null && Instant.now().isAfter(expiresAt);
        }
    }

    private final Map<String, CacheEntry<T>> cache = new ConcurrentHashMap<>();
    private final String cacheName;
    private final Map<String, Set<String>> tenantKeys = new ConcurrentHashMap<>();

    public TenantAwareCache(String cacheName) {
        this.cacheName = cacheName;
    }

    /**
     * Get value from cache using current tenant context
     */
    public Optional<T> get(String key) {
        String tenantId = TenantContext.requireCurrentTenantId();
        return get(key, tenantId);
    }

    /**
     * Get value from cache for specific tenant
     */
    public Optional<T> get(String key, String tenantId) {
        String tenantKey = TenantContext.getTenantKey(tenantId, key);
        CacheEntry<T> entry = cache.get(tenantKey);

        if (entry != null && !entry.isExpired()) {
            log.debug("Cache hit for tenant '{}', key '{}' in cache '{}'", tenantId, key, cacheName);
            return Optional.of(entry.getValue());
        } else {
            if (entry != null && entry.isExpired()) {
                // Remove expired entry
                cache.remove(tenantKey);
                removeFromTenantKeys(tenantId, key);
            }
            log.debug("Cache miss for tenant '{}', key '{}' in cache '{}'", tenantId, key, cacheName);
            return Optional.empty();
        }
    }

    /**
     * Get value from cache or compute if absent using current tenant context
     */
    public T get(String key, Supplier<T> supplier) {
        String tenantId = TenantContext.requireCurrentTenantId();
        return get(key, tenantId, supplier);
    }

    /**
     * Get value from cache or compute if absent for specific tenant
     */
    public T get(String key, String tenantId, Supplier<T> supplier) {
        Optional<T> cached = get(key, tenantId);
        if (cached.isPresent()) {
            return cached.get();
        }

        // Compute value and cache it
        T value = supplier.get();
        if (value != null) {
            put(key, value, tenantId);
        }

        return value;
    }

    /**
     * Put value in cache using current tenant context
     */
    public void put(String key, T value) {
        String tenantId = TenantContext.requireCurrentTenantId();
        put(key, value, tenantId);
    }

    /**
     * Put value in cache for specific tenant
     */
    public void put(String key, T value, String tenantId) {
        String tenantKey = TenantContext.getTenantKey(tenantId, key);
        cache.put(tenantKey, new CacheEntry<>(value, null));

        // Track tenant keys for cleanup
        tenantKeys.computeIfAbsent(tenantId, k -> ConcurrentHashMap.newKeySet()).add(key);

        log.debug("Cached value for tenant '{}', key '{}' in cache '{}'", tenantId, key, cacheName);
    }

    /**
     * Put value in cache with TTL using current tenant context
     */
    public void put(String key, T value, Duration ttl) {
        String tenantId = TenantContext.requireCurrentTenantId();
        put(key, value, tenantId, ttl);
    }

    /**
     * Put value in cache with TTL for specific tenant
     */
    public void put(String key, T value, String tenantId, Duration ttl) {
        String tenantKey = TenantContext.getTenantKey(tenantId, key);
        cache.put(tenantKey, new CacheEntry<>(value, ttl));

        // Track tenant keys for cleanup
        tenantKeys.computeIfAbsent(tenantId, k -> ConcurrentHashMap.newKeySet()).add(key);

        log.debug("Cached value with TTL {}s for tenant '{}', key '{}' in cache '{}'",
                 ttl.getSeconds(), tenantId, key, cacheName);
    }

    /**
     * Remove value from cache using current tenant context
     */
    public boolean remove(String key) {
        String tenantId = TenantContext.requireCurrentTenantId();
        return remove(key, tenantId);
    }

    /**
     * Remove value from cache for specific tenant
     */
    public boolean remove(String key, String tenantId) {
        String tenantKey = TenantContext.getTenantKey(tenantId, key);
        boolean removed = cache.remove(tenantKey) != null;

        // Remove from tenant keys tracking
        removeFromTenantKeys(tenantId, key);

        if (removed) {
            log.debug("Removed cached value for tenant '{}', key '{}' from cache '{}'", tenantId, key, cacheName);
        }

        return removed;
    }

    /**
     * Remove key from tenant keys tracking
     */
    private void removeFromTenantKeys(String tenantId, String key) {
        Set<String> keys = tenantKeys.get(tenantId);
        if (keys != null) {
            keys.remove(key);
            if (keys.isEmpty()) {
                tenantKeys.remove(tenantId);
            }
        }
    }

    /**
     * Check if key exists in cache using current tenant context
     */
    public boolean containsKey(String key) {
        String tenantId = TenantContext.requireCurrentTenantId();
        return containsKey(key, tenantId);
    }

    /**
     * Check if key exists in cache for specific tenant
     */
    public boolean containsKey(String key, String tenantId) {
        return get(key, tenantId).isPresent();
    }

    /**
     * Clear all cache entries for current tenant
     */
    public void clearTenant() {
        String tenantId = TenantContext.requireCurrentTenantId();
        clearTenant(tenantId);
    }

    /**
     * Clear all cache entries for specific tenant
     */
    public void clearTenant(String tenantId) {
        Set<String> keys = tenantKeys.get(tenantId);
        if (keys != null) {
            int removedCount = 0;
            for (String key : keys) {
                String tenantKey = TenantContext.getTenantKey(tenantId, key);
                if (cache.remove(tenantKey) != null) {
                    removedCount++;
                }
            }
            tenantKeys.remove(tenantId);

            log.info("Cleared {} cached entries for tenant '{}' from cache '{}'",
                    removedCount, tenantId, cacheName);
        }
    }

    /**
     * Get all keys for current tenant
     */
    public Set<String> getKeys() {
        String tenantId = TenantContext.requireCurrentTenantId();
        return getKeys(tenantId);
    }

    /**
     * Get all keys for specific tenant
     */
    public Set<String> getKeys(String tenantId) {
        return tenantKeys.getOrDefault(tenantId, Set.of());
    }

    /**
     * Get cache size for current tenant
     */
    public int size() {
        String tenantId = TenantContext.requireCurrentTenantId();
        return size(tenantId);
    }

    /**
     * Get cache size for specific tenant
     */
    public int size(String tenantId) {
        Set<String> keys = tenantKeys.get(tenantId);
        return keys != null ? keys.size() : 0;
    }

    /**
     * Check if cache is empty for current tenant
     */
    public boolean isEmpty() {
        String tenantId = TenantContext.requireCurrentTenantId();
        return isEmpty(tenantId);
    }

    /**
     * Check if cache is empty for specific tenant
     */
    public boolean isEmpty(String tenantId) {
        return size(tenantId) == 0;
    }

    /**
     * Get cache statistics for current tenant
     */
    public CacheStats getStats() {
        String tenantId = TenantContext.requireCurrentTenantId();
        return getStats(tenantId);
    }

    /**
     * Get cache statistics for specific tenant
     */
    public CacheStats getStats(String tenantId) {
        Set<String> keys = getKeys(tenantId);
        int totalKeys = keys.size();

        // Count existing entries (some may have expired)
        int existingEntries = 0;
        for (String key : keys) {
            if (containsKey(key, tenantId)) {
                existingEntries++;
            }
        }

        return new CacheStats(tenantId, cacheName, totalKeys, existingEntries);
    }

    /**
     * Get all tenant IDs that have cached data
     */
    public Set<String> getTenantIds() {
        return tenantKeys.keySet();
    }

    /**
     * Clear all cache entries (all tenants)
     */
    public void clearAll() {
        cache.clear();
        tenantKeys.clear();
        log.info("Cleared all cached entries from cache '{}'", cacheName);
    }

    /**
     * Cache statistics for a tenant
     */
    public static class CacheStats {
        private final String tenantId;
        private final String cacheName;
        private final int totalKeys;
        private final int existingEntries;

        public CacheStats(String tenantId, String cacheName, int totalKeys, int existingEntries) {
            this.tenantId = tenantId;
            this.cacheName = cacheName;
            this.totalKeys = totalKeys;
            this.existingEntries = existingEntries;
        }

        public String getTenantId() { return tenantId; }
        public String getCacheName() { return cacheName; }
        public int getTotalKeys() { return totalKeys; }
        public int getExistingEntries() { return existingEntries; }
        public int getExpiredEntries() { return totalKeys - existingEntries; }

        @Override
        public String toString() {
            return String.format("CacheStats{tenant='%s', cache='%s', total=%d, existing=%d, expired=%d}",
                               tenantId, cacheName, totalKeys, existingEntries, getExpiredEntries());
        }
    }
}
