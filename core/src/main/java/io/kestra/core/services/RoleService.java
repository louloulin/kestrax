package io.kestra.core.services;

import io.kestra.core.models.PagedResults;
import io.kestra.core.models.rbac.Role;
import io.kestra.core.exceptions.IllegalVariableEvaluationException;
import jakarta.inject.Singleton;

import java.util.List;
import java.util.Optional;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Service for managing roles in the RBAC system.
 */
@Singleton
public class RoleService {

    // Mock data storage - in real implementation this would use a repository
    private final Map<String, Role> roles = new HashMap<>();

    public RoleService() {
        // Initialize with default roles
        initializeDefaultRoles();
    }

    /**
     * Search for roles with pagination and filtering.
     */
    public PagedResults<Role> findRoles(String query, int page, int size, String tenantId) {
        List<Role> allRoles = new ArrayList<>(roles.values());

        // Filter by tenant if specified
        if (tenantId != null) {
            allRoles = allRoles.stream()
                .filter(role -> tenantId.equals(role.getTenantId()))
                .toList();
        }

        // Filter by query if specified
        if (query != null && !query.trim().isEmpty()) {
            String lowerQuery = query.toLowerCase();
            allRoles = allRoles.stream()
                .filter(role ->
                    role.getName().toLowerCase().contains(lowerQuery) ||
                    (role.getDescription() != null && role.getDescription().toLowerCase().contains(lowerQuery))
                )
                .toList();
        }

        // Apply pagination
        int start = page * size;
        int end = Math.min(start + size, allRoles.size());
        List<Role> pageResults = start < allRoles.size() ? allRoles.subList(start, end) : new ArrayList<>();

        return PagedResults.of(pageResults, allRoles.size(), page, size);
    }

    /**
     * Find a role by ID.
     */
    public Optional<Role> findById(String id) {
        return Optional.ofNullable(roles.get(id));
    }

    /**
     * Create a new role.
     */
    public Role create(Role role) throws IllegalVariableEvaluationException {
        if (roles.containsKey(role.getId())) {
            throw new IllegalVariableEvaluationException("Role with ID " + role.getId() + " already exists");
        }
        roles.put(role.getId(), role);
        return role;
    }

    /**
     * Update an existing role.
     */
    public Role update(String id, Role role) throws IllegalVariableEvaluationException {
        if (!roles.containsKey(id)) {
            throw new IllegalVariableEvaluationException("Role with ID " + id + " not found");
        }
        Role updatedRole = role.withId(id);
        roles.put(id, updatedRole);
        return updatedRole;
    }

    /**
     * Delete a role.
     */
    public void delete(String id) throws IllegalVariableEvaluationException {
        if (!roles.containsKey(id)) {
            throw new IllegalVariableEvaluationException("Role with ID " + id + " not found");
        }
        roles.remove(id);
    }

    /**
     * Get all permissions for a role.
     */
    public List<String> getRolePermissions(String roleId) {
        return findById(roleId)
            .map(role -> role.getPermissions().stream()
                .map(permission -> permission.getCode())
                .toList())
            .orElse(new ArrayList<>());
    }

    /**
     * Check if a role has a specific permission.
     */
    public boolean hasPermission(String roleId, String permission) {
        return getRolePermissions(roleId).contains(permission) ||
               getRolePermissions(roleId).contains("*"); // Wildcard permission
    }

    /**
     * Get roles by user ID.
     */
    public List<Role> getRolesByUserId(String userId) {
        // This would typically query user-role relationships
        // For now, return empty list as mock implementation
        return new ArrayList<>();
    }

    /**
     * Initialize default system roles.
     */
    private void initializeDefaultRoles() {
        // Use the existing system roles from Role.SystemRoles
        Role adminRole = Role.SystemRoles.createAdmin("default");
        roles.put(adminRole.getId(), adminRole);

        Role developerRole = Role.SystemRoles.createDeveloper("default");
        roles.put(developerRole.getId(), developerRole);

        Role viewerRole = Role.SystemRoles.createViewer("default");
        roles.put(viewerRole.getId(), viewerRole);
    }
}
