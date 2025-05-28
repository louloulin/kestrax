package io.kestra.core.models.rbac;

import io.kestra.core.models.TenantInterface;
import io.micronaut.core.annotation.Introspected;
import lombok.Builder;
import lombok.Value;
import lombok.With;
import lombok.extern.jackson.Jacksonized;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

/**
 * Binding entity for RBAC system - binds subjects (users/groups) to roles with optional namespace scope
 */
@Value
@Builder
@Jacksonized
@Introspected
@With
public class Binding implements TenantInterface {
    
    @NotBlank
    String id;
    
    @NotBlank
    String tenantId;
    
    @NotBlank
    String name;
    
    String description;
    
    /**
     * The role being bound
     */
    @NotBlank
    String roleId;
    
    /**
     * Users bound to this role
     */
    @NotNull
    @Builder.Default
    Set<String> userIds = Set.of();
    
    /**
     * Groups bound to this role
     */
    @NotNull
    @Builder.Default
    Set<String> groupIds = Set.of();
    
    /**
     * Optional namespace scope - if null, binding applies to all namespaces
     */
    String namespace;
    
    @NotNull
    @Builder.Default
    Instant createdAt = Instant.now();
    
    Instant updatedAt;
    
    @Builder.Default
    boolean system = false;
    
    /**
     * Add user to binding
     */
    public Binding addUser(String userId) {
        Set<String> newUsers = new HashSet<>(userIds);
        newUsers.add(userId);
        return this.withUserIds(newUsers).withUpdatedAt(Instant.now());
    }
    
    /**
     * Remove user from binding
     */
    public Binding removeUser(String userId) {
        Set<String> newUsers = new HashSet<>(userIds);
        newUsers.remove(userId);
        return this.withUserIds(newUsers).withUpdatedAt(Instant.now());
    }
    
    /**
     * Add group to binding
     */
    public Binding addGroup(String groupId) {
        Set<String> newGroups = new HashSet<>(groupIds);
        newGroups.add(groupId);
        return this.withGroupIds(newGroups).withUpdatedAt(Instant.now());
    }
    
    /**
     * Remove group from binding
     */
    public Binding removeGroup(String groupId) {
        Set<String> newGroups = new HashSet<>(groupIds);
        newGroups.remove(groupId);
        return this.withGroupIds(newGroups).withUpdatedAt(Instant.now());
    }
    
    /**
     * Check if binding contains user
     */
    public boolean containsUser(String userId) {
        return userIds.contains(userId);
    }
    
    /**
     * Check if binding contains group
     */
    public boolean containsGroup(String groupId) {
        return groupIds.contains(groupId);
    }
    
    /**
     * Check if binding applies to namespace
     */
    public boolean appliesTo(String namespace) {
        return this.namespace == null || this.namespace.equals(namespace);
    }
    
    /**
     * Check if this is a global binding (applies to all namespaces)
     */
    public boolean isGlobal() {
        return namespace == null;
    }
    
    /**
     * Check if this is a namespace-scoped binding
     */
    public boolean isNamespaceScoped() {
        return namespace != null;
    }
    
    /**
     * Check if this is a system binding (cannot be deleted)
     */
    public boolean isSystemBinding() {
        return system;
    }
    
    /**
     * Get number of users in binding
     */
    public int getUserCount() {
        return userIds.size();
    }
    
    /**
     * Get number of groups in binding
     */
    public int getGroupCount() {
        return groupIds.size();
    }
    
    /**
     * Get total number of subjects (users + groups) in binding
     */
    public int getSubjectCount() {
        return userIds.size() + groupIds.size();
    }
    
    /**
     * Check if binding has any subjects
     */
    public boolean hasSubjects() {
        return !userIds.isEmpty() || !groupIds.isEmpty();
    }
    
    /**
     * Create a copy with updated timestamp
     */
    public Binding touch() {
        return this.withUpdatedAt(Instant.now());
    }
    
    /**
     * Predefined system bindings
     */
    public static class SystemBindings {
        
        /**
         * Create global admin binding
         */
        public static Binding createGlobalAdmin(String tenantId, String adminUserId) {
            return Binding.builder()
                .id("GLOBAL_ADMIN")
                .tenantId(tenantId)
                .name("Global Administrator")
                .description("Global administrator with full access to all namespaces")
                .roleId(Role.SystemRoles.SUPER_ADMIN)
                .userIds(Set.of(adminUserId))
                .namespace(null) // Global scope
                .system(true)
                .build();
        }
        
        /**
         * Create default developers binding for a namespace
         */
        public static Binding createNamespaceDevelopers(String tenantId, String namespace) {
            return Binding.builder()
                .id("DEVELOPERS_" + namespace.toUpperCase())
                .tenantId(tenantId)
                .name("Developers - " + namespace)
                .description("Developers with access to " + namespace + " namespace")
                .roleId(Role.SystemRoles.DEVELOPER)
                .groupIds(Set.of(Group.SystemGroups.DEVELOPERS))
                .namespace(namespace)
                .system(true)
                .build();
        }
        
        /**
         * Create default viewers binding for a namespace
         */
        public static Binding createNamespaceViewers(String tenantId, String namespace) {
            return Binding.builder()
                .id("VIEWERS_" + namespace.toUpperCase())
                .tenantId(tenantId)
                .name("Viewers - " + namespace)
                .description("Read-only access to " + namespace + " namespace")
                .roleId(Role.SystemRoles.VIEWER)
                .groupIds(Set.of(Group.SystemGroups.VIEWERS))
                .namespace(namespace)
                .system(true)
                .build();
        }
    }
}
