package io.kestra.core.services;

import io.kestra.core.models.rbac.*;
import io.kestra.core.models.PagedResults;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for the complete RBAC system workflow
 */
@DisplayName("RBAC System Integration Tests")
class RbacIntegrationTest {

    private RoleService roleService;
    private GroupService groupService;
    private BindingService bindingService;
    private AuditService auditService;

    @BeforeEach
    void setUp() {
        roleService = new RoleService();
        groupService = new GroupService();
        bindingService = new BindingService();
        auditService = new AuditService();
    }

    @Test
    @DisplayName("Complete RBAC workflow: Create role, group, and binding")
    void testCompleteRbacWorkflow() throws Exception {
        String tenantId = "test-tenant";

        // 1. Create a custom role
        Role customRole = Role.builder()
            .id("custom-developer")
            .tenantId(tenantId)
            .name("Custom Developer")
            .description("Custom developer role with specific permissions")
            .permissions(Set.of(
                Permission.FLOW_READ, Permission.FLOW_CREATE, Permission.FLOW_UPDATE,
                Permission.EXECUTION_READ, Permission.EXECUTION_CREATE
            ))
            .build();

        Role createdRole = roleService.create(customRole);
        assertNotNull(createdRole);
        assertEquals("custom-developer", createdRole.getId());

        // 2. Create a group
        Group devGroup = Group.builder()
            .id("dev-team")
            .tenantId(tenantId)
            .name("Development Team")
            .description("Main development team")
            .build();

        Group createdGroup = groupService.create(devGroup);
        assertNotNull(createdGroup);
        assertEquals("dev-team", createdGroup.getId());

        // 3. Create a binding that connects the group to the role
        Binding roleBinding = Binding.builder()
            .id("dev-team-binding")
            .tenantId(tenantId)
            .name("Dev Team Custom Developer Binding")
            .description("Binds dev team to custom developer role")
            .roleId("custom-developer")
            .groupIds(Set.of("dev-team"))
            .build();

        Binding createdBinding = bindingService.create(roleBinding);
        assertNotNull(createdBinding);
        assertEquals("dev-team-binding", createdBinding.getId());
        assertTrue(createdBinding.containsGroup("dev-team"));

        // 4. Verify the complete setup
        // Check role exists and has correct permissions
        assertTrue(roleService.hasPermission("custom-developer", "flow:read"));
        assertTrue(roleService.hasPermission("custom-developer", "flow:create"));
        assertFalse(roleService.hasPermission("custom-developer", "user:delete"));

        // Check group exists
        assertTrue(groupService.findById("dev-team").isPresent());

        // Check binding exists and links correctly
        assertTrue(bindingService.findById("dev-team-binding").isPresent());
        Binding foundBinding = bindingService.findById("dev-team-binding").get();
        assertEquals("custom-developer", foundBinding.getRoleId());
        assertTrue(foundBinding.getGroupIds().contains("dev-team"));
    }

    @Test
    @DisplayName("System roles are properly initialized")
    void testSystemRolesInitialization() {
        // Verify all system roles exist
        assertTrue(roleService.findById("ADMIN").isPresent());
        assertTrue(roleService.findById("DEVELOPER").isPresent());
        assertTrue(roleService.findById("VIEWER").isPresent());

        // Verify admin has comprehensive permissions
        assertTrue(roleService.hasPermission("ADMIN", "flow:read"));
        assertTrue(roleService.hasPermission("ADMIN", "flow:create"));
        assertTrue(roleService.hasPermission("ADMIN", "user:create"));
        assertTrue(roleService.hasPermission("ADMIN", "role:create"));

        // Verify viewer has limited permissions
        assertTrue(roleService.hasPermission("VIEWER", "flow:read"));
        assertFalse(roleService.hasPermission("VIEWER", "flow:create"));
        assertFalse(roleService.hasPermission("VIEWER", "user:create"));
    }

