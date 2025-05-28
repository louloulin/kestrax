package io.kestra.webserver.controllers.api;

import io.kestra.core.models.rbac.Group;
import io.kestra.core.models.rbac.User;
import io.kestra.core.models.rbac.Role;
import io.kestra.core.services.GroupService;
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
 * REST API controller for group management in RBAC system
 */
@Controller("/api/v1/rbac/groups")
@Tag(name = "RBAC Groups", description = "Group management for Role-Based Access Control")
@Validated
@Slf4j
public class GroupController {
    
    @Inject
    private GroupService groupService;
    
    @Inject
    private TenantService tenantService;
    
    /**
     * Get all groups with pagination
     */
    @Get
    @ExecuteOn(TaskExecutors.IO)
    @Operation(summary = "List groups", description = "Get paginated list of groups")
    @Secured("GROUP_READ")
    public HttpResponse<PagedResults<Group>> getGroups(
        @Parameter(description = "The current page") @QueryValue(defaultValue = "1") @Min(1) int page,
        @Parameter(description = "The current page size") @QueryValue(defaultValue = "10") @Min(1) int size,
        @Parameter(description = "Search query") @Nullable @QueryValue(value = "q") String query,
        @Parameter(description = "Include system groups") @QueryValue(defaultValue = "true") boolean includeSystem
    ) {
        try {
            String tenantId = tenantService.resolveTenant();
            Pageable pageable = PageableUtils.from(page, size);
            
            PagedResults<Group> groups = groupService.findGroups(
                tenantId, 
                pageable, 
                query, 
                includeSystem
            );
            
            return HttpResponse.ok(groups);
        } catch (Exception e) {
            log.error("Error getting groups", e);
            return HttpResponse.serverError();
        }
    }
    
    /**
     * Get group by ID
     */
    @Get("/{groupId}")
    @ExecuteOn(TaskExecutors.IO)
    @Operation(summary = "Get group", description = "Get group by ID")
    @Secured("GROUP_READ")
    public HttpResponse<Group> getGroup(@PathVariable String groupId) {
        try {
            String tenantId = tenantService.resolveTenant();
            Optional<Group> group = groupService.findById(tenantId, groupId);
            
            return group.map(HttpResponse::ok)
                       .orElse(HttpResponse.notFound());
        } catch (Exception e) {
            log.error("Error getting group: " + groupId, e);
            return HttpResponse.serverError();
        }
    }
    
    /**
     * Create new group
     */
    @Post
    @ExecuteOn(TaskExecutors.IO)
    @Operation(summary = "Create group", description = "Create a new group")
    @Secured("GROUP_CREATE")
    public HttpResponse<Group> createGroup(@Body @Valid CreateGroupRequest request) {
        try {
            String tenantId = tenantService.resolveTenant();
            
            // Check if group name already exists
            if (groupService.existsByName(tenantId, request.getName())) {
                return HttpResponse.badRequest();
            }
            
            Group group = Group.builder()
                .tenantId(tenantId)
                .name(request.getName())
                .description(request.getDescription())
                .systemGroup(false) // Custom groups are never system groups
                .build();
            
            Group createdGroup = groupService.create(group);
            
            log.info("Created group: {} in tenant: {}", createdGroup.getName(), tenantId);
            return HttpResponse.ok(createdGroup);
        } catch (Exception e) {
            log.error("Error creating group", e);
            return HttpResponse.serverError();
        }
    }
    
    /**
     * Update group
     */
    @Put("/{groupId}")
    @ExecuteOn(TaskExecutors.IO)
    @Operation(summary = "Update group", description = "Update group information")
    @Secured("GROUP_UPDATE")
    public HttpResponse<Group> updateGroup(
        @PathVariable String groupId,
        @Body @Valid UpdateGroupRequest request
    ) {
        try {
            String tenantId = tenantService.resolveTenant();
            Optional<Group> existingGroup = groupService.findById(tenantId, groupId);
            
            if (existingGroup.isEmpty()) {
                return HttpResponse.notFound();
            }
            
            Group group = existingGroup.get();
            
            // Prevent modification of system groups
            if (group.isSystemGroup()) {
                return HttpResponse.badRequest();
            }
            
            group.setDescription(request.getDescription());
            
            Group updatedGroup = groupService.update(group);
            
            log.info("Updated group: {} in tenant: {}", updatedGroup.getName(), tenantId);
            return HttpResponse.ok(updatedGroup);
        } catch (Exception e) {
            log.error("Error updating group: " + groupId, e);
            return HttpResponse.serverError();
        }
    }
    
    /**
     * Delete group
     */
    @Delete("/{groupId}")
    @ExecuteOn(TaskExecutors.IO)
    @Operation(summary = "Delete group", description = "Delete group by ID")
    @Secured("GROUP_DELETE")
    public HttpResponse<Void> deleteGroup(@PathVariable String groupId) {
        try {
            String tenantId = tenantService.resolveTenant();
            Optional<Group> group = groupService.findById(tenantId, groupId);
            
            if (group.isEmpty()) {
                return HttpResponse.notFound();
            }
            
            // Prevent deletion of system groups
            if (group.get().isSystemGroup()) {
                return HttpResponse.badRequest();
            }
            
            groupService.delete(tenantId, groupId);
            
            log.info("Deleted group: {} in tenant: {}", groupId, tenantId);
            return HttpResponse.noContent();
        } catch (Exception e) {
            log.error("Error deleting group: " + groupId, e);
            return HttpResponse.serverError();
        }
    }
    
