package io.kestra.core.security.cache;

import io.kestra.core.models.rbac.Permission;
import io.micronaut.core.annotation.Nullable;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Cache for user permissions to improve performance of permission checks
 */
@Singleton
@Slf4j
public class PermissionCache {

    private final ConcurrentMap<String, CachedPermissions> cache = new ConcurrentHashMap<>();
    private final Duration cacheExpiry = Duration.ofMinutes(15); // 15 minutes default

    /**
     * Get cached permissions for a user
     */
    public Set<Permission> getUserPermissions(String userId, String tenantId, @Nullable String namespace) {
        String cacheKey = createCacheKey(userId, tenantId, namespace);
        CachedPermissions cached = cache.get(cacheKey);

        if (cached != null && !cached.isExpired()) {
            log.debug("Cache hit for user permissions: {}", cacheKey);
            return cached.getPermissions();
        }

        log.debug("Cache miss for user permissions: {}", cacheKey);
        return null;
    }

    /**
     * Cache user permissions
     */
    public void cacheUserPermissions(String userId, String tenantId, @Nullable String namespace, Set<Permission> permissions) {
        String cacheKey = createCacheKey(userId, tenantId, namespace);
        CachedPermissions cached = new CachedPermissions(permissions, Instant.now().plus(cacheExpiry));
        cache.put(cacheKey, cached);

        log.debug("Cached permissions for user: {} (count: {})", cacheKey, permissions.size());
    }

    /**
     * Invalidate cached permissions for a user
     */
    public void invalidateUserPermissions(String userId, String tenantId) {
        // Remove all cache entries for this user across all namespaces
        cache.entrySet().removeIf(entry -> {
            String key = entry.getKey();
            return key.startsWith(userId + ":" + tenantId + ":");
        });

        log.debug("Invalidated permissions cache for user: {}:{}", userId, tenantId);
    }

    /**
     * Invalidate cached permissions for a specific namespace
     */
    public void invalidateNamespacePermissions(String tenantId, String namespace) {
        // Remove all cache entries for this namespace
        String namespacePattern = ":" + tenantId + ":" + namespace;
        cache.entrySet().removeIf(entry -> {
            String key = entry.getKey();
            return key.endsWith(namespacePattern);
        });

        log.debug("Invalidated permissions cache for namespace: {}:{}", tenantId, namespace);
    }

    /**
     * Invalidate all cached permissions for a tenant
     */
    public void invalidateTenantPermissions(String tenantId) {
        cache.entrySet().removeIf(entry -> {
            String key = entry.getKey();
            return key.contains(":" + tenantId + ":");
        });

        log.debug("Invalidated permissions cache for tenant: {}", tenantId);
    }

    /**
     * Clear all cached permissions
     */
    public void clearAll() {
        cache.clear();
        log.debug("Cleared all permissions cache");
    }

    /**
     * Get cache statistics
     */
    public CacheStats getStats() {
        int totalEntries = cache.size();
        long expiredEntries = cache.values().stream()
            .mapToLong(cached -> cached.isExpired() ? 1 : 0)
            .sum();

        return new CacheStats(totalEntries, (int) expiredEntries);
    }

    /**
     * Clean up expired cache entries
     */
    public void cleanupExpired() {
        int removedCount = 0;
        var iterator = cache.entrySet().iterator();

        while (iterator.hasNext()) {
            var entry = iterator.next();
            if (entry.getValue().isExpired()) {
                iterator.remove();
                removedCount++;
            }
        }

        if (removedCount > 0) {
            log.debug("Cleaned up {} expired permission cache entries", removedCount);
        }
    }

    /**
     * Create cache key for user permissions
     */
    private String createCacheKey(String userId, String tenantId, @Nullable String namespace) {
        return userId + ":" + tenantId + ":" + (namespace != null ? namespace : "global");
    }

    /**
     * Cached permissions with expiry
     */
    private static class CachedPermissions {
        private final Set<Permission> permissions;
        private final Instant expiryTime;

        public CachedPermissions(Set<Permission> permissions, Instant expiryTime) {
            this.permissions = permissions;
            this.expiryTime = expiryTime;
        }

        public Set<Permission> getPermissions() {
            return permissions;
        }

        public boolean isExpired() {
            return Instant.now().isAfter(expiryTime);
        }
    }

    /**
     * Cache statistics
     */
    public static class CacheStats {
        private final int totalEntries;
        private final int expiredEntries;

        public CacheStats(int totalEntries, int expiredEntries) {
            this.totalEntries = totalEntries;
            this.expiredEntries = expiredEntries;
        }

        public int getTotalEntries() {
            return totalEntries;
        }

        public int getExpiredEntries() {
            return expiredEntries;
        }

        public int getActiveEntries() {
            return totalEntries - expiredEntries;
        }

        @Override
        public String toString() {
            return String.format("CacheStats{total=%d, active=%d, expired=%d}",
                               totalEntries, getActiveEntries(), expiredEntries);
        }
    }
}
