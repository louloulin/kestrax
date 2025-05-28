package io.kestra.core.models.rbac;

import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

class UserTest {

    @Test
    void testUserCreation() {
        User user = User.builder()
            .id("user-123")
            .tenantId("tenant1")
            .username("john.doe")
            .email("john.doe@example.com")
            .firstName("John")
            .lastName("Doe")
            .build();

        assertEquals("user-123", user.getId());
        assertEquals("tenant1", user.getTenantId());
        assertEquals("john.doe", user.getUsername());
        assertEquals("john.doe@example.com", user.getEmail());
        assertEquals("John", user.getFirstName());
        assertEquals("Doe", user.getLastName());
        assertTrue(user.isEnabled());
        assertTrue(user.isActive());
        assertNotNull(user.getCreatedAt());
        assertNull(user.getLastLoginAt());
        assertTrue(user.getGroupIds().isEmpty());
        assertTrue(user.getRoleIds().isEmpty());
    }

    @Test
    void testGetFullName() {
        // Test with both first and last name
        User user1 = User.builder()
            .id("user1")
            .tenantId("tenant1")
            .username("john.doe")
            .email("john@example.com")
            .firstName("John")
            .lastName("Doe")
            .build();
        assertEquals("John Doe", user1.getFullName());

        // Test with only first name
        User user2 = User.builder()
            .id("user2")
            .tenantId("tenant1")
            .username("jane.smith")
            .email("jane@example.com")
            .firstName("Jane")
            .build();
        assertEquals("Jane", user2.getFullName());

        // Test with only last name
        User user3 = User.builder()
            .id("user3")
            .tenantId("tenant1")
            .username("bob.wilson")
            .email("bob@example.com")
            .lastName("Wilson")
            .build();
        assertEquals("Wilson", user3.getFullName());

        // Test with no names (fallback to username)
        User user4 = User.builder()
            .id("user4")
            .tenantId("tenant1")
            .username("admin")
            .email("admin@example.com")
            .build();
        assertEquals("admin", user4.getFullName());
    }

    @Test
    void testIsActive() {
        // Test enabled user
        User enabledUser = User.builder()
            .id("user1")
            .tenantId("tenant1")
            .username("enabled")
            .email("enabled@example.com")
            .enabled(true)
            .build();
        assertTrue(enabledUser.isActive());

        // Test disabled user
        User disabledUser = User.builder()
            .id("user2")
            .tenantId("tenant1")
            .username("disabled")
            .email("disabled@example.com")
            .enabled(false)
            .build();
        assertFalse(disabledUser.isActive());
    }

    @Test
    void testUpdateLastLogin() {
        User user = User.builder()
            .id("user1")
            .tenantId("tenant1")
            .username("test")
            .email("test@example.com")
            .build();

        assertNull(user.getLastLoginAt());

        User updatedUser = user.updateLastLogin();
        assertNotNull(updatedUser.getLastLoginAt());
        assertTrue(updatedUser.getLastLoginAt().isAfter(user.getCreatedAt()));

        // Original user should be unchanged
        assertNull(user.getLastLoginAt());
    }

    @Test
    void testAddRole() {
        User user = User.builder()
            .id("user1")
            .tenantId("tenant1")
            .username("test")
            .email("test@example.com")
            .build();

        assertTrue(user.getRoleIds().isEmpty());

        User userWithRole = user.addRole("role1");
        assertEquals(1, userWithRole.getRoleIds().size());
        assertTrue(userWithRole.getRoleIds().contains("role1"));

        // Original user should be unchanged
        assertTrue(user.getRoleIds().isEmpty());

        // Add another role
        User userWithTwoRoles = userWithRole.addRole("role2");
        assertEquals(2, userWithTwoRoles.getRoleIds().size());
        assertTrue(userWithTwoRoles.getRoleIds().contains("role1"));
        assertTrue(userWithTwoRoles.getRoleIds().contains("role2"));
    }

    @Test
    void testRemoveRole() {
        User user = User.builder()
            .id("user1")
            .tenantId("tenant1")
            .username("test")
            .email("test@example.com")
            .roleIds(Set.of("role1", "role2", "role3"))
            .build();

        assertEquals(3, user.getRoleIds().size());

        User userWithoutRole = user.removeRole("role2");
        assertEquals(2, userWithoutRole.getRoleIds().size());
        assertTrue(userWithoutRole.getRoleIds().contains("role1"));
        assertFalse(userWithoutRole.getRoleIds().contains("role2"));
        assertTrue(userWithoutRole.getRoleIds().contains("role3"));

        // Original user should be unchanged
        assertEquals(3, user.getRoleIds().size());

        // Remove non-existent role should not change anything
        User userUnchanged = userWithoutRole.removeRole("non-existent");
        assertEquals(2, userUnchanged.getRoleIds().size());
        assertEquals(userWithoutRole.getRoleIds(), userUnchanged.getRoleIds());
    }

    @Test
    void testAddGroup() {
        User user = User.builder()
            .id("user1")
            .tenantId("tenant1")
            .username("test")
            .email("test@example.com")
            .build();

        assertTrue(user.getGroupIds().isEmpty());

        User userWithGroup = user.addGroup("group1");
        assertEquals(1, userWithGroup.getGroupIds().size());
        assertTrue(userWithGroup.getGroupIds().contains("group1"));

        // Original user should be unchanged
        assertTrue(user.getGroupIds().isEmpty());

        // Add another group
        User userWithTwoGroups = userWithGroup.addGroup("group2");
        assertEquals(2, userWithTwoGroups.getGroupIds().size());
        assertTrue(userWithTwoGroups.getGroupIds().contains("group1"));
        assertTrue(userWithTwoGroups.getGroupIds().contains("group2"));
    }

    @Test
    void testRemoveGroup() {
        User user = User.builder()
            .id("user1")
            .tenantId("tenant1")
            .username("test")
            .email("test@example.com")
            .groupIds(Set.of("group1", "group2", "group3"))
            .build();

        assertEquals(3, user.getGroupIds().size());

        User userWithoutGroup = user.removeGroup("group2");
        assertEquals(2, userWithoutGroup.getGroupIds().size());
        assertTrue(userWithoutGroup.getGroupIds().contains("group1"));
        assertFalse(userWithoutGroup.getGroupIds().contains("group2"));
        assertTrue(userWithoutGroup.getGroupIds().contains("group3"));

        // Original user should be unchanged
        assertEquals(3, user.getGroupIds().size());

        // Remove non-existent group should not change anything
        User userUnchanged = userWithoutGroup.removeGroup("non-existent");
        assertEquals(2, userUnchanged.getGroupIds().size());
        assertEquals(userWithoutGroup.getGroupIds(), userUnchanged.getGroupIds());
    }

    @Test
    void testImmutability() {
        User originalUser = User.builder()
            .id("user1")
            .tenantId("tenant1")
            .username("test")
            .email("test@example.com")
            .roleIds(Set.of("role1"))
            .groupIds(Set.of("group1"))
            .build();

        // Test that modifications return new instances
        User modifiedUser = originalUser
            .addRole("role2")
            .addGroup("group2")
            .updateLastLogin();

        // Original should be unchanged
        assertEquals(1, originalUser.getRoleIds().size());
        assertEquals(1, originalUser.getGroupIds().size());
        assertNull(originalUser.getLastLoginAt());

        // Modified should have changes
        assertEquals(2, modifiedUser.getRoleIds().size());
        assertEquals(2, modifiedUser.getGroupIds().size());
        assertNotNull(modifiedUser.getLastLoginAt());
    }

    @Test
    void testWithMethods() {
        User user = User.builder()
            .id("user1")
            .tenantId("tenant1")
            .username("test")
            .email("test@example.com")
            .build();

        User modifiedUser = user
            .withFirstName("John")
            .withLastName("Doe")
            .withEnabled(false);

        assertEquals("John", modifiedUser.getFirstName());
        assertEquals("Doe", modifiedUser.getLastName());
        assertFalse(modifiedUser.isEnabled());

        // Original should be unchanged
        assertNull(user.getFirstName());
        assertNull(user.getLastName());
        assertTrue(user.isEnabled());
    }

    @Test
    void testTenantInterface() {
        User user = User.builder()
            .id("user1")
            .tenantId("tenant-123")
            .username("test")
            .email("test@example.com")
            .build();

        assertEquals("tenant-123", user.getTenantId());
    }
}
