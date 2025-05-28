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
import java.util.stream.Collectors;

/**
 * Role entity for RBAC system
 */
@Value
@Builder
@Jacksonized
@Introspected
@With
public class Role implements TenantInterface {
    
    @NotBlank
    String id;
    
    @NotBlank
    String tenantId;
    
    @NotBlank
    String name;
    
    String description;
    
    @NotNull
    @Builder.Default
    Set<Permission> permissions = Set.of();
    
    @NotNull
    @Builder.Default
    Instant createdAt = Instant.now();
    
    Instant updatedAt;
    
    @Builder.Default
    boolean system = false;
    
    /**
     * Check if role has specific permission
     */
    public boolean hasPermission(Permission permission) {
        return permissions.contains(permission) || 
               permissions.stream().anyMatch(p -> p.implies(permission));
    }
    
    /**
     * Check if role has any of the specified permissions
     */
    public boolean hasAnyPermission(Set<Permission> requiredPermissions) {
        return requiredPermissions.stream().anyMatch(this::hasPermission);
    }
    
    /**
     * Check if role has all of the specified permissions
     */
    public boolean hasAllPermissions(Set<Permission> requiredPermissions) {
        return requiredPermissions.stream().allMatch(this::hasPermission);
    }
    
    /**
     * Add permission to role
     */
    public Role addPermission(Permission permission) {
        Set<Permission> newPermissions = new HashSet<>(permissions);
        newPermissions.add(permission);
        return this.withPermissions(newPermissions).withUpdatedAt(Instant.now());
    }
    
    /**
     * Remove permission from role
     */
    public Role removePermission(Permission permission) {
        Set<Permission> newPermissions = new HashSet<>(permissions);
        newPermissions.remove(permission);
        return this.withPermissions(newPermissions).withUpdatedAt(Instant.now());
    }
    
    /**
     * Add multiple permissions to role
     */
    public Role addPermissions(Set<Permission> newPermissions) {
        Set<Permission> allPermissions = new HashSet<>(permissions);
        allPermissions.addAll(newPermissions);
        return this.withPermissions(allPermissions).withUpdatedAt(Instant.now());
    }
    
    /**
     * Check if this is a system role (cannot be deleted)
     */
    public boolean isSystemRole() {
        return system;
    }
    
    /**
     * Get permission codes as strings
     */
    public Set<String> getPermissionCodes() {
        return permissions.stream()
            .map(Permission::getCode)
            .collect(Collectors.toSet());
    }
    
    /**
     * Create a copy with updated timestamp
     */
    public Role touch() {
        return this.withUpdatedAt(Instant.now());
    }
    
    /**
     * Predefined system roles
     */
    public static class SystemRoles {
        public static final String SUPER_ADMIN = "SUPER_ADMIN";
        public static final String ADMIN = "ADMIN";
        public static final String DEVELOPER = "DEVELOPER";
        public static final String VIEWER = "VIEWER";
        public static final String EXECUTOR = "EXECUTOR";
        
        /**
         * Create super admin role with all permissions
         */
        public static Role createSuperAdmin(String tenantId) {
            return Role.builder()
                .id(SUPER_ADMIN)
                .tenantId(tenantId)
                .name("Super Administrator")
                .description("Full system access with all permissions")
                .permissions(Set.of(Permission.values()))
                .system(true)
                .build();
        }
        
        /**
         * Create admin role with administrative permissions
         */
        public static Role createAdmin(String tenantId) {
            return Role.builder()
                .id(ADMIN)
                .tenantId(tenantId)
                .name("Administrator")
                .description("Administrative access to tenant resources")
                .permissions(Set.of(
                    Permission.FLOW_CREATE, Permission.FLOW_READ, Permission.FLOW_UPDATE, Permission.FLOW_DELETE,
                    Permission.EXECUTION_CREATE, Permission.EXECUTION_READ, Permission.EXECUTION_UPDATE, Permission.EXECUTION_DELETE,
                    Permission.TEMPLATE_CREATE, Permission.TEMPLATE_READ, Permission.TEMPLATE_UPDATE, Permission.TEMPLATE_DELETE,
                    Permission.NAMESPACE_CREATE, Permission.NAMESPACE_READ, Permission.NAMESPACE_UPDATE, Permission.NAMESPACE_DELETE,
                    Permission.USER_CREATE, Permission.USER_READ, Permission.USER_UPDATE, Permission.USER_DELETE,
                    Permission.ROLE_CREATE, Permission.ROLE_READ, Permission.ROLE_UPDATE, Permission.ROLE_DELETE,
                    Permission.GROUP_CREATE, Permission.GROUP_READ, Permission.GROUP_UPDATE, Permission.GROUP_DELETE,
                    Permission.SECRET_CREATE, Permission.SECRET_READ, Permission.SECRET_UPDATE, Permission.SECRET_DELETE,
                    Permission.KV_CREATE, Permission.KV_READ, Permission.KV_UPDATE, Permission.KV_DELETE,
                    Permission.AUDIT_READ, Permission.AUDIT_EXPORT
                ))
                .system(true)
                .build();
        }
        
        /**
         * Create developer role with development permissions
         */
        public static Role createDeveloper(String tenantId) {
            return Role.builder()
                .id(DEVELOPER)
                .tenantId(tenantId)
                .name("Developer")
                .description("Development access to flows and executions")
                .permissions(Set.of(
                    Permission.FLOW_CREATE, Permission.FLOW_READ, Permission.FLOW_UPDATE, Permission.FLOW_EXECUTE,
                    Permission.EXECUTION_CREATE, Permission.EXECUTION_READ, Permission.EXECUTION_RESTART, Permission.EXECUTION_KILL,
                    Permission.TEMPLATE_CREATE, Permission.TEMPLATE_READ, Permission.TEMPLATE_UPDATE,
                    Permission.SECRET_READ, Permission.KV_READ, Permission.KV_CREATE, Permission.KV_UPDATE
                ))
                .system(true)
                .build();
        }
        
        /**
         * Create viewer role with read-only permissions
         */
        public static Role createViewer(String tenantId) {
            return Role.builder()
                .id(VIEWER)
                .tenantId(tenantId)
                .name("Viewer")
                .description("Read-only access to flows and executions")
                .permissions(Set.of(
                    Permission.FLOW_READ, Permission.EXECUTION_READ, Permission.TEMPLATE_READ,
                    Permission.NAMESPACE_READ, Permission.PLUGIN_READ
                ))
                .system(true)
                .build();
        }
        
        /**
         * Create executor role with execution permissions
         */
        public static Role createExecutor(String tenantId) {
            return Role.builder()
                .id(EXECUTOR)
                .tenantId(tenantId)
                .name("Executor")
                .description("Execute flows and manage executions")
                .permissions(Set.of(
                    Permission.FLOW_READ, Permission.FLOW_EXECUTE,
                    Permission.EXECUTION_CREATE, Permission.EXECUTION_READ, Permission.EXECUTION_RESTART, Permission.EXECUTION_KILL,
                    Permission.SECRET_READ, Permission.KV_READ
                ))
                .system(true)
                .build();
        }
    }
}
