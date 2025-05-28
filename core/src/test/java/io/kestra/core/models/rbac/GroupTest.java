package io.kestra.core.models.rbac;

import org.junit.jupiter.api.Test;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

class GroupTest {

    @Test
    void testGroupCreation() {
        Group group = Group.builder()
            .id("group-123")
            .tenantId("tenant1")
            .name("Test Group")
            .description("A test group")
            .roleIds(Set.of("role1", "role2"))
            .userIds(Set.of("user1", "user2"))
            .build();

        assertEquals("group-123", group.getId());
        assertEquals("tenant1", group.getTenantId());
        assertEquals("Test Group", group.getName());
        assertEquals("A test group", group.getDescription());
        assertEquals(2, group.getRoleIds().size());
        assertEquals(2, group.getUserIds().size());
        assertTrue(group.getRoleIds().contains("role1"));
        assertTrue(group.getRoleIds().contains("role2"));
        assertTrue(group.getUserIds().contains("user1"));
        assertTrue(group.getUserIds().contains("user2"));
        assertFalse(group.isSystemGroup());
        assertNotNull(group.getCreatedAt());
        assertNull(group.getUpdatedAt());
    }

    @Test
    void testAddRole() {
        Group group = Group.builder()
            .id("group1")
            .tenantId("tenant1")
            .name("Test Group")
            .roleIds(Set.of("role1"))
            .build();

        Group updatedGroup = group.addRole("role2");

        // Original group unchanged
        assertEquals(1, group.getRoleIds().size());
        assertTrue(group.getRoleIds().contains("role1"));
        assertNull(group.getUpdatedAt());

        // Updated group has new role
        assertEquals(2, updatedGroup.getRoleIds().size());
        assertTrue(updatedGroup.getRoleIds().contains("role1"));
        assertTrue(updatedGroup.getRoleIds().contains("role2"));
        assertNotNull(updatedGroup.getUpdatedAt());
    }

    @Test
    void testRemoveRole() {
        Group group = Group.builder()
            .id("group1")
            .tenantId("tenant1")
            .name("Test Group")
            .roleIds(Set.of("role1", "role2", "role3"))
            .build();

        Group updatedGroup = group.removeRole("role2");

        // Original group unchanged
        assertEquals(3, group.getRoleIds().size());

        // Updated group has role removed
        assertEquals(2, updatedGroup.getRoleIds().size());
        assertTrue(updatedGroup.getRoleIds().contains("role1"));
        assertFalse(updatedGroup.getRoleIds().contains("role2"));
        assertTrue(updatedGroup.getRoleIds().contains("role3"));
        assertNotNull(updatedGroup.getUpdatedAt());

        // Remove non-existent role should not change anything
        Group unchangedGroup = updatedGroup.removeRole("non-existent");
        assertEquals(2, unchangedGroup.getRoleIds().size());
        assertEquals(updatedGroup.getRoleIds(), unchangedGroup.getRoleIds());
    }

    @Test
    void testAddUser() {
        Group group = Group.builder()
            .id("group1")
            .tenantId("tenant1")
            .name("Test Group")
            .userIds(Set.of("user1"))
            .build();

        Group updatedGroup = group.addUser("user2");

        // Original group unchanged
        assertEquals(1, group.getUserIds().size());
        assertTrue(group.getUserIds().contains("user1"));

        // Updated group has new user
        assertEquals(2, updatedGroup.getUserIds().size());
        assertTrue(updatedGroup.getUserIds().contains("user1"));
        assertTrue(updatedGroup.getUserIds().contains("user2"));
        assertNotNull(updatedGroup.getUpdatedAt());
    }

    @Test
    void testRemoveUser() {
        Group group = Group.builder()
            .id("group1")
            .tenantId("tenant1")
            .name("Test Group")
            .userIds(Set.of("user1", "user2", "user3"))
            .build();

        Group updatedGroup = group.removeUser("user2");

        // Original group unchanged
        assertEquals(3, group.getUserIds().size());

        // Updated group has user removed
        assertEquals(2, updatedGroup.getUserIds().size());
        assertTrue(updatedGroup.getUserIds().contains("user1"));
        assertFalse(updatedGroup.getUserIds().contains("user2"));
        assertTrue(updatedGroup.getUserIds().contains("user3"));
        assertNotNull(updatedGroup.getUpdatedAt());

        // Remove non-existent user should not change anything
        Group unchangedGroup = updatedGroup.removeUser("non-existent");
        assertEquals(2, unchangedGroup.getUserIds().size());
        assertEquals(updatedGroup.getUserIds(), unchangedGroup.getUserIds());
    }

    @Test
    void testContainsUser() {
        Group group = Group.builder()
            .id("group1")
            .tenantId("tenant1")
            .name("Test Group")
            .userIds(Set.of("user1", "user2"))
            .build();

        assertTrue(group.containsUser("user1"));
        assertTrue(group.containsUser("user2"));
        assertFalse(group.containsUser("user3"));
        assertFalse(group.containsUser("non-existent"));
    }

