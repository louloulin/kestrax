package io.kestra.core.services;

import io.kestra.core.models.PagedResults;
import io.kestra.core.models.rbac.Binding;
import io.kestra.core.exceptions.IllegalVariableEvaluationException;
import jakarta.inject.Singleton;

import java.util.List;
import java.util.Optional;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Service for managing role bindings in the RBAC system.
 */
@Singleton
public class BindingService {

    // Mock data storage - in real implementation this would use a repository
    private final Map<String, Binding> bindings = new HashMap<>();

    public BindingService() {
        // Initialize with default bindings
        initializeDefaultBindings();
    }

    /**
     * Search for bindings with pagination and filtering.
     */
    public PagedResults<Binding> findBindings(
        String query,
        Binding.SubjectType subjectType,
        String roleRef,
        String scope,
        int page,
        int size,
        String tenantId
    ) {
        List<Binding> allBindings = new ArrayList<>(bindings.values());

        // Filter by tenant if specified
        if (tenantId != null) {
            allBindings = allBindings.stream()
                .filter(binding -> tenantId.equals(binding.getTenantId()))
                .toList();
        }

        // Filter by subject type if specified
        if (subjectType != null) {
            allBindings = allBindings.stream()
                .filter(binding -> subjectType.equals(binding.getSubjectType()))
                .toList();
        }

        // Filter by role reference if specified
        if (roleRef != null && !roleRef.trim().isEmpty()) {
            allBindings = allBindings.stream()
                .filter(binding -> roleRef.equals(binding.getRoleRef()))
                .toList();
        }

        // Filter by scope if specified
        if (scope != null && !scope.trim().isEmpty()) {
            allBindings = allBindings.stream()
                .filter(binding -> scope.equals(binding.getScope()))
                .toList();
        }

        // Filter by query if specified
        if (query != null && !query.trim().isEmpty()) {
            String lowerQuery = query.toLowerCase();
            allBindings = allBindings.stream()
                .filter(binding ->
                    binding.getName().toLowerCase().contains(lowerQuery) ||
                    binding.getSubjectName().toLowerCase().contains(lowerQuery) ||
                    binding.getRoleRef().toLowerCase().contains(lowerQuery)
                )
                .toList();
        }

        // Apply pagination
        int start = page * size;
        int end = Math.min(start + size, allBindings.size());
        List<Binding> pageResults = start < allBindings.size() ? allBindings.subList(start, end) : new ArrayList<>();

        return PagedResults.of(pageResults, allBindings.size(), page, size);
    }

    /**
     * Find a binding by ID.
     */
    public Optional<Binding> findById(String id) {
        return Optional.ofNullable(bindings.get(id));
    }

    /**
     * Create a new binding.
     */
    public Binding create(Binding binding) throws IllegalVariableEvaluationException {
        if (bindings.containsKey(binding.getId())) {
            throw new IllegalVariableEvaluationException("Binding with ID " + binding.getId() + " already exists");
        }
        bindings.put(binding.getId(), binding);
        return binding;
    }

    /**
     * Update an existing binding.
     */
    public Binding update(String id, Binding binding) throws IllegalVariableEvaluationException {
        if (!bindings.containsKey(id)) {
            throw new IllegalVariableEvaluationException("Binding with ID " + id + " not found");
        }
        Binding updatedBinding = binding.withId(id);
        bindings.put(id, updatedBinding);
        return updatedBinding;
    }

    /**
     * Delete a binding.
     */
    public void delete(String id) throws IllegalVariableEvaluationException {
        if (!bindings.containsKey(id)) {
            throw new IllegalVariableEvaluationException("Binding with ID " + id + " not found");
        }
        bindings.remove(id);
    }

    /**
     * Get bindings for a specific subject.
     */
    public List<Binding> getBindingsForSubject(Binding.SubjectType subjectType, String subjectName) {
        return bindings.values().stream()
            .filter(binding ->
                subjectType.equals(binding.getSubjectType()) &&
                subjectName.equals(binding.getSubjectName())
            )
            .toList();
    }

    /**
     * Get bindings for a specific role.
     */
    public List<Binding> getBindingsForRole(String roleRef) {
        return bindings.values().stream()
            .filter(binding -> roleRef.equals(binding.getRoleRef()))
            .toList();
    }

    /**
     * Check if a subject has a specific role binding.
     */
    public boolean hasRoleBinding(Binding.SubjectType subjectType, String subjectName, String roleRef) {
        return bindings.values().stream()
            .anyMatch(binding ->
                subjectType.equals(binding.getSubjectType()) &&
                subjectName.equals(binding.getSubjectName()) &&
                roleRef.equals(binding.getRoleRef())
            );
    }

    /**
     * Initialize default system bindings.
     */
    private void initializeDefaultBindings() {
        // Admin user global binding
        Binding adminBinding = Binding.builder()
            .id("admin-global-binding")
            .name("admin-global-binding")
            .subjectType(Binding.SubjectType.USER)
            .subjectName("admin")
            .roleRef("admin")
            .scope("global")
            .build();
        bindings.put(adminBinding.getId(), adminBinding);

        // Developer user namespace binding
        Binding developerBinding = Binding.builder()
            .id("developer-namespace-binding")
            .name("developer-namespace-binding")
            .subjectType(Binding.SubjectType.USER)
            .subjectName("developer")
            .roleRef("developer")
            .scope("namespace")
            .namespace("development")
            .build();
        bindings.put(developerBinding.getId(), developerBinding);
    }
}
