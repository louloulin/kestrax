package io.kestra.webserver.controllers.api;

import io.kestra.core.models.rbac.AuditEvent;
import io.kestra.core.services.AuditService;
import io.kestra.core.services.TenantService;
import io.kestra.webserver.utils.PageableUtils;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.data.model.Pageable;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.*;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.micronaut.security.annotation.Secured;
import io.micronaut.validation.Validated;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import jakarta.validation.constraints.Min;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * REST API controller for audit log management
 */
@Controller("/api/v1/audit")
@Tag(name = "Audit", description = "Audit log management and compliance reporting")
@Validated
@Slf4j
public class AuditController {
    
    @Inject
    private AuditService auditService;
    
    @Inject
    private TenantService tenantService;
    
    /**
     * Search audit logs with pagination
     */
    @Get("/logs")
    @ExecuteOn(TaskExecutors.IO)
    @Operation(summary = "Search audit logs", description = "Search audit logs with filters and pagination")
    @Secured("AUDIT_READ")
    public HttpResponse<PagedResults<AuditEvent>> searchAuditLogs(
        @Parameter(description = "The current page") @QueryValue(defaultValue = "1") @Min(1) int page,
        @Parameter(description = "The current page size") @QueryValue(defaultValue = "50") @Min(1) int size,
        @Parameter(description = "Filter by user ID") @Nullable @QueryValue String userId,
        @Parameter(description = "Filter by action") @Nullable @QueryValue String action,
        @Parameter(description = "Filter by resource type") @Nullable @QueryValue String resourceType,
        @Parameter(description = "Filter by resource ID") @Nullable @QueryValue String resourceId,
        @Parameter(description = "Filter by namespace") @Nullable @QueryValue String namespace,
        @Parameter(description = "Filter by result") @Nullable @QueryValue AuditEvent.AuditResult result,
        @Parameter(description = "Start date (ISO format)") @Nullable @QueryValue String startDate,
        @Parameter(description = "End date (ISO format)") @Nullable @QueryValue String endDate,
        @Parameter(description = "Search query") @Nullable @QueryValue(value = "q") String query
    ) {
        try {
            String tenantId = tenantService.resolveTenant();
            Pageable pageable = PageableUtils.from(page, size);
            
            Instant start = startDate != null ? Instant.parse(startDate) : null;
            Instant end = endDate != null ? Instant.parse(endDate) : null;
            
            PagedResults<AuditEvent> auditLogs = auditService.searchAuditLogs(
                tenantId,
                pageable,
                userId,
                action,
                resourceType,
                resourceId,
                namespace,
                result,
                start,
                end,
                query
            );
            
            return HttpResponse.ok(auditLogs);
        } catch (Exception e) {
            log.error("Error searching audit logs", e);
            return HttpResponse.serverError();
        }
    }
    
    /**
     * Get audit log by ID
     */
    @Get("/logs/{auditId}")
    @ExecuteOn(TaskExecutors.IO)
    @Operation(summary = "Get audit log", description = "Get audit log by ID")
    @Secured("AUDIT_READ")
    public HttpResponse<AuditEvent> getAuditLog(@PathVariable String auditId) {
        try {
            String tenantId = tenantService.resolveTenant();
            Optional<AuditEvent> auditLog = auditService.findById(tenantId, auditId);
            
            return auditLog.map(HttpResponse::ok)
                          .orElse(HttpResponse.notFound());
        } catch (Exception e) {
            log.error("Error getting audit log: " + auditId, e);
            return HttpResponse.serverError();
        }
    }
    
    /**
     * Get audit statistics
     */
    @Get("/statistics")
    @ExecuteOn(TaskExecutors.IO)
    @Operation(summary = "Get audit statistics", description = "Get audit log statistics and metrics")
    @Secured("AUDIT_READ")
    public HttpResponse<AuditStatistics> getAuditStatistics(
        @Parameter(description = "Start date (ISO format)") @Nullable @QueryValue String startDate,
        @Parameter(description = "End date (ISO format)") @Nullable @QueryValue String endDate
    ) {
        try {
            String tenantId = tenantService.resolveTenant();
            
            Instant start = startDate != null ? Instant.parse(startDate) : 
                           LocalDate.now().minusDays(30).atStartOfDay().toInstant(ZoneOffset.UTC);
            Instant end = endDate != null ? Instant.parse(endDate) : Instant.now();
            
            AuditService.AuditStatistics stats = auditService.getStatistics(tenantId, start, end);
            
            AuditStatistics response = new AuditStatistics(
                stats.getTotalEvents(),
                stats.getSuccessfulEvents(),
                stats.getFailedEvents(),
                stats.getUniqueUsers(),
                stats.getTopActions(),
                stats.getTopResources(),
                stats.getEventsByDay()
            );
            
            return HttpResponse.ok(response);
        } catch (Exception e) {
            log.error("Error getting audit statistics", e);
            return HttpResponse.serverError();
        }
    }
    
