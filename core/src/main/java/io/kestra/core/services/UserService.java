package io.kestra.core.services;

import io.kestra.core.models.rbac.User;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

/**
 * Service for user management operations
 */
@Singleton
@Slf4j
public class UserService {
    
    // In-memory storage for users (in production, this would be a database)
    private final ConcurrentMap<String, User> users = new ConcurrentHashMap<>();
    
    // Index by username for quick lookup
    private final ConcurrentMap<String, String> usernameToId = new ConcurrentHashMap<>();
    
    // Index by email for quick lookup
    private final ConcurrentMap<String, String> emailToId = new ConcurrentHashMap<>();
    
    /**
     * Create a new user
     */
    public User create(User user) {
        if (user.getId() == null || user.getId().trim().isEmpty()) {
            throw new IllegalArgumentException("User ID cannot be null or empty");
        }
        
        if (users.containsKey(user.getId())) {
            throw new IllegalArgumentException("User with ID '" + user.getId() + "' already exists");
        }
        
        if (usernameToId.containsKey(user.getUsername())) {
            throw new IllegalArgumentException("User with username '" + user.getUsername() + "' already exists");
        }
        
        if (emailToId.containsKey(user.getEmail())) {
            throw new IllegalArgumentException("User with email '" + user.getEmail() + "' already exists");
        }
        
        // Ensure user has creation timestamp
        User newUser = user.getCreatedAt() == null ? 
            user.withCreatedAt(Instant.now()) : user;
        
        users.put(newUser.getId(), newUser);
        usernameToId.put(newUser.getUsername(), newUser.getId());
        emailToId.put(newUser.getEmail(), newUser.getId());
        
        log.info("Created user: {} ({})", newUser.getUsername(), newUser.getId());
        return newUser;
    }
    
    /**
     * Update an existing user
     */
    public User update(User user) {
        if (!users.containsKey(user.getId())) {
            throw new IllegalArgumentException("User with ID '" + user.getId() + "' not found");
        }
        
        User existingUser = users.get(user.getId());
        
        // Check if username is being changed and if it conflicts
        if (!existingUser.getUsername().equals(user.getUsername())) {
            if (usernameToId.containsKey(user.getUsername())) {
                throw new IllegalArgumentException("User with username '" + user.getUsername() + "' already exists");
            }
            usernameToId.remove(existingUser.getUsername());
            usernameToId.put(user.getUsername(), user.getId());
        }
        
        // Check if email is being changed and if it conflicts
        if (!existingUser.getEmail().equals(user.getEmail())) {
            if (emailToId.containsKey(user.getEmail())) {
                throw new IllegalArgumentException("User with email '" + user.getEmail() + "' already exists");
            }
            emailToId.remove(existingUser.getEmail());
            emailToId.put(user.getEmail(), user.getId());
        }
        
        users.put(user.getId(), user);
        
        log.info("Updated user: {} ({})", user.getUsername(), user.getId());
        return user;
    }
    
    /**
     * Find user by ID
     */
    public Optional<User> findById(String id) {
        return Optional.ofNullable(users.get(id))
                      .filter(user -> !user.isDeleted());
    }
    
    /**
     * Find user by username
     */
    public Optional<User> findByUsername(String username) {
        String userId = usernameToId.get(username);
        if (userId == null) {
            return Optional.empty();
        }
        return findById(userId);
    }
    
    /**
     * Find user by email
     */
    public Optional<User> findByEmail(String email) {
        String userId = emailToId.get(email);
        if (userId == null) {
            return Optional.empty();
        }
        return findById(userId);
    }
    
    /**
     * Find users by tenant ID
     */
    public List<User> findByTenantId(String tenantId) {
        return users.values().stream()
                   .filter(user -> tenantId.equals(user.getTenantId()))
                   .filter(user -> !user.isDeleted())
                   .collect(Collectors.toList());
    }
    
    /**
     * Find users by role
     */
    public List<User> findByRole(String roleId) {
        return users.values().stream()
                   .filter(user -> user.getRoleIds().contains(roleId))
                   .filter(user -> !user.isDeleted())
                   .collect(Collectors.toList());
    }
    
    /**
     * Find users by group
     */
    public List<User> findByGroup(String groupId) {
        return users.values().stream()
                   .filter(user -> user.getGroupIds().contains(groupId))
                   .filter(user -> !user.isDeleted())
                   .collect(Collectors.toList());
    }
    
    /**
     * Get all users (with pagination support)
     */
    public List<User> findAll(int offset, int limit) {
        return users.values().stream()
                   .filter(user -> !user.isDeleted())
                   .sorted(Comparator.comparing(User::getUsername))
                   .skip(offset)
                   .limit(limit)
                   .collect(Collectors.toList());
    }
    
    /**
     * Get all users
     */
    public List<User> findAll() {
        return users.values().stream()
                   .filter(user -> !user.isDeleted())
                   .sorted(Comparator.comparing(User::getUsername))
                   .collect(Collectors.toList());
    }
    
    /**
     * Search users by query
     */
    public List<User> search(String query, int offset, int limit) {
        String lowerQuery = query.toLowerCase();
        
        return users.values().stream()
                   .filter(user -> !user.isDeleted())
                   .filter(user -> 
                       user.getUsername().toLowerCase().contains(lowerQuery) ||
                       user.getEmail().toLowerCase().contains(lowerQuery) ||
                       (user.getFirstName() != null && user.getFirstName().toLowerCase().contains(lowerQuery)) ||
                       (user.getLastName() != null && user.getLastName().toLowerCase().contains(lowerQuery))
                   )
                   .sorted(Comparator.comparing(User::getUsername))
                   .skip(offset)
                   .limit(limit)
                   .collect(Collectors.toList());
    }
    
