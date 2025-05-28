package io.kestra.core.tenant;

import io.kestra.core.models.tenants.Tenant;
import io.micronaut.core.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

/**
 * Thread-local tenant context for multi-tenant data isolation
 */
@Slf4j
public class TenantContext {

    private static final ThreadLocal<TenantInfo> TENANT_CONTEXT = new ThreadLocal<>();

    /**
     * Tenant information holder
     */
    public static class TenantInfo {
        private final String tenantId;
        private final Tenant tenant;
        private final String userId;
        private final String sessionId;

        public TenantInfo(String tenantId, @Nullable Tenant tenant, @Nullable String userId, @Nullable String sessionId) {
            this.tenantId = tenantId;
            this.tenant = tenant;
            this.userId = userId;
            this.sessionId = sessionId;
        }

        public String getTenantId() { return tenantId; }
        public Optional<Tenant> getTenant() { return Optional.ofNullable(tenant); }
        public Optional<String> getUserId() { return Optional.ofNullable(userId); }
        public Optional<String> getSessionId() { return Optional.ofNullable(sessionId); }

        @Override
        public String toString() {
            return String.format("TenantInfo{tenantId='%s', userId='%s', sessionId='%s'}",
                               tenantId, userId, sessionId);
        }
    }

    /**
     * Set tenant context for current thread
     */
    public static void setTenant(String tenantId) {
        setTenant(tenantId, null, null, null);
    }

    /**
     * Set tenant context with full information
     */
    public static void setTenant(String tenantId, @Nullable Tenant tenant, @Nullable String userId, @Nullable String sessionId) {
        if (tenantId == null || tenantId.trim().isEmpty()) {
            throw new IllegalArgumentException("Tenant ID cannot be null or empty");
        }

        TenantInfo tenantInfo = new TenantInfo(tenantId, tenant, userId, sessionId);
        TENANT_CONTEXT.set(tenantInfo);

        log.debug("Set tenant context: {}", tenantInfo);
    }

    /**
     * Get current tenant ID
     */
    public static Optional<String> getCurrentTenantId() {
        TenantInfo tenantInfo = TENANT_CONTEXT.get();
        return tenantInfo != null ? Optional.of(tenantInfo.getTenantId()) : Optional.empty();
    }

    /**
     * Get current tenant ID or throw exception if not set
     */
    public static String requireCurrentTenantId() {
        return getCurrentTenantId()
            .orElseThrow(() -> new IllegalStateException("No tenant context set for current thread"));
    }

    /**
     * Get current tenant information
     */
    public static Optional<TenantInfo> getCurrentTenantInfo() {
        return Optional.ofNullable(TENANT_CONTEXT.get());
    }

    /**
     * Get current tenant
     */
    public static Optional<Tenant> getCurrentTenant() {
        return getCurrentTenantInfo()
            .flatMap(TenantInfo::getTenant);
    }

    /**
     * Get current user ID
     */
    public static Optional<String> getCurrentUserId() {
        return getCurrentTenantInfo()
            .flatMap(TenantInfo::getUserId);
    }

    /**
     * Get current session ID
     */
    public static Optional<String> getCurrentSessionId() {
        return getCurrentTenantInfo()
            .flatMap(TenantInfo::getSessionId);
    }

    /**
     * Check if tenant context is set
     */
    public static boolean hasTenantContext() {
        return TENANT_CONTEXT.get() != null;
    }

    /**
     * Check if current tenant matches the given tenant ID
     */
    public static boolean isCurrentTenant(String tenantId) {
        return getCurrentTenantId()
            .map(currentTenantId -> currentTenantId.equals(tenantId))
            .orElse(false);
    }

    /**
     * Clear tenant context for current thread
     */
    public static void clear() {
        TenantInfo tenantInfo = TENANT_CONTEXT.get();
        if (tenantInfo != null) {
            log.debug("Clearing tenant context: {}", tenantInfo);
            TENANT_CONTEXT.remove();
        }
    }

