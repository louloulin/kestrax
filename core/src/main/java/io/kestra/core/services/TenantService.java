package io.kestra.core.services;

import io.kestra.core.models.tenants.Tenant;
import io.kestra.core.exceptions.IllegalVariableEvaluationException;
import io.micronaut.core.annotation.Nullable;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * Service for managing tenants in multi-tenant architecture
 */
@Singleton
@Slf4j
public class TenantService {
    
    // In-memory storage for demonstration - would be replaced with actual repository
    private final Map<String, Tenant> tenants = new ConcurrentHashMap<>();
    private final Map<String, TenantMetrics> tenantMetrics = new ConcurrentHashMap<>();
    
    public TenantService() {
        // Initialize with default tenant
        Tenant defaultTenant = Tenant.createDefault();
        tenants.put(defaultTenant.getId(), defaultTenant);
        tenantMetrics.put(defaultTenant.getId(), new TenantMetrics());
    }
    
    /**
     * Create a new tenant
     */
    public Tenant createTenant(Tenant tenant) {
        validateTenant(tenant);
        
        if (tenants.containsKey(tenant.getId())) {
            throw new IllegalArgumentException("Tenant with ID '" + tenant.getId() + "' already exists");
        }
        
        Tenant newTenant = tenant.withUpdate();
        tenants.put(newTenant.getId(), newTenant);
        tenantMetrics.put(newTenant.getId(), new TenantMetrics());
        
        log.info("Created tenant: {} ({})", newTenant.getName(), newTenant.getId());
        return newTenant;
    }
    
    /**
     * Get tenant by ID
     */
    public Optional<Tenant> getTenant(String tenantId) {
        return Optional.ofNullable(tenants.get(tenantId))
                      .filter(tenant -> !tenant.isDeleted());
    }
    
    /**
     * Get active tenant by ID
     */
    public Optional<Tenant> getActiveTenant(String tenantId) {
        return getTenant(tenantId)
                .filter(Tenant::isActive);
    }
    
    /**
     * Update tenant
     */
    public Tenant updateTenant(String tenantId, Tenant updatedTenant) {
        Tenant existingTenant = getTenant(tenantId)
            .orElseThrow(() -> new IllegalArgumentException("Tenant not found: " + tenantId));
        
        if (!existingTenant.getId().equals(updatedTenant.getId())) {
            throw new IllegalArgumentException("Cannot change tenant ID");
        }
        
        validateTenant(updatedTenant);
        
        Tenant newTenant = updatedTenant.withUpdate();
        tenants.put(tenantId, newTenant);
        
        log.info("Updated tenant: {} ({})", newTenant.getName(), newTenant.getId());
        return newTenant;
    }
    
    /**
     * Delete tenant (soft delete)
     */
    public void deleteTenant(String tenantId) {
        Tenant tenant = getTenant(tenantId)
            .orElseThrow(() -> new IllegalArgumentException("Tenant not found: " + tenantId));
        
        if ("default".equals(tenantId)) {
            throw new IllegalArgumentException("Cannot delete default tenant");
        }
        
        Tenant deletedTenant = tenant.markAsDeleted();
        tenants.put(tenantId, deletedTenant);
        
        log.info("Deleted tenant: {} ({})", tenant.getName(), tenant.getId());
    }
    
    /**
     * Activate tenant
     */
    public Tenant activateTenant(String tenantId) {
        Tenant tenant = getTenant(tenantId)
            .orElseThrow(() -> new IllegalArgumentException("Tenant not found: " + tenantId));
        
        Tenant activatedTenant = tenant.activate();
        tenants.put(tenantId, activatedTenant);
        
        log.info("Activated tenant: {} ({})", tenant.getName(), tenant.getId());
        return activatedTenant;
    }
    
    /**
     * Suspend tenant
     */
    public Tenant suspendTenant(String tenantId) {
        Tenant tenant = getTenant(tenantId)
            .orElseThrow(() -> new IllegalArgumentException("Tenant not found: " + tenantId));
        
        if ("default".equals(tenantId)) {
            throw new IllegalArgumentException("Cannot suspend default tenant");
        }
        
        Tenant suspendedTenant = tenant.suspend();
        tenants.put(tenantId, suspendedTenant);
        
        log.info("Suspended tenant: {} ({})", tenant.getName(), tenant.getId());
        return suspendedTenant;
    }
    
    /**
     * List all tenants
     */
    public List<Tenant> listTenants() {
        return tenants.values().stream()
                     .filter(tenant -> !tenant.isDeleted())
                     .sorted(Comparator.comparing(Tenant::getName))
                     .collect(Collectors.toList());
    }
    
    /**
     * List active tenants
     */
    public List<Tenant> listActiveTenants() {
        return tenants.values().stream()
                     .filter(Tenant::isActive)
                     .sorted(Comparator.comparing(Tenant::getName))
                     .collect(Collectors.toList());
    }
    
    /**
     * Search tenants by name
     */
    public List<Tenant> searchTenants(String namePattern) {
        String pattern = namePattern.toLowerCase();
        return tenants.values().stream()
                     .filter(tenant -> !tenant.isDeleted())
                     .filter(tenant -> tenant.getName().toLowerCase().contains(pattern))
                     .sorted(Comparator.comparing(Tenant::getName))
                     .collect(Collectors.toList());
    }
    
