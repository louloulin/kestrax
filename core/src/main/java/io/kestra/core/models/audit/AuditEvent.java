package io.kestra.core.models.audit;

import io.kestra.core.models.TenantInterface;
import io.micronaut.core.annotation.Introspected;
import lombok.Builder;
import lombok.Value;
import lombok.With;
import lombok.extern.jackson.Jacksonized;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.Map;

/**
 * Audit event entity for tracking security and operational events
 */
@Value
@Builder
@Jacksonized
@Introspected
@With
public class AuditEvent implements TenantInterface {

    @NotBlank
    String id;

    @NotBlank
    String tenantId;

    @NotNull
    AuditEventType eventType;

    @NotNull
    AuditEventCategory category;

    @NotBlank
    String userId;

    String sessionId;

    String ipAddress;

    String userAgent;

    @NotBlank
    String resource;

    String resourceId;

    @NotBlank
    String action;

    @NotNull
    AuditEventResult result;

    String description;

    Map<String, Object> details;

    String namespace;

    @NotNull
    @Builder.Default
    Instant timestamp = Instant.now();

    Long duration;

    String errorMessage;

    /**
     * Audit event types
     */
    public enum AuditEventType {
        AUTHENTICATION,
        AUTHORIZATION,
        PERMISSION_CHANGE,
        USER_MANAGEMENT,
        ROLE_MANAGEMENT,
        GROUP_MANAGEMENT,
        BINDING_MANAGEMENT,
        FLOW_OPERATION,
        EXECUTION_OPERATION,
        SYSTEM_OPERATION,
        SECURITY_EVENT
    }

    /**
     * Audit event categories
     */
    public enum AuditEventCategory {
        SECURITY,
        RBAC,
        WORKFLOW,
        SYSTEM,
        DATA_ACCESS,
        CONFIGURATION
    }

    /**
     * Audit event results
     */
    public enum AuditEventResult {
        SUCCESS,
        FAILURE,
        PARTIAL_SUCCESS,
        DENIED,
        ERROR
    }

    /**
     * Create authentication event
     */
    public static AuditEvent createAuthenticationEvent(String tenantId, String userId, String sessionId,
                                                     String ipAddress, AuditEventResult result) {
        return AuditEvent.builder()
            .id(generateId())
            .tenantId(tenantId)
            .eventType(AuditEventType.AUTHENTICATION)
            .category(AuditEventCategory.SECURITY)
            .userId(userId)
            .sessionId(sessionId)
            .ipAddress(ipAddress)
            .resource("authentication")
            .action("login")
            .result(result)
            .description("User authentication attempt")
            .build();
    }

    /**
     * Create authorization event
     */
    public static AuditEvent createAuthorizationEvent(String tenantId, String userId, String resource,
                                                    String action, AuditEventResult result, String namespace) {
        return AuditEvent.builder()
            .id(generateId())
            .tenantId(tenantId)
            .eventType(AuditEventType.AUTHORIZATION)
            .category(AuditEventCategory.SECURITY)
            .userId(userId)
            .resource(resource)
            .action(action)
            .result(result)
            .namespace(namespace)
            .description("Permission check for " + action + " on " + resource)
            .build();
    }

    /**
     * Create permission change event
     */
    public static AuditEvent createPermissionChangeEvent(String tenantId, String userId, String targetResource,
                                                       String targetId, String action, AuditEventResult result) {
        return AuditEvent.builder()
            .id(generateId())
            .tenantId(tenantId)
            .eventType(AuditEventType.PERMISSION_CHANGE)
            .category(AuditEventCategory.RBAC)
            .userId(userId)
            .resource(targetResource)
            .resourceId(targetId)
            .action(action)
            .result(result)
            .description("Permission change: " + action + " on " + targetResource)
            .build();
    }

    /**
     * Create user management event
     */
    public static AuditEvent createUserManagementEvent(String tenantId, String userId, String targetUserId,
                                                     String action, AuditEventResult result) {
        return AuditEvent.builder()
            .id(generateId())
            .tenantId(tenantId)
            .eventType(AuditEventType.USER_MANAGEMENT)
            .category(AuditEventCategory.RBAC)
            .userId(userId)
            .resource("user")
            .resourceId(targetUserId)
            .action(action)
            .result(result)
            .description("User management: " + action + " user " + targetUserId)
            .build();
    }

    /**
     * Create security event
     */
    public static AuditEvent createSecurityEvent(String tenantId, String userId, String description,
                                               AuditEventResult result, String ipAddress) {
        return AuditEvent.builder()
            .id(generateId())
            .tenantId(tenantId)
            .eventType(AuditEventType.SECURITY_EVENT)
            .category(AuditEventCategory.SECURITY)
            .userId(userId)
            .ipAddress(ipAddress)
            .resource("security")
            .action("security_event")
            .result(result)
            .description(description)
            .build();
    }

    /**
     * Check if this is a security-related event
     */
    public boolean isSecurityEvent() {
        return category == AuditEventCategory.SECURITY ||
               eventType == AuditEventType.AUTHENTICATION ||
               eventType == AuditEventType.AUTHORIZATION ||
               eventType == AuditEventType.SECURITY_EVENT;
    }

    /**
     * Check if this is a failed event
     */
    public boolean isFailedEvent() {
        return result == AuditEventResult.FAILURE ||
               result == AuditEventResult.DENIED ||
               result == AuditEventResult.ERROR;
    }

    /**
     * Check if this is a high-priority event
     */
    public boolean isHighPriority() {
        return isSecurityEvent() && isFailedEvent();
    }

    /**
     * Add duration to the event based on start time
     */
    public AuditEvent withDurationFromStartTime(long startTime) {
        long duration = System.currentTimeMillis() - startTime;
        return this.withDuration(duration);
    }

    /**
     * Add error message to the event
     */
    public AuditEvent withError(String errorMessage) {
        return this.withErrorMessage(errorMessage)
                   .withResult(AuditEventResult.ERROR);
    }

    /**
     * Generate unique audit event ID
     */
    private static String generateId() {
        return "audit_" + System.currentTimeMillis() + "_" +
               Integer.toHexString((int)(Math.random() * 0x10000));
    }
}
