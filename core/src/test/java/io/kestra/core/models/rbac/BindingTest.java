package io.kestra.core.models.rbac;

import org.junit.jupiter.api.Test;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

class BindingTest {

    @Test
    void testBindingCreation() {
        Binding binding = Binding.builder()
            .id("binding-123")
            .tenantId("tenant1")
            .name("Test Binding")
            .description("A test binding")
            .roleId("role1")
            .userIds(Set.of("user1", "user2"))
            .groupIds(Set.of("group1"))
            .namespace("test-namespace")
            .build();

        assertEquals("binding-123", binding.getId());
        assertEquals("tenant1", binding.getTenantId());
        assertEquals("Test Binding", binding.getName());
        assertEquals("A test binding", binding.getDescription());
        assertEquals("role1", binding.getRoleId());
        assertEquals(2, binding.getUserIds().size());
        assertEquals(1, binding.getGroupIds().size());
        assertEquals("test-namespace", binding.getNamespace());
        assertTrue(binding.getUserIds().contains("user1"));
        assertTrue(binding.getUserIds().contains("user2"));
        assertTrue(binding.getGroupIds().contains("group1"));
        assertFalse(binding.isSystemBinding());
        assertFalse(binding.isGlobal());
        assertTrue(binding.isNamespaceScoped());
        assertNotNull(binding.getCreatedAt());
        assertNull(binding.getUpdatedAt());
    }

    @Test
    void testGlobalBinding() {
        Binding globalBinding = Binding.builder()
            .id("global-binding")
            .tenantId("tenant1")
            .name("Global Binding")
            .roleId("admin-role")
            .userIds(Set.of("admin"))
            .namespace(null) // Global scope
            .build();

        assertTrue(globalBinding.isGlobal());
        assertFalse(globalBinding.isNamespaceScoped());
        assertNull(globalBinding.getNamespace());
        assertTrue(globalBinding.appliesTo("any-namespace"));
        assertTrue(globalBinding.appliesTo("another-namespace"));
    }

    @Test
    void testNamespaceScopedBinding() {
        Binding namespacedBinding = Binding.builder()
            .id("namespaced-binding")
            .tenantId("tenant1")
            .name("Namespaced Binding")
            .roleId("dev-role")
            .userIds(Set.of("dev1"))
            .namespace("development")
            .build();

        assertFalse(namespacedBinding.isGlobal());
        assertTrue(namespacedBinding.isNamespaceScoped());
        assertEquals("development", namespacedBinding.getNamespace());
        assertTrue(namespacedBinding.appliesTo("development"));
        assertFalse(namespacedBinding.appliesTo("production"));
        assertFalse(namespacedBinding.appliesTo("staging"));
    }

    @Test
    void testAddUser() {
        Binding binding = Binding.builder()
            .id("binding1")
            .tenantId("tenant1")
            .name("Test Binding")
            .roleId("role1")
            .userIds(Set.of("user1"))
            .build();

        Binding updatedBinding = binding.addUser("user2");

        // Original binding unchanged
        assertEquals(1, binding.getUserIds().size());
        assertTrue(binding.getUserIds().contains("user1"));
        assertNull(binding.getUpdatedAt());

        // Updated binding has new user
        assertEquals(2, updatedBinding.getUserIds().size());
        assertTrue(updatedBinding.getUserIds().contains("user1"));
        assertTrue(updatedBinding.getUserIds().contains("user2"));
        assertNotNull(updatedBinding.getUpdatedAt());
    }

    @Test
    void testRemoveUser() {
        Binding binding = Binding.builder()
            .id("binding1")
            .tenantId("tenant1")
            .name("Test Binding")
            .roleId("role1")
            .userIds(Set.of("user1", "user2", "user3"))
            .build();

        Binding updatedBinding = binding.removeUser("user2");

        // Original binding unchanged
        assertEquals(3, binding.getUserIds().size());

        // Updated binding has user removed
        assertEquals(2, updatedBinding.getUserIds().size());
        assertTrue(updatedBinding.getUserIds().contains("user1"));
        assertFalse(updatedBinding.getUserIds().contains("user2"));
        assertTrue(updatedBinding.getUserIds().contains("user3"));
        assertNotNull(updatedBinding.getUpdatedAt());

        // Remove non-existent user should not change anything
        Binding unchangedBinding = updatedBinding.removeUser("non-existent");
        assertEquals(2, unchangedBinding.getUserIds().size());
        assertEquals(updatedBinding.getUserIds(), unchangedBinding.getUserIds());
    }

