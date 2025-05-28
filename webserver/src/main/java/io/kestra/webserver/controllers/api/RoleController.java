package io.kestra.webserver.controllers.api;

import io.kestra.core.models.rbac.Role;
import io.kestra.core.models.rbac.Permission;
import io.kestra.core.services.RoleService;
import io.kestra.core.services.TenantService;
import io.kestra.webserver.utils.PageableUtils;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.data.model.Pageable;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.*;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.micronaut.security.annotation.Secured;
import io.micronaut.validation.Validated;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * REST API controller for role management in RBAC system
 */
@Controller("/api/v1/rbac/roles")
@Tag(name = "RBAC Roles", description = "Role management for Role-Based Access Control")
@Validated
@Slf4j
public class RoleController {
    
    @Inject
    private RoleService roleService;
    
    @Inject
    private TenantService tenantService;
    
    /**
     * Get all roles with pagination
     */
    @Get
    @ExecuteOn(TaskExecutors.IO)
    @Operation(summary = "List roles", description = "Get paginated list of roles")
    @Secured("ROLE_READ")
    public HttpResponse<PagedResults<Role>> getRoles(
        @Parameter(description = "The current page") @QueryValue(defaultValue = "1") @Min(1) int page,
        @Parameter(description = "The current page size") @QueryValue(defaultValue = "10") @Min(1) int size,
        @Parameter(description = "Search query") @Nullable @QueryValue(value = "q") String query,
        @Parameter(description = "Include system roles") @QueryValue(defaultValue = "true") boolean includeSystem
    ) {
        try {
            String tenantId = tenantService.resolveTenant();
            Pageable pageable = PageableUtils.from(page, size);
            
            PagedResults<Role> roles = roleService.findRoles(
                tenantId, 
                pageable, 
                query, 
                includeSystem
            );
            
            return HttpResponse.ok(roles);
        } catch (Exception e) {
            log.error("Error getting roles", e);
            return HttpResponse.serverError();
        }
    }
    
    /**
     * Get role by ID
     */
    @Get("/{roleId}")
    @ExecuteOn(TaskExecutors.IO)
    @Operation(summary = "Get role", description = "Get role by ID")
    @Secured("ROLE_READ")
    public HttpResponse<Role> getRole(@PathVariable String roleId) {
        try {
            String tenantId = tenantService.resolveTenant();
            Optional<Role> role = roleService.findById(tenantId, roleId);
            
            return role.map(HttpResponse::ok)
                      .orElse(HttpResponse.notFound());
        } catch (Exception e) {
            log.error("Error getting role: " + roleId, e);
            return HttpResponse.serverError();
        }
    }
    
    /**
     * Create new role
     */
    @Post
    @ExecuteOn(TaskExecutors.IO)
    @Operation(summary = "Create role", description = "Create a new role")
    @Secured("ROLE_CREATE")
    public HttpResponse<Role> createRole(@Body @Valid CreateRoleRequest request) {
        try {
            String tenantId = tenantService.resolveTenant();
            
            // Check if role name already exists
            if (roleService.existsByName(tenantId, request.getName())) {
                return HttpResponse.badRequest();
            }
            
            Role role = Role.builder()
                .tenantId(tenantId)
                .name(request.getName())
                .description(request.getDescription())
                .permissions(request.getPermissions())
                .systemRole(false) // Custom roles are never system roles
                .build();
            
            Role createdRole = roleService.create(role);
            
            log.info("Created role: {} in tenant: {}", createdRole.getName(), tenantId);
            return HttpResponse.ok(createdRole);
        } catch (Exception e) {
            log.error("Error creating role", e);
            return HttpResponse.serverError();
        }
    }
    
    /**
     * Update role
     */
    @Put("/{roleId}")
    @ExecuteOn(TaskExecutors.IO)
    @Operation(summary = "Update role", description = "Update role information")
    @Secured("ROLE_UPDATE")
    public HttpResponse<Role> updateRole(
        @PathVariable String roleId,
        @Body @Valid UpdateRoleRequest request
    ) {
        try {
            String tenantId = tenantService.resolveTenant();
            Optional<Role> existingRole = roleService.findById(tenantId, roleId);
            
            if (existingRole.isEmpty()) {
                return HttpResponse.notFound();
            }
            
            Role role = existingRole.get();
            
            // Prevent modification of system roles
            if (role.isSystemRole()) {
                return HttpResponse.badRequest();
            }
            
            role.setDescription(request.getDescription());
            role.setPermissions(request.getPermissions());
            
            Role updatedRole = roleService.update(role);
            
            log.info("Updated role: {} in tenant: {}", updatedRole.getName(), tenantId);
            return HttpResponse.ok(updatedRole);
        } catch (Exception e) {
            log.error("Error updating role: " + roleId, e);
            return HttpResponse.serverError();
        }
    }
    
