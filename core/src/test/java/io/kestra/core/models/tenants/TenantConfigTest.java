package io.kestra.core.models.tenants;

import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class TenantConfigTest {

    @Test
    void testTenantConfigCreation() {
        TenantConfig config = TenantConfig.builder()
            .tenantId("test-tenant")
            .key("app.timeout")
            .value(30)
            .type(TenantConfig.ConfigType.INTEGER)
            .category(TenantConfig.ConfigCategory.PERFORMANCE)
            .description("Application timeout in seconds")
            .required(true)
            .sensitive(false)
            .readOnly(false)
            .validation(Map.of("min", 1, "max", 3600))
            .uiHints(Map.of("step", 1, "unit", "seconds"))
            .build();

        assertEquals("test-tenant", config.getTenantId());
        assertEquals("app.timeout", config.getKey());
        assertEquals(30, config.getValue());
        assertEquals(TenantConfig.ConfigType.INTEGER, config.getType());
        assertEquals(TenantConfig.ConfigCategory.PERFORMANCE, config.getCategory());
        assertEquals("Application timeout in seconds", config.getDescription());
        assertTrue(config.getRequired());
        assertFalse(config.getSensitive());
        assertFalse(config.getReadOnly());
        assertNotNull(config.getCreatedAt());
        assertNotNull(config.getUpdatedAt());
        assertEquals(1L, config.getVersion());
    }

    @Test
    void testTenantInterface() {
        TenantConfig config = TenantConfig.builder()
            .tenantId("tenant-123")
            .key("test.key")
            .value("test-value")
            .type(TenantConfig.ConfigType.STRING)
            .category(TenantConfig.ConfigCategory.GENERAL)
            .build();

        assertEquals("tenant-123", config.getTenantId());
    }

    @Test
    void testValueTypeConversion() {
        TenantConfig config = TenantConfig.builder()
            .tenantId("test-tenant")
            .key("test.key")
            .value(42)
            .type(TenantConfig.ConfigType.INTEGER)
            .category(TenantConfig.ConfigCategory.GENERAL)
            .build();

        // Test type conversions
        assertEquals(Integer.valueOf(42), config.getValue(Integer.class));
        assertEquals(Long.valueOf(42L), config.getValue(Long.class));
        assertEquals(Double.valueOf(42.0), config.getValue(Double.class));
        assertEquals("42", config.getValue(String.class));
    }

    @Test
    void testStringValueConversion() {
        TenantConfig config = TenantConfig.builder()
            .tenantId("test-tenant")
            .key("test.string")
            .value("hello world")
            .type(TenantConfig.ConfigType.STRING)
            .category(TenantConfig.ConfigCategory.GENERAL)
            .build();

        assertEquals("hello world", config.getStringValue());
        assertEquals("hello world", config.getValue(String.class));
    }

    @Test
    void testBooleanValueConversion() {
        // Boolean value
        TenantConfig boolConfig = TenantConfig.builder()
            .tenantId("test-tenant")
            .key("test.bool")
            .value(true)
            .type(TenantConfig.ConfigType.BOOLEAN)
            .category(TenantConfig.ConfigCategory.GENERAL)
            .build();

        assertTrue(boolConfig.getBooleanValue());
        assertTrue(boolConfig.getValue(Boolean.class));

        // String boolean value
        TenantConfig stringBoolConfig = TenantConfig.builder()
            .tenantId("test-tenant")
            .key("test.string.bool")
            .value("false")
            .type(TenantConfig.ConfigType.BOOLEAN)
            .category(TenantConfig.ConfigCategory.GENERAL)
            .build();

        assertFalse(stringBoolConfig.getValue(Boolean.class));
    }

    @Test
    void testNumericValueConversions() {
        TenantConfig config = TenantConfig.builder()
            .tenantId("test-tenant")
            .key("test.number")
            .value(123.45)
            .type(TenantConfig.ConfigType.DOUBLE)
            .category(TenantConfig.ConfigCategory.GENERAL)
            .build();

        assertEquals(Integer.valueOf(123), config.getIntegerValue());
        assertEquals(Long.valueOf(123L), config.getLongValue());
        assertEquals(Double.valueOf(123.45), config.getDoubleValue());
    }

    @Test
    void testInvalidTypeConversion() {
        TenantConfig config = TenantConfig.builder()
            .tenantId("test-tenant")
            .key("test.key")
            .value("not-a-number")
            .type(TenantConfig.ConfigType.STRING)
            .category(TenantConfig.ConfigCategory.GENERAL)
            .build();

        assertThrows(IllegalArgumentException.class, () -> {
            config.getValue(Integer.class);
        });
    }

    @Test
    void testNullValue() {
        TenantConfig config = TenantConfig.builder()
            .tenantId("test-tenant")
            .key("test.null")
            .value(null)
            .type(TenantConfig.ConfigType.STRING)
            .category(TenantConfig.ConfigCategory.GENERAL)
            .build();

        assertNull(config.getValue(String.class));
        assertNull(config.getStringValue());
        assertFalse(config.hasValue());
    }

    @Test
    void testDefaultValue() {
        TenantConfig config = TenantConfig.builder()
            .tenantId("test-tenant")
            .key("test.default")
            .value(null)
            .defaultValue("default-value")
            .type(TenantConfig.ConfigType.STRING)
            .category(TenantConfig.ConfigCategory.GENERAL)
            .build();

        assertFalse(config.hasValue());
        assertTrue(config.isUsingDefault());
        assertEquals("default-value", config.getEffectiveValue());
    }

    @Test
    void testValidationRules() {
        TenantConfig config = TenantConfig.builder()
            .tenantId("test-tenant")
            .key("test.validation")
            .value(50)
            .type(TenantConfig.ConfigType.INTEGER)
            .category(TenantConfig.ConfigCategory.GENERAL)
            .validation(Map.of("min", 10, "max", 100, "step", 5))
            .build();

        assertEquals(Integer.valueOf(10), config.getValidationRule("min", 0));
        assertEquals(Integer.valueOf(100), config.getValidationRule("max", 0));
        assertEquals(Integer.valueOf(5), config.getValidationRule("step", 1));
        assertEquals(Integer.valueOf(1), config.getValidationRule("missing", 1)); // default value
    }

    @Test
    void testUiHints() {
        TenantConfig config = TenantConfig.builder()
            .tenantId("test-tenant")
            .key("test.ui")
            .value("value")
            .type(TenantConfig.ConfigType.STRING)
            .category(TenantConfig.ConfigCategory.UI)
            .uiHints(Map.of("placeholder", "Enter value", "help", "This is help text"))
            .build();

        assertEquals("Enter value", config.getUiHint("placeholder", ""));
        assertEquals("This is help text", config.getUiHint("help", ""));
        assertEquals("", config.getUiHint("missing", "")); // default value
    }

    @Test
    void testConfigFlags() {
        TenantConfig editableConfig = TenantConfig.builder()
            .tenantId("test-tenant")
            .key("editable.config")
            .value("value")
            .type(TenantConfig.ConfigType.STRING)
            .category(TenantConfig.ConfigCategory.GENERAL)
            .readOnly(false)
            .sensitive(false)
            .build();

        assertTrue(editableConfig.isEditable());
        assertFalse(editableConfig.shouldMask());

        TenantConfig readOnlyConfig = editableConfig.withReadOnly(true);
        assertFalse(readOnlyConfig.isEditable());

        TenantConfig sensitiveConfig = editableConfig.withSensitive(true);
        assertTrue(sensitiveConfig.shouldMask());
    }

    @Test
    void testConfigUpdate() {
        TenantConfig original = TenantConfig.builder()
            .tenantId("test-tenant")
            .key("test.update")
            .value("original")
            .type(TenantConfig.ConfigType.STRING)
            .category(TenantConfig.ConfigCategory.GENERAL)
            .version(1L)
            .build();

        TenantConfig updated = original.withUpdate("user123");

        assertEquals(2L, updated.getVersion());
        assertEquals("user123", updated.getUpdatedBy());
        assertTrue(updated.getUpdatedAt().isAfter(original.getUpdatedAt()));
    }

    @Test
    void testFactoryMethods() {
        // Test string factory
        TenantConfig stringConfig = TenantConfig.createString("tenant1", "app.name", "MyApp", TenantConfig.ConfigCategory.GENERAL);
        assertEquals("tenant1", stringConfig.getTenantId());
        assertEquals("app.name", stringConfig.getKey());
        assertEquals("MyApp", stringConfig.getValue());
        assertEquals(TenantConfig.ConfigType.STRING, stringConfig.getType());
        assertEquals(TenantConfig.ConfigCategory.GENERAL, stringConfig.getCategory());

        // Test boolean factory
        TenantConfig boolConfig = TenantConfig.createBoolean("tenant1", "app.enabled", true, TenantConfig.ConfigCategory.GENERAL);
        assertEquals(true, boolConfig.getValue());
        assertEquals(TenantConfig.ConfigType.BOOLEAN, boolConfig.getType());

        // Test integer factory
        TenantConfig intConfig = TenantConfig.createInteger("tenant1", "app.port", 8080, TenantConfig.ConfigCategory.GENERAL);
        assertEquals(8080, intConfig.getValue());
        assertEquals(TenantConfig.ConfigType.INTEGER, intConfig.getType());

        // Test secret factory
        TenantConfig secretConfig = TenantConfig.createSecret("tenant1", "app.password", "secret123", TenantConfig.ConfigCategory.SECURITY);
        assertEquals("secret123", secretConfig.getValue());
        assertEquals(TenantConfig.ConfigType.SECRET, secretConfig.getType());
        assertTrue(secretConfig.getSensitive());
    }

    @Test
    void testConfigCategories() {
        for (TenantConfig.ConfigCategory category : TenantConfig.ConfigCategory.values()) {
            assertNotNull(category.getDisplayName());
            assertFalse(category.getDisplayName().isEmpty());
        }

        assertEquals("General Settings", TenantConfig.ConfigCategory.GENERAL.getDisplayName());
        assertEquals("Security & Authentication", TenantConfig.ConfigCategory.SECURITY.getDisplayName());
        assertEquals("User Interface", TenantConfig.ConfigCategory.UI.getDisplayName());
    }

    @Test
    void testConfigTypes() {
        // Test all config types exist
        TenantConfig.ConfigType[] types = TenantConfig.ConfigType.values();
        assertTrue(types.length > 0);

        // Test specific types
        assertNotNull(TenantConfig.ConfigType.STRING);
        assertNotNull(TenantConfig.ConfigType.INTEGER);
        assertNotNull(TenantConfig.ConfigType.BOOLEAN);
        assertNotNull(TenantConfig.ConfigType.SECRET);
        assertNotNull(TenantConfig.ConfigType.JSON);
    }

    @Test
    void testImmutability() {
        TenantConfig original = TenantConfig.builder()
            .tenantId("test-tenant")
            .key("test.immutable")
            .value("original")
            .type(TenantConfig.ConfigType.STRING)
            .category(TenantConfig.ConfigCategory.GENERAL)
            .version(1L)
            .build();

        TenantConfig modified = original
            .withValue("modified")
            .withDescription("Modified description")
            .withVersion(2L);

        // Original should be unchanged
        assertEquals("original", original.getValue());
        assertNull(original.getDescription());
        assertEquals(1L, original.getVersion());

        // Modified should have changes
        assertEquals("modified", modified.getValue());
        assertEquals("Modified description", modified.getDescription());
        assertEquals(2L, modified.getVersion());
    }
}
