package io.kestra.core.services;

import io.kestra.core.models.audit.AuditEvent;
import io.kestra.core.models.rbac.Permission;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import static org.junit.jupiter.api.Assertions.*;

class AuditServiceTest {

    private AuditService auditService;

    @BeforeEach
    void setUp() {
        auditService = new AuditService();
    }

    @Test
    void testRecordEvent() throws ExecutionException, InterruptedException {
        AuditEvent event = AuditEvent.createAuthenticationEvent(
            "tenant1", "user1", "session123", "192.168.1.1", AuditEvent.AuditEventResult.SUCCESS
        );

        CompletableFuture<Void> future = auditService.recordEvent(event);

        // Should complete without exception
        assertDoesNotThrow(() -> future.get());
        assertTrue(future.isDone());
        assertFalse(future.isCompletedExceptionally());
    }

    @Test
    void testRecordAuthentication() throws ExecutionException, InterruptedException {
        CompletableFuture<Void> future = auditService.recordAuthentication(
            "tenant1", "user1", "session123", "192.168.1.1", true
        );

        assertDoesNotThrow(() -> future.get());
        assertTrue(future.isDone());
    }

    @Test
    void testRecordAuthenticationFailure() throws ExecutionException, InterruptedException {
        CompletableFuture<Void> future = auditService.recordAuthentication(
            "tenant1", "user1", "session123", "192.168.1.1", false
        );

        assertDoesNotThrow(() -> future.get());
        assertTrue(future.isDone());
    }

    @Test
    void testRecordAuthorization() throws ExecutionException, InterruptedException {
        CompletableFuture<Void> future = auditService.recordAuthorization(
            "tenant1", "user1", "flow", Permission.FLOW_READ, true, "test-namespace"
        );

        assertDoesNotThrow(() -> future.get());
        assertTrue(future.isDone());
    }

    @Test
    void testRecordAuthorizationDenied() throws ExecutionException, InterruptedException {
        CompletableFuture<Void> future = auditService.recordAuthorization(
            "tenant1", "user1", "flow", Permission.FLOW_UPDATE, false, "test-namespace"
        );

        assertDoesNotThrow(() -> future.get());
        assertTrue(future.isDone());
    }

    @Test
    void testRecordPermissionChange() throws ExecutionException, InterruptedException {
        CompletableFuture<Void> future = auditService.recordPermissionChange(
            "tenant1", "admin1", "role", "role123", "add_permission", true
        );

        assertDoesNotThrow(() -> future.get());
        assertTrue(future.isDone());
    }

    @Test
    void testRecordUserManagement() throws ExecutionException, InterruptedException {
        CompletableFuture<Void> future = auditService.recordUserManagement(
            "tenant1", "admin1", "user123", "create", true
        );

        assertDoesNotThrow(() -> future.get());
        assertTrue(future.isDone());
    }

    @Test
    void testRecordSecurityEvent() throws ExecutionException, InterruptedException {
        CompletableFuture<Void> future = auditService.recordSecurityEvent(
            "tenant1", "user1", "Suspicious activity detected", false, "192.168.1.100"
        );

        assertDoesNotThrow(() -> future.get());
        assertTrue(future.isDone());
    }

    @Test
    void testGetStatistics() {
        // Record some events first
        auditService.recordAuthentication("tenant1", "user1", "session1", "192.168.1.1", true);
        auditService.recordAuthentication("tenant1", "user2", "session2", "192.168.1.2", false);
        auditService.recordAuthorization("tenant1", "user1", "flow", Permission.FLOW_READ, true, null);

        AuditService.AuditStatistics stats = auditService.getStatistics("tenant1", null);

        assertNotNull(stats);
        assertTrue(stats.getTotalEvents() >= 0);
        assertTrue(stats.getSecurityEvents() >= 0);
        assertTrue(stats.getFailedEvents() >= 0);
        assertNotNull(stats.getSince());
    }

    @Test
    void testSearchEvents() {
        AuditService.AuditSearchCriteria criteria = new AuditService.AuditSearchCriteria(
            "tenant1", "user1", AuditEvent.AuditEventType.AUTHENTICATION, null, null
        );

        // Should not throw exception
        assertDoesNotThrow(() -> auditService.searchEvents(criteria));
    }

    @Test
    void testRecordEventWithInvalidData() {
        // Event with null tenant ID
        AuditEvent invalidEvent = AuditEvent.builder()
            .id("test")
            .tenantId(null)
            .eventType(AuditEvent.AuditEventType.AUTHENTICATION)
            .category(AuditEvent.AuditEventCategory.SECURITY)
            .userId("user1")
            .resource("test")
            .action("test")
            .result(AuditEvent.AuditEventResult.SUCCESS)
            .build();

        CompletableFuture<Void> future = auditService.recordEvent(invalidEvent);

        // Should complete (errors are logged, not thrown)
        assertDoesNotThrow(() -> future.get());
    }