    /**
     * Export audit logs
     */
    @Get(value = "/export", produces = {MediaType.APPLICATION_JSON, "text/csv", "application/pdf"})
    @ExecuteOn(TaskExecutors.IO)
    @Operation(summary = "Export audit logs", description = "Export audit logs in various formats")
    @Secured("AUDIT_READ")
    public HttpResponse<?> exportAuditLogs(
        @Parameter(description = "Export format") @QueryValue(defaultValue = "json") String format,
        @Parameter(description = "Filter by user ID") @Nullable @QueryValue String userId,
        @Parameter(description = "Filter by action") @Nullable @QueryValue String action,
        @Parameter(description = "Filter by resource type") @Nullable @QueryValue String resourceType,
        @Parameter(description = "Start date (ISO format)") @Nullable @QueryValue String startDate,
        @Parameter(description = "End date (ISO format)") @Nullable @QueryValue String endDate,
        @Parameter(description = "Maximum records") @QueryValue(defaultValue = "10000") @Min(1) int maxRecords
    ) {
        try {
            String tenantId = tenantService.resolveTenant();
            
            Instant start = startDate != null ? Instant.parse(startDate) : null;
            Instant end = endDate != null ? Instant.parse(endDate) : null;
            
            byte[] exportData = auditService.exportAuditLogs(
                tenantId,
                format,
                userId,
                action,
                resourceType,
                start,
                end,
                maxRecords
            );
            
            String filename = "audit_logs_" + Instant.now().getEpochSecond();
            String contentType;
            
            switch (format.toLowerCase()) {
                case "csv":
                    contentType = "text/csv";
                    filename += ".csv";
                    break;
                case "pdf":
                    contentType = "application/pdf";
                    filename += ".pdf";
                    break;
                default:
                    contentType = MediaType.APPLICATION_JSON;
                    filename += ".json";
            }
            
            return HttpResponse.ok(exportData)
                .contentType(contentType)
                .header("Content-Disposition", "attachment; filename=" + filename);
                
        } catch (Exception e) {
            log.error("Error exporting audit logs", e);
            return HttpResponse.serverError();
        }
    }
    
    /**
     * Get user activity timeline
     */
    @Get("/users/{userId}/timeline")
    @ExecuteOn(TaskExecutors.IO)
    @Operation(summary = "Get user timeline", description = "Get user activity timeline")
    @Secured("AUDIT_READ")
    public HttpResponse<List<AuditEvent>> getUserTimeline(
        @PathVariable String userId,
        @Parameter(description = "Start date (ISO format)") @Nullable @QueryValue String startDate,
        @Parameter(description = "End date (ISO format)") @Nullable @QueryValue String endDate,
        @Parameter(description = "Limit") @QueryValue(defaultValue = "100") @Min(1) int limit
    ) {
        try {
            String tenantId = tenantService.resolveTenant();
            
            Instant start = startDate != null ? Instant.parse(startDate) : 
                           LocalDate.now().minusDays(7).atStartOfDay().toInstant(ZoneOffset.UTC);
            Instant end = endDate != null ? Instant.parse(endDate) : Instant.now();
            
            List<AuditEvent> timeline = auditService.getUserTimeline(tenantId, userId, start, end, limit);
            
            return HttpResponse.ok(timeline);
        } catch (Exception e) {
            log.error("Error getting user timeline: " + userId, e);
            return HttpResponse.serverError();
        }
    }
    
    /**
     * Get resource access history
     */
    @Get("/resources/{resourceType}/{resourceId}/history")
    @ExecuteOn(TaskExecutors.IO)
    @Operation(summary = "Get resource history", description = "Get resource access and modification history")
    @Secured("AUDIT_READ")
    public HttpResponse<List<AuditEvent>> getResourceHistory(
        @PathVariable String resourceType,
        @PathVariable String resourceId,
        @Parameter(description = "Start date (ISO format)") @Nullable @QueryValue String startDate,
        @Parameter(description = "End date (ISO format)") @Nullable @QueryValue String endDate,
        @Parameter(description = "Limit") @QueryValue(defaultValue = "100") @Min(1) int limit
    ) {
        try {
            String tenantId = tenantService.resolveTenant();
            
            Instant start = startDate != null ? Instant.parse(startDate) : 
                           LocalDate.now().minusDays(30).atStartOfDay().toInstant(ZoneOffset.UTC);
            Instant end = endDate != null ? Instant.parse(endDate) : Instant.now();
            
            List<AuditEvent> history = auditService.getResourceHistory(
                tenantId, resourceType, resourceId, start, end, limit
            );
            
            return HttpResponse.ok(history);
        } catch (Exception e) {
            log.error("Error getting resource history: {} {}", resourceType, resourceId, e);
            return HttpResponse.serverError();
        }
    }
    
