package io.kestra.core.models.rbac;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.kestra.core.models.TenantInterface;
import io.kestra.core.models.annotations.PluginProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.Map;

/**
 * Represents an audit event in the system for compliance and security tracking.
 */
@Value
@Builder
@Jacksonized
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(
    title = "Audit Event",
    description = "An audit event representing a security or compliance-relevant action in the system"
)
public class AuditEvent implements TenantInterface {
    
    @Schema(title = "Unique identifier for the audit event")
    String id;
    
    @Schema(title = "Tenant ID this audit event belongs to")
    String tenantId;
    
    @NotNull
    @Schema(title = "Timestamp when the event occurred")
    @PluginProperty(dynamic = false)
    Instant timestamp;
    
    @NotNull
    @Schema(title = "Type of audit event")
    @PluginProperty(dynamic = false)
    AuditEventType eventType;
    
    @NotNull
    @Schema(title = "Severity level of the event")
    @PluginProperty(dynamic = false)
    AuditSeverity severity;
    
    @Schema(title = "ID of the user who performed the action")
    @PluginProperty(dynamic = false)
    String userId;
    
    @Schema(title = "Name of the user who performed the action")
    @PluginProperty(dynamic = false)
    String userName;
    
    @Schema(title = "Email of the user who performed the action")
    @PluginProperty(dynamic = false)
    String userEmail;
    
    @NotNull
    @Schema(title = "Description of the action performed")
    @PluginProperty(dynamic = false)
    String action;
    
    @Schema(title = "Type of resource affected")
    @PluginProperty(dynamic = false)
    String resource;
    
    @Schema(title = "ID of the specific resource affected")
    @PluginProperty(dynamic = false)
    String resourceId;
    
    @Schema(title = "IP address from which the action was performed")
    @PluginProperty(dynamic = false)
    String ipAddress;
    
    @Schema(title = "User agent string of the client")
    @PluginProperty(dynamic = false)
    String userAgent;
    
    @NotNull
    @Schema(title = "Result of the action")
    @PluginProperty(dynamic = false)
    AuditResult outcome;
    
    @Schema(title = "Additional details about the event")
    @PluginProperty(dynamic = false)
    Map<String, Object> details;
    
    /**
     * Enumeration of audit event types
     */
    public enum AuditEventType {
        USER_LOGIN,
        USER_LOGOUT,
        USER_CREATE,
        USER_UPDATE,
        USER_DELETE,
        ROLE_CREATE,
        ROLE_UPDATE,
        ROLE_DELETE,
        ROLE_ASSIGN,
        ROLE_REVOKE,
        GROUP_CREATE,
        GROUP_UPDATE,
        GROUP_DELETE,
        GROUP_MEMBER_ADD,
        GROUP_MEMBER_REMOVE,
        PERMISSION_GRANT,
        PERMISSION_REVOKE,
        FLOW_CREATE,
        FLOW_UPDATE,
        FLOW_DELETE,
        FLOW_EXECUTE,
        EXECUTION_CREATE,
        EXECUTION_UPDATE,
        EXECUTION_DELETE,
        TENANT_CREATE,
        TENANT_UPDATE,
        TENANT_DELETE,
        TENANT_SWITCH,
        SYSTEM_CONFIG,
        DATA_EXPORT,
        DATA_IMPORT,
        PERMISSION_DENIED,
        AUTHENTICATION_FAILED,
        SESSION_EXPIRED,
        PASSWORD_CHANGE,
        MFA_ENABLE,
        MFA_DISABLE,
        SSO_LOGIN,
        API_KEY_CREATE,
        API_KEY_REVOKE
    }
    
    /**
     * Enumeration of audit severity levels
     */
    public enum AuditSeverity {
        INFO,
        WARNING,
        ERROR,
        CRITICAL
    }
    
    /**
     * Enumeration of audit result types
     */
    public enum AuditResult {
        SUCCESS,
        FAILURE,
        PARTIAL_SUCCESS,
        DENIED
    }
}
