package io.kestra.core.models.audit;

import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class AuditEventTest {

    @Test
    void testAuditEventCreation() {
        AuditEvent event = AuditEvent.builder()
            .id("audit-123")
            .tenantId("tenant1")
            .eventType(AuditEvent.AuditEventType.AUTHENTICATION)
            .category(AuditEvent.AuditEventCategory.SECURITY)
            .userId("user1")
            .sessionId("session123")
            .ipAddress("192.168.1.1")
            .userAgent("Mozilla/5.0")
            .resource("authentication")
            .resourceId("login")
            .action("login")
            .result(AuditEvent.AuditEventResult.SUCCESS)
            .description("User login successful")
            .details(Map.of("method", "password"))
            .namespace("default")
            .duration(150L)
            .build();

        assertEquals("audit-123", event.getId());
        assertEquals("tenant1", event.getTenantId());
        assertEquals(AuditEvent.AuditEventType.AUTHENTICATION, event.getEventType());
        assertEquals(AuditEvent.AuditEventCategory.SECURITY, event.getCategory());
        assertEquals("user1", event.getUserId());
        assertEquals("session123", event.getSessionId());
        assertEquals("192.168.1.1", event.getIpAddress());
        assertEquals("Mozilla/5.0", event.getUserAgent());
        assertEquals("authentication", event.getResource());
        assertEquals("login", event.getResourceId());
        assertEquals("login", event.getAction());
        assertEquals(AuditEvent.AuditEventResult.SUCCESS, event.getResult());
        assertEquals("User login successful", event.getDescription());
        assertEquals(Map.of("method", "password"), event.getDetails());
        assertEquals("default", event.getNamespace());
        assertEquals(150L, event.getDuration());
        assertNotNull(event.getTimestamp());
        assertNull(event.getErrorMessage());
    }

    @Test
    void testCreateAuthenticationEvent() {
        AuditEvent event = AuditEvent.createAuthenticationEvent(
            "tenant1", "user1", "session123", "192.168.1.1", AuditEvent.AuditEventResult.SUCCESS
        );

        assertNotNull(event.getId());
        assertTrue(event.getId().startsWith("audit_"));
        assertEquals("tenant1", event.getTenantId());
        assertEquals(AuditEvent.AuditEventType.AUTHENTICATION, event.getEventType());
        assertEquals(AuditEvent.AuditEventCategory.SECURITY, event.getCategory());
        assertEquals("user1", event.getUserId());
        assertEquals("session123", event.getSessionId());
        assertEquals("192.168.1.1", event.getIpAddress());
        assertEquals("authentication", event.getResource());
        assertEquals("login", event.getAction());
        assertEquals(AuditEvent.AuditEventResult.SUCCESS, event.getResult());
        assertEquals("User authentication attempt", event.getDescription());
        assertNotNull(event.getTimestamp());
    }

    @Test
    void testCreateAuthorizationEvent() {
        AuditEvent event = AuditEvent.createAuthorizationEvent(
            "tenant1", "user1", "flow", "read", AuditEvent.AuditEventResult.SUCCESS, "test-namespace"
        );

        assertNotNull(event.getId());
        assertEquals("tenant1", event.getTenantId());
        assertEquals(AuditEvent.AuditEventType.AUTHORIZATION, event.getEventType());
        assertEquals(AuditEvent.AuditEventCategory.SECURITY, event.getCategory());
        assertEquals("user1", event.getUserId());
        assertEquals("flow", event.getResource());
        assertEquals("read", event.getAction());
        assertEquals(AuditEvent.AuditEventResult.SUCCESS, event.getResult());
        assertEquals("test-namespace", event.getNamespace());
        assertEquals("Permission check for read on flow", event.getDescription());
    }

    @Test
    void testCreatePermissionChangeEvent() {
        AuditEvent event = AuditEvent.createPermissionChangeEvent(
            "tenant1", "admin1", "role", "role123", "add_permission", AuditEvent.AuditEventResult.SUCCESS
        );

        assertNotNull(event.getId());
        assertEquals("tenant1", event.getTenantId());
        assertEquals(AuditEvent.AuditEventType.PERMISSION_CHANGE, event.getEventType());
        assertEquals(AuditEvent.AuditEventCategory.RBAC, event.getCategory());
        assertEquals("admin1", event.getUserId());
        assertEquals("role", event.getResource());
        assertEquals("role123", event.getResourceId());
        assertEquals("add_permission", event.getAction());
        assertEquals(AuditEvent.AuditEventResult.SUCCESS, event.getResult());
        assertEquals("Permission change: add_permission on role", event.getDescription());
    }

    @Test
    void testCreateUserManagementEvent() {
        AuditEvent event = AuditEvent.createUserManagementEvent(
            "tenant1", "admin1", "user123", "create", AuditEvent.AuditEventResult.SUCCESS
        );

        assertNotNull(event.getId());
        assertEquals("tenant1", event.getTenantId());
        assertEquals(AuditEvent.AuditEventType.USER_MANAGEMENT, event.getEventType());
        assertEquals(AuditEvent.AuditEventCategory.RBAC, event.getCategory());
        assertEquals("admin1", event.getUserId());
        assertEquals("user", event.getResource());
        assertEquals("user123", event.getResourceId());
        assertEquals("create", event.getAction());
        assertEquals(AuditEvent.AuditEventResult.SUCCESS, event.getResult());
        assertEquals("User management: create user user123", event.getDescription());
    }

    @Test
    void testCreateSecurityEvent() {
        AuditEvent event = AuditEvent.createSecurityEvent(
            "tenant1", "user1", "Suspicious login attempt", AuditEvent.AuditEventResult.FAILURE, "192.168.1.100"
        );

        assertNotNull(event.getId());
        assertEquals("tenant1", event.getTenantId());
        assertEquals(AuditEvent.AuditEventType.SECURITY_EVENT, event.getEventType());
        assertEquals(AuditEvent.AuditEventCategory.SECURITY, event.getCategory());
        assertEquals("user1", event.getUserId());
        assertEquals("192.168.1.100", event.getIpAddress());
        assertEquals("security", event.getResource());
        assertEquals("security_event", event.getAction());
        assertEquals(AuditEvent.AuditEventResult.FAILURE, event.getResult());
        assertEquals("Suspicious login attempt", event.getDescription());
    }

    @Test
    void testIsSecurityEvent() {
        // Security category event
        AuditEvent securityEvent = AuditEvent.builder()
            .id("test")
            .tenantId("tenant1")
            .eventType(AuditEvent.AuditEventType.FLOW_OPERATION)
            .category(AuditEvent.AuditEventCategory.SECURITY)
            .userId("user1")
            .resource("test")
            .action("test")
            .result(AuditEvent.AuditEventResult.SUCCESS)
            .build();
        assertTrue(securityEvent.isSecurityEvent());

        // Authentication event
        AuditEvent authEvent = AuditEvent.builder()
            .id("test")
            .tenantId("tenant1")
            .eventType(AuditEvent.AuditEventType.AUTHENTICATION)
            .category(AuditEvent.AuditEventCategory.RBAC)
            .userId("user1")
            .resource("test")
            .action("test")
            .result(AuditEvent.AuditEventResult.SUCCESS)
            .build();
        assertTrue(authEvent.isSecurityEvent());

        // Non-security event
        AuditEvent workflowEvent = AuditEvent.builder()
            .id("test")
            .tenantId("tenant1")
            .eventType(AuditEvent.AuditEventType.FLOW_OPERATION)
            .category(AuditEvent.AuditEventCategory.WORKFLOW)
            .userId("user1")
            .resource("test")
            .action("test")
            .result(AuditEvent.AuditEventResult.SUCCESS)
            .build();
        assertFalse(workflowEvent.isSecurityEvent());
    }

    @Test
    void testIsFailedEvent() {
        // Failed event
        AuditEvent failedEvent = AuditEvent.builder()
            .id("test")
            .tenantId("tenant1")
            .eventType(AuditEvent.AuditEventType.AUTHENTICATION)
            .category(AuditEvent.AuditEventCategory.SECURITY)
            .userId("user1")
            .resource("test")
            .action("test")
            .result(AuditEvent.AuditEventResult.FAILURE)
            .build();
        assertTrue(failedEvent.isFailedEvent());

        // Denied event
        AuditEvent deniedEvent = AuditEvent.builder()
            .id("test")
            .tenantId("tenant1")
            .eventType(AuditEvent.AuditEventType.AUTHORIZATION)
            .category(AuditEvent.AuditEventCategory.SECURITY)
            .userId("user1")
            .resource("test")
            .action("test")
            .result(AuditEvent.AuditEventResult.DENIED)
            .build();
        assertTrue(deniedEvent.isFailedEvent());

        // Error event
        AuditEvent errorEvent = AuditEvent.builder()
            .id("test")
            .tenantId("tenant1")
            .eventType(AuditEvent.AuditEventType.SYSTEM_OPERATION)
            .category(AuditEvent.AuditEventCategory.SYSTEM)
            .userId("user1")
            .resource("test")
            .action("test")
            .result(AuditEvent.AuditEventResult.ERROR)
            .build();
        assertTrue(errorEvent.isFailedEvent());

        // Success event
        AuditEvent successEvent = AuditEvent.builder()
            .id("test")
            .tenantId("tenant1")
            .eventType(AuditEvent.AuditEventType.AUTHENTICATION)
            .category(AuditEvent.AuditEventCategory.SECURITY)
            .userId("user1")
            .resource("test")
            .action("test")
            .result(AuditEvent.AuditEventResult.SUCCESS)
            .build();
        assertFalse(successEvent.isFailedEvent());
    }

    @Test
    void testIsHighPriority() {
        // High priority: security + failed
        AuditEvent highPriorityEvent = AuditEvent.builder()
            .id("test")
            .tenantId("tenant1")
            .eventType(AuditEvent.AuditEventType.AUTHENTICATION)
            .category(AuditEvent.AuditEventCategory.SECURITY)
            .userId("user1")
            .resource("test")
            .action("test")
            .result(AuditEvent.AuditEventResult.FAILURE)
            .build();
        assertTrue(highPriorityEvent.isHighPriority());

        // Not high priority: security + success
        AuditEvent securitySuccessEvent = AuditEvent.builder()
            .id("test")
            .tenantId("tenant1")
            .eventType(AuditEvent.AuditEventType.AUTHENTICATION)
            .category(AuditEvent.AuditEventCategory.SECURITY)
            .userId("user1")
            .resource("test")
            .action("test")
            .result(AuditEvent.AuditEventResult.SUCCESS)
            .build();
        assertFalse(securitySuccessEvent.isHighPriority());

        // Not high priority: non-security + failed
        AuditEvent workflowFailedEvent = AuditEvent.builder()
            .id("test")
            .tenantId("tenant1")
            .eventType(AuditEvent.AuditEventType.FLOW_OPERATION)
            .category(AuditEvent.AuditEventCategory.WORKFLOW)
            .userId("user1")
            .resource("test")
            .action("test")
            .result(AuditEvent.AuditEventResult.FAILURE)
            .build();
        assertFalse(workflowFailedEvent.isHighPriority());
    }

    @Test
    void testWithDuration() {
        long startTime = System.currentTimeMillis() - 1000; // 1 second ago

        AuditEvent event = AuditEvent.builder()
            .id("test")
            .tenantId("tenant1")
            .eventType(AuditEvent.AuditEventType.AUTHENTICATION)
            .category(AuditEvent.AuditEventCategory.SECURITY)
            .userId("user1")
            .resource("test")
            .action("test")
            .result(AuditEvent.AuditEventResult.SUCCESS)
            .build();

        AuditEvent eventWithDuration = event.withDurationFromStartTime(startTime);

        assertNotNull(eventWithDuration.getDuration());
        assertTrue(eventWithDuration.getDuration() >= 1000); // At least 1 second
    }

    @Test
    void testWithError() {
        AuditEvent event = AuditEvent.builder()
            .id("test")
            .tenantId("tenant1")
            .eventType(AuditEvent.AuditEventType.AUTHENTICATION)
            .category(AuditEvent.AuditEventCategory.SECURITY)
            .userId("user1")
            .resource("test")
            .action("test")
            .result(AuditEvent.AuditEventResult.SUCCESS)
            .build();

        AuditEvent eventWithError = event.withError("Authentication failed");

        assertEquals("Authentication failed", eventWithError.getErrorMessage());
        assertEquals(AuditEvent.AuditEventResult.ERROR, eventWithError.getResult());
    }

    @Test
    void testImmutability() {
        AuditEvent originalEvent = AuditEvent.builder()
            .id("test")
            .tenantId("tenant1")
            .eventType(AuditEvent.AuditEventType.AUTHENTICATION)
            .category(AuditEvent.AuditEventCategory.SECURITY)
            .userId("user1")
            .resource("test")
            .action("test")
            .result(AuditEvent.AuditEventResult.SUCCESS)
            .build();

        AuditEvent modifiedEvent = originalEvent
            .withDescription("Modified description")
            .withError("Some error");

        // Original should be unchanged
        assertNull(originalEvent.getDescription());
        assertNull(originalEvent.getErrorMessage());
        assertEquals(AuditEvent.AuditEventResult.SUCCESS, originalEvent.getResult());

        // Modified should have changes
        assertEquals("Modified description", modifiedEvent.getDescription());
        assertEquals("Some error", modifiedEvent.getErrorMessage());
        assertEquals(AuditEvent.AuditEventResult.ERROR, modifiedEvent.getResult());
    }

    @Test
    void testTenantInterface() {
        AuditEvent event = AuditEvent.builder()
            .id("test")
            .tenantId("tenant-123")
            .eventType(AuditEvent.AuditEventType.AUTHENTICATION)
            .category(AuditEvent.AuditEventCategory.SECURITY)
            .userId("user1")
            .resource("test")
            .action("test")
            .result(AuditEvent.AuditEventResult.SUCCESS)
            .build();

        assertEquals("tenant-123", event.getTenantId());
    }
}
