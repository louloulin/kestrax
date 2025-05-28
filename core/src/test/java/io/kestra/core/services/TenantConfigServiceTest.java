package io.kestra.core.services;

import io.kestra.core.models.tenants.TenantConfig;
import io.kestra.core.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class TenantConfigServiceTest {

    private TenantConfigService tenantConfigService;

    @BeforeEach
    void setUp() {
        tenantConfigService = new TenantConfigService();
        TenantContext.clear();
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void testSetAndGetConfig() {
        TenantContext.setTenant("test-tenant");

        TenantConfig config = tenantConfigService.setConfig(
            "app.timeout",
            30,
            TenantConfig.ConfigType.INTEGER,
            TenantConfig.ConfigCategory.PERFORMANCE,
            "Application timeout in seconds"
        );

        assertNotNull(config);
        assertEquals("test-tenant", config.getTenantId());
        assertEquals("app.timeout", config.getKey());
        assertEquals(30, config.getValue());
        assertEquals(TenantConfig.ConfigType.INTEGER, config.getType());
        assertEquals(TenantConfig.ConfigCategory.PERFORMANCE, config.getCategory());

        Optional<TenantConfig> retrieved = tenantConfigService.getConfig("app.timeout");
        assertTrue(retrieved.isPresent());
        assertEquals(config.getKey(), retrieved.get().getKey());
        assertEquals(config.getValue(), retrieved.get().getValue());
    }

    @Test
    void testSetConfigWithSpecificTenant() {
        TenantContext.setTenant("tenant1");

        TenantConfig config = tenantConfigService.setConfig(
            "tenant2",
            "app.name",
            "MyApp",
            TenantConfig.ConfigType.STRING,
            TenantConfig.ConfigCategory.GENERAL,
            "Application name",
            "admin"
        );

        assertEquals("tenant2", config.getTenantId());
        assertEquals("admin", config.getCreatedBy());
        assertEquals("admin", config.getUpdatedBy());
    }

    @Test
    void testGetConfigValue() {
        TenantContext.setTenant("test-tenant");

        tenantConfigService.setConfig(
            "app.enabled",
            true,
            TenantConfig.ConfigType.BOOLEAN,
            TenantConfig.ConfigCategory.GENERAL,
            null
        );

        Optional<Boolean> value = tenantConfigService.getConfigValue("app.enabled", Boolean.class);
        assertTrue(value.isPresent());
        assertTrue(value.get());

        Boolean valueWithDefault = tenantConfigService.getConfigValue("app.enabled", Boolean.class, false);
        assertTrue(valueWithDefault);

        Boolean missingWithDefault = tenantConfigService.getConfigValue("missing.key", Boolean.class, false);
        assertFalse(missingWithDefault);
    }

    @Test
    void testGetAllConfigs() {
        TenantContext.setTenant("test-tenant");

        tenantConfigService.setConfig(
            "config1",
            "value1",
            TenantConfig.ConfigType.STRING,
            TenantConfig.ConfigCategory.GENERAL,
            null
        );

        tenantConfigService.setConfig(
            "config2",
            42,
            TenantConfig.ConfigType.INTEGER,
            TenantConfig.ConfigCategory.PERFORMANCE,
            null
        );

        List<TenantConfig> configs = tenantConfigService.getAllConfigs();
        
        // Should include tenant configs + global defaults
        assertTrue(configs.size() >= 2);
        
        // Check that our configs are present
        assertTrue(configs.stream().anyMatch(c -> c.getKey().equals("config1")));
        assertTrue(configs.stream().anyMatch(c -> c.getKey().equals("config2")));
        
        // Check that global defaults are present
        assertTrue(configs.stream().anyMatch(c -> c.getKey().equals("app.name")));
    }

    @Test
    void testGetConfigsByCategory() {
        TenantContext.setTenant("test-tenant");

        tenantConfigService.setConfig(
            "perf.timeout",
            30,
            TenantConfig.ConfigType.INTEGER,
            TenantConfig.ConfigCategory.PERFORMANCE,
            null
        );

        tenantConfigService.setConfig(
            "ui.theme",
            "dark",
            TenantConfig.ConfigType.STRING,
            TenantConfig.ConfigCategory.UI,
            null
        );

        List<TenantConfig> perfConfigs = tenantConfigService.getConfigsByCategory(TenantConfig.ConfigCategory.PERFORMANCE);
        List<TenantConfig> uiConfigs = tenantConfigService.getConfigsByCategory(TenantConfig.ConfigCategory.UI);

        assertTrue(perfConfigs.stream().anyMatch(c -> c.getKey().equals("perf.timeout")));
        assertTrue(uiConfigs.stream().anyMatch(c -> c.getKey().equals("ui.theme")));
        
        // Should not cross-contaminate
        assertFalse(perfConfigs.stream().anyMatch(c -> c.getKey().equals("ui.theme")));
        assertFalse(uiConfigs.stream().anyMatch(c -> c.getKey().equals("perf.timeout")));
    }

    @Test
    void testDeleteConfig() {
        TenantContext.setTenant("test-tenant");

        tenantConfigService.setConfig(
            "deletable.config",
            "value",
            TenantConfig.ConfigType.STRING,
            TenantConfig.ConfigCategory.GENERAL,
            null
        );

        assertTrue(tenantConfigService.hasConfig("deletable.config"));

        boolean deleted = tenantConfigService.deleteConfig("deletable.config");
        assertTrue(deleted);
        assertFalse(tenantConfigService.hasConfig("deletable.config"));

        // Try to delete non-existent config
        boolean notDeleted = tenantConfigService.deleteConfig("non.existent");
        assertFalse(notDeleted);
    }

    @Test
    void testSearchConfigs() {
        TenantContext.setTenant("test-tenant");

        tenantConfigService.setConfig(
            "app.timeout",
            30,
            TenantConfig.ConfigType.INTEGER,
            TenantConfig.ConfigCategory.PERFORMANCE,
            null
        );

        tenantConfigService.setConfig(
            "app.retries",
            3,
            TenantConfig.ConfigType.INTEGER,
            TenantConfig.ConfigCategory.PERFORMANCE,
            null
        );

        tenantConfigService.setConfig(
            "ui.theme",
            "dark",
            TenantConfig.ConfigType.STRING,
            TenantConfig.ConfigCategory.UI,
            null
        );

        List<TenantConfig> appConfigs = tenantConfigService.searchConfigs("app");
        assertTrue(appConfigs.stream().anyMatch(c -> c.getKey().equals("app.timeout")));
        assertTrue(appConfigs.stream().anyMatch(c -> c.getKey().equals("app.retries")));
        assertFalse(appConfigs.stream().anyMatch(c -> c.getKey().equals("ui.theme")));
    }

    @Test
    void testBulkUpdateConfigs() {
        TenantContext.setTenant("test-tenant");

        // Create initial configs
        tenantConfigService.setConfig(
            "config1",
            "value1",
            TenantConfig.ConfigType.STRING,
            TenantConfig.ConfigCategory.GENERAL,
            null
        );

        tenantConfigService.setConfig(
            "config2",
            10,
            TenantConfig.ConfigType.INTEGER,
            TenantConfig.ConfigCategory.GENERAL,
            null
        );

        Map<String, Object> updates = Map.of(
            "config1", "updated-value1",
            "config2", 20
        );

        List<TenantConfig> updated = tenantConfigService.bulkUpdateConfigs(updates, "admin");

        assertEquals(2, updated.size());
        
        Optional<TenantConfig> config1 = tenantConfigService.getConfig("config1");
        Optional<TenantConfig> config2 = tenantConfigService.getConfig("config2");
        
        assertTrue(config1.isPresent());
        assertTrue(config2.isPresent());
        assertEquals("updated-value1", config1.get().getValue());
        assertEquals(20, config2.get().getValue());
        assertEquals("admin", config1.get().getUpdatedBy());
        assertEquals("admin", config2.get().getUpdatedBy());
    }

    @Test
    void testResetToDefaults() {
        TenantContext.setTenant("test-tenant");

        // Add some custom configs
        tenantConfigService.setConfig(
            "custom.config",
            "custom-value",
            TenantConfig.ConfigType.STRING,
            TenantConfig.ConfigCategory.GENERAL,
            null
        );

        assertTrue(tenantConfigService.hasConfig("custom.config"));

        tenantConfigService.resetToDefaults();

        // Custom config should be gone, but defaults should remain
        assertFalse(tenantConfigService.hasConfig("custom.config"));
        assertTrue(tenantConfigService.hasConfig("app.name")); // global default
    }

    @Test
    void testGetConfigCategoryCounts() {
        TenantContext.setTenant("test-tenant");

        tenantConfigService.setConfig(
            "perf1",
            1,
            TenantConfig.ConfigType.INTEGER,
            TenantConfig.ConfigCategory.PERFORMANCE,
            null
        );

        tenantConfigService.setConfig(
            "perf2",
            2,
            TenantConfig.ConfigType.INTEGER,
            TenantConfig.ConfigCategory.PERFORMANCE,
            null
        );

        tenantConfigService.setConfig(
            "ui1",
            "value",
            TenantConfig.ConfigType.STRING,
            TenantConfig.ConfigCategory.UI,
            null
        );

        Map<TenantConfig.ConfigCategory, Long> counts = tenantConfigService.getConfigCategoryCounts();

        assertTrue(counts.containsKey(TenantConfig.ConfigCategory.PERFORMANCE));
        assertTrue(counts.containsKey(TenantConfig.ConfigCategory.UI));
        assertTrue(counts.containsKey(TenantConfig.ConfigCategory.GENERAL)); // from defaults
        
        // Should have at least our custom configs
        assertTrue(counts.get(TenantConfig.ConfigCategory.PERFORMANCE) >= 2);
        assertTrue(counts.get(TenantConfig.ConfigCategory.UI) >= 1);
    }

    @Test
    void testValidation() {
        TenantContext.setTenant("test-tenant");

        // Test invalid key
        assertThrows(IllegalArgumentException.class, () -> {
            tenantConfigService.setConfig(
                "",
                "value",
                TenantConfig.ConfigType.STRING,
                TenantConfig.ConfigCategory.GENERAL,
                null
            );
        });

        assertThrows(IllegalArgumentException.class, () -> {
            tenantConfigService.setConfig(
                "Invalid Key!",
                "value",
                TenantConfig.ConfigType.STRING,
                TenantConfig.ConfigCategory.GENERAL,
                null
            );
        });

        // Test invalid value for type
        assertThrows(IllegalArgumentException.class, () -> {
            tenantConfigService.setConfig(
                "valid.key",
                "not-a-number",
                TenantConfig.ConfigType.INTEGER,
                TenantConfig.ConfigCategory.GENERAL,
                null
            );
        });

        assertThrows(IllegalArgumentException.class, () -> {
            tenantConfigService.setConfig(
                "valid.key",
                "not-a-boolean",
                TenantConfig.ConfigType.BOOLEAN,
                TenantConfig.ConfigCategory.GENERAL,
                null
            );
        });
    }

    @Test
    void testUpdateExistingConfig() {
        TenantContext.setTenant("test-tenant");

        // Create initial config
        TenantConfig initial = tenantConfigService.setConfig(
            "update.test",
            "initial-value",
            TenantConfig.ConfigType.STRING,
            TenantConfig.ConfigCategory.GENERAL,
            "Initial description"
        );

        assertEquals(1L, initial.getVersion());

        // Update the config
        TenantConfig updated = tenantConfigService.setConfig(
            "update.test",
            "updated-value",
            TenantConfig.ConfigType.STRING,
            TenantConfig.ConfigCategory.GENERAL,
            "Updated description"
        );

        assertEquals(2L, updated.getVersion());
        assertEquals("updated-value", updated.getValue());
        assertEquals("Updated description", updated.getDescription());
        assertTrue(updated.getUpdatedAt().isAfter(initial.getUpdatedAt()));
    }

    @Test
    void testTenantIsolation() {
        // Set config for tenant1
        TenantContext.setTenant("tenant1");
        tenantConfigService.setConfig(
            "shared.key",
            "tenant1-value",
            TenantConfig.ConfigType.STRING,
            TenantConfig.ConfigCategory.GENERAL,
            null
        );

        // Set config for tenant2
        TenantContext.setTenant("tenant2");
        tenantConfigService.setConfig(
            "shared.key",
            "tenant2-value",
            TenantConfig.ConfigType.STRING,
            TenantConfig.ConfigCategory.GENERAL,
            null
        );

        // Verify isolation
        TenantContext.setTenant("tenant1");
        Optional<TenantConfig> tenant1Config = tenantConfigService.getConfig("shared.key");
        assertTrue(tenant1Config.isPresent());
        assertEquals("tenant1-value", tenant1Config.get().getValue());

        TenantContext.setTenant("tenant2");
        Optional<TenantConfig> tenant2Config = tenantConfigService.getConfig("shared.key");
        assertTrue(tenant2Config.isPresent());
        assertEquals("tenant2-value", tenant2Config.get().getValue());
    }

    @Test
    void testRequiresTenantContext() {
        // Operations should fail without tenant context
        assertThrows(IllegalStateException.class, () -> {
            tenantConfigService.setConfig(
                "test.key",
                "value",
                TenantConfig.ConfigType.STRING,
                TenantConfig.ConfigCategory.GENERAL,
                null
            );
        });

        assertThrows(IllegalStateException.class, () -> {
            tenantConfigService.getConfig("test.key");
        });

        assertThrows(IllegalStateException.class, () -> {
            tenantConfigService.getAllConfigs();
        });
    }
}