    @Test
    void testHasRole() {
        Group group = Group.builder()
            .id("group1")
            .tenantId("tenant1")
            .name("Test Group")
            .roleIds(Set.of("role1", "role2"))
            .build();

        assertTrue(group.hasRole("role1"));
        assertTrue(group.hasRole("role2"));
        assertFalse(group.hasRole("role3"));
        assertFalse(group.hasRole("non-existent"));
    }

    @Test
    void testGetCounts() {
        Group group = Group.builder()
            .id("group1")
            .tenantId("tenant1")
            .name("Test Group")
            .roleIds(Set.of("role1", "role2", "role3"))
            .userIds(Set.of("user1", "user2"))
            .build();

        assertEquals(3, group.getRoleCount());
        assertEquals(2, group.getUserCount());

        // Empty group
        Group emptyGroup = Group.builder()
            .id("empty")
            .tenantId("tenant1")
            .name("Empty Group")
            .build();

        assertEquals(0, emptyGroup.getRoleCount());
        assertEquals(0, emptyGroup.getUserCount());
    }

    @Test
    void testTouch() {
        Group group = Group.builder()
            .id("group1")
            .tenantId("tenant1")
            .name("Test Group")
            .build();

        assertNull(group.getUpdatedAt());

        Group touchedGroup = group.touch();
        assertNotNull(touchedGroup.getUpdatedAt());

        // Original should be unchanged
        assertNull(group.getUpdatedAt());
    }

    @Test
    void testSystemGroups() {
        String tenantId = "test-tenant";

        // Test Administrators group
        Group administrators = Group.SystemGroups.createAdministrators(tenantId);
        assertEquals(Group.SystemGroups.ADMINISTRATORS, administrators.getId());
        assertEquals(tenantId, administrators.getTenantId());
        assertEquals("Administrators", administrators.getName());
        assertTrue(administrators.isSystemGroup());
        assertTrue(administrators.hasRole(Role.SystemRoles.ADMIN));
        assertEquals(1, administrators.getRoleCount());
        assertEquals(0, administrators.getUserCount());

        // Test Developers group
        Group developers = Group.SystemGroups.createDevelopers(tenantId);
        assertEquals(Group.SystemGroups.DEVELOPERS, developers.getId());
        assertTrue(developers.isSystemGroup());
        assertTrue(developers.hasRole(Role.SystemRoles.DEVELOPER));

        // Test Viewers group
        Group viewers = Group.SystemGroups.createViewers(tenantId);
        assertEquals(Group.SystemGroups.VIEWERS, viewers.getId());
        assertTrue(viewers.isSystemGroup());
        assertTrue(viewers.hasRole(Role.SystemRoles.VIEWER));

        // Test Executors group
        Group executors = Group.SystemGroups.createExecutors(tenantId);
        assertEquals(Group.SystemGroups.EXECUTORS, executors.getId());
        assertTrue(executors.isSystemGroup());
        assertTrue(executors.hasRole(Role.SystemRoles.EXECUTOR));
    }

    @Test
    void testImmutability() {
        Group originalGroup = Group.builder()
            .id("group1")
            .tenantId("tenant1")
            .name("Test Group")
            .roleIds(Set.of("role1"))
            .userIds(Set.of("user1"))
            .build();

        // Test that modifications return new instances
        Group modifiedGroup = originalGroup
            .addRole("role2")
            .addUser("user2")
            .touch();

        // Original should be unchanged
        assertEquals(1, originalGroup.getRoleIds().size());
        assertEquals(1, originalGroup.getUserIds().size());
        assertNull(originalGroup.getUpdatedAt());

        // Modified should have changes
        assertEquals(2, modifiedGroup.getRoleIds().size());
        assertEquals(2, modifiedGroup.getUserIds().size());
        assertNotNull(modifiedGroup.getUpdatedAt());
    }

    @Test
    void testWithMethods() {
        Group group = Group.builder()
            .id("group1")
            .tenantId("tenant1")
            .name("Test Group")
            .build();

        Group modifiedGroup = group
            .withName("Modified Group")
            .withDescription("Modified description")
            .withSystem(true);

        assertEquals("Modified Group", modifiedGroup.getName());
        assertEquals("Modified description", modifiedGroup.getDescription());
        assertTrue(modifiedGroup.isSystemGroup());

        // Original should be unchanged
        assertEquals("Test Group", group.getName());
        assertNull(group.getDescription());
        assertFalse(group.isSystemGroup());
    }

    @Test
    void testTenantInterface() {
        Group group = Group.builder()
            .id("group1")
            .tenantId("tenant-123")
            .name("Test Group")
            .build();

        assertEquals("tenant-123", group.getTenantId());
    }
}