    @Test
    void testAddGroup() {
        Binding binding = Binding.builder()
            .id("binding1")
            .tenantId("tenant1")
            .name("Test Binding")
            .roleId("role1")
            .groupIds(Set.of("group1"))
            .build();

        Binding updatedBinding = binding.addGroup("group2");

        // Original binding unchanged
        assertEquals(1, binding.getGroupIds().size());
        assertTrue(binding.getGroupIds().contains("group1"));

        // Updated binding has new group
        assertEquals(2, updatedBinding.getGroupIds().size());
        assertTrue(updatedBinding.getGroupIds().contains("group1"));
        assertTrue(updatedBinding.getGroupIds().contains("group2"));
        assertNotNull(updatedBinding.getUpdatedAt());
    }

    @Test
    void testRemoveGroup() {
        Binding binding = Binding.builder()
            .id("binding1")
            .tenantId("tenant1")
            .name("Test Binding")
            .roleId("role1")
            .groupIds(Set.of("group1", "group2", "group3"))
            .build();

        Binding updatedBinding = binding.removeGroup("group2");

        // Original binding unchanged
        assertEquals(3, binding.getGroupIds().size());

        // Updated binding has group removed
        assertEquals(2, updatedBinding.getGroupIds().size());
        assertTrue(updatedBinding.getGroupIds().contains("group1"));
        assertFalse(updatedBinding.getGroupIds().contains("group2"));
        assertTrue(updatedBinding.getGroupIds().contains("group3"));
        assertNotNull(updatedBinding.getUpdatedAt());

        // Remove non-existent group should not change anything
        Binding unchangedBinding = updatedBinding.removeGroup("non-existent");
        assertEquals(2, unchangedBinding.getGroupIds().size());
        assertEquals(updatedBinding.getGroupIds(), unchangedBinding.getGroupIds());
    }

    @Test
    void testContainsUser() {
        Binding binding = Binding.builder()
            .id("binding1")
            .tenantId("tenant1")
            .name("Test Binding")
            .roleId("role1")
            .userIds(Set.of("user1", "user2"))
            .build();

        assertTrue(binding.containsUser("user1"));
        assertTrue(binding.containsUser("user2"));
        assertFalse(binding.containsUser("user3"));
        assertFalse(binding.containsUser("non-existent"));
    }

    @Test
    void testContainsGroup() {
        Binding binding = Binding.builder()
            .id("binding1")
            .tenantId("tenant1")
            .name("Test Binding")
            .roleId("role1")
            .groupIds(Set.of("group1", "group2"))
            .build();

        assertTrue(binding.containsGroup("group1"));
        assertTrue(binding.containsGroup("group2"));
        assertFalse(binding.containsGroup("group3"));
        assertFalse(binding.containsGroup("non-existent"));
    }

    @Test
    void testGetCounts() {
        Binding binding = Binding.builder()
            .id("binding1")
            .tenantId("tenant1")
            .name("Test Binding")
            .roleId("role1")
            .userIds(Set.of("user1", "user2", "user3"))
            .groupIds(Set.of("group1", "group2"))
            .build();

        assertEquals(3, binding.getUserCount());
        assertEquals(2, binding.getGroupCount());
        assertEquals(5, binding.getSubjectCount());
        assertTrue(binding.hasSubjects());

        // Empty binding
        Binding emptyBinding = Binding.builder()
            .id("empty")
            .tenantId("tenant1")
            .name("Empty Binding")
            .roleId("role1")
            .build();

        assertEquals(0, emptyBinding.getUserCount());
        assertEquals(0, emptyBinding.getGroupCount());
        assertEquals(0, emptyBinding.getSubjectCount());
        assertFalse(emptyBinding.hasSubjects());
    }

    @Test
    void testTouch() {
        Binding binding = Binding.builder()
            .id("binding1")
            .tenantId("tenant1")
            .name("Test Binding")
            .roleId("role1")
            .build();

        assertNull(binding.getUpdatedAt());

        Binding touchedBinding = binding.touch();
        assertNotNull(touchedBinding.getUpdatedAt());

        // Original should be unchanged
        assertNull(binding.getUpdatedAt());
    }

