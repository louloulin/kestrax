package io.kestra.core.services;

import io.kestra.core.models.PagedResults;
import io.kestra.core.models.rbac.Role;
import io.kestra.core.models.rbac.Permission;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class RoleServiceTest {

    private RoleService roleService;

    @BeforeEach
    void setUp() {
        roleService = new RoleService();
    }

    @Test
    void testFindRoles() {
        PagedResults<Role> results = roleService.findRoles(null, 0, 10, "default");

        assertNotNull(results);
        assertNotNull(results.getResults());
        assertTrue(results.getTotal() >= 0);

        // Should have default system roles
        assertTrue(results.getResults().size() > 0);
    }

    @Test
    void testFindRoleById() {
        Optional<Role> adminRole = roleService.findById("ADMIN");

        assertTrue(adminRole.isPresent());
        assertEquals("Administrator", adminRole.get().getName());
    }

    @Test
    void testSearchRoles() {
        PagedResults<Role> results = roleService.findRoles("Administrator", 0, 10, "default");

        assertNotNull(results);
        assertTrue(results.getResults().stream()
            .anyMatch(role -> role.getName().contains("Administrator")));
    }

    @Test
    void testGetRolePermissions() {
        var permissions = roleService.getRolePermissions("ADMIN");

        assertNotNull(permissions);
        assertFalse(permissions.isEmpty());
    }

    @Test
    void testHasPermission() {
        // Admin should have all permissions
        assertTrue(roleService.hasPermission("ADMIN", "flow:read"));

        // Viewer should have read permissions
        assertTrue(roleService.hasPermission("VIEWER", "flow:read"));
    }

    @Test
    void testCreateRole() throws Exception {
        Role newRole = Role.builder()
            .id("test-role")
            .name("test-role")
            .description("Test role")
            .permissions(Set.of())
            .build();

        Role created = roleService.create(newRole);

        assertNotNull(created);
        assertEquals("test-role", created.getId());

        // Verify it can be found
        Optional<Role> found = roleService.findById("test-role");
        assertTrue(found.isPresent());
    }

    @Test
    void testUpdateRole() throws Exception {
        // First create a role
        Role newRole = Role.builder()
            .id("update-test")
            .name("update-test")
            .description("Original description")
            .permissions(Set.of())
            .build();

        roleService.create(newRole);

        // Update it
        Role updatedRole = newRole.withDescription("Updated description");
        Role result = roleService.update("update-test", updatedRole);

        assertNotNull(result);
        assertEquals("Updated description", result.getDescription());
    }

    @Test
    void testDeleteRole() throws Exception {
        // First create a role
        Role newRole = Role.builder()
            .id("delete-test")
            .name("delete-test")
            .description("To be deleted")
            .permissions(Set.of())
            .build();

        roleService.create(newRole);

        // Verify it exists
        assertTrue(roleService.findById("delete-test").isPresent());

        // Delete it
        roleService.delete("delete-test");

        // Verify it's gone
        assertFalse(roleService.findById("delete-test").isPresent());
    }
}
