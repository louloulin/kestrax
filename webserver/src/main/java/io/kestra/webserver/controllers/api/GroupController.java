package io.kestra.webserver.controllers.api;

import io.kestra.core.models.rbac.Group;
import io.kestra.core.models.PagedResults;
import io.kestra.core.services.GroupService;
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
 * REST API controller for group management
 */
@Validated
@Controller("/api/v1/groups")
@Tag(name = "Groups", description = "Group management operations")
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
    public HttpResponse<PagedResults<Group>> getGroups(
        @Parameter(description = "The current page") @QueryValue(defaultValue = "1") @Min(1) int page,
        @Parameter(description = "The current page size") @QueryValue(defaultValue = "10") @Min(1) int size,
        @Parameter(description = "Search query") @Nullable @QueryValue(value = "q") String query
    ) {
        try {
            String tenantId = tenantService.resolveTenant();

            PagedResults<Group> groups = groupService.findGroups(
                query,
                page - 1, // Convert to 0-based
                size,
                tenantId
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
    public HttpResponse<Group> getGroup(@PathVariable String groupId) {
        try {
            Optional<Group> group = groupService.findById(groupId);

            return group.map(HttpResponse::ok)
                       .orElse(HttpResponse.notFound());
        } catch (Exception e) {
            log.error("Error getting group: " + groupId, e);
            return HttpResponse.serverError();
        }
    }
}