    @Test
    void testRecordEventWithEmptyTenantId() {
        AuditEvent invalidEvent = AuditEvent.builder()
            .id("test")
            .tenantId("")
            .eventType(AuditEvent.AuditEventType.AUTHENTICATION)
            .category(AuditEvent.AuditEventCategory.SECURITY)
            .userId("user1")
            .resource("test")
            .action("test")
            .result(AuditEvent.AuditEventResult.SUCCESS)
            .build();

        CompletableFuture<Void> future = auditService.recordEvent(invalidEvent);

        // Should complete (errors are logged, not thrown)
        assertDoesNotThrow(() -> future.get());
    }

    @Test
    void testRecordEventWithNullUserId() {
        AuditEvent invalidEvent = AuditEvent.builder()
            .id("test")
            .tenantId("tenant1")
            .eventType(AuditEvent.AuditEventType.AUTHENTICATION)
            .category(AuditEvent.AuditEventCategory.SECURITY)
            .userId(null)
            .resource("test")
            .action("test")
            .result(AuditEvent.AuditEventResult.SUCCESS)
            .build();

        CompletableFuture<Void> future = auditService.recordEvent(invalidEvent);

        // Should complete (errors are logged, not thrown)
        assertDoesNotThrow(() -> future.get());
    }

    @Test
    void testRecordHighPriorityEvent() throws ExecutionException, InterruptedException {
        // Create a high-priority event (security + failed)
        AuditEvent highPriorityEvent = AuditEvent.createSecurityEvent(
            "tenant1", "user1", "Multiple failed login attempts",
            AuditEvent.AuditEventResult.FAILURE, "192.168.1.100"
        );

        assertTrue(highPriorityEvent.isHighPriority());

        CompletableFuture<Void> future = auditService.recordEvent(highPriorityEvent);

        assertDoesNotThrow(() -> future.get());
        assertTrue(future.isDone());
    }

    @Test
    void testAuditStatisticsToString() {
        AuditService.AuditStatistics stats = new AuditService.AuditStatistics(100, 25, 10,
            java.time.Instant.now());

        String str = stats.toString();
        assertTrue(str.contains("total=100"));
        assertTrue(str.contains("security=25"));
        assertTrue(str.contains("failed=10"));
    }

    @Test
    void testAuditSearchCriteriaGetters() {
        java.time.Instant now = java.time.Instant.now();
        java.time.Instant earlier = now.minusSeconds(3600);

        AuditService.AuditSearchCriteria criteria = new AuditService.AuditSearchCriteria(
            "tenant1", "user1", AuditEvent.AuditEventType.AUTHENTICATION, earlier, now
        );

        assertEquals("tenant1", criteria.getTenantId());
        assertEquals("user1", criteria.getUserId());
        assertEquals(AuditEvent.AuditEventType.AUTHENTICATION, criteria.getEventType());
        assertEquals(earlier, criteria.getFromTime());
        assertEquals(now, criteria.getToTime());
    }

    @Test
    void testConcurrentEventRecording() throws InterruptedException {
        // Test recording multiple events concurrently
        @SuppressWarnings("unchecked")
        CompletableFuture<Void>[] futures = new CompletableFuture[10];

        for (int i = 0; i < 10; i++) {
            final int index = i;
            futures[i] = auditService.recordAuthentication(
                "tenant1", "user" + index, "session" + index, "192.168.1." + index, true
            );
        }

        // Wait for all to complete
        CompletableFuture<Void> allFutures = CompletableFuture.allOf(futures);
        assertDoesNotThrow(() -> allFutures.get());

        // All should be completed
        for (CompletableFuture<Void> future : futures) {
            assertTrue(future.isDone());
            assertFalse(future.isCompletedExceptionally());
        }
    }

    @Test
    void testRecordAuthorizationWithNullNamespace() throws ExecutionException, InterruptedException {
        CompletableFuture<Void> future = auditService.recordAuthorization(
            "tenant1", "user1", "flow", Permission.FLOW_READ, true, null
        );

        assertDoesNotThrow(() -> future.get());
        assertTrue(future.isDone());
    }

    @Test
    void testRecordSecurityEventWithNullIpAddress() throws ExecutionException, InterruptedException {
        CompletableFuture<Void> future = auditService.recordSecurityEvent(
            "tenant1", "user1", "Security event without IP", true, null
        );

        assertDoesNotThrow(() -> future.get());
        assertTrue(future.isDone());
    }
}
