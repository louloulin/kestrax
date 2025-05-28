package io.kestra.core.models.rbac;

/**
 * Permission enumeration for RBAC system
 * Defines all available permissions in the DataFlare system
 */
public enum Permission {
    
    // Flow permissions
    FLOW_CREATE("flow:create", "Create flows"),
    FLOW_READ("flow:read", "Read flows"),
    FLOW_UPDATE("flow:update", "Update flows"),
    FLOW_DELETE("flow:delete", "Delete flows"),
    FLOW_EXECUTE("flow:execute", "Execute flows"),
    
    // Execution permissions
    EXECUTION_CREATE("execution:create", "Create executions"),
    EXECUTION_READ("execution:read", "Read executions"),
    EXECUTION_UPDATE("execution:update", "Update executions"),
    EXECUTION_DELETE("execution:delete", "Delete executions"),
    EXECUTION_KILL("execution:kill", "Kill executions"),
    EXECUTION_RESTART("execution:restart", "Restart executions"),
    
    // Template permissions
    TEMPLATE_CREATE("template:create", "Create templates"),
    TEMPLATE_READ("template:read", "Read templates"),
    TEMPLATE_UPDATE("template:update", "Update templates"),
    TEMPLATE_DELETE("template:delete", "Delete templates"),
    
    // Namespace permissions
    NAMESPACE_CREATE("namespace:create", "Create namespaces"),
    NAMESPACE_READ("namespace:read", "Read namespaces"),
    NAMESPACE_UPDATE("namespace:update", "Update namespaces"),
    NAMESPACE_DELETE("namespace:delete", "Delete namespaces"),
    
    // User management permissions
    USER_CREATE("user:create", "Create users"),
    USER_READ("user:read", "Read users"),
    USER_UPDATE("user:update", "Update users"),
    USER_DELETE("user:delete", "Delete users"),
    
    // Role management permissions
    ROLE_CREATE("role:create", "Create roles"),
    ROLE_READ("role:read", "Read roles"),
    ROLE_UPDATE("role:update", "Update roles"),
    ROLE_DELETE("role:delete", "Delete roles"),
    
    // Group management permissions
    GROUP_CREATE("group:create", "Create groups"),
    GROUP_READ("group:read", "Read groups"),
    GROUP_UPDATE("group:update", "Update groups"),
    GROUP_DELETE("group:delete", "Delete groups"),
    
    // Tenant management permissions (Super Admin only)
    TENANT_CREATE("tenant:create", "Create tenants"),
    TENANT_READ("tenant:read", "Read tenants"),
    TENANT_UPDATE("tenant:update", "Update tenants"),
    TENANT_DELETE("tenant:delete", "Delete tenants"),
    
    // Audit permissions
    AUDIT_READ("audit:read", "Read audit logs"),
    AUDIT_EXPORT("audit:export", "Export audit logs"),
    
    // Worker group permissions
    WORKER_GROUP_CREATE("worker-group:create", "Create worker groups"),
    WORKER_GROUP_READ("worker-group:read", "Read worker groups"),
    WORKER_GROUP_UPDATE("worker-group:update", "Update worker groups"),
    WORKER_GROUP_DELETE("worker-group:delete", "Delete worker groups"),
    
    // Secret permissions
    SECRET_CREATE("secret:create", "Create secrets"),
    SECRET_READ("secret:read", "Read secrets"),
    SECRET_UPDATE("secret:update", "Update secrets"),
    SECRET_DELETE("secret:delete", "Delete secrets"),
    
    // KV Store permissions
    KV_CREATE("kv:create", "Create KV entries"),
    KV_READ("kv:read", "Read KV entries"),
    KV_UPDATE("kv:update", "Update KV entries"),
    KV_DELETE("kv:delete", "Delete KV entries"),
    
    // Plugin permissions
    PLUGIN_READ("plugin:read", "Read plugins"),
    PLUGIN_INSTALL("plugin:install", "Install plugins"),
    PLUGIN_UNINSTALL("plugin:uninstall", "Uninstall plugins"),
    
    // System administration permissions
    SYSTEM_ADMIN("system:admin", "System administration"),
    SYSTEM_MONITOR("system:monitor", "System monitoring"),
    SYSTEM_CONFIG("system:config", "System configuration");
    
    private final String code;
    private final String description;
    
    Permission(String code, String description) {
        this.code = code;
        this.description = description;
    }
    
    public String getCode() {
        return code;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * Get permission by code
     */
    public static Permission fromCode(String code) {
        for (Permission permission : values()) {
            if (permission.code.equals(code)) {
                return permission;
            }
        }
        throw new IllegalArgumentException("Unknown permission code: " + code);
    }
    
    /**
     * Check if this permission implies another permission
     */
    public boolean implies(Permission other) {
        // System admin implies all permissions
        if (this == SYSTEM_ADMIN) {
            return true;
        }
        
        // Same permission
        if (this == other) {
            return true;
        }
        
        // Specific implications
        return switch (this) {
            case FLOW_UPDATE -> other == FLOW_READ;
            case FLOW_DELETE -> other == FLOW_READ;
            case EXECUTION_UPDATE -> other == EXECUTION_READ;
            case EXECUTION_DELETE -> other == EXECUTION_READ;
            case USER_UPDATE -> other == USER_READ;
            case USER_DELETE -> other == USER_READ;
            case ROLE_UPDATE -> other == ROLE_READ;
            case ROLE_DELETE -> other == ROLE_READ;
            default -> false;
        };
    }
    
    /**
     * Get resource type from permission
     */
    public String getResourceType() {
        return code.split(":")[0];
    }
    
    /**
     * Get action from permission
     */
    public String getAction() {
        return code.split(":")[1];
    }
}
