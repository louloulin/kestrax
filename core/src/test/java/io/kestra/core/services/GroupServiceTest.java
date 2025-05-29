package io.kestra.core.services;

import io.kestra.core.models.PagedResults;
import io.kestra.core.models.rbac.Group;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class GroupServiceTest {

    private GroupService groupService;

    @BeforeEach
    void setUp() {
        groupService = new GroupService();
    }

    @Test
    void testFindGroups() {
        PagedResults<Group> results = groupService.findGroups(null, 0, 10, "default");

        assertNotNull(results);
        assertNotNull(results.getResults());
        assertTrue(results.getTotal() >= 0);
    }

    @Test
    void testFindGroupById() {
        // Create a test group first
        Group testGroup = Group.builder()
            .id("test-group")
            .name("test-group")
            .description("Test group")
            .build();

        try {
            groupService.create(testGroup);

            Optional<Group> found = groupService.findById("test-group");
            assertTrue(found.isPresent());
            assertEquals("test-group", found.get().getName());
        } catch (Exception e) {
            fail("Failed to create test group: " + e.getMessage());
        }
    }

    @Test
    void testSearchGroups() {
        PagedResults<Group> results = groupService.findGroups("test", 0, 10, "default");

        assertNotNull(results);
        // Results may be empty if no groups match "test"
        assertTrue(results.getTotal() >= 0);
    }

    @Test
    void testCreateGroup() throws Exception {
        Group newGroup = Group.builder()
            .id("create-test")
            .name("create-test")
            .description("Test group creation")
            .build();

        Group created = groupService.create(newGroup);

        assertNotNull(created);
        assertEquals("create-test", created.getId());

        // Verify it can be found
        Optional<Group> found = groupService.findById("create-test");
        assertTrue(found.isPresent());
    }

    @Test
    void testUpdateGroup() throws Exception {
        // First create a group
        Group newGroup = Group.builder()
            .id("update-group-test")
            .name("update-group-test")
            .description("Original description")
            .build();

        groupService.create(newGroup);

        // Update it
        Group updatedGroup = newGroup.withDescription("Updated description");
        Group result = groupService.update("update-group-test", updatedGroup);

        assertNotNull(result);
        assertEquals("Updated description", result.getDescription());
    }

    @Test
    void testDeleteGroup() throws Exception {
        // First create a group
        Group newGroup = Group.builder()
            .id("delete-group-test")
            .name("delete-group-test")
            .description("To be deleted")
            .build();

        groupService.create(newGroup);

        // Verify it exists
        assertTrue(groupService.findById("delete-group-test").isPresent());

        // Delete it
        groupService.delete("delete-group-test");

        // Verify it's gone
        assertFalse(groupService.findById("delete-group-test").isPresent());
    }

    // TODO: Add existsByName and existsById tests when these methods are implemented in GroupService
}
