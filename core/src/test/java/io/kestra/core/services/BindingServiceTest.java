package io.kestra.core.services;

import io.kestra.core.models.PagedResults;
import io.kestra.core.models.rbac.Binding;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class BindingServiceTest {

    private BindingService bindingService;

    @BeforeEach
    void setUp() {
        bindingService = new BindingService();
    }

    @Test
    void testFindBindings() {
        PagedResults<Binding> results = bindingService.findBindings(
            null, null, null, null, 0, 10, "default"
        );

        assertNotNull(results);
        assertNotNull(results.getResults());
        assertTrue(results.getTotal() >= 0);
    }

    @Test
    void testFindBindingById() {
        // Create a test binding first
        Binding testBinding = Binding.builder()
            .id("test-binding")
            .name("test-binding")
            .description("Test binding")
            .roleId("viewer")
            .subjectType(Binding.SubjectType.USER)
            .userIds(Set.of("test-user"))
            .build();

        try {
            bindingService.create(testBinding);

            Optional<Binding> found = bindingService.findById("test-binding");
            assertTrue(found.isPresent());
            assertEquals("test-binding", found.get().getName());
        } catch (Exception e) {
            fail("Failed to create test binding: " + e.getMessage());
        }
    }

    @Test
    void testSearchBindings() {
        PagedResults<Binding> results = bindingService.findBindings(
            "test", null, null, null, 0, 10, "default"
        );

        assertNotNull(results);
        assertTrue(results.getTotal() >= 0);
    }

    @Test
    void testCreateBinding() throws Exception {
        Binding newBinding = Binding.builder()
            .id("create-binding-test")
            .name("create-binding-test")
            .description("Test binding creation")
            .roleId("viewer")
            .subjectType(Binding.SubjectType.USER)
            .userIds(Set.of("test-user"))
            .build();

        Binding created = bindingService.create(newBinding);

        assertNotNull(created);
        assertEquals("create-binding-test", created.getId());

        // Verify it can be found
        Optional<Binding> found = bindingService.findById("create-binding-test");
        assertTrue(found.isPresent());
    }

    @Test
    void testUpdateBinding() throws Exception {
        // First create a binding
        Binding newBinding = Binding.builder()
            .id("update-binding-test")
            .name("update-binding-test")
            .description("Original description")
            .roleId("viewer")
            .subjectType(Binding.SubjectType.USER)
            .userIds(Set.of("test-user"))
            .build();

        bindingService.create(newBinding);

        // Update it
        Binding updatedBinding = newBinding.withDescription("Updated description");
        Binding result = bindingService.update("update-binding-test", updatedBinding);

        assertNotNull(result);
        assertEquals("Updated description", result.getDescription());
    }

    @Test
    void testDeleteBinding() throws Exception {
        // First create a binding
        Binding newBinding = Binding.builder()
            .id("delete-binding-test")
            .name("delete-binding-test")
            .description("To be deleted")
            .roleId("viewer")
            .subjectType(Binding.SubjectType.USER)
            .userIds(Set.of("test-user"))
            .build();

        bindingService.create(newBinding);

        // Verify it exists
        assertTrue(bindingService.findById("delete-binding-test").isPresent());

        // Delete it
        bindingService.delete("delete-binding-test");

        // Verify it's gone
        assertFalse(bindingService.findById("delete-binding-test").isPresent());
    }

    @Test
    void testFilterBySubjectType() {
        PagedResults<Binding> userBindings = bindingService.findBindings(
            null, Binding.SubjectType.USER, null, null, 0, 10, "default"
        );

        assertNotNull(userBindings);

        PagedResults<Binding> groupBindings = bindingService.findBindings(
            null, Binding.SubjectType.GROUP, null, null, 0, 10, "default"
        );

        assertNotNull(groupBindings);
    }

    @Test
    void testFilterByRole() {
        PagedResults<Binding> adminBindings = bindingService.findBindings(
            null, null, "admin", null, 0, 10, "default"
        );

        assertNotNull(adminBindings);

        PagedResults<Binding> viewerBindings = bindingService.findBindings(
            null, null, "viewer", null, 0, 10, "default"
        );

        assertNotNull(viewerBindings);
    }
}