    /**
     * Delete user (soft delete)
     */
    public boolean delete(String id) {
        User user = users.get(id);
        if (user == null) {
            return false;
        }
        
        User deletedUser = user.withDeleted(true);
        users.put(id, deletedUser);
        
        log.info("Deleted user: {} ({})", user.getUsername(), user.getId());
        return true;
    }
    
    /**
     * Permanently delete user
     */
    public boolean permanentDelete(String id) {
        User user = users.remove(id);
        if (user == null) {
            return false;
        }
        
        usernameToId.remove(user.getUsername());
        emailToId.remove(user.getEmail());
        
        log.info("Permanently deleted user: {} ({})", user.getUsername(), user.getId());
        return true;
    }
    
    /**
     * Enable user
     */
    public boolean enable(String id) {
        User user = users.get(id);
        if (user == null) {
            return false;
        }
        
        User enabledUser = user.withEnabled(true);
        users.put(id, enabledUser);
        
        log.info("Enabled user: {} ({})", user.getUsername(), user.getId());
        return true;
    }
    
    /**
     * Disable user
     */
    public boolean disable(String id) {
        User user = users.get(id);
        if (user == null) {
            return false;
        }
        
        User disabledUser = user.withEnabled(false);
        users.put(id, disabledUser);
        
        log.info("Disabled user: {} ({})", user.getUsername(), user.getId());
        return true;
    }
    
    /**
     * Update user's last login time
     */
    public void updateLastLogin(String id) {
        User user = users.get(id);
        if (user != null) {
            User updatedUser = user.withLastLoginAt(Instant.now());
            users.put(id, updatedUser);
        }
    }
    
    /**
     * Add role to user
     */
    public boolean addRole(String userId, String roleId) {
        User user = users.get(userId);
        if (user == null) {
            return false;
        }
        
        Set<String> newRoles = new HashSet<>(user.getRoleIds());
        if (newRoles.add(roleId)) {
            User updatedUser = user.withRoleIds(newRoles);
            users.put(userId, updatedUser);
            
            log.info("Added role '{}' to user: {} ({})", roleId, user.getUsername(), user.getId());
            return true;
        }
        
        return false; // Role already exists
    }
    
    /**
     * Remove role from user
     */
    public boolean removeRole(String userId, String roleId) {
        User user = users.get(userId);
        if (user == null) {
            return false;
        }
        
        Set<String> newRoles = new HashSet<>(user.getRoleIds());
        if (newRoles.remove(roleId)) {
            User updatedUser = user.withRoleIds(newRoles);
            users.put(userId, updatedUser);
            
            log.info("Removed role '{}' from user: {} ({})", roleId, user.getUsername(), user.getId());
            return true;
        }
        
        return false; // Role didn't exist
    }
    
    /**
     * Add group to user
     */
    public boolean addGroup(String userId, String groupId) {
        User user = users.get(userId);
        if (user == null) {
            return false;
        }
        
        Set<String> newGroups = new HashSet<>(user.getGroupIds());
        if (newGroups.add(groupId)) {
            User updatedUser = user.withGroupIds(newGroups);
            users.put(userId, updatedUser);
            
            log.info("Added group '{}' to user: {} ({})", groupId, user.getUsername(), user.getId());
            return true;
        }
        
        return false; // Group already exists
    }
    
    /**
     * Remove group from user
     */
    public boolean removeGroup(String userId, String groupId) {
        User user = users.get(userId);
        if (user == null) {
            return false;
        }
        
        Set<String> newGroups = new HashSet<>(user.getGroupIds());
        if (newGroups.remove(groupId)) {
            User updatedUser = user.withGroupIds(newGroups);
            users.put(userId, updatedUser);
            
            log.info("Removed group '{}' from user: {} ({})", groupId, user.getUsername(), user.getId());
            return true;
        }
        
        return false; // Group didn't exist
    }
    
    /**
     * Get user count
     */
    public long count() {
        return users.values().stream()
                   .filter(user -> !user.isDeleted())
                   .count();
    }
    
    /**
     * Get user count by tenant
     */
    public long countByTenant(String tenantId) {
        return users.values().stream()
                   .filter(user -> tenantId.equals(user.getTenantId()))
                   .filter(user -> !user.isDeleted())
                   .count();
    }
    
    /**
     * Check if username exists
     */
    public boolean existsByUsername(String username) {
        return usernameToId.containsKey(username) && 
               findByUsername(username).isPresent();
    }
    
    /**
     * Check if email exists
     */
    public boolean existsByEmail(String email) {
        return emailToId.containsKey(email) && 
               findByEmail(email).isPresent();
    }
    
    /**
     * Get user statistics
     */
    public UserStatistics getStatistics() {
        long totalUsers = count();
        long enabledUsers = users.values().stream()
                                 .filter(user -> !user.isDeleted())
                                 .filter(User::isEnabled)
                                 .count();
        long disabledUsers = totalUsers - enabledUsers;
        
        Map<String, Long> usersByTenant = users.values().stream()
                                               .filter(user -> !user.isDeleted())
                                               .collect(Collectors.groupingBy(
                                                   User::getTenantId,
                                                   Collectors.counting()
                                               ));
        
        return new UserStatistics(totalUsers, enabledUsers, disabledUsers, usersByTenant);
    }
    
    /**
     * User statistics
     */
    public static class UserStatistics {
        public final long totalUsers;
        public final long enabledUsers;
        public final long disabledUsers;
        public final Map<String, Long> usersByTenant;
        
        public UserStatistics(long totalUsers, long enabledUsers, long disabledUsers, Map<String, Long> usersByTenant) {
            this.totalUsers = totalUsers;
            this.enabledUsers = enabledUsers;
            this.disabledUsers = disabledUsers;
            this.usersByTenant = usersByTenant;
        }
    }
}
