package io.kestra.core.services;

import io.kestra.core.models.PagedResults;
import io.kestra.core.models.rbac.Group;
import io.kestra.core.exceptions.IllegalVariableEvaluationException;
import jakarta.inject.Singleton;

import java.util.List;
import java.util.Optional;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Service for managing groups in the RBAC system.
 */
@Singleton
public class GroupService {

    // Mock data storage - in real implementation this would use a repository
    private final Map<String, Group> groups = new HashMap<>();

    public GroupService() {
        // Initialize with default groups
        initializeDefaultGroups();
    }

    /**
     * Search for groups with pagination and filtering.
     */
    public PagedResults<Group> findGroups(String query, int page, int size, String tenantId) {
        List<Group> allGroups = new ArrayList<>(groups.values());

        // Filter by tenant if specified
        if (tenantId != null) {
            allGroups = allGroups.stream()
                .filter(group -> tenantId.equals(group.getTenantId()))
                .toList();
        }

        // Filter by query if specified
        if (query != null && !query.trim().isEmpty()) {
            String lowerQuery = query.toLowerCase();
            allGroups = allGroups.stream()
                .filter(group ->
                    group.getName().toLowerCase().contains(lowerQuery) ||
                    (group.getDescription() != null && group.getDescription().toLowerCase().contains(lowerQuery))
                )
                .toList();
        }

        // Apply pagination
        int start = page * size;
        int end = Math.min(start + size, allGroups.size());
        List<Group> pageResults = start < allGroups.size() ? allGroups.subList(start, end) : new ArrayList<>();

        return PagedResults.of(pageResults, allGroups.size(), page, size);
    }

    /**
     * Find a group by ID.
     */
    public Optional<Group> findById(String id) {
        return Optional.ofNullable(groups.get(id));
    }

    /**
     * Create a new group.
     */
    public Group create(Group group) throws IllegalVariableEvaluationException {
        if (groups.containsKey(group.getId())) {
            throw new IllegalVariableEvaluationException("Group with ID " + group.getId() + " already exists");
        }
        groups.put(group.getId(), group);
        return group;
    }

    /**
     * Update an existing group.
     */
    public Group update(String id, Group group) throws IllegalVariableEvaluationException {
        if (!groups.containsKey(id)) {
            throw new IllegalVariableEvaluationException("Group with ID " + id + " not found");
        }
        Group updatedGroup = group.withId(id);
        groups.put(id, updatedGroup);
        return updatedGroup;
    }

    /**
     * Delete a group.
     */
    public void delete(String id) throws IllegalVariableEvaluationException {
        if (!groups.containsKey(id)) {
            throw new IllegalVariableEvaluationException("Group with ID " + id + " not found");
        }
        groups.remove(id);
    }

    /**
     * Add a member to a group.
     */
    public Group addMember(String groupId, String userId) throws IllegalVariableEvaluationException {
        Group group = findById(groupId)
            .orElseThrow(() -> new IllegalVariableEvaluationException("Group with ID " + groupId + " not found"));

        Group updatedGroup = group.addUser(userId);
        groups.put(groupId, updatedGroup);
        return updatedGroup;
    }

    /**
     * Remove a member from a group.
     */
    public Group removeMember(String groupId, String userId) throws IllegalVariableEvaluationException {
        Group group = findById(groupId)
            .orElseThrow(() -> new IllegalVariableEvaluationException("Group with ID " + groupId + " not found"));

        Group updatedGroup = group.removeUser(userId);
        groups.put(groupId, updatedGroup);
        return updatedGroup;
    }

    /**
     * Get groups for a specific user.
     */
    public List<Group> getGroupsForUser(String userId) {
        return groups.values().stream()
            .filter(group -> group.containsUser(userId))
            .toList();
    }

    /**
     * Check if a user is a member of a group.
     */
    public boolean isMember(String groupId, String userId) {
        return findById(groupId)
            .map(group -> group.containsUser(userId))
            .orElse(false);
    }

    /**
     * Initialize default system groups.
     */
    private void initializeDefaultGroups() {
        // Use the existing system groups from Group.SystemGroups
        Group adminGroup = Group.SystemGroups.createAdministrators("default");
        groups.put(adminGroup.getId(), adminGroup);

        Group developerGroup = Group.SystemGroups.createDevelopers("default");
        groups.put(developerGroup.getId(), developerGroup);

        Group viewerGroup = Group.SystemGroups.createViewers("default");
        groups.put(viewerGroup.getId(), viewerGroup);
    }
}