    /**
     * Check if tenant exists and is active
     */
    public boolean isActiveTenant(String tenantId) {
        return getActiveTenant(tenantId).isPresent();
    }
    
    /**
     * Validate tenant limits for execution
     */
    public boolean canExecute(String tenantId) {
        return getActiveTenant(tenantId)
                .map(tenant -> {
                    TenantMetrics metrics = tenantMetrics.get(tenantId);
                    return tenant.canExecute(metrics.getCurrentExecutions());
                })
                .orElse(false);
    }
    
    /**
     * Validate tenant limits for flow creation
     */
    public boolean canCreateFlow(String tenantId) {
        return getActiveTenant(tenantId)
                .map(tenant -> {
                    TenantMetrics metrics = tenantMetrics.get(tenantId);
                    return tenant.canCreateFlow(metrics.getFlowCount());
                })
                .orElse(false);
    }
    
    /**
     * Validate tenant limits for user addition
     */
    public boolean canAddUser(String tenantId) {
        return getActiveTenant(tenantId)
                .map(tenant -> {
                    TenantMetrics metrics = tenantMetrics.get(tenantId);
                    return tenant.canAddUser(metrics.getUserCount());
                })
                .orElse(false);
    }
    
    /**
     * Record execution start
     */
    public void recordExecutionStart(String tenantId) {
        TenantMetrics metrics = tenantMetrics.get(tenantId);
        if (metrics != null) {
            metrics.incrementExecutions();
        }
    }
    
    /**
     * Record execution end
     */
    public void recordExecutionEnd(String tenantId) {
        TenantMetrics metrics = tenantMetrics.get(tenantId);
        if (metrics != null) {
            metrics.decrementExecutions();
        }
    }
    
    /**
     * Get tenant metrics
     */
    public TenantMetrics getTenantMetrics(String tenantId) {
        return tenantMetrics.get(tenantId);
    }
    
    /**
     * Get tenant statistics
     */
    public TenantStatistics getTenantStatistics() {
        long totalTenants = tenants.size();
        long activeTenants = tenants.values().stream()
                                   .mapToLong(tenant -> tenant.isActive() ? 1 : 0)
                                   .sum();
        long suspendedTenants = tenants.values().stream()
                                      .mapToLong(tenant -> tenant.isSuspended() ? 1 : 0)
                                      .sum();
        
        return new TenantStatistics(totalTenants, activeTenants, suspendedTenants);
    }
    
    /**
     * Validate tenant data
     */
    private void validateTenant(Tenant tenant) {
        if (tenant.getId() == null || tenant.getId().trim().isEmpty()) {
            throw new IllegalArgumentException("Tenant ID is required");
        }
        
        if (tenant.getName() == null || tenant.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Tenant name is required");
        }
        
        if (tenant.getStatus() == null) {
            throw new IllegalArgumentException("Tenant status is required");
        }
        
        if (tenant.getPlan() == null) {
            throw new IllegalArgumentException("Tenant plan is required");
        }
        
        // Validate ID format
        if (!tenant.getId().matches("^[a-z0-9][a-z0-9-]*[a-z0-9]$")) {
            throw new IllegalArgumentException("Tenant ID must be lowercase alphanumeric with hyphens");
        }
    }
    
    /**
     * Tenant metrics for tracking usage
     */
    public static class TenantMetrics {
        private final AtomicLong currentExecutions = new AtomicLong(0);
        private final AtomicLong totalExecutions = new AtomicLong(0);
        private final AtomicLong flowCount = new AtomicLong(0);
        private final AtomicLong userCount = new AtomicLong(0);
        private final AtomicLong storageUsed = new AtomicLong(0);
        
        public void incrementExecutions() {
            currentExecutions.incrementAndGet();
            totalExecutions.incrementAndGet();
        }
        
        public void decrementExecutions() {
            currentExecutions.decrementAndGet();
        }
        
        public int getCurrentExecutions() {
            return (int) currentExecutions.get();
        }
        
        public long getTotalExecutions() {
            return totalExecutions.get();
        }
        
        public int getFlowCount() {
            return (int) flowCount.get();
        }
        
        public int getUserCount() {
            return (int) userCount.get();
        }
        
        public long getStorageUsed() {
            return storageUsed.get();
        }
        
        public void setFlowCount(int count) {
            flowCount.set(count);
        }
        
        public void setUserCount(int count) {
            userCount.set(count);
        }
        
        public void setStorageUsed(long bytes) {
            storageUsed.set(bytes);
        }
    }
    
    /**
     * Tenant statistics
     */
    public static class TenantStatistics {
        private final long totalTenants;
        private final long activeTenants;
        private final long suspendedTenants;
        
        public TenantStatistics(long totalTenants, long activeTenants, long suspendedTenants) {
            this.totalTenants = totalTenants;
            this.activeTenants = activeTenants;
            this.suspendedTenants = suspendedTenants;
        }
        
        public long getTotalTenants() { return totalTenants; }
        public long getActiveTenants() { return activeTenants; }
        public long getSuspendedTenants() { return suspendedTenants; }
        
        @Override
        public String toString() {
            return String.format("TenantStatistics{total=%d, active=%d, suspended=%d}", 
                               totalTenants, activeTenants, suspendedTenants);
        }
    }
}
