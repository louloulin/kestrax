package io.kestra.core.models.rbac;

import io.kestra.core.models.TenantInterface;
import io.micronaut.core.annotation.Introspected;
import lombok.Builder;
import lombok.Value;
import lombok.With;
import lombok.extern.jackson.Jacksonized;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

/**
 * User entity for RBAC system
 */
@Value
@Builder(toBuilder = true)
@Jacksonized
@Introspected
@With
public class User implements TenantInterface {

    @NotBlank
    String id;

    @NotBlank
    String tenantId;

    @NotBlank
    String username;

    @NotBlank
    @Email
    String email;

    String firstName;

    String lastName;

    @Builder.Default
    boolean enabled = true;

    @NotNull
    @Builder.Default
    Instant createdAt = Instant.now();

    Instant lastLoginAt;

    @Builder.Default
    Set<String> groupIds = Set.of();

    @Builder.Default
    Set<String> roleIds = Set.of();

    @Builder.Default
    boolean deleted = false;

    // Additional fields for SSO integration
    @Builder.Default
    Set<String> roles = Set.of();

    @Builder.Default
    Set<String> permissions = Set.of();

    /**
     * Get full name of the user
     */
    public String getFullName() {
        if (firstName != null && lastName != null) {
            return firstName + " " + lastName;
        } else if (firstName != null) {
            return firstName;
        } else if (lastName != null) {
            return lastName;
        } else {
            return username;
        }
    }

    /**
     * Check if user is active
     */
    public boolean isActive() {
        return enabled && !deleted;
    }

    /**
     * Check if user is deleted
     */
    public boolean isDeleted() {
        return deleted;
    }

    /**
     * Update last login time
     */
    public User updateLastLogin() {
        return this.withLastLoginAt(Instant.now());
    }

    /**
     * Add role to user
     */
    public User addRole(String roleId) {
        Set<String> newRoles = new HashSet<>(roleIds);
        newRoles.add(roleId);
        return this.withRoleIds(newRoles);
    }

    /**
     * Remove role from user
     */
    public User removeRole(String roleId) {
        Set<String> newRoles = new HashSet<>(roleIds);
        newRoles.remove(roleId);
        return this.withRoleIds(newRoles);
    }

    /**
     * Add group to user
     */
    public User addGroup(String groupId) {
        Set<String> newGroups = new HashSet<>(groupIds);
        newGroups.add(groupId);
        return this.withGroupIds(newGroups);
    }

    /**
     * Remove group from user
     */
    public User removeGroup(String groupId) {
        Set<String> newGroups = new HashSet<>(groupIds);
        newGroups.remove(groupId);
        return this.withGroupIds(newGroups);
    }
}
