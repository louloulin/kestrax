package io.kestra.webserver.controllers.api;

import io.kestra.core.models.tenants.TenantConfig;
import io.kestra.core.services.TenantConfigService;
import io.kestra.core.tenant.TenantContext;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.*;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;
import io.micronaut.validation.Validated;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * REST API controller for tenant configuration management
 */
@Controller("/api/v1/tenant/configs")
@Secured(SecurityRule.IS_AUTHENTICATED)
@Validated
@Slf4j
public class TenantConfigController {
    
    @Inject
    private TenantConfigService tenantConfigService;
    
    /**
     * Get all configurations for current tenant
     */
    @Get
    public HttpResponse<List<TenantConfig>> getAllConfigs() {
        try {
            List<TenantConfig> configs = tenantConfigService.getAllConfigs();
            return HttpResponse.ok(configs);
        } catch (Exception e) {
            log.error("Error getting all configurations", e);
            return HttpResponse.serverError();
        }
    }
    
    /**
     * Get configurations by category
     */
    @Get("/category/{category}")
    public HttpResponse<List<TenantConfig>> getConfigsByCategory(@PathVariable TenantConfig.ConfigCategory category) {
        try {
            List<TenantConfig> configs = tenantConfigService.getConfigsByCategory(category);
            return HttpResponse.ok(configs);
        } catch (Exception e) {
            log.error("Error getting configurations by category: " + category, e);
            return HttpResponse.serverError();
        }
    }
    
    /**
     * Get configuration categories with counts
     */
    @Get("/categories")
    public HttpResponse<Map<TenantConfig.ConfigCategory, Long>> getConfigCategoryCounts() {
        try {
            Map<TenantConfig.ConfigCategory, Long> counts = tenantConfigService.getConfigCategoryCounts();
            return HttpResponse.ok(counts);
        } catch (Exception e) {
            log.error("Error getting configuration category counts", e);
            return HttpResponse.serverError();
        }
    }
    
    /**
     * Get a specific configuration
     */
    @Get("/{key}")
    public HttpResponse<TenantConfig> getConfig(@PathVariable @NotBlank String key) {
        try {
            Optional<TenantConfig> config = tenantConfigService.getConfig(key);
            return config.map(HttpResponse::ok)
                        .orElse(HttpResponse.notFound());
        } catch (Exception e) {
            log.error("Error getting configuration: " + key, e);
            return HttpResponse.serverError();
        }
    }
    
    /**
     * Search configurations by key pattern
     */
    @Get("/search")
    public HttpResponse<List<TenantConfig>> searchConfigs(@QueryValue @NotBlank String q) {
        try {
            List<TenantConfig> configs = tenantConfigService.searchConfigs(q);
            return HttpResponse.ok(configs);
        } catch (Exception e) {
            log.error("Error searching configurations with pattern: " + q, e);
            return HttpResponse.serverError();
        }
    }
    
    /**
     * Create or update a configuration
     */
    @Put("/{key}")
    public HttpResponse<TenantConfig> setConfig(
            @PathVariable @NotBlank String key,
            @Body @Valid ConfigRequest request,
            Principal principal) {
        try {
            String username = principal != null ? principal.getName() : "system";
            
            TenantConfig config = tenantConfigService.setConfig(
                key,
                request.value,
                request.type,
                request.category,
                request.description
            );
            
            return HttpResponse.ok(config);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid configuration request for key {}: {}", key, e.getMessage());
            return HttpResponse.badRequest();
        } catch (Exception e) {
            log.error("Error setting configuration: " + key, e);
            return HttpResponse.serverError();
        }
    }
    
    /**
     * Bulk update configurations
     */
    @Put("/bulk")
    public HttpResponse<List<TenantConfig>> bulkUpdateConfigs(
            @Body @Valid BulkConfigRequest request,
            Principal principal) {
        try {
            String username = principal != null ? principal.getName() : "system";
            
            List<TenantConfig> configs = tenantConfigService.bulkUpdateConfigs(
                request.configs,
                username
            );
            
            return HttpResponse.ok(configs);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid bulk configuration request: {}", e.getMessage());
            return HttpResponse.badRequest();
        } catch (Exception e) {
            log.error("Error bulk updating configurations", e);
            return HttpResponse.serverError();
        }
    }
    
    /**
     * Delete a configuration
     */
    @Delete("/{key}")
    public HttpResponse<Void> deleteConfig(@PathVariable @NotBlank String key) {
        try {
            boolean deleted = tenantConfigService.deleteConfig(key);
            return deleted ? HttpResponse.noContent() : HttpResponse.notFound();
        } catch (Exception e) {
            log.error("Error deleting configuration: " + key, e);
            return HttpResponse.serverError();
        }
    }
    
    /**
     * Reset configurations to defaults
     */
    @Post("/reset")
    public HttpResponse<Void> resetToDefaults() {
        try {
            tenantConfigService.resetToDefaults();
            return HttpResponse.noContent();
        } catch (Exception e) {
            log.error("Error resetting configurations to defaults", e);
            return HttpResponse.serverError();
        }
    }
    
    /**
     * Get configuration schema for UI rendering
     */
    @Get("/schema")
    public HttpResponse<ConfigSchema> getConfigSchema() {
        try {
            ConfigSchema schema = ConfigSchema.builder()
                .types(List.of(TenantConfig.ConfigType.values()))
                .categories(List.of(TenantConfig.ConfigCategory.values()))
                .build();
            
            return HttpResponse.ok(schema);
        } catch (Exception e) {
            log.error("Error getting configuration schema", e);
            return HttpResponse.serverError();
        }
    }
    
    /**
     * Request model for configuration updates
     */
    public static class ConfigRequest {
        @NotNull
        public Object value;
        
        @NotNull
        public TenantConfig.ConfigType type;
        
        @NotNull
        public TenantConfig.ConfigCategory category;
        
        @Nullable
        public String description;
        
        @Nullable
        public Map<String, Object> validation;
        
        @Nullable
        public Map<String, Object> uiHints;
        
        @Nullable
        public Boolean required;
        
        @Nullable
        public Boolean sensitive;
        
        @Nullable
        public Boolean readOnly;
    }
    
    /**
     * Request model for bulk configuration updates
     */
    public static class BulkConfigRequest {
        @NotNull
        public Map<String, Object> configs;
    }
    
    /**
     * Configuration schema for UI
     */
    public static class ConfigSchema {
        public List<TenantConfig.ConfigType> types;
        public List<TenantConfig.ConfigCategory> categories;
        
        public static ConfigSchemaBuilder builder() {
            return new ConfigSchemaBuilder();
        }
        
        public static class ConfigSchemaBuilder {
            private List<TenantConfig.ConfigType> types;
            private List<TenantConfig.ConfigCategory> categories;
            
            public ConfigSchemaBuilder types(List<TenantConfig.ConfigType> types) {
                this.types = types;
                return this;
            }
            
            public ConfigSchemaBuilder categories(List<TenantConfig.ConfigCategory> categories) {
                this.categories = categories;
                return this;
            }
            
            public ConfigSchema build() {
                ConfigSchema schema = new ConfigSchema();
                schema.types = this.types;
                schema.categories = this.categories;
                return schema;
            }
        }
    }
}
