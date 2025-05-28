package io.kestra.webserver.controllers.api;

import io.kestra.core.models.rbac.User;
import io.kestra.core.models.rbac.Role;
import io.kestra.core.models.rbac.Group;
import io.kestra.core.services.UserService;
import io.kestra.core.services.TenantService;
import io.kestra.core.security.PermissionService;
import io.kestra.core.models.rbac.Permission;
import io.kestra.webserver.utils.PageableUtils;
import io.kestra.webserver.utils.RequestUtils;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.data.model.Pageable;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
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
 * REST API controller for user management in RBAC system
 */
@Controller("/api/v1/rbac/users")
@Tag(name = "RBAC Users", description = "User management for Role-Based Access Control")
@Validated
@Slf4j
public class UserController {
    
    @Inject
    private UserService userService;
    
    @Inject
    private TenantService tenantService;
    
    @Inject
    private PermissionService permissionService;
    
    /**
     * Get all users with pagination
     */
    @Get
    @ExecuteOn(TaskExecutors.IO)
    @Operation(summary = "List users", description = "Get paginated list of users")
    @Secured("USER_READ")
    public HttpResponse<PagedResults<User>> getUsers(
        @Parameter(description = "The current page") @QueryValue(defaultValue = "1") @Min(1) int page,
        @Parameter(description = "The current page size") @QueryValue(defaultValue = "10") @Min(1) int size,
        @Parameter(description = "Search query") @Nullable @QueryValue(value = "q") String query,
        @Parameter(description = "Filter by enabled status") @Nullable @QueryValue Boolean enabled,
        @Parameter(description = "Filter by role") @Nullable @QueryValue String roleId
    ) {
        try {
            String tenantId = tenantService.resolveTenant();
            Pageable pageable = PageableUtils.from(page, size);
            
            PagedResults<User> users = userService.findUsers(
                tenantId, 
                pageable, 
                query, 
                enabled, 
                roleId
            );
            
            return HttpResponse.ok(users);
        } catch (Exception e) {
            log.error("Error getting users", e);
            return HttpResponse.serverError();
        }
    }
    
    /**
     * Get user by ID
     */
    @Get("/{userId}")
    @ExecuteOn(TaskExecutors.IO)
    @Operation(summary = "Get user", description = "Get user by ID")
    @Secured("USER_READ")
    public HttpResponse<User> getUser(@PathVariable String userId) {
        try {
            String tenantId = tenantService.resolveTenant();
            Optional<User> user = userService.findById(tenantId, userId);
            
            return user.map(HttpResponse::ok)
                      .orElse(HttpResponse.notFound());
        } catch (Exception e) {
            log.error("Error getting user: " + userId, e);
            return HttpResponse.serverError();
        }
    }
    
    /**
     * Create new user
     */
    @Post
    @ExecuteOn(TaskExecutors.IO)
    @Operation(summary = "Create user", description = "Create a new user")
    @Secured("USER_CREATE")
    public HttpResponse<User> createUser(@Body @Valid CreateUserRequest request) {
        try {
            String tenantId = tenantService.resolveTenant();
            
            // Check if username already exists
            if (userService.existsByUsername(tenantId, request.getUsername())) {
                return HttpResponse.badRequest();
            }
            
            User user = User.builder()
                .tenantId(tenantId)
                .username(request.getUsername())
                .email(request.getEmail())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .enabled(request.isEnabled())
                .build();
            
            User createdUser = userService.create(user);
            
            // Assign initial roles if provided
            if (request.getRoleIds() != null && !request.getRoleIds().isEmpty()) {
                userService.assignRoles(createdUser.getId(), request.getRoleIds());
            }
            
            log.info("Created user: {} in tenant: {}", createdUser.getUsername(), tenantId);
            return HttpResponse.ok(createdUser);
        } catch (Exception e) {
            log.error("Error creating user", e);
            return HttpResponse.serverError();
        }
    }
    
    /**
     * Update user
     */
    @Put("/{userId}")
    @ExecuteOn(TaskExecutors.IO)
    @Operation(summary = "Update user", description = "Update user information")
    @Secured("USER_UPDATE")
    public HttpResponse<User> updateUser(
        @PathVariable String userId,
        @Body @Valid UpdateUserRequest request
    ) {
        try {
            String tenantId = tenantService.resolveTenant();
            Optional<User> existingUser = userService.findById(tenantId, userId);
            
            if (existingUser.isEmpty()) {
                return HttpResponse.notFound();
            }
            
            User user = existingUser.get();
            user.setEmail(request.getEmail());
            user.setFirstName(request.getFirstName());
            user.setLastName(request.getLastName());
            user.setEnabled(request.isEnabled());
            
            User updatedUser = userService.update(user);
            
            log.info("Updated user: {} in tenant: {}", updatedUser.getUsername(), tenantId);
            return HttpResponse.ok(updatedUser);
        } catch (Exception e) {
            log.error("Error updating user: " + userId, e);
            return HttpResponse.serverError();
        }
    }
    
