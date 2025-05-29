package io.kestra.core.services;

import io.kestra.core.models.audit.AuditEvent;
import io.kestra.core.models.rbac.Permission;
import io.kestra.core.models.PagedResults;
import io.micronaut.core.annotation.Nullable;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Service for managing audit events and security logging
 */
@Singleton
@Slf4j
public class AuditService {

    private final Map<String, AtomicLong> eventCounters = new ConcurrentHashMap<>();
    private final Map<String, Instant> lastEventTimes = new ConcurrentHashMap<>();

    /**
     * Record an audit event asynchronously
     */
    public CompletableFuture<Void> recordEvent(AuditEvent event) {
        return CompletableFuture.runAsync(() -> {
            try {
                // Validate event
                validateAuditEvent(event);

                // Update counters
                updateEventCounters(event);

                // Store event (placeholder - would integrate with actual storage)
                storeAuditEvent(event);

                // Log high-priority events immediately
                if (event.isHighPriority()) {
                    logHighPriorityEvent(event);
                }

                log.debug("Audit event recorded: {} - {} - {}",
                         event.getEventType(), event.getAction(), event.getResult());

            } catch (Exception e) {
                log.error("Failed to record audit event: {}", e.getMessage(), e);
            }
        });
    }

    /**
     * Record authentication event
     */
    public CompletableFuture<Void> recordAuthentication(String tenantId, String userId, String sessionId,
                                                       String ipAddress, boolean success) {
        AuditEvent.AuditEventResult result = success ?
            AuditEvent.AuditEventResult.SUCCESS : AuditEvent.AuditEventResult.FAILURE;

        AuditEvent event = AuditEvent.createAuthenticationEvent(tenantId, userId, sessionId, ipAddress, result);
        return recordEvent(event);
    }

    /**
     * Record authorization event
     */
    public CompletableFuture<Void> recordAuthorization(String tenantId, String userId, String resource,
                                                      Permission permission, boolean granted, @Nullable String namespace) {
        AuditEvent.AuditEventResult result = granted ?
            AuditEvent.AuditEventResult.SUCCESS : AuditEvent.AuditEventResult.DENIED;

        AuditEvent event = AuditEvent.createAuthorizationEvent(tenantId, userId, resource,
                                                              permission.getCode(), result, namespace);
        return recordEvent(event);
    }

    /**
     * Record permission change event
     */
    public CompletableFuture<Void> recordPermissionChange(String tenantId, String userId, String targetResource,
                                                         String targetId, String action, boolean success) {
        AuditEvent.AuditEventResult result = success ?
            AuditEvent.AuditEventResult.SUCCESS : AuditEvent.AuditEventResult.FAILURE;

        AuditEvent event = AuditEvent.createPermissionChangeEvent(tenantId, userId, targetResource,
                                                                 targetId, action, result);
        return recordEvent(event);
    }

    /**
     * Record user management event
     */
    public CompletableFuture<Void> recordUserManagement(String tenantId, String userId, String targetUserId,
                                                       String action, boolean success) {
        AuditEvent.AuditEventResult result = success ?
            AuditEvent.AuditEventResult.SUCCESS : AuditEvent.AuditEventResult.FAILURE;

        AuditEvent event = AuditEvent.createUserManagementEvent(tenantId, userId, targetUserId, action, result);
        return recordEvent(event);
    }

    /**
     * Record security event
     */
    public CompletableFuture<Void> recordSecurityEvent(String tenantId, String userId, String description,
                                                      boolean success, @Nullable String ipAddress) {
        AuditEvent.AuditEventResult result = success ?
            AuditEvent.AuditEventResult.SUCCESS : AuditEvent.AuditEventResult.FAILURE;

        AuditEvent event = AuditEvent.createSecurityEvent(tenantId, userId, description, result, ipAddress);
        return recordEvent(event);
    }

    /**
     * Get audit event statistics
     */
    public AuditStatistics getStatistics(String tenantId, @Nullable String userId) {
        // This would typically query from storage
        // For now, return statistics from in-memory counters

        long totalEvents = eventCounters.values().stream()
            .mapToLong(AtomicLong::get)
            .sum();

        long securityEvents = eventCounters.getOrDefault("SECURITY", new AtomicLong(0)).get();
        long failedEvents = eventCounters.getOrDefault("FAILED", new AtomicLong(0)).get();

        return new AuditStatistics(totalEvents, securityEvents, failedEvents,
                                 Instant.now().minus(24, ChronoUnit.HOURS));
    }

