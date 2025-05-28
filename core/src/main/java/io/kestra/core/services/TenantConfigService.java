package io.kestra.core.services;

import io.kestra.core.models.tenants.TenantConfig;
import io.kestra.core.tenant.TenantContext;
import io.micronaut.core.annotation.Nullable;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Service for managing tenant-specific configurations
 */
@Singleton
@Slf4j
public class TenantConfigService {
    
    // In-memory storage for demonstration - would be replaced with actual repository
    private final Map<String, Map<String, TenantConfig>> tenantConfigs = new ConcurrentHashMap<>();
    private final Map<String, TenantConfig> globalDefaults = new ConcurrentHashMap<>();
    
    public TenantConfigService() {
        initializeGlobalDefaults();
    }
    
    /**
     * Set a configuration value for current tenant
     */
    public TenantConfig setConfig(String key, Object value, TenantConfig.ConfigType type, 
                                 TenantConfig.ConfigCategory category, @Nullable String description) {
        String tenantId = TenantContext.requireCurrentTenantId();
        return setConfig(tenantId, key, value, type, category, description, null);
    }
    
    /**
     * Set a configuration value for specific tenant
     */
    public TenantConfig setConfig(String tenantId, String key, Object value, TenantConfig.ConfigType type, 
                                 TenantConfig.ConfigCategory category, @Nullable String description, 
                                 @Nullable String updatedBy) {
        validateConfigKey(key);
        validateConfigValue(value, type);
        
        Map<String, TenantConfig> configs = tenantConfigs.computeIfAbsent(tenantId, k -> new ConcurrentHashMap<>());
        
        TenantConfig existingConfig = configs.get(key);
        TenantConfig newConfig;
        
        if (existingConfig != null) {
            // Update existing configuration
            newConfig = existingConfig
                .withValue(value)
                .withType(type)
                .withCategory(category)
                .withDescription(description)
                .withUpdate(updatedBy);
        } else {
            // Create new configuration
            newConfig = TenantConfig.builder()
                .tenantId(tenantId)
                .key(key)
                .value(value)
                .type(type)
                .category(category)
                .description(description)
                .createdBy(updatedBy)
                .updatedBy(updatedBy)
                .build();
        }
        
        configs.put(key, newConfig);
        
        log.info("Set configuration '{}' for tenant '{}': {} = {}", 
                key, tenantId, type, newConfig.shouldMask() ? "***" : value);
        
        return newConfig;
    }
    
    /**
     * Get a configuration value for current tenant
     */
    public Optional<TenantConfig> getConfig(String key) {
        String tenantId = TenantContext.requireCurrentTenantId();
        return getConfig(tenantId, key);
    }
    
    /**
     * Get a configuration value for specific tenant
     */
    public Optional<TenantConfig> getConfig(String tenantId, String key) {
        Map<String, TenantConfig> configs = tenantConfigs.get(tenantId);
        if (configs != null && configs.containsKey(key)) {
            return Optional.of(configs.get(key));
        }
        
        // Fall back to global default
        return Optional.ofNullable(globalDefaults.get(key));
    }
    
    /**
     * Get configuration value with type conversion for current tenant
     */
    public <T> Optional<T> getConfigValue(String key, Class<T> type) {
        String tenantId = TenantContext.requireCurrentTenantId();
        return getConfigValue(tenantId, key, type);
    }
    
    /**
     * Get configuration value with type conversion for specific tenant
     */
    public <T> Optional<T> getConfigValue(String tenantId, String key, Class<T> type) {
        return getConfig(tenantId, key)
            .map(config -> config.getValue(type));
    }
    
    /**
     * Get configuration value with default for current tenant
     */
    public <T> T getConfigValue(String key, Class<T> type, T defaultValue) {
        String tenantId = TenantContext.requireCurrentTenantId();
        return getConfigValue(tenantId, key, type, defaultValue);
    }
    
    /**
     * Get configuration value with default for specific tenant
     */
    public <T> T getConfigValue(String tenantId, String key, Class<T> type, T defaultValue) {
        return getConfigValue(tenantId, key, type).orElse(defaultValue);
    }
    
    /**
     * Get all configurations for current tenant
     */
    public List<TenantConfig> getAllConfigs() {
        String tenantId = TenantContext.requireCurrentTenantId();
        return getAllConfigs(tenantId);
    }
    