    @Test
    void testSystemBindings() {
        String tenantId = "test-tenant";
        String adminUserId = "admin-user";
        String namespace = "test-namespace";

        // Test Global Admin binding
        Binding globalAdmin = Binding.SystemBindings.createGlobalAdmin(tenantId, adminUserId);
        assertEquals("GLOBAL_ADMIN", globalAdmin.getId());
        assertEquals(tenantId, globalAdmin.getTenantId());
        assertEquals("Global Administrator", globalAdmin.getName());
        assertEquals(Role.SystemRoles.SUPER_ADMIN, globalAdmin.getRoleId());
        assertTrue(globalAdmin.containsUser(adminUserId));
        assertTrue(globalAdmin.isGlobal());
        assertTrue(globalAdmin.isSystemBinding());
        assertTrue(globalAdmin.appliesTo("any-namespace"));

        // Test Namespace Developers binding
        Binding namespaceDevelopers = Binding.SystemBindings.createNamespaceDevelopers(tenantId, namespace);
        assertEquals("DEVELOPERS_" + namespace.toUpperCase(), namespaceDevelopers.getId());
        assertEquals(Role.SystemRoles.DEVELOPER, namespaceDevelopers.getRoleId());
        assertTrue(namespaceDevelopers.containsGroup(Group.SystemGroups.DEVELOPERS));
        assertEquals(namespace, namespaceDevelopers.getNamespace());
        assertTrue(namespaceDevelopers.isNamespaceScoped());
        assertTrue(namespaceDevelopers.isSystemBinding());
        assertTrue(namespaceDevelopers.appliesTo(namespace));
        assertFalse(namespaceDevelopers.appliesTo("other-namespace"));

        // Test Namespace Viewers binding
        Binding namespaceViewers = Binding.SystemBindings.createNamespaceViewers(tenantId, namespace);
        assertEquals("VIEWERS_" + namespace.toUpperCase(), namespaceViewers.getId());
        assertEquals(Role.SystemRoles.VIEWER, namespaceViewers.getRoleId());
        assertTrue(namespaceViewers.containsGroup(Group.SystemGroups.VIEWERS));
        assertEquals(namespace, namespaceViewers.getNamespace());
        assertTrue(namespaceViewers.isNamespaceScoped());
        assertTrue(namespaceViewers.isSystemBinding());
    }

    @Test
    void testImmutability() {
        Binding originalBinding = Binding.builder()
            .id("binding1")
            .tenantId("tenant1")
            .name("Test Binding")
            .roleId("role1")
            .userIds(Set.of("user1"))
            .groupIds(Set.of("group1"))
            .build();

        // Test that modifications return new instances
        Binding modifiedBinding = originalBinding
            .addUser("user2")
            .addGroup("group2")
            .touch();

        // Original should be unchanged
        assertEquals(1, originalBinding.getUserIds().size());
        assertEquals(1, originalBinding.getGroupIds().size());
        assertNull(originalBinding.getUpdatedAt());

        // Modified should have changes
        assertEquals(2, modifiedBinding.getUserIds().size());
        assertEquals(2, modifiedBinding.getGroupIds().size());
        assertNotNull(modifiedBinding.getUpdatedAt());
    }

    @Test
    void testWithMethods() {
        Binding binding = Binding.builder()
            .id("binding1")
            .tenantId("tenant1")
            .name("Test Binding")
            .roleId("role1")
            .build();

        Binding modifiedBinding = binding
            .withName("Modified Binding")
            .withDescription("Modified description")
            .withNamespace("new-namespace")
            .withSystem(true);

        assertEquals("Modified Binding", modifiedBinding.getName());
        assertEquals("Modified description", modifiedBinding.getDescription());
        assertEquals("new-namespace", modifiedBinding.getNamespace());
        assertTrue(modifiedBinding.isSystemBinding());

        // Original should be unchanged
        assertEquals("Test Binding", binding.getName());
        assertNull(binding.getDescription());
        assertNull(binding.getNamespace());
        assertFalse(binding.isSystemBinding());
    }

    @Test
    void testTenantInterface() {
        Binding binding = Binding.builder()
            .id("binding1")
            .tenantId("tenant-123")
            .name("Test Binding")
            .roleId("role1")
            .build();

        assertEquals("tenant-123", binding.getTenantId());
    }
}
