package io.kestra.core.security;

import io.kestra.core.models.rbac.Permission;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

class PermissionServiceTest {

    private PermissionService permissionService;

    @BeforeEach
    void setUp() {
        permissionService = new PermissionService();
    }

    @Test
    void testValidatePermissionCheck() {
        // Valid parameters should not throw
        assertDoesNotThrow(() -> 
            permissionService.validatePermissionCheck("user1", "tenant1", Permission.FLOW_READ)
        );

        // Null user ID should throw
        assertThrows(IllegalArgumentException.class, () ->
            permissionService.validatePermissionCheck(null, "tenant1", Permission.FLOW_READ)
        );

        // Empty user ID should throw
        assertThrows(IllegalArgumentException.class, () ->
            permissionService.validatePermissionCheck("", "tenant1", Permission.FLOW_READ)
        );

        // Null tenant ID should throw
        assertThrows(IllegalArgumentException.class, () ->
            permissionService.validatePermissionCheck("user1", null, Permission.FLOW_READ)
        );

        // Empty tenant ID should throw
        assertThrows(IllegalArgumentException.class, () ->
            permissionService.validatePermissionCheck("user1", "", Permission.FLOW_READ)
        );

        // Null permission should throw
        assertThrows(IllegalArgumentException.class, () ->
            permissionService.validatePermissionCheck("user1", "tenant1", null)
        );
    }

    @Test
    void testCreatePermissionDeniedException() {
        SecurityException exception = permissionService.createPermissionDeniedException(
            "user1", Permission.FLOW_READ, "test-namespace"
        );

        assertNotNull(exception);
        assertTrue(exception.getMessage().contains("user1"));
        assertTrue(exception.getMessage().contains(Permission.FLOW_READ.getCode()));
        assertTrue(exception.getMessage().contains("test-namespace"));
    }

    @Test
    void testCreatePermissionDeniedExceptionWithoutNamespace() {
        SecurityException exception = permissionService.createPermissionDeniedException(
            "user1", Permission.FLOW_READ, null
        );

        assertNotNull(exception);
        assertTrue(exception.getMessage().contains("user1"));
        assertTrue(exception.getMessage().contains(Permission.FLOW_READ.getCode()));
        assertFalse(exception.getMessage().contains("namespace"));
    }

    @Test
    void testHasPermissionWithNullParameters() {
        // Null user ID should return false
        assertFalse(permissionService.hasPermission(null, "tenant1", Permission.FLOW_READ, "namespace"));

        // Null tenant ID should return false
        assertFalse(permissionService.hasPermission("user1", null, Permission.FLOW_READ, "namespace"));

        // Null permission should return false
        assertFalse(permissionService.hasPermission("user1", "tenant1", null, "namespace"));
    }

    @Test
    void testHasAnyPermissionWithEmptySet() {
        // Empty permission set should return true
        assertTrue(permissionService.hasAnyPermission("user1", "tenant1", Set.of(), "namespace"));

        // Null permission set should return true
        assertTrue(permissionService.hasAnyPermission("user1", "tenant1", null, "namespace"));
    }

    @Test
    void testHasAllPermissionsWithEmptySet() {
        // Empty permission set should return true
        assertTrue(permissionService.hasAllPermissions("user1", "tenant1", Set.of(), "namespace"));

        // Null permission set should return true
        assertTrue(permissionService.hasAllPermissions("user1", "tenant1", null, "namespace"));
    }

    @Test
    void testGetUserEffectivePermissions() {
        // This test verifies the method doesn't throw and returns a non-null set
        // In a real implementation, this would be mocked or use test data
        Set<Permission> permissions = permissionService.getUserEffectivePermissions("user1", "tenant1", "namespace");
        
        assertNotNull(permissions);
        // Since we don't have actual data sources, the set should be empty
        assertTrue(permissions.isEmpty());
    }

    @Test
    void testIsSystemAdmin() {
        // This test verifies the method doesn't throw
        // In a real implementation, this would be mocked or use test data
        boolean isAdmin = permissionService.isSystemAdmin("user1", "tenant1");
        
        // Since we don't have actual data sources, should return false
        assertFalse(isAdmin);
    }

    @Test
    void testHasPermissionWithValidParameters() {
        // This test verifies the method doesn't throw with valid parameters
        // In a real implementation, this would be mocked or use test data
        boolean hasPermission = permissionService.hasPermission("user1", "tenant1", Permission.FLOW_READ, "namespace");
        
        // Since we don't have actual data sources, should return false
        assertFalse(hasPermission);
    }

    @Test
    void testHasAnyPermissionWithValidParameters() {
        Set<Permission> permissions = Set.of(Permission.FLOW_READ, Permission.FLOW_UPDATE);
        
        // This test verifies the method doesn't throw with valid parameters
        boolean hasAny = permissionService.hasAnyPermission("user1", "tenant1", permissions, "namespace");
        
        // Since we don't have actual data sources, should return false
        assertFalse(hasAny);
    }

    @Test
    void testHasAllPermissionsWithValidParameters() {
        Set<Permission> permissions = Set.of(Permission.FLOW_READ, Permission.FLOW_UPDATE);
        
        // This test verifies the method doesn't throw with valid parameters
        boolean hasAll = permissionService.hasAllPermissions("user1", "tenant1", permissions, "namespace");
        
        // Since we don't have actual data sources, should return false
        assertFalse(hasAll);
    }
}
