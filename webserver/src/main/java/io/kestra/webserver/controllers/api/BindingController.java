package io.kestra.webserver.controllers.api;

import io.kestra.core.models.rbac.Binding;
import io.kestra.core.models.rbac.Binding.SubjectType;
import io.kestra.core.services.BindingService;
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
 * REST API controller for permission binding management in RBAC system
 */
@Controller("/api/v1/rbac/bindings")
@Tag(name = "RBAC Bindings", description = "Permission binding management for Role-Based Access Control")
@Validated
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
    @Secured("BINDING_READ")
    public HttpResponse<PagedResults<Binding>> getBindings(
        @Parameter(description = "The current page") @QueryValue(defaultValue = "1") @Min(1) int page,
        @Parameter(description = "The current page size") @QueryValue(defaultValue = "10") @Min(1) int size,
        @Parameter(description = "Filter by subject type") @Nullable @QueryValue SubjectType subjectType,
        @Parameter(description = "Filter by subject ID") @Nullable @QueryValue String subjectId,
        @Parameter(description = "Filter by role ID") @Nullable @QueryValue String roleId,
        @Parameter(description = "Filter by namespace") @Nullable @QueryValue String namespace,
        @Parameter(description = "Include system bindings") @QueryValue(defaultValue = "true") boolean includeSystem
    ) {
        try {
            String tenantId = tenantService.resolveTenant();
            Pageable pageable = PageableUtils.from(page, size);
            
            PagedResults<Binding> bindings = bindingService.findBindings(
                tenantId, 
                pageable, 
                subjectType,
                subjectId,
                roleId,
                namespace,
                includeSystem
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
    @Secured("BINDING_READ")
    public HttpResponse<Binding> getBinding(@PathVariable String bindingId) {
        try {
            String tenantId = tenantService.resolveTenant();
            Optional<Binding> binding = bindingService.findById(tenantId, bindingId);
            
            return binding.map(HttpResponse::ok)
                         .orElse(HttpResponse.notFound());
        } catch (Exception e) {
            log.error("Error getting binding: " + bindingId, e);
            return HttpResponse.serverError();
        }
    }
    
    /**
     * Create new binding
     */
    @Post
    @ExecuteOn(TaskExecutors.IO)
    @Operation(summary = "Create binding", description = "Create a new permission binding")
    @Secured("BINDING_CREATE")
    public HttpResponse<Binding> createBinding(@Body @Valid CreateBindingRequest request) {
        try {
            String tenantId = tenantService.resolveTenant();
            
            Binding binding = Binding.builder()
                .tenantId(tenantId)
                .name(request.getName())
                .description(request.getDescription())
                .roleId(request.getRoleId())
                .subjectType(request.getSubjectType())
                .subjectIds(request.getSubjectIds())
                .namespace(request.getNamespace())
                .systemBinding(false) // Custom bindings are never system bindings
                .build();
            
            Binding createdBinding = bindingService.create(binding);
            
            log.info("Created binding: {} in tenant: {}", createdBinding.getName(), tenantId);
            return HttpResponse.ok(createdBinding);
        } catch (Exception e) {
            log.error("Error creating binding", e);
            return HttpResponse.serverError();
        }
    }
    
    /**
     * Update binding
     */
    @Put("/{bindingId}")
    @ExecuteOn(TaskExecutors.IO)
    @Operation(summary = "Update binding", description = "Update binding information")
    @Secured("BINDING_UPDATE")
    public HttpResponse<Binding> updateBinding(
        @PathVariable String bindingId,
        @Body @Valid UpdateBindingRequest request
    ) {
        try {
            String tenantId = tenantService.resolveTenant();
            Optional<Binding> existingBinding = bindingService.findById(tenantId, bindingId);
            
            if (existingBinding.isEmpty()) {
                return HttpResponse.notFound();
            }
            
            Binding binding = existingBinding.get();
            
            // Prevent modification of system bindings
            if (binding.isSystemBinding()) {
                return HttpResponse.badRequest();
            }
            
            binding.setDescription(request.getDescription());
            binding.setSubjectIds(request.getSubjectIds());
            
            Binding updatedBinding = bindingService.update(binding);
            
            log.info("Updated binding: {} in tenant: {}", updatedBinding.getName(), tenantId);
            return HttpResponse.ok(updatedBinding);
        } catch (Exception e) {
            log.error("Error updating binding: " + bindingId, e);
            return HttpResponse.serverError();
        }
    }
    
    /**
     * Delete binding
     */
    @Delete("/{bindingId}")
    @ExecuteOn(TaskExecutors.IO)
    @Operation(summary = "Delete binding", description = "Delete binding by ID")
    @Secured("BINDING_DELETE")
    public HttpResponse<Void> deleteBinding(@PathVariable String bindingId) {
        try {
            String tenantId = tenantService.resolveTenant();
            Optional<Binding> binding = bindingService.findById(tenantId, bindingId);
            
            if (binding.isEmpty()) {
                return HttpResponse.notFound();
            }
            
            // Prevent deletion of system bindings
            if (binding.get().isSystemBinding()) {
                return HttpResponse.badRequest();
            }
            
            bindingService.delete(tenantId, bindingId);
            
            log.info("Deleted binding: {} in tenant: {}", bindingId, tenantId);
            return HttpResponse.noContent();
        } catch (Exception e) {
            log.error("Error deleting binding: " + bindingId, e);
            return HttpResponse.serverError();
        }
    }
    
    /**
     * Get bindings for subject
     */
    @Get("/subject/{subjectType}/{subjectId}")
    @ExecuteOn(TaskExecutors.IO)
    @Operation(summary = "Get subject bindings", description = "Get bindings for specific subject")
    @Secured("BINDING_READ")
    public HttpResponse<List<Binding>> getSubjectBindings(
        @PathVariable SubjectType subjectType,
        @PathVariable String subjectId,
        @Nullable @QueryValue String namespace
    ) {
        try {
            String tenantId = tenantService.resolveTenant();
            List<Binding> bindings = bindingService.getSubjectBindings(
                tenantId, 
                subjectType, 
                subjectId, 
                namespace
            );
            return HttpResponse.ok(bindings);
        } catch (Exception e) {
            log.error("Error getting subject bindings: {} {}", subjectType, subjectId, e);
            return HttpResponse.serverError();
        }
    }
    
    /**
     * Get bindings for role
     */
    @Get("/role/{roleId}")
    @ExecuteOn(TaskExecutors.IO)
    @Operation(summary = "Get role bindings", description = "Get bindings for specific role")
    @Secured("BINDING_READ")
    public HttpResponse<List<Binding>> getRoleBindings(
        @PathVariable String roleId,
        @Nullable @QueryValue String namespace
    ) {
        try {
            String tenantId = tenantService.resolveTenant();
            List<Binding> bindings = bindingService.getRoleBindings(tenantId, roleId, namespace);
            return HttpResponse.ok(bindings);
        } catch (Exception e) {
            log.error("Error getting role bindings: " + roleId, e);
            return HttpResponse.serverError();
        }
    }
    
    /**
     * Get system bindings
     */
    @Get("/system")
    @ExecuteOn(TaskExecutors.IO)
    @Operation(summary = "Get system bindings", description = "Get predefined system bindings")
    @Secured("BINDING_READ")
    public HttpResponse<List<Binding>> getSystemBindings() {
        try {
            String tenantId = tenantService.resolveTenant();
            List<Binding> systemBindings = bindingService.getSystemBindings(tenantId);
            return HttpResponse.ok(systemBindings);
        } catch (Exception e) {
            log.error("Error getting system bindings", e);
            return HttpResponse.serverError();
        }
    }
    
    /**
     * Bulk create bindings
     */
    @Post("/bulk")
    @ExecuteOn(TaskExecutors.IO)
    @Operation(summary = "Bulk create bindings", description = "Create multiple bindings at once")
    @Secured("BINDING_CREATE")
    public HttpResponse<List<Binding>> bulkCreateBindings(@Body @Valid BulkCreateBindingsRequest request) {
        try {
            String tenantId = tenantService.resolveTenant();
            
            List<Binding> bindings = request.getBindings().stream()
                .map(req -> Binding.builder()
                    .tenantId(tenantId)
                    .name(req.getName())
                    .description(req.getDescription())
                    .roleId(req.getRoleId())
                    .subjectType(req.getSubjectType())
                    .subjectIds(req.getSubjectIds())
                    .namespace(req.getNamespace())
                    .systemBinding(false)
                    .build())
                .toList();
            
            List<Binding> createdBindings = bindingService.bulkCreate(bindings);
            
            log.info("Bulk created {} bindings in tenant: {}", createdBindings.size(), tenantId);
            return HttpResponse.ok(createdBindings);
        } catch (Exception e) {
            log.error("Error bulk creating bindings", e);
            return HttpResponse.serverError();
        }
    }
    
    // Request DTOs
    public static class CreateBindingRequest {
        private String name;
        private String description;
        private String roleId;
        private SubjectType subjectType;
        private Set<String> subjectIds;
        private String namespace;
        
        // Getters and setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getRoleId() { return roleId; }
        public void setRoleId(String roleId) { this.roleId = roleId; }
        public SubjectType getSubjectType() { return subjectType; }
        public void setSubjectType(SubjectType subjectType) { this.subjectType = subjectType; }
        public Set<String> getSubjectIds() { return subjectIds; }
        public void setSubjectIds(Set<String> subjectIds) { this.subjectIds = subjectIds; }
        public String getNamespace() { return namespace; }
        public void setNamespace(String namespace) { this.namespace = namespace; }
    }
    
    public static class UpdateBindingRequest {
        private String description;
        private Set<String> subjectIds;
        
        // Getters and setters
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public Set<String> getSubjectIds() { return subjectIds; }
        public void setSubjectIds(Set<String> subjectIds) { this.subjectIds = subjectIds; }
    }
    
    public static class BulkCreateBindingsRequest {
        private List<CreateBindingRequest> bindings;
        
        public List<CreateBindingRequest> getBindings() { return bindings; }
        public void setBindings(List<CreateBindingRequest> bindings) { this.bindings = bindings; }
    }
}