    /**
     * Get audit event statistics with time range
     */
    public ExtendedAuditStatistics getStatistics(String tenantId, Instant startDate, Instant endDate) {
        // Mock implementation
        return new ExtendedAuditStatistics(0L, 0L, 0L, 0L, new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
    }

    /**
     * Search audit events (placeholder implementation)
     */
    public List<AuditEvent> searchEvents(AuditSearchCriteria criteria) {
        // This would typically query from storage
        // For now, return empty list as placeholder
        log.debug("Searching audit events with criteria: {}", criteria);
        return List.of();
    }

    /**
     * Search audit logs with pagination and filtering.
     */
    public PagedResults<io.kestra.core.models.rbac.AuditEvent> searchAuditLogs(
        String tenantId,
        Object pageable,
        String userId,
        String action,
        String resource,
        String resourceId,
        String eventType,
        io.kestra.core.models.rbac.AuditEvent.AuditResult result,
        Instant startDate,
        Instant endDate,
        String query
    ) {
        // Mock implementation
        List<io.kestra.core.models.rbac.AuditEvent> events = new ArrayList<>();
        return PagedResults.of(events, 0L);
    }

    /**
     * Find audit event by ID.
     */
    public Optional<io.kestra.core.models.rbac.AuditEvent> findById(String tenantId, String auditId) {
        // Mock implementation
        return Optional.empty();
    }

    /**
     * Export audit logs.
     */
    public byte[] exportAuditLogs(
        String tenantId,
        String format,
        String userId,
        String action,
        String resource,
        Instant startDate,
        Instant endDate,
        int maxRecords
    ) {
        // Mock implementation
        return new byte[0];
    }

    /**
     * Get user timeline.
     */
    public List<io.kestra.core.models.rbac.AuditEvent> getUserTimeline(
        String tenantId,
        String userId,
        Instant startDate,
        Instant endDate,
        int limit
    ) {
        // Mock implementation
        return new ArrayList<>();
    }

    /**
     * Get resource history.
     */
    public List<io.kestra.core.models.rbac.AuditEvent> getResourceHistory(
        String tenantId,
        String resourceType,
        String resourceId,
        Instant startDate,
        Instant endDate,
        int limit
    ) {
        // Mock implementation
        return new ArrayList<>();
    }

    /**
     * Generate compliance report.
     */
    public ComplianceReport generateComplianceReport(
        String tenantId,
        String reportType,
        Instant startDate,
        Instant endDate
    ) {
        // Mock implementation
        return new ComplianceReport("STANDARD", 0L, 0L, new ArrayList<>(), new ArrayList<>());
    }

    /**
     * Validate audit event
     */
    private void validateAuditEvent(AuditEvent event) {
        if (event.getTenantId() == null || event.getTenantId().trim().isEmpty()) {
            throw new IllegalArgumentException("Tenant ID is required for audit event");
        }
        if (event.getUserId() == null || event.getUserId().trim().isEmpty()) {
            throw new IllegalArgumentException("User ID is required for audit event");
        }
        if (event.getEventType() == null) {
            throw new IllegalArgumentException("Event type is required for audit event");
        }
        if (event.getResult() == null) {
            throw new IllegalArgumentException("Event result is required for audit event");
        }
    }

    /**
     * Update event counters
     */
    private void updateEventCounters(AuditEvent event) {
        // Update total counter
        eventCounters.computeIfAbsent("TOTAL", k -> new AtomicLong(0)).incrementAndGet();

        // Update type counter
        eventCounters.computeIfAbsent(event.getEventType().name(), k -> new AtomicLong(0)).incrementAndGet();

        // Update category counter
        eventCounters.computeIfAbsent(event.getCategory().name(), k -> new AtomicLong(0)).incrementAndGet();

        // Update result counter
        eventCounters.computeIfAbsent(event.getResult().name(), k -> new AtomicLong(0)).incrementAndGet();

        // Update security events counter
        if (event.isSecurityEvent()) {
            eventCounters.computeIfAbsent("SECURITY", k -> new AtomicLong(0)).incrementAndGet();
        }

        // Update failed events counter
        if (event.isFailedEvent()) {
            eventCounters.computeIfAbsent("FAILED", k -> new AtomicLong(0)).incrementAndGet();
        }

        // Update last event time
        lastEventTimes.put(event.getEventType().name(), event.getTimestamp());
    }

    /**
     * Store audit event (placeholder implementation)
     */
    private void storeAuditEvent(AuditEvent event) {
        // This would typically store to database or audit log system
        // For now, just log the event
        log.info("AUDIT: {} - {} - {} - {} - {} - {}",
                event.getTimestamp(),
                event.getTenantId(),
                event.getUserId(),
                event.getEventType(),
                event.getAction(),
                event.getResult());
    }

    /**
     * Log high-priority events immediately
     */
    private void logHighPriorityEvent(AuditEvent event) {
        log.warn("HIGH PRIORITY AUDIT EVENT: {} - {} - {} - {} - {}",
                event.getEventType(),
                event.getUserId(),
                event.getAction(),
                event.getResult(),
                event.getDescription());
    }

    /**
     * Audit statistics
     */
    public static class AuditStatistics {
        private final long totalEvents;
        private final long securityEvents;
        private final long failedEvents;
        private final Instant since;

        public AuditStatistics(long totalEvents, long securityEvents, long failedEvents, Instant since) {
            this.totalEvents = totalEvents;
            this.securityEvents = securityEvents;
            this.failedEvents = failedEvents;
            this.since = since;
        }

        public long getTotalEvents() { return totalEvents; }
        public long getSecurityEvents() { return securityEvents; }
        public long getFailedEvents() { return failedEvents; }
        public Instant getSince() { return since; }

        @Override
        public String toString() {
            return String.format("AuditStatistics{total=%d, security=%d, failed=%d, since=%s}",
                               totalEvents, securityEvents, failedEvents, since);
        }
    }

    /**
     * Audit search criteria
     */
    public static class AuditSearchCriteria {
        private final String tenantId;
        private final String userId;
        private final AuditEvent.AuditEventType eventType;
        private final Instant fromTime;
        private final Instant toTime;

        public AuditSearchCriteria(String tenantId, String userId, AuditEvent.AuditEventType eventType,
                                 Instant fromTime, Instant toTime) {
            this.tenantId = tenantId;
            this.userId = userId;
            this.eventType = eventType;
            this.fromTime = fromTime;
            this.toTime = toTime;
        }

        public String getTenantId() { return tenantId; }
        public String getUserId() { return userId; }
        public AuditEvent.AuditEventType getEventType() { return eventType; }
        public Instant getFromTime() { return fromTime; }
        public Instant getToTime() { return toTime; }
    }

    /**
     * Compliance report data class.
     */
    public static class ComplianceReport {
        private final String reportType;
        private final Long totalEvents;
        private final Long complianceScore;
        private final List<Object> violations;
        private final List<Object> recommendations;

        public ComplianceReport(String reportType, Long totalEvents, Long complianceScore,
                              List<Object> violations, List<Object> recommendations) {
            this.reportType = reportType;
            this.totalEvents = totalEvents;
            this.complianceScore = complianceScore;
            this.violations = violations;
            this.recommendations = recommendations;
        }

        public String getReportType() { return reportType; }
        public Long getTotalEvents() { return totalEvents; }
        public Long getComplianceScore() { return complianceScore; }
        public List<Object> getViolations() { return violations; }
        public List<Object> getRecommendations() { return recommendations; }
    }

    /**
     * Extended audit statistics data class.
     */
    public static class ExtendedAuditStatistics {
        private final Long totalEvents;
        private final Long successfulEvents;
        private final Long failedEvents;
        private final Long uniqueUsers;
        private final List<Object> topActions;
        private final List<Object> topResources;
        private final List<Object> eventsByDay;

        public ExtendedAuditStatistics(Long totalEvents, Long successfulEvents, Long failedEvents,
                             Long uniqueUsers, List<Object> topActions, List<Object> topResources,
                             List<Object> eventsByDay) {
            this.totalEvents = totalEvents;
            this.successfulEvents = successfulEvents;
            this.failedEvents = failedEvents;
            this.uniqueUsers = uniqueUsers;
            this.topActions = topActions;
            this.topResources = topResources;
            this.eventsByDay = eventsByDay;
        }

        public Long getTotalEvents() { return totalEvents; }
        public Long getSuccessfulEvents() { return successfulEvents; }
        public Long getFailedEvents() { return failedEvents; }
        public Long getUniqueUsers() { return uniqueUsers; }
        public List<Object> getTopActions() { return topActions; }
        public List<Object> getTopResources() { return topResources; }
        public List<Object> getEventsByDay() { return eventsByDay; }
    }
}
