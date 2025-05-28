package io.kestra.core.models.rbac;

import org.junit.jupiter.api.Test;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

class RoleTest {

    @Test
    void testRoleCreation() {
        Role role = Role.builder()
            .id("test-role")
            .tenantId("tenant1")
            .name("Test Role")
            .description("A test role")
            .permissions(Set.of(Permission.FLOW_READ, Permission.FLOW_CREATE))
            .build();

        assertEquals("test-role", role.getId());
        assertEquals("tenant1", role.getTenantId());
        assertEquals("Test Role", role.getName());
        assertEquals("A test role", role.getDescription());
        assertEquals(2, role.getPermissions().size());
        assertTrue(role.getPermissions().contains(Permission.FLOW_READ));
        assertTrue(role.getPermissions().contains(Permission.FLOW_CREATE));
        assertFalse(role.isSystemRole());
    }

    @Test
    void testHasPermission() {
        Role role = Role.builder()
            .id("test-role")
            .tenantId("tenant1")
            .name("Test Role")
            .permissions(Set.of(Permission.FLOW_UPDATE, Permission.USER_READ))
            .build();

        // Direct permissions
        assertTrue(role.hasPermission(Permission.FLOW_UPDATE));
        assertTrue(role.hasPermission(Permission.USER_READ));

        // Implied permissions (UPDATE implies READ)
        assertTrue(role.hasPermission(Permission.FLOW_READ));

        // Non-existent permissions
        assertFalse(role.hasPermission(Permission.FLOW_DELETE));
        assertFalse(role.hasPermission(Permission.USER_CREATE));
    }

    @Test
    void testHasAnyPermission() {
        Role role = Role.builder()
            .id("test-role")
            .tenantId("tenant1")
            .name("Test Role")
            .permissions(Set.of(Permission.FLOW_READ, Permission.USER_READ))
            .build();

        // Has one of the required permissions
        assertTrue(role.hasAnyPermission(Set.of(Permission.FLOW_READ, Permission.EXECUTION_READ)));
        assertTrue(role.hasAnyPermission(Set.of(Permission.USER_READ, Permission.ROLE_READ)));

        // Has none of the required permissions
        assertFalse(role.hasAnyPermission(Set.of(Permission.FLOW_DELETE, Permission.USER_DELETE)));
    }

    @Test
    void testHasAllPermissions() {
        Role role = Role.builder()
            .id("test-role")
            .tenantId("tenant1")
            .name("Test Role")
            .permissions(Set.of(Permission.FLOW_READ, Permission.FLOW_UPDATE, Permission.USER_READ))
            .build();

        // Has all required permissions
        assertTrue(role.hasAllPermissions(Set.of(Permission.FLOW_READ, Permission.USER_READ)));
        assertTrue(role.hasAllPermissions(Set.of(Permission.FLOW_READ))); // Single permission

        // Missing some permissions
        assertFalse(role.hasAllPermissions(Set.of(Permission.FLOW_READ, Permission.EXECUTION_READ)));
        assertFalse(role.hasAllPermissions(Set.of(Permission.FLOW_DELETE, Permission.USER_DELETE)));
    }

    @Test
    void testAddPermission() {
        Role role = Role.builder()
            .id("test-role")
            .tenantId("tenant1")
            .name("Test Role")
            .permissions(Set.of(Permission.FLOW_READ))
            .build();

        Role updatedRole = role.addPermission(Permission.FLOW_CREATE);

        // Original role unchanged
        assertEquals(1, role.getPermissions().size());
        assertTrue(role.getPermissions().contains(Permission.FLOW_READ));

        // Updated role has new permission
        assertEquals(2, updatedRole.getPermissions().size());
        assertTrue(updatedRole.getPermissions().contains(Permission.FLOW_READ));
        assertTrue(updatedRole.getPermissions().contains(Permission.FLOW_CREATE));
        assertNotNull(updatedRole.getUpdatedAt());
    }

    @Test
    void testRemovePermission() {
        Role role = Role.builder()
            .id("test-role")
            .tenantId("tenant1")
            .name("Test Role")
            .permissions(Set.of(Permission.FLOW_READ, Permission.FLOW_CREATE))
            .build();

        Role updatedRole = role.removePermission(Permission.FLOW_CREATE);

        // Original role unchanged
        assertEquals(2, role.getPermissions().size());

        // Updated role has permission removed
        assertEquals(1, updatedRole.getPermissions().size());
        assertTrue(updatedRole.getPermissions().contains(Permission.FLOW_READ));
        assertFalse(updatedRole.getPermissions().contains(Permission.FLOW_CREATE));
        assertNotNull(updatedRole.getUpdatedAt());
    }