    /**
     * Get all configurations for specific tenant
     */
    public List<TenantConfig> getAllConfigs(String tenantId) {
        Map<String, TenantConfig> configs = tenantConfigs.getOrDefault(tenantId, Map.of());
        
        // Merge with global defaults
        Map<String, TenantConfig> merged = new HashMap<>(globalDefaults);
        merged.putAll(configs);
        
        return merged.values().stream()
                    .sorted(Comparator.comparing(TenantConfig::getCategory)
                                     .thenComparing(TenantConfig::getKey))
                    .collect(Collectors.toList());
    }
    
    /**
     * Get configurations by category for current tenant
     */
    public List<TenantConfig> getConfigsByCategory(TenantConfig.ConfigCategory category) {
        String tenantId = TenantContext.requireCurrentTenantId();
        return getConfigsByCategory(tenantId, category);
    }
    
    /**
     * Get configurations by category for specific tenant
     */
    public List<TenantConfig> getConfigsByCategory(String tenantId, TenantConfig.ConfigCategory category) {
        return getAllConfigs(tenantId).stream()
                                     .filter(config -> config.getCategory() == category)
                                     .collect(Collectors.toList());
    }
    
    /**
     * Delete a configuration for current tenant
     */
    public boolean deleteConfig(String key) {
        String tenantId = TenantContext.requireCurrentTenantId();
        return deleteConfig(tenantId, key);
    }
    
    /**
     * Delete a configuration for specific tenant
     */
    public boolean deleteConfig(String tenantId, String key) {
        Map<String, TenantConfig> configs = tenantConfigs.get(tenantId);
        if (configs != null) {
            TenantConfig removed = configs.remove(key);
            if (removed != null) {
                log.info("Deleted configuration '{}' for tenant '{}'", key, tenantId);
                return true;
            }
        }
        return false;
    }
    
    /**
     * Check if a configuration exists for current tenant
     */
    public boolean hasConfig(String key) {
        String tenantId = TenantContext.requireCurrentTenantId();
        return hasConfig(tenantId, key);
    }
    
    /**
     * Check if a configuration exists for specific tenant
     */
    public boolean hasConfig(String tenantId, String key) {
        return getConfig(tenantId, key).isPresent();
    }
    
    /**
     * Get configuration categories with counts for current tenant
     */
    public Map<TenantConfig.ConfigCategory, Long> getConfigCategoryCounts() {
        String tenantId = TenantContext.requireCurrentTenantId();
        return getConfigCategoryCounts(tenantId);
    }
    
    /**
     * Get configuration categories with counts for specific tenant
     */
    public Map<TenantConfig.ConfigCategory, Long> getConfigCategoryCounts(String tenantId) {
        return getAllConfigs(tenantId).stream()
                                     .collect(Collectors.groupingBy(
                                         TenantConfig::getCategory,
                                         Collectors.counting()
                                     ));
    }
    
    /**
     * Search configurations by key pattern for current tenant
     */
    public List<TenantConfig> searchConfigs(String keyPattern) {
        String tenantId = TenantContext.requireCurrentTenantId();
        return searchConfigs(tenantId, keyPattern);
    }
    
    /**
     * Search configurations by key pattern for specific tenant
     */
    public List<TenantConfig> searchConfigs(String tenantId, String keyPattern) {
        String pattern = keyPattern.toLowerCase();
        return getAllConfigs(tenantId).stream()
                                     .filter(config -> config.getKey().toLowerCase().contains(pattern))
                                     .collect(Collectors.toList());
    }
    
    /**
     * Bulk update configurations for current tenant
     */
    public List<TenantConfig> bulkUpdateConfigs(Map<String, Object> configUpdates, String updatedBy) {
        String tenantId = TenantContext.requireCurrentTenantId();
        return bulkUpdateConfigs(tenantId, configUpdates, updatedBy);
    }
    
    /**
     * Bulk update configurations for specific tenant
     */
    public List<TenantConfig> bulkUpdateConfigs(String tenantId, Map<String, Object> configUpdates, String updatedBy) {
        List<TenantConfig> updatedConfigs = new ArrayList<>();
        
        for (Map.Entry<String, Object> entry : configUpdates.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            
            Optional<TenantConfig> existingConfig = getConfig(tenantId, key);
            if (existingConfig.isPresent()) {
                TenantConfig config = existingConfig.get();
                TenantConfig updated = setConfig(tenantId, key, value, config.getType(), 
                                               config.getCategory(), config.getDescription(), updatedBy);
                updatedConfigs.add(updated);
            }
        }
        
        log.info("Bulk updated {} configurations for tenant '{}'", updatedConfigs.size(), tenantId);
        return updatedConfigs;
    }
    
