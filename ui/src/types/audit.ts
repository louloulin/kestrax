// Audit Type Definitions
export interface AuditEvent {
    id: string;
    timestamp: string;
    eventType: string;
    severity: "INFO" | "WARNING" | "ERROR";
    userId: string;
    userName: string;
    userEmail: string;
    action: string;
    resource: string;
    resourceId?: string;
    ipAddress: string;
    userAgent: string;
    outcome: "SUCCESS" | "FAILURE";
    details: Record<string, any>;
}

export interface AuditStats {
    totalEvents: number;
    todayEvents: number;
    successRate: number;
    failureRate: number;
    topEventTypes: Array<{
        type: string;
        count: number;
    }>;
    severityDistribution: {
        INFO: number;
        WARNING: number;
        ERROR: number;
    };
}

export interface AuditFilter {
    startDate?: string;
    endDate?: string;
    eventTypes?: string[];
    severities?: string[];
    userIds?: string[];
    resources?: string[];
    outcomes?: string[];
    searchQuery?: string;
}

export interface ExportResult {
    success: boolean;
    downloadUrl: string;
    fileName: string;
    recordCount: number;
}