    /**
     * Delete user
     */
    @Delete("/{userId}")
    @ExecuteOn(TaskExecutors.IO)
    @Operation(summary = "Delete user", description = "Delete user by ID")
    @Secured("USER_DELETE")
    public HttpResponse<Void> deleteUser(@PathVariable String userId) {
        try {
            String tenantId = tenantService.resolveTenant();
            
            if (!userService.existsById(tenantId, userId)) {
                return HttpResponse.notFound();
            }
            
            userService.delete(tenantId, userId);
            
            log.info("Deleted user: {} in tenant: {}", userId, tenantId);
            return HttpResponse.noContent();
        } catch (Exception e) {
            log.error("Error deleting user: " + userId, e);
            return HttpResponse.serverError();
        }
    }
    
    /**
     * Get user roles
     */
    @Get("/{userId}/roles")
    @ExecuteOn(TaskExecutors.IO)
    @Operation(summary = "Get user roles", description = "Get roles assigned to user")
    @Secured("USER_READ")
    public HttpResponse<List<Role>> getUserRoles(@PathVariable String userId) {
        try {
            String tenantId = tenantService.resolveTenant();
            List<Role> roles = userService.getUserRoles(tenantId, userId);
            return HttpResponse.ok(roles);
        } catch (Exception e) {
            log.error("Error getting user roles: " + userId, e);
            return HttpResponse.serverError();
        }
    }
    
    /**
     * Assign roles to user
     */
    @Post("/{userId}/roles")
    @ExecuteOn(TaskExecutors.IO)
    @Operation(summary = "Assign roles", description = "Assign roles to user")
    @Secured("USER_UPDATE")
    public HttpResponse<Void> assignRoles(
        @PathVariable String userId,
        @Body @Valid AssignRolesRequest request
    ) {
        try {
            String tenantId = tenantService.resolveTenant();
            
            if (!userService.existsById(tenantId, userId)) {
                return HttpResponse.notFound();
            }
            
            userService.assignRoles(userId, request.getRoleIds());
            
            log.info("Assigned roles to user: {} in tenant: {}", userId, tenantId);
            return HttpResponse.noContent();
        } catch (Exception e) {
            log.error("Error assigning roles to user: " + userId, e);
            return HttpResponse.serverError();
        }
    }
    
    /**
     * Get user groups
     */
    @Get("/{userId}/groups")
    @ExecuteOn(TaskExecutors.IO)
    @Operation(summary = "Get user groups", description = "Get groups user belongs to")
    @Secured("USER_READ")
    public HttpResponse<List<Group>> getUserGroups(@PathVariable String userId) {
        try {
            String tenantId = tenantService.resolveTenant();
            List<Group> groups = userService.getUserGroups(tenantId, userId);
            return HttpResponse.ok(groups);
        } catch (Exception e) {
            log.error("Error getting user groups: " + userId, e);
            return HttpResponse.serverError();
        }
    }
    
    /**
     * Get user effective permissions
     */
    @Get("/{userId}/permissions")
    @ExecuteOn(TaskExecutors.IO)
    @Operation(summary = "Get user permissions", description = "Get effective permissions for user")
    @Secured("USER_READ")
    public HttpResponse<Set<Permission>> getUserPermissions(
        @PathVariable String userId,
        @Nullable @QueryValue String namespace
    ) {
        try {
            String tenantId = tenantService.resolveTenant();
            Set<Permission> permissions = permissionService.getUserPermissions(userId, tenantId, namespace);
            return HttpResponse.ok(permissions);
        } catch (Exception e) {
            log.error("Error getting user permissions: " + userId, e);
            return HttpResponse.serverError();
        }
    }
    
    // Request/Response DTOs
    public static class CreateUserRequest {
        private String username;
        private String email;
        private String firstName;
        private String lastName;
        private boolean enabled = true;
        private Set<String> roleIds;
        
        // Getters and setters
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getFirstName() { return firstName; }
        public void setFirstName(String firstName) { this.firstName = firstName; }
        public String getLastName() { return lastName; }
        public void setLastName(String lastName) { this.lastName = lastName; }
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public Set<String> getRoleIds() { return roleIds; }
        public void setRoleIds(Set<String> roleIds) { this.roleIds = roleIds; }
    }
    
    public static class UpdateUserRequest {
        private String email;
        private String firstName;
        private String lastName;
        private boolean enabled;
        
        // Getters and setters
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getFirstName() { return firstName; }
        public void setFirstName(String firstName) { this.firstName = firstName; }
        public String getLastName() { return lastName; }
        public void setLastName(String lastName) { this.lastName = lastName; }
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
    }
    
    public static class AssignRolesRequest {
        private Set<String> roleIds;
        
        public Set<String> getRoleIds() { return roleIds; }
        public void setRoleIds(Set<String> roleIds) { this.roleIds = roleIds; }
    }
}