    /**
     * Reset configurations to defaults for current tenant
     */
    public void resetToDefaults() {
        String tenantId = TenantContext.requireCurrentTenantId();
        resetToDefaults(tenantId);
    }
    
    /**
     * Reset configurations to defaults for specific tenant
     */
    public void resetToDefaults(String tenantId) {
        tenantConfigs.remove(tenantId);
        log.info("Reset configurations to defaults for tenant '{}'", tenantId);
    }
    
    /**
     * Initialize global default configurations
     */
    private void initializeGlobalDefaults() {
        // General settings
        addGlobalDefault("app.name", "DataFlare", TenantConfig.ConfigType.STRING, 
                        TenantConfig.ConfigCategory.GENERAL, "Application name");
        addGlobalDefault("app.timezone", "UTC", TenantConfig.ConfigType.STRING, 
                        TenantConfig.ConfigCategory.GENERAL, "Default timezone");
        addGlobalDefault("app.language", "en", TenantConfig.ConfigType.STRING, 
                        TenantConfig.ConfigCategory.GENERAL, "Default language");
        
        // Performance settings
        addGlobalDefault("execution.max-concurrent", 10, TenantConfig.ConfigType.INTEGER, 
                        TenantConfig.ConfigCategory.PERFORMANCE, "Maximum concurrent executions");
        addGlobalDefault("execution.timeout", 3600, TenantConfig.ConfigType.INTEGER, 
                        TenantConfig.ConfigCategory.PERFORMANCE, "Default execution timeout in seconds");
        
        // UI settings
        addGlobalDefault("ui.theme", "light", TenantConfig.ConfigType.STRING, 
                        TenantConfig.ConfigCategory.UI, "Default UI theme");
        addGlobalDefault("ui.page-size", 20, TenantConfig.ConfigType.INTEGER, 
                        TenantConfig.ConfigCategory.UI, "Default page size for lists");
        
        // Security settings
        addGlobalDefault("security.session-timeout", 1800, TenantConfig.ConfigType.INTEGER, 
                        TenantConfig.ConfigCategory.SECURITY, "Session timeout in seconds");
        addGlobalDefault("security.password-min-length", 8, TenantConfig.ConfigType.INTEGER, 
                        TenantConfig.ConfigCategory.SECURITY, "Minimum password length");
    }
    
    /**
     * Add a global default configuration
     */
    private void addGlobalDefault(String key, Object value, TenantConfig.ConfigType type, 
                                 TenantConfig.ConfigCategory category, String description) {
        TenantConfig config = TenantConfig.builder()
            .tenantId("global")
            .key(key)
            .value(value)
            .defaultValue(value)
            .type(type)
            .category(category)
            .description(description)
            .readOnly(false)
            .required(false)
            .sensitive(false)
            .build();
        
        globalDefaults.put(key, config);
    }
    
    /**
     * Validate configuration key
     */
    private void validateConfigKey(String key) {
        if (key == null || key.trim().isEmpty()) {
            throw new IllegalArgumentException("Configuration key cannot be null or empty");
        }
        
        if (!key.matches("^[a-z0-9][a-z0-9-_.]*[a-z0-9]$")) {
            throw new IllegalArgumentException("Invalid configuration key format: " + key);
        }
    }
    
    /**
     * Validate configuration value
     */
    private void validateConfigValue(Object value, TenantConfig.ConfigType type) {
        if (value == null) {
            return; // Null values are allowed
        }
        
        switch (type) {
            case STRING, SECRET, URL, EMAIL, CRON, REGEX -> {
                if (!(value instanceof String)) {
                    throw new IllegalArgumentException("Value must be a string for type " + type);
                }
            }
            case INTEGER -> {
                if (!(value instanceof Integer) && !(value instanceof Number)) {
                    throw new IllegalArgumentException("Value must be an integer for type " + type);
                }
            }
            case LONG -> {
                if (!(value instanceof Long) && !(value instanceof Number)) {
                    throw new IllegalArgumentException("Value must be a long for type " + type);
                }
            }
            case DOUBLE -> {
                if (!(value instanceof Double) && !(value instanceof Number)) {
                    throw new IllegalArgumentException("Value must be a double for type " + type);
                }
            }
            case BOOLEAN -> {
                if (!(value instanceof Boolean)) {
                    throw new IllegalArgumentException("Value must be a boolean for type " + type);
                }
            }
            // JSON, ARRAY, OBJECT types can accept any value
        }
    }
}