    /**
     * Get group members
     */
    @Get("/{groupId}/members")
    @ExecuteOn(TaskExecutors.IO)
    @Operation(summary = "Get group members", description = "Get users in group")
    @Secured("GROUP_READ")
    public HttpResponse<List<User>> getGroupMembers(@PathVariable String groupId) {
        try {
            String tenantId = tenantService.resolveTenant();
            List<User> members = groupService.getGroupMembers(tenantId, groupId);
            return HttpResponse.ok(members);
        } catch (Exception e) {
            log.error("Error getting group members: " + groupId, e);
            return HttpResponse.serverError();
        }
    }
    
    /**
     * Add members to group
     */
    @Post("/{groupId}/members")
    @ExecuteOn(TaskExecutors.IO)
    @Operation(summary = "Add members", description = "Add users to group")
    @Secured("GROUP_UPDATE")
    public HttpResponse<Void> addMembers(
        @PathVariable String groupId,
        @Body @Valid AddMembersRequest request
    ) {
        try {
            String tenantId = tenantService.resolveTenant();
            
            if (!groupService.existsById(tenantId, groupId)) {
                return HttpResponse.notFound();
            }
            
            groupService.addMembers(groupId, request.getUserIds());
            
            log.info("Added members to group: {} in tenant: {}", groupId, tenantId);
            return HttpResponse.noContent();
        } catch (Exception e) {
            log.error("Error adding members to group: " + groupId, e);
            return HttpResponse.serverError();
        }
    }
    
    /**
     * Remove member from group
     */
    @Delete("/{groupId}/members/{userId}")
    @ExecuteOn(TaskExecutors.IO)
    @Operation(summary = "Remove member", description = "Remove user from group")
    @Secured("GROUP_UPDATE")
    public HttpResponse<Void> removeMember(
        @PathVariable String groupId,
        @PathVariable String userId
    ) {
        try {
            String tenantId = tenantService.resolveTenant();
            
            if (!groupService.existsById(tenantId, groupId)) {
                return HttpResponse.notFound();
            }
            
            groupService.removeMember(groupId, userId);
            
            log.info("Removed member {} from group: {} in tenant: {}", userId, groupId, tenantId);
            return HttpResponse.noContent();
        } catch (Exception e) {
            log.error("Error removing member from group: " + groupId, e);
            return HttpResponse.serverError();
        }
    }
    
    /**
     * Get group roles
     */
    @Get("/{groupId}/roles")
    @ExecuteOn(TaskExecutors.IO)
    @Operation(summary = "Get group roles", description = "Get roles assigned to group")
    @Secured("GROUP_READ")
    public HttpResponse<List<Role>> getGroupRoles(@PathVariable String groupId) {
        try {
            String tenantId = tenantService.resolveTenant();
            List<Role> roles = groupService.getGroupRoles(tenantId, groupId);
            return HttpResponse.ok(roles);
        } catch (Exception e) {
            log.error("Error getting group roles: " + groupId, e);
            return HttpResponse.serverError();
        }
    }
    
    /**
     * Assign roles to group
     */
    @Post("/{groupId}/roles")
    @ExecuteOn(TaskExecutors.IO)
    @Operation(summary = "Assign roles", description = "Assign roles to group")
    @Secured("GROUP_UPDATE")
    public HttpResponse<Void> assignRoles(
        @PathVariable String groupId,
        @Body @Valid AssignRolesRequest request
    ) {
        try {
            String tenantId = tenantService.resolveTenant();
            
            if (!groupService.existsById(tenantId, groupId)) {
                return HttpResponse.notFound();
            }
            
            groupService.assignRoles(groupId, request.getRoleIds());
            
            log.info("Assigned roles to group: {} in tenant: {}", groupId, tenantId);
            return HttpResponse.noContent();
        } catch (Exception e) {
            log.error("Error assigning roles to group: " + groupId, e);
            return HttpResponse.serverError();
        }
    }
    
    /**
     * Get system groups
     */
    @Get("/system")
    @ExecuteOn(TaskExecutors.IO)
    @Operation(summary = "Get system groups", description = "Get predefined system groups")
    @Secured("GROUP_READ")
    public HttpResponse<List<Group>> getSystemGroups() {
        try {
            String tenantId = tenantService.resolveTenant();
            List<Group> systemGroups = groupService.getSystemGroups(tenantId);
            return HttpResponse.ok(systemGroups);
        } catch (Exception e) {
            log.error("Error getting system groups", e);
            return HttpResponse.serverError();
        }
    }
    
    // Request DTOs
    public static class CreateGroupRequest {
        private String name;
        private String description;
        
        // Getters and setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }
    
    public static class UpdateGroupRequest {
        private String description;
        
        // Getters and setters
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }
    
    public static class AddMembersRequest {
        private Set<String> userIds;
        
        public Set<String> getUserIds() { return userIds; }
        public void setUserIds(Set<String> userIds) { this.userIds = userIds; }
    }
    
    public static class AssignRolesRequest {
        private Set<String> roleIds;
        
        public Set<String> getRoleIds() { return roleIds; }
        public void setRoleIds(Set<String> roleIds) { this.roleIds = roleIds; }
    }
}
