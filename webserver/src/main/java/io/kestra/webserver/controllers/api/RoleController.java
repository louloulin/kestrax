package io.kestra.webserver.controllers.api;

import io.kestra.core.models.rbac.Role;
import io.kestra.core.models.PagedResults;
import io.kestra.core.services.RoleService;
import io.kestra.core.tenant.TenantService;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.*;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.micronaut.validation.Validated;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import jakarta.validation.constraints.Min;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

/**
 * REST API controller for role management
 */
@Validated
@Controller("/api/v1/roles")
@Tag(name = "Roles", description = "Role management operations")
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
    public HttpResponse<PagedResults<Role>> getRoles(
        @Parameter(description = "The current page") @QueryValue(defaultValue = "1") @Min(1) int page,
        @Parameter(description = "The current page size") @QueryValue(defaultValue = "10") @Min(1) int size,
        @Parameter(description = "Search query") @Nullable @QueryValue(value = "q") String query
    ) {
        try {
            String tenantId = tenantService.resolveTenant();

            PagedResults<Role> roles = roleService.findRoles(
                query,
                page - 1, // Convert to 0-based
                size,
                tenantId
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
    public HttpResponse<Role> getRole(@PathVariable String roleId) {
        try {
            Optional<Role> role = roleService.findById(roleId);

            return role.map(HttpResponse::ok)
                      .orElse(HttpResponse.notFound());
        } catch (Exception e) {
            log.error("Error getting role: " + roleId, e);
            return HttpResponse.serverError();
        }
    }
}