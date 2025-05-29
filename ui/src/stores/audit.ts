// Audit Store - Mock implementation for testing
export const useAuditStore = () => {
    return {
        fetchAuditLogs: async (_params: any = {}) => {
            return {
                data: [
                    {
                        id: "1",
                        timestamp: "2024-01-15T10:30:00Z",
                        eventType: "USER_LOGIN",
                        severity: "INFO",
                        userId: "admin",
                        userName: "Admin User",
                        userEmail: "admin@dataflare.com",
                        action: "User logged in successfully",
                        resource: "Authentication",
                        resourceId: null,
                        ipAddress: "192.168.1.100",
                        userAgent: "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36",
                        outcome: "SUCCESS",
                        details: {
                            loginMethod: "SSO",
                            provider: "Azure AD"
                        }
                    },
                    {
                        id: "2",
                        timestamp: "2024-01-15T10:25:00Z",
                        eventType: "FLOW_CREATE",
                        severity: "INFO",
                        userId: "developer",
                        userName: "Developer User",
                        userEmail: "dev@dataflare.com",
                        action: "Created new flow",
                        resource: "Flow",
                        resourceId: "data-processing-v2",
                        ipAddress: "192.168.1.101",
                        userAgent: "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36",
                        outcome: "SUCCESS",
                        details: {
                            flowName: "Data Processing Pipeline v2.1",
                            namespace: "production"
                        }
                    }
                ],
                total: 5
            };
        },
        
        fetchAuditStats: async () => {
            return {
                totalEvents: 15420,
                todayEvents: 156,
                successRate: 94.2,
                failureRate: 5.8,
                topEventTypes: [
                    {type: "USER_LOGIN", count: 3250},
                    {type: "FLOW_EXECUTE", count: 2890},
                    {type: "FLOW_CREATE", count: 1120},
                    {type: "PERMISSION_DENIED", count: 890},
                    {type: "SYSTEM_CONFIG", count: 450}
                ],
                severityDistribution: {
                    INFO: 12500,
                    WARNING: 2100,
                    ERROR: 820
                }
            };
        },
        
        exportAuditLogs: async (params: any) => {
            return {
                success: true,
                downloadUrl: "/api/audit/export/audit-logs-2024-01-15.csv",
                fileName: "audit-logs-2024-01-15.csv",
                recordCount: params.recordCount || 100
            };
        },
        
        fetchEventTypes: async () => {
            return [
                "USER_LOGIN",
                "USER_LOGOUT",
                "USER_CREATE",
                "USER_UPDATE",
                "USER_DELETE",
                "FLOW_CREATE",
                "FLOW_UPDATE",
                "FLOW_DELETE",
                "FLOW_EXECUTE",
                "PERMISSION_DENIED",
                "SYSTEM_CONFIG",
                "DATA_EXPORT",
                "TENANT_CREATE",
                "TENANT_UPDATE",
                "ROLE_ASSIGN",
                "ROLE_REVOKE"
            ];
        }
    };
};
