package io.kestra.core.models.rbac;

import io.kestra.core.models.TenantInterface;
import io.micronaut.core.annotation.Introspected;
import lombok.Builder;
import lombok.Value;
import lombok.With;
import lombok.extern.jackson.Jacksonized;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

/**
 * Group entity for RBAC system
 */
@Value
@Builder
@Jacksonized
@Introspected
@With
public class Group implements TenantInterface {
    
    @NotBlank
    String id;
    
    @NotBlank
    String tenantId;
    
    @NotBlank
    String name;
    
    String description;
    
    @NotNull
    @Builder.Default
    Set<String> roleIds = Set.of();
    
    @NotNull
    @Builder.Default
    Set<String> userIds = Set.of();
    
    @NotNull
    @Builder.Default
    Instant createdAt = Instant.now();
    
    Instant updatedAt;
    
    @Builder.Default
    boolean system = false;
    
    /**
     * Add role to group
     */
    public Group addRole(String roleId) {
        Set<String> newRoles = new HashSet<>(roleIds);
        newRoles.add(roleId);
        return this.withRoleIds(newRoles).withUpdatedAt(Instant.now());
    }
    
    /**
     * Remove role from group
     */
    public Group removeRole(String roleId) {
        Set<String> newRoles = new HashSet<>(roleIds);
        newRoles.remove(roleId);
        return this.withRoleIds(newRoles).withUpdatedAt(Instant.now());
    }
    
    /**
     * Add user to group
     */
    public Group addUser(String userId) {
        Set<String> newUsers = new HashSet<>(userIds);
        newUsers.add(userId);
        return this.withUserIds(newUsers).withUpdatedAt(Instant.now());
    }
    
    /**
     * Remove user from group
     */
    public Group removeUser(String userId) {
        Set<String> newUsers = new HashSet<>(userIds);
        newUsers.remove(userId);
        return this.withUserIds(newUsers).withUpdatedAt(Instant.now());
    }
    
    /**
     * Check if group contains user
     */
    public boolean containsUser(String userId) {
        return userIds.contains(userId);
    }
    
    /**
     * Check if group has role
     */
    public boolean hasRole(String roleId) {
        return roleIds.contains(roleId);
    }
    
    /**
     * Check if this is a system group (cannot be deleted)
     */
    public boolean isSystemGroup() {
        return system;
    }
    
    /**
     * Get number of users in group
     */
    public int getUserCount() {
        return userIds.size();
    }
    
    /**
     * Get number of roles in group
     */
    public int getRoleCount() {
        return roleIds.size();
    }
    
    /**
     * Create a copy with updated timestamp
     */
    public Group touch() {
        return this.withUpdatedAt(Instant.now());
    }
    
    /**
     * Predefined system groups
     */
    public static class SystemGroups {
        public static final String ADMINISTRATORS = "ADMINISTRATORS";
        public static final String DEVELOPERS = "DEVELOPERS";
        public static final String VIEWERS = "VIEWERS";
        public static final String EXECUTORS = "EXECUTORS";
        
        /**
         * Create administrators group
         */
        public static Group createAdministrators(String tenantId) {
            return Group.builder()
                .id(ADMINISTRATORS)
                .tenantId(tenantId)
                .name("Administrators")
                .description("System administrators with full access")
                .roleIds(Set.of(Role.SystemRoles.ADMIN))
                .system(true)
                .build();
        }
        
        /**
         * Create developers group
         */
        public static Group createDevelopers(String tenantId) {
            return Group.builder()
                .id(DEVELOPERS)
                .tenantId(tenantId)
                .name("Developers")
                .description("Developers with flow creation and execution access")
                .roleIds(Set.of(Role.SystemRoles.DEVELOPER))
                .system(true)
                .build();
        }
        
        /**
         * Create viewers group
         */
        public static Group createViewers(String tenantId) {
            return Group.builder()
                .id(VIEWERS)
                .tenantId(tenantId)
                .name("Viewers")
                .description("Read-only access to flows and executions")
                .roleIds(Set.of(Role.SystemRoles.VIEWER))
                .system(true)
                .build();
        }
        
        /**
         * Create executors group
         */
        public static Group createExecutors(String tenantId) {
            return Group.builder()
                .id(EXECUTORS)
                .tenantId(tenantId)
                .name("Executors")
                .description("Execute flows and manage executions")
                .roleIds(Set.of(Role.SystemRoles.EXECUTOR))
                .system(true)
                .build();
        }
    }
}