    @Test
    @DisplayName("Search and pagination work across all services")
    void testSearchAndPagination() {
        String tenantId = "test-tenant";

        // Test role search (use "default" tenant where system roles exist)
        PagedResults<Role> roles = roleService.findRoles("Administrator", 0, 10, "default");
        assertNotNull(roles);
        assertTrue(roles.getResults().stream()
            .anyMatch(role -> role.getName().contains("Administrator")));

        // Test group search (after creating some test groups)
        try {
            Group testGroup1 = Group.builder()
                .id("search-test-1")
                .tenantId(tenantId)
                .name("Search Test Group 1")
                .description("Test group for search")
                .build();
            groupService.create(testGroup1);

            PagedResults<Group> groups = groupService.findGroups("Search", 0, 10, tenantId);
            assertNotNull(groups);
            assertTrue(groups.getResults().stream()
                .anyMatch(group -> group.getName().contains("Search")));
        } catch (Exception e) {
            fail("Failed to test group search: " + e.getMessage());
        }

        // Test binding search
        PagedResults<Binding> bindings = bindingService.findBindings(
            null, null, "ADMIN", null, 0, 10, tenantId
        );
        assertNotNull(bindings);
    }

    @Test
    @DisplayName("Permission inheritance and validation")
    void testPermissionInheritance() {
        // Test that admin role has comprehensive permissions
        var adminPermissions = roleService.getRolePermissions("ADMIN");
        assertFalse(adminPermissions.isEmpty());
        assertTrue(adminPermissions.contains("flow:read"));
        assertTrue(adminPermissions.contains("flow:create"));
        assertTrue(adminPermissions.contains("user:create"));

        // Test that viewer role has limited permissions
        var viewerPermissions = roleService.getRolePermissions("VIEWER");
        assertFalse(viewerPermissions.isEmpty());
        assertTrue(viewerPermissions.contains("flow:read"));
        assertFalse(viewerPermissions.contains("flow:create"));
        assertFalse(viewerPermissions.contains("user:create"));
    }

    @Test
    @DisplayName("Multi-tenant isolation")
    void testMultiTenantIsolation() throws Exception {
        // Create roles in different tenants
        Role tenant1Role = Role.builder()
            .id("tenant1-role")
            .tenantId("tenant1")
            .name("Tenant 1 Role")
            .description("Role for tenant 1")
            .permissions(Set.of(Permission.FLOW_READ))
            .build();

        Role tenant2Role = Role.builder()
            .id("tenant2-role")
            .tenantId("tenant2")
            .name("Tenant 2 Role")
            .description("Role for tenant 2")
            .permissions(Set.of(Permission.FLOW_READ))
            .build();

        roleService.create(tenant1Role);
        roleService.create(tenant2Role);

        // Verify tenant isolation in search results
        PagedResults<Role> tenant1Roles = roleService.findRoles(null, 0, 10, "tenant1");
        PagedResults<Role> tenant2Roles = roleService.findRoles(null, 0, 10, "tenant2");

        // Each tenant should see their own roles plus system roles
        assertTrue(tenant1Roles.getResults().stream()
            .anyMatch(role -> "tenant1-role".equals(role.getId())));
        assertFalse(tenant1Roles.getResults().stream()
            .anyMatch(role -> "tenant2-role".equals(role.getId())));

        assertTrue(tenant2Roles.getResults().stream()
            .anyMatch(role -> "tenant2-role".equals(role.getId())));
        assertFalse(tenant2Roles.getResults().stream()
            .anyMatch(role -> "tenant1-role".equals(role.getId())));
    }

    @Test
    @DisplayName("CRUD operations work correctly")
    void testCrudOperations() throws Exception {
        String tenantId = "crud-test";

        // CREATE
        Role testRole = Role.builder()
            .id("crud-test-role")
            .tenantId(tenantId)
            .name("CRUD Test Role")
            .description("Original description")
            .permissions(Set.of(Permission.FLOW_READ))
            .build();

        Role created = roleService.create(testRole);
        assertNotNull(created);
        assertEquals("CRUD Test Role", created.getName());

        // READ
        assertTrue(roleService.findById("crud-test-role").isPresent());

        // UPDATE
        Role updated = created.withDescription("Updated description");
        Role result = roleService.update("crud-test-role", updated);
        assertEquals("Updated description", result.getDescription());

        // DELETE
        roleService.delete("crud-test-role");
        assertFalse(roleService.findById("crud-test-role").isPresent());
    }
}
