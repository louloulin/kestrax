package io.kestra.webserver.controllers.api;

import io.kestra.core.models.rbac.User;
import io.kestra.core.models.PagedResults;
import io.kestra.core.services.UserService;
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
 * REST API controller for user management
 */
@Validated
@Controller("/api/v1/users")
@Tag(name = "Users", description = "User management operations")
@Slf4j
public class UserController {

    @Inject
    private UserService userService;

    @Inject
    private TenantService tenantService;

    /**
     * Get all users with pagination
     */
    @Get
    @ExecuteOn(TaskExecutors.IO)
    @Operation(summary = "List users", description = "Get paginated list of users")
    public HttpResponse<PagedResults<User>> getUsers(
        @Parameter(description = "The current page") @QueryValue(defaultValue = "1") @Min(1) int page,
        @Parameter(description = "The current page size") @QueryValue(defaultValue = "10") @Min(1) int size,
        @Parameter(description = "Search query") @Nullable @QueryValue(value = "q") String query
    ) {
        try {
            String tenantId = tenantService.resolveTenant();

            // Mock implementation - return empty results
            PagedResults<User> users = PagedResults.of(
                java.util.List.of(), // Empty list
                0L // Total count
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
    public HttpResponse<User> getUser(@PathVariable String userId) {
        try {
            Optional<User> user = userService.findById(userId);

            return user.map(HttpResponse::ok)
                      .orElse(HttpResponse.notFound());
        } catch (Exception e) {
            log.error("Error getting user: " + userId, e);
            return HttpResponse.serverError();
        }
    }
}