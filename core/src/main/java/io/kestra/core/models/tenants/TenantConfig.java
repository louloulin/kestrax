package io.kestra.core.models.tenants;

import io.kestra.core.models.TenantInterface;
import io.micronaut.core.annotation.Introspected;
import lombok.Builder;
import lombok.Value;
import lombok.With;
import lombok.extern.jackson.Jacksonized;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.Map;

/**
 * Tenant configuration entity for multi-tenant settings management
 */
@Value
@Builder
@Jacksonized
@Introspected
@With
public class TenantConfig implements TenantInterface {

    @NotBlank
    @Pattern(regexp = "^[a-z0-9][a-z0-9-_.]*[a-z0-9]$", message = "Config key must be lowercase alphanumeric with hyphens, dots, and underscores")
    @Size(min = 2, max = 255, message = "Config key must be between 2 and 255 characters")
    String key;

    @NotBlank
    String tenantId;

    /**
     * Configuration value (can be any JSON-serializable type)
     */
    Object value;

    /**
     * Configuration type for validation and UI rendering
     */
    @NotNull
    ConfigType type;

    /**
     * Configuration category for organization
     */
    @NotNull
    ConfigCategory category;

    /**
     * Human-readable description
     */
    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    String description;

    /**
     * Default value for this configuration
     */
    Object defaultValue;

    /**
     * Whether this configuration is required
     */
    @Builder.Default
    Boolean required = false;

    /**
     * Whether this configuration is sensitive (passwords, tokens, etc.)
     */
    @Builder.Default
    Boolean sensitive = false;

    /**
     * Whether this configuration is read-only
     */
    @Builder.Default
    Boolean readOnly = false;

    /**
     * Validation rules for the configuration value
     */
    Map<String, Object> validation;

    /**
     * UI hints for rendering this configuration
     */
    Map<String, Object> uiHints;

    /**
     * Configuration creation timestamp
     */
    @NotNull
    @Builder.Default
    Instant createdAt = Instant.now();

    /**
     * Last update timestamp
     */
    @Builder.Default
    Instant updatedAt = Instant.now();

    /**
     * User who created this configuration
     */
    String createdBy;

    /**
     * User who last updated this configuration
     */
    String updatedBy;

    /**
     * Version for optimistic locking
     */
    @Builder.Default
    Long version = 1L;

    /**
     * Configuration type enumeration
     */
    public enum ConfigType {
        STRING,
        INTEGER,
        LONG,
        DOUBLE,
        BOOLEAN,
        JSON,
        ARRAY,
        OBJECT,
        SECRET,
        URL,
        EMAIL,
        DURATION,
        CRON,
        REGEX
    }

    /**
     * Configuration category enumeration
     */
    public enum ConfigCategory {
        GENERAL("General Settings"),
        SECURITY("Security & Authentication"),
        STORAGE("Storage & Database"),
        NOTIFICATION("Notifications"),
        INTEGRATION("External Integrations"),
        PERFORMANCE("Performance & Limits"),
        UI("User Interface"),
        WORKFLOW("Workflow Settings"),
        MONITORING("Monitoring & Logging"),
        CUSTOM("Custom Settings");

        private final String displayName;

        ConfigCategory(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    @Override
    public String getTenantId() {
        return tenantId;
    }

    /**
     * Get the configuration value as a specific type
     */
    @SuppressWarnings("unchecked")
    public <T> T getValue(Class<T> clazz) {
        if (value == null) {
            return null;
        }

        try {
            if (clazz.isInstance(value)) {
                return (T) value;
            }

            // Handle type conversions
            if (clazz == String.class) {
                return (T) value.toString();
            } else if (clazz == Integer.class && value instanceof Number) {
                return (T) Integer.valueOf(((Number) value).intValue());
            } else if (clazz == Long.class && value instanceof Number) {
                return (T) Long.valueOf(((Number) value).longValue());
            } else if (clazz == Double.class && value instanceof Number) {
                return (T) Double.valueOf(((Number) value).doubleValue());
            } else if (clazz == Boolean.class) {
                if (value instanceof Boolean) {
                    return (T) value;
                } else if (value instanceof String) {
                    return (T) Boolean.valueOf((String) value);
                }
            }

            // If no conversion is possible, throw exception
            throw new IllegalArgumentException("Cannot convert value of type " + value.getClass().getSimpleName() + " to " + clazz.getSimpleName());
        } catch (Exception e) {
            throw new IllegalArgumentException("Cannot convert value to " + clazz.getSimpleName(), e);
        }
    }

    /**
     * Get the configuration value as string
     */
    public String getStringValue() {
        return getValue(String.class);
    }

    /**
     * Get the configuration value as integer
     */
    public Integer getIntegerValue() {
        return getValue(Integer.class);
    }

    /**
     * Get the configuration value as long
     */
    public Long getLongValue() {
        return getValue(Long.class);
    }

    /**
     * Get the configuration value as double
     */
    public Double getDoubleValue() {
        return getValue(Double.class);
    }

    /**
     * Get the configuration value as boolean
     */
    public Boolean getBooleanValue() {
        return getValue(Boolean.class);
    }

    /**
     * Check if this configuration has a value
     */
    public boolean hasValue() {
        return value != null;
    }

    /**
     * Check if this configuration uses the default value
     */
    public boolean isUsingDefault() {
        return value == null && defaultValue != null;
    }

    /**
     * Get the effective value (value or default)
     */
    public Object getEffectiveValue() {
        return hasValue() ? value : defaultValue;
    }

    /**
     * Get a validation rule
     */
    @SuppressWarnings("unchecked")
    public <T> T getValidationRule(String rule, T defaultValue) {
        if (validation == null || !validation.containsKey(rule)) {
            return defaultValue;
        }
        try {
            return (T) validation.get(rule);
        } catch (ClassCastException e) {
            return defaultValue;
        }
    }

    /**
     * Get a UI hint
     */
    @SuppressWarnings("unchecked")
    public <T> T getUiHint(String hint, T defaultValue) {
        if (uiHints == null || !uiHints.containsKey(hint)) {
            return defaultValue;
        }
        try {
            return (T) uiHints.get(hint);
        } catch (ClassCastException e) {
            return defaultValue;
        }
    }

    /**
     * Check if this configuration is editable
     */
    public boolean isEditable() {
        return !Boolean.TRUE.equals(readOnly);
    }

    /**
     * Check if this configuration should be masked in UI
     */
    public boolean shouldMask() {
        return Boolean.TRUE.equals(sensitive);
    }

    /**
     * Update configuration with new timestamp and version
     */
    public TenantConfig withUpdate(String updatedBy) {
        return this.withUpdatedAt(Instant.now())
                   .withUpdatedBy(updatedBy)
                   .withVersion(version + 1);
    }

    /**
     * Create a configuration with string value
     */
    public static TenantConfig createString(String tenantId, String key, String value, ConfigCategory category) {
        return TenantConfig.builder()
            .tenantId(tenantId)
            .key(key)
            .value(value)
            .type(ConfigType.STRING)
            .category(category)
            .build();
    }

    /**
     * Create a configuration with boolean value
     */
    public static TenantConfig createBoolean(String tenantId, String key, Boolean value, ConfigCategory category) {
        return TenantConfig.builder()
            .tenantId(tenantId)
            .key(key)
            .value(value)
            .type(ConfigType.BOOLEAN)
            .category(category)
            .build();
    }

    /**
     * Create a configuration with integer value
     */
    public static TenantConfig createInteger(String tenantId, String key, Integer value, ConfigCategory category) {
        return TenantConfig.builder()
            .tenantId(tenantId)
            .key(key)
            .value(value)
            .type(ConfigType.INTEGER)
            .category(category)
            .build();
    }

    /**
     * Create a secret configuration
     */
    public static TenantConfig createSecret(String tenantId, String key, String value, ConfigCategory category) {
        return TenantConfig.builder()
            .tenantId(tenantId)
            .key(key)
            .value(value)
            .type(ConfigType.SECRET)
            .category(category)
            .sensitive(true)
            .build();
    }
}