    /**
     * Execute code with tenant context
     */
    public static <T> T withTenant(String tenantId, TenantSupplier<T> supplier) {
        return withTenant(tenantId, null, null, null, supplier);
    }

    /**
     * Execute code with full tenant context
     */
    public static <T> T withTenant(String tenantId, @Nullable Tenant tenant, @Nullable String userId,
                                  @Nullable String sessionId, TenantSupplier<T> supplier) {
        TenantInfo previousContext = TENANT_CONTEXT.get();
        try {
            setTenant(tenantId, tenant, userId, sessionId);
            return supplier.get();
        } catch (Exception e) {
            throw new RuntimeException("Error executing with tenant context", e);
        } finally {
            if (previousContext != null) {
                TENANT_CONTEXT.set(previousContext);
            } else {
                clear();
            }
        }
    }

    /**
     * Execute code with tenant context (void return)
     */
    public static void withTenant(String tenantId, TenantRunnable runnable) {
        withTenant(tenantId, null, null, null, runnable);
    }

    /**
     * Execute code with full tenant context (void return)
     */
    public static void withTenant(String tenantId, @Nullable Tenant tenant, @Nullable String userId,
                                 @Nullable String sessionId, TenantRunnable runnable) {
        TenantInfo previousContext = TENANT_CONTEXT.get();
        try {
            setTenant(tenantId, tenant, userId, sessionId);
            runnable.run();
        } catch (Exception e) {
            throw new RuntimeException("Error executing with tenant context", e);
        } finally {
            if (previousContext != null) {
                TENANT_CONTEXT.set(previousContext);
            } else {
                clear();
            }
        }
    }

    /**
     * Validate tenant access for given tenant ID
     */
    public static void validateTenantAccess(String targetTenantId) {
        String currentTenantId = requireCurrentTenantId();
        if (!currentTenantId.equals(targetTenantId)) {
            throw new TenantAccessException(
                String.format("Access denied: Current tenant '%s' cannot access tenant '%s'",
                            currentTenantId, targetTenantId)
            );
        }
    }

    /**
     * Get tenant-prefixed key for data isolation
     */
    public static String getTenantKey(String key) {
        String tenantId = requireCurrentTenantId();
        return getTenantKey(tenantId, key);
    }

    /**
     * Get tenant-prefixed key for specific tenant
     */
    public static String getTenantKey(String tenantId, String key) {
        if (tenantId == null || tenantId.trim().isEmpty()) {
            throw new IllegalArgumentException("Tenant ID cannot be null or empty");
        }
        if (key == null) {
            throw new IllegalArgumentException("Key cannot be null");
        }
        return tenantId + ":" + key;
    }

    /**
     * Extract tenant ID from tenant-prefixed key
     */
    public static Optional<String> extractTenantId(String tenantKey) {
        if (tenantKey == null || !tenantKey.contains(":")) {
            return Optional.empty();
        }
        int separatorIndex = tenantKey.indexOf(':');
        return Optional.of(tenantKey.substring(0, separatorIndex));
    }

    /**
     * Extract original key from tenant-prefixed key
     */
    public static Optional<String> extractKey(String tenantKey) {
        if (tenantKey == null || !tenantKey.contains(":")) {
            return Optional.empty();
        }
        int separatorIndex = tenantKey.indexOf(':');
        return Optional.of(tenantKey.substring(separatorIndex + 1));
    }

    /**
     * Functional interface for tenant-aware suppliers
     */
    @FunctionalInterface
    public interface TenantSupplier<T> {
        T get() throws Exception;
    }

    /**
     * Functional interface for tenant-aware runnables
     */
    @FunctionalInterface
    public interface TenantRunnable {
        void run() throws Exception;
    }

    /**
     * Exception thrown when tenant access is denied
     */
    public static class TenantAccessException extends RuntimeException {
        public TenantAccessException(String message) {
            super(message);
        }

        public TenantAccessException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
