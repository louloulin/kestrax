package io.kestra.core.models.tenants;

import io.kestra.core.models.TenantInterface;
import io.micronaut.core.annotation.Introspected;
import lombok.Builder;
import lombok.Value;
import lombok.With;
import lombok.extern.jackson.Jacksonized;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.Map;
import java.util.Set;

/**
 * Tenant entity for multi-tenant architecture
 */
@Value
@Builder
@Jacksonized
@Introspected
@With
public class Tenant implements TenantInterface {
    
    @NotBlank
    @Pattern(regexp = "^[a-z0-9][a-z0-9-]*[a-z0-9]$", message = "Tenant ID must be lowercase alphanumeric with hyphens")
    @Size(min = 2, max = 63, message = "Tenant ID must be between 2 and 63 characters")
    String id;
    
    @NotBlank
    @Size(min = 1, max = 255, message = "Tenant name must be between 1 and 255 characters")
    String name;
    
    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    String description;
    
    @NotNull
    TenantStatus status;
    
    @NotNull
    TenantPlan plan;
    
    /**
     * Tenant configuration settings
     */
    Map<String, Object> settings;
    
    /**
     * Resource limits for the tenant
     */
    TenantLimits limits;
    
    /**
     * Enabled features for the tenant
     */
    Set<String> enabledFeatures;
    
    /**
     * Custom properties for tenant-specific configuration
     */
    Map<String, String> properties;
    
    /**
     * Administrative contact information
     */
    String adminEmail;
    
    /**
     * Billing contact information
     */
    String billingEmail;
    
    /**
     * Tenant creation timestamp
     */
    @NotNull
    @Builder.Default
    Instant createdAt = Instant.now();
    
    /**
     * Last update timestamp
     */
    @Builder.Default
    Instant updatedAt = Instant.now();
    
    /**
     * Tenant deletion timestamp (soft delete)
     */
    Instant deletedAt;
    
    /**
     * Version for optimistic locking
     */
    @Builder.Default
    Long version = 1L;
    
    /**
     * Tenant status enumeration
     */
    public enum TenantStatus {
        ACTIVE,
        SUSPENDED,
        INACTIVE,
        PENDING_ACTIVATION,
        PENDING_DELETION
    }
    
    /**
     * Tenant plan enumeration
     */
    public enum TenantPlan {
        FREE,
        BASIC,
        PROFESSIONAL,
        ENTERPRISE,
        CUSTOM
    }
    
    /**
     * Resource limits for tenant
     */
    @Value
    @Builder
    @Jacksonized
    @With
    public static class TenantLimits {
        
        /**
         * Maximum number of flows
         */
        @Builder.Default
        Integer maxFlows = 100;
        
        /**
         * Maximum number of executions per day
         */
        @Builder.Default
        Integer maxExecutionsPerDay = 1000;
        
        /**
         * Maximum number of concurrent executions
         */
        @Builder.Default
        Integer maxConcurrentExecutions = 10;
        
        /**
         * Maximum number of users
         */
        @Builder.Default
        Integer maxUsers = 10;
        
        /**
         * Maximum storage size in bytes
         */
        @Builder.Default
        Long maxStorageBytes = 1024L * 1024L * 1024L; // 1GB
        
        /**
         * Maximum execution duration in seconds
         */
        @Builder.Default
        Integer maxExecutionDurationSeconds = 3600; // 1 hour
        
        /**
         * Rate limit for API calls per minute
         */
        @Builder.Default
        Integer apiCallsPerMinute = 1000;
        
        /**
         * Maximum number of webhooks
         */
        @Builder.Default
        Integer maxWebhooks = 10;
        
        /**
         * Maximum number of triggers
         */
        @Builder.Default
        Integer maxTriggers = 50;
    }
    
    @Override
    public String getTenantId() {
        return id;
    }
    
    /**
     * Check if tenant is active
     */
    public boolean isActive() {
        return status == TenantStatus.ACTIVE && deletedAt == null;
    }
    
    /**
     * Check if tenant is suspended
     */
    public boolean isSuspended() {
        return status == TenantStatus.SUSPENDED;
    }
    