    /**
     * Get compliance report
     */
    @Get("/compliance/report")
    @ExecuteOn(TaskExecutors.IO)
    @Operation(summary = "Get compliance report", description = "Generate compliance report")
    @Secured("AUDIT_READ")
    public HttpResponse<ComplianceReport> getComplianceReport(
        @Parameter(description = "Report type") @QueryValue(defaultValue = "STANDARD") String reportType,
        @Parameter(description = "Start date (ISO format)") @Nullable @QueryValue String startDate,
        @Parameter(description = "End date (ISO format)") @Nullable @QueryValue String endDate
    ) {
        try {
            String tenantId = tenantService.resolveTenant();
            
            Instant start = startDate != null ? Instant.parse(startDate) : 
                           LocalDate.now().minusDays(30).atStartOfDay().toInstant(ZoneOffset.UTC);
            Instant end = endDate != null ? Instant.parse(endDate) : Instant.now();
            
            AuditService.ComplianceReport report = auditService.generateComplianceReport(
                tenantId, reportType, start, end
            );
            
            ComplianceReport response = new ComplianceReport(
                report.getReportId(),
                report.getReportType(),
                report.getGeneratedAt(),
                report.getPeriodStart(),
                report.getPeriodEnd(),
                report.getTotalEvents(),
                report.getSecurityEvents(),
                report.getAccessViolations(),
                report.getDataChanges(),
                report.getRecommendations()
            );
            
            return HttpResponse.ok(response);
        } catch (Exception e) {
            log.error("Error generating compliance report", e);
            return HttpResponse.serverError();
        }
    }
    
    // Response DTOs
    public static class AuditStatistics {
        private long totalEvents;
        private long successfulEvents;
        private long failedEvents;
        private long uniqueUsers;
        private Map<String, Long> topActions;
        private Map<String, Long> topResources;
        private Map<String, Long> eventsByDay;
        
        public AuditStatistics(long totalEvents, long successfulEvents, long failedEvents, 
                              long uniqueUsers, Map<String, Long> topActions, 
                              Map<String, Long> topResources, Map<String, Long> eventsByDay) {
            this.totalEvents = totalEvents;
            this.successfulEvents = successfulEvents;
            this.failedEvents = failedEvents;
            this.uniqueUsers = uniqueUsers;
            this.topActions = topActions;
            this.topResources = topResources;
            this.eventsByDay = eventsByDay;
        }
        
        // Getters
        public long getTotalEvents() { return totalEvents; }
        public long getSuccessfulEvents() { return successfulEvents; }
        public long getFailedEvents() { return failedEvents; }
        public long getUniqueUsers() { return uniqueUsers; }
        public Map<String, Long> getTopActions() { return topActions; }
        public Map<String, Long> getTopResources() { return topResources; }
        public Map<String, Long> getEventsByDay() { return eventsByDay; }
    }
    
    public static class ComplianceReport {
        private String reportId;
        private String reportType;
        private Instant generatedAt;
        private Instant periodStart;
        private Instant periodEnd;
        private long totalEvents;
        private long securityEvents;
        private long accessViolations;
        private long dataChanges;
        private List<String> recommendations;
        
        public ComplianceReport(String reportId, String reportType, Instant generatedAt,
                               Instant periodStart, Instant periodEnd, long totalEvents,
                               long securityEvents, long accessViolations, long dataChanges,
                               List<String> recommendations) {
            this.reportId = reportId;
            this.reportType = reportType;
            this.generatedAt = generatedAt;
            this.periodStart = periodStart;
            this.periodEnd = periodEnd;
            this.totalEvents = totalEvents;
            this.securityEvents = securityEvents;
            this.accessViolations = accessViolations;
            this.dataChanges = dataChanges;
            this.recommendations = recommendations;
        }
        
        // Getters
        public String getReportId() { return reportId; }
        public String getReportType() { return reportType; }
        public Instant getGeneratedAt() { return generatedAt; }
        public Instant getPeriodStart() { return periodStart; }
        public Instant getPeriodEnd() { return periodEnd; }
        public long getTotalEvents() { return totalEvents; }
        public long getSecurityEvents() { return securityEvents; }
        public long getAccessViolations() { return accessViolations; }
        public long getDataChanges() { return dataChanges; }
        public List<String> getRecommendations() { return recommendations; }
    }
}