    @Test
    void testAddMultiplePermissions() {
        Role role = Role.builder()
            .id("test-role")
            .tenantId("tenant1")
            .name("Test Role")
            .permissions(Set.of(Permission.FLOW_READ))
            .build();

        Set<Permission> newPermissions = Set.of(Permission.FLOW_CREATE, Permission.FLOW_UPDATE);
        Role updatedRole = role.addPermissions(newPermissions);

        assertEquals(3, updatedRole.getPermissions().size());
        assertTrue(updatedRole.getPermissions().contains(Permission.FLOW_READ));
        assertTrue(updatedRole.getPermissions().contains(Permission.FLOW_CREATE));
        assertTrue(updatedRole.getPermissions().contains(Permission.FLOW_UPDATE));
    }

    @Test
    void testGetPermissionCodes() {
        Role role = Role.builder()
            .id("test-role")
            .tenantId("tenant1")
            .name("Test Role")
            .permissions(Set.of(Permission.FLOW_READ, Permission.USER_CREATE))
            .build();

        Set<String> codes = role.getPermissionCodes();
        assertEquals(2, codes.size());
        assertTrue(codes.contains("flow:read"));
        assertTrue(codes.contains("user:create"));
    }

    @Test
    void testSystemRoles() {
        String tenantId = "test-tenant";

        // Test Super Admin role
        Role superAdmin = Role.SystemRoles.createSuperAdmin(tenantId);
        assertEquals(Role.SystemRoles.SUPER_ADMIN, superAdmin.getId());
        assertEquals(tenantId, superAdmin.getTenantId());
        assertTrue(superAdmin.isSystemRole());
        assertTrue(superAdmin.hasPermission(Permission.SYSTEM_ADMIN));
        assertTrue(superAdmin.hasPermission(Permission.FLOW_CREATE));
        assertTrue(superAdmin.hasPermission(Permission.USER_DELETE));

        // Test Admin role
        Role admin = Role.SystemRoles.createAdmin(tenantId);
        assertEquals(Role.SystemRoles.ADMIN, admin.getId());
        assertTrue(admin.isSystemRole());
        assertTrue(admin.hasPermission(Permission.FLOW_CREATE));
        assertTrue(admin.hasPermission(Permission.USER_CREATE));
        assertFalse(admin.hasPermission(Permission.SYSTEM_ADMIN));

        // Test Developer role
        Role developer = Role.SystemRoles.createDeveloper(tenantId);
        assertEquals(Role.SystemRoles.DEVELOPER, developer.getId());
        assertTrue(developer.isSystemRole());
        assertTrue(developer.hasPermission(Permission.FLOW_CREATE));
        assertTrue(developer.hasPermission(Permission.EXECUTION_READ));
        assertFalse(developer.hasPermission(Permission.USER_CREATE));

        // Test Viewer role
        Role viewer = Role.SystemRoles.createViewer(tenantId);
        assertEquals(Role.SystemRoles.VIEWER, viewer.getId());
        assertTrue(viewer.isSystemRole());
        assertTrue(viewer.hasPermission(Permission.FLOW_READ));
        assertFalse(viewer.hasPermission(Permission.FLOW_CREATE));
        assertFalse(viewer.hasPermission(Permission.USER_READ));

        // Test Executor role
        Role executor = Role.SystemRoles.createExecutor(tenantId);
        assertEquals(Role.SystemRoles.EXECUTOR, executor.getId());
        assertTrue(executor.isSystemRole());
        assertTrue(executor.hasPermission(Permission.FLOW_EXECUTE));
        assertTrue(executor.hasPermission(Permission.EXECUTION_CREATE));
        assertFalse(executor.hasPermission(Permission.FLOW_CREATE));
    }

    @Test
    void testTouch() {
        Role role = Role.builder()
            .id("test-role")
            .tenantId("tenant1")
            .name("Test Role")
            .build();

        assertNull(role.getUpdatedAt());

        Role touchedRole = role.touch();
        assertNotNull(touchedRole.getUpdatedAt());
    }
}