    /**
     * Check if tenant is deleted (soft delete)
     */
    public boolean isDeleted() {
        return deletedAt != null;
    }
    
    /**
     * Check if tenant has a specific feature enabled
     */
    public boolean hasFeature(String feature) {
        return enabledFeatures != null && enabledFeatures.contains(feature);
    }
    
    /**
     * Get a setting value
     */
    @SuppressWarnings("unchecked")
    public <T> T getSetting(String key, T defaultValue) {
        if (settings == null || !settings.containsKey(key)) {
            return defaultValue;
        }
        try {
            return (T) settings.get(key);
        } catch (ClassCastException e) {
            return defaultValue;
        }
    }
    
    /**
     * Get a property value
     */
    public String getProperty(String key, String defaultValue) {
        if (properties == null || !properties.containsKey(key)) {
            return defaultValue;
        }
        return properties.get(key);
    }
    
    /**
     * Check if tenant can perform an action based on limits
     */
    public boolean canExecute(int currentExecutions) {
        return limits == null || 
               limits.getMaxConcurrentExecutions() == null || 
               currentExecutions < limits.getMaxConcurrentExecutions();
    }
    
    /**
     * Check if tenant can create more flows
     */
    public boolean canCreateFlow(int currentFlows) {
        return limits == null || 
               limits.getMaxFlows() == null || 
               currentFlows < limits.getMaxFlows();
    }
    
    /**
     * Check if tenant can add more users
     */
    public boolean canAddUser(int currentUsers) {
        return limits == null || 
               limits.getMaxUsers() == null || 
               currentUsers < limits.getMaxUsers();
    }
    
    /**
     * Check if tenant has storage capacity
     */
    public boolean hasStorageCapacity(long currentStorageBytes, long additionalBytes) {
        return limits == null || 
               limits.getMaxStorageBytes() == null || 
               (currentStorageBytes + additionalBytes) <= limits.getMaxStorageBytes();
    }
    
    /**
     * Create a default tenant for system use
     */
    public static Tenant createDefault() {
        return Tenant.builder()
            .id("default")
            .name("Default Tenant")
            .description("Default system tenant")
            .status(TenantStatus.ACTIVE)
            .plan(TenantPlan.ENTERPRISE)
            .limits(TenantLimits.builder()
                .maxFlows(Integer.MAX_VALUE)
                .maxExecutionsPerDay(Integer.MAX_VALUE)
                .maxConcurrentExecutions(Integer.MAX_VALUE)
                .maxUsers(Integer.MAX_VALUE)
                .maxStorageBytes(Long.MAX_VALUE)
                .maxExecutionDurationSeconds(Integer.MAX_VALUE)
                .apiCallsPerMinute(Integer.MAX_VALUE)
                .maxWebhooks(Integer.MAX_VALUE)
                .maxTriggers(Integer.MAX_VALUE)
                .build())
            .build();
    }
    
    /**
     * Create a tenant with basic plan defaults
     */
    public static Tenant createBasic(String id, String name) {
        return Tenant.builder()
            .id(id)
            .name(name)
            .status(TenantStatus.PENDING_ACTIVATION)
            .plan(TenantPlan.BASIC)
            .limits(TenantLimits.builder().build()) // Use default limits
            .build();
    }
    
    /**
     * Update tenant with new timestamp and version
     */
    public Tenant withUpdate() {
        return this.withUpdatedAt(Instant.now())
                   .withVersion(version + 1);
    }
    
    /**
     * Mark tenant as deleted (soft delete)
     */
    public Tenant markAsDeleted() {
        return this.withStatus(TenantStatus.PENDING_DELETION)
                   .withDeletedAt(Instant.now())
                   .withUpdate();
    }
    
    /**
     * Activate tenant
     */
    public Tenant activate() {
        return this.withStatus(TenantStatus.ACTIVE)
                   .withUpdate();
    }
    
    /**
     * Suspend tenant
     */
    public Tenant suspend() {
        return this.withStatus(TenantStatus.SUSPENDED)
                   .withUpdate();
    }
}
