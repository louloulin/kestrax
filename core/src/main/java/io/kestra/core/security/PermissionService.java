package io.kestra.core.security;

import io.kestra.core.models.rbac.Permission;
import io.kestra.core.models.rbac.Role;
import io.kestra.core.models.rbac.User;
import io.kestra.core.models.rbac.Group;
import io.kestra.core.models.rbac.Binding;
import io.micronaut.core.annotation.Nullable;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for checking user permissions in the RBAC system
 */
@Singleton
@Slf4j
public class PermissionService {
    
    /**
     * Check if user has the specified permission in the given namespace
     */
    public boolean hasPermission(String userId, String tenantId, Permission permission, @Nullable String namespace) {
        if (userId == null || tenantId == null || permission == null) {
            return false;
        }
        
        log.debug("Checking permission {} for user {} in tenant {} namespace {}", 
                 permission.getCode(), userId, tenantId, namespace);
        
        // Get user's effective permissions
        Set<Permission> userPermissions = getUserEffectivePermissions(userId, tenantId, namespace);
        
        // Check if user has the required permission (including implied permissions)
        return userPermissions.stream()
            .anyMatch(p -> p.implies(permission));
    }
    
    /**
     * Check if user has any of the specified permissions
     */
    public boolean hasAnyPermission(String userId, String tenantId, Set<Permission> permissions, @Nullable String namespace) {
        if (permissions == null || permissions.isEmpty()) {
            return true;
        }
        
        return permissions.stream()
            .anyMatch(permission -> hasPermission(userId, tenantId, permission, namespace));
    }
    
    /**
     * Check if user has all of the specified permissions
     */
    public boolean hasAllPermissions(String userId, String tenantId, Set<Permission> permissions, @Nullable String namespace) {
        if (permissions == null || permissions.isEmpty()) {
            return true;
        }
        
        return permissions.stream()
            .allMatch(permission -> hasPermission(userId, tenantId, permission, namespace));
    }
    
    /**
     * Get all effective permissions for a user in a specific namespace
     */
    public Set<Permission> getUserEffectivePermissions(String userId, String tenantId, @Nullable String namespace) {
        Set<Permission> effectivePermissions = new HashSet<>();
        
        // This would typically load from database/cache
        // For now, we'll simulate the logic
        
        // 1. Get user's direct role bindings
        Set<String> userRoleIds = getUserDirectRoles(userId, tenantId, namespace);
        
        // 2. Get user's group memberships and their role bindings
        Set<String> groupRoleIds = getUserGroupRoles(userId, tenantId, namespace);
        
        // 3. Combine all role IDs
        Set<String> allRoleIds = new HashSet<>();
        allRoleIds.addAll(userRoleIds);
        allRoleIds.addAll(groupRoleIds);
        
        // 4. Get permissions from all roles
        for (String roleId : allRoleIds) {
            Role role = getRoleById(roleId);
            if (role != null) {
                effectivePermissions.addAll(role.getPermissions());
            }
        }
        
        log.debug("User {} has {} effective permissions in namespace {}", 
                 userId, effectivePermissions.size(), namespace);
        
        return effectivePermissions;
    }
    
    /**
     * Check if user is a system administrator
     */
    public boolean isSystemAdmin(String userId, String tenantId) {
        return hasPermission(userId, tenantId, Permission.SYSTEM_ADMIN, null);
    }
    
    /**
     * Get user's direct role bindings for a namespace
     */
    private Set<String> getUserDirectRoles(String userId, String tenantId, @Nullable String namespace) {
        // This would query the binding repository
        // For now, return empty set as placeholder
        return new HashSet<>();
    }
    
    /**
     * Get roles from user's group memberships
     */
    private Set<String> getUserGroupRoles(String userId, String tenantId, @Nullable String namespace) {
        // This would:
        // 1. Get user's group memberships
        // 2. Get role bindings for those groups
        // 3. Return the role IDs
        return new HashSet<>();
    }
    
    /**
     * Get role by ID
     */
    private Role getRoleById(String roleId) {
        // This would query the role repository
        // For now, return null as placeholder
        return null;
    }
    
    /**
     * Validate permission check parameters
     */
    public void validatePermissionCheck(String userId, String tenantId, Permission permission) {
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("User ID cannot be null or empty");
        }
        if (tenantId == null || tenantId.trim().isEmpty()) {
            throw new IllegalArgumentException("Tenant ID cannot be null or empty");
        }
        if (permission == null) {
            throw new IllegalArgumentException("Permission cannot be null");
        }
    }
    
    /**
     * Create permission denied exception with context
     */
    public SecurityException createPermissionDeniedException(String userId, Permission permission, @Nullable String namespace) {
        String message = String.format(
            "Access denied: User %s does not have permission %s%s",
            userId,
            permission.getCode(),
            namespace != null ? " in namespace " + namespace : ""
        );
        return new SecurityException(message);
    }
}