    /**
     * Delete role
     */
    @Delete("/{roleId}")
    @ExecuteOn(TaskExecutors.IO)
    @Operation(summary = "Delete role", description = "Delete role by ID")
    @Secured("ROLE_DELETE")
    public HttpResponse<Void> deleteRole(@PathVariable String roleId) {
        try {
            String tenantId = tenantService.resolveTenant();
            Optional<Role> role = roleService.findById(tenantId, roleId);
            
            if (role.isEmpty()) {
                return HttpResponse.notFound();
            }
            
            // Prevent deletion of system roles
            if (role.get().isSystemRole()) {
                return HttpResponse.badRequest();
            }
            
            roleService.delete(tenantId, roleId);
            
            log.info("Deleted role: {} in tenant: {}", roleId, tenantId);
            return HttpResponse.noContent();
        } catch (Exception e) {
            log.error("Error deleting role: " + roleId, e);
            return HttpResponse.serverError();
        }
    }
    
    /**
     * Get all available permissions
     */
    @Get("/permissions")
    @ExecuteOn(TaskExecutors.IO)
    @Operation(summary = "Get permissions", description = "Get all available permissions")
    @Secured("ROLE_READ")
    public HttpResponse<List<PermissionInfo>> getPermissions() {
        try {
            List<PermissionInfo> permissions = Permission.getAllPermissions()
                .stream()
                .map(permission -> new PermissionInfo(
                    permission.name(),
                    permission.getCode(),
                    permission.getDescription(),
                    permission.getCategory()
                ))
                .toList();
            
            return HttpResponse.ok(permissions);
        } catch (Exception e) {
            log.error("Error getting permissions", e);
            return HttpResponse.serverError();
        }
    }
    
    /**
     * Get system roles
     */
    @Get("/system")
    @ExecuteOn(TaskExecutors.IO)
    @Operation(summary = "Get system roles", description = "Get predefined system roles")
    @Secured("ROLE_READ")
    public HttpResponse<List<Role>> getSystemRoles() {
        try {
            String tenantId = tenantService.resolveTenant();
            List<Role> systemRoles = roleService.getSystemRoles(tenantId);
            return HttpResponse.ok(systemRoles);
        } catch (Exception e) {
            log.error("Error getting system roles", e);
            return HttpResponse.serverError();
        }
    }
    
    /**
     * Clone role
     */
    @Post("/{roleId}/clone")
    @ExecuteOn(TaskExecutors.IO)
    @Operation(summary = "Clone role", description = "Create a copy of existing role")
    @Secured("ROLE_CREATE")
    public HttpResponse<Role> cloneRole(
        @PathVariable String roleId,
        @Body @Valid CloneRoleRequest request
    ) {
        try {
            String tenantId = tenantService.resolveTenant();
            Optional<Role> sourceRole = roleService.findById(tenantId, roleId);
            
            if (sourceRole.isEmpty()) {
                return HttpResponse.notFound();
            }
            
            // Check if new role name already exists
            if (roleService.existsByName(tenantId, request.getName())) {
                return HttpResponse.badRequest();
            }
            
            Role clonedRole = Role.builder()
                .tenantId(tenantId)
                .name(request.getName())
                .description(request.getDescription())
                .permissions(sourceRole.get().getPermissions())
                .systemRole(false)
                .build();
            
            Role createdRole = roleService.create(clonedRole);
            
            log.info("Cloned role: {} from {} in tenant: {}", 
                    createdRole.getName(), sourceRole.get().getName(), tenantId);
            return HttpResponse.ok(createdRole);
        } catch (Exception e) {
            log.error("Error cloning role: " + roleId, e);
            return HttpResponse.serverError();
        }
    }
    
    // Request/Response DTOs
    public static class CreateRoleRequest {
        private String name;
        private String description;
        private Set<Permission> permissions;
        
        // Getters and setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public Set<Permission> getPermissions() { return permissions; }
        public void setPermissions(Set<Permission> permissions) { this.permissions = permissions; }
    }
    
    public static class UpdateRoleRequest {
        private String description;
        private Set<Permission> permissions;
        
        // Getters and setters
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public Set<Permission> getPermissions() { return permissions; }
        public void setPermissions(Set<Permission> permissions) { this.permissions = permissions; }
    }
    
    public static class CloneRoleRequest {
        private String name;
        private String description;
        
        // Getters and setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }
    
    public static class PermissionInfo {
        private String name;
        private String code;
        private String description;
        private String category;
        
        public PermissionInfo(String name, String code, String description, String category) {
            this.name = name;
            this.code = code;
            this.description = description;
            this.category = category;
        }
        
        // Getters
        public String getName() { return name; }
        public String getCode() { return code; }
        public String getDescription() { return description; }
        public String getCategory() { return category; }
    }
}
