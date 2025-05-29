// Tenant Type Definitions
export interface Tenant {
    id: string;
    name: string;
    description: string;
    enabled: boolean;
    subscriptionPlan: "FREE" | "BASIC" | "PROFESSIONAL" | "ENTERPRISE";
    userCount: number;
    flowCount: number;
    executionCount: number;
    storageUsed: number;
    storageLimit: number;
    bandwidthUsed: number;
    bandwidthLimit: number;
    logo?: string;
    createdAt: string;
    lastActiveAt: string;
}

export interface TenantStats {
    totalFlows: number;
    totalExecutions: number;
    totalUsers: number;
    storageUsed: number;
    storageLimit: number;
    bandwidthUsed: number;
    bandwidthLimit: number;
    apiCallsUsed: number;
    apiCallsLimit: number;
    flowsChange: number;
    executionsChange: number;
    usersChange: number;
}

export interface Activity {
    id: string;
    type: string;
    title: string;
    description: string;
    status: "success" | "warning" | "error" | "pending";
    timestamp: string;
}

export interface TenantFormData {
    name: string;
    description: string;
    subscriptionPlan: "FREE" | "BASIC" | "PROFESSIONAL" | "ENTERPRISE";
    enabled: boolean;
    storageLimit: number;
    bandwidthLimit: number;
    apiCallsLimit: number;
}
