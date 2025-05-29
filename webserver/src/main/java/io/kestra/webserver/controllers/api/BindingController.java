package io.kestra.webserver.controllers.api;

import io.kestra.core.models.rbac.Binding;
import io.kestra.core.models.PagedResults;
import io.kestra.core.services.BindingService;
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
 * REST API controller for binding management
 */
@Validated
@Controller("/api/v1/bindings")
@Tag(name = "Bindings", description = "Permission binding management operations")
@Slf4j
public class BindingController {

    @Inject
    private BindingService bindingService;

    @Inject
    private TenantService tenantService;

    /**
     * Get all bindings with pagination
     */
    @Get
    @ExecuteOn(TaskExecutors.IO)
    @Operation(summary = "List bindings", description = "Get paginated list of permission bindings")
    public HttpResponse<PagedResults<Binding>> getBindings(
        @Parameter(description = "The current page") @QueryValue(defaultValue = "1") @Min(1) int page,
        @Parameter(description = "The current page size") @QueryValue(defaultValue = "10") @Min(1) int size,
        @Parameter(description = "Search query") @Nullable @QueryValue(value = "q") String query
    ) {
        try {
            String tenantId = tenantService.resolveTenant();

            PagedResults<Binding> bindings = bindingService.findBindings(
                query,
                null, // subjectType
                null, // roleRef
                null, // scope
                page - 1, // Convert to 0-based
                size,
                tenantId
            );

            return HttpResponse.ok(bindings);
        } catch (Exception e) {
            log.error("Error getting bindings", e);
            return HttpResponse.serverError();
        }
    }

    /**
     * Get binding by ID
     */
    @Get("/{bindingId}")
    @ExecuteOn(TaskExecutors.IO)
    @Operation(summary = "Get binding", description = "Get binding by ID")
    public HttpResponse<Binding> getBinding(@PathVariable String bindingId) {
        try {
            Optional<Binding> binding = bindingService.findById(bindingId);

            return binding.map(HttpResponse::ok)
                         .orElse(HttpResponse.notFound());
        } catch (Exception e) {
            log.error("Error getting binding: " + bindingId, e);
            return HttpResponse.serverError();
        }
    }
}