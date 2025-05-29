// Auth Type Definitions
export interface SSOProvider {
    id: string;
    name: string;
    type: "SAML" | "OIDC" | "OAuth2";
    enabled: boolean;
    clientId: string;
    issuerUrl: string;
    userCount: number;
    lastUsed?: string;
    createdAt: string;
}

export interface MFASettings {
    enabled: boolean;
    enforced: boolean;
    allowedMethods: string[];
    gracePeriodDays: number;
    maxAttempts: number;
}

export interface MFAStats {
    totalUsers: number;
    enabledUsers: number;
    adoptionRate: number;
    successRate: number;
}

export interface Token {
    id: string;
    name: string;
    type: "ACCESS" | "REFRESH" | "API" | "SERVICE";
    status: "active" | "expired" | "revoked";
    userId: string;
    userName: string;
    scopes: string[];
    expiresAt?: string;
    lastUsedAt?: string;
    createdAt: string;
}

export interface TokenStats {
    totalTokens: number;
    activeTokens: number;
    expiredTokens: number;
    revokedTokens: number;
}

export interface SSOProviderFormData {
    name: string;
    type: "SAML" | "OIDC" | "OAuth2";
    clientId: string;
    clientSecret: string;
    issuerUrl: string;
    redirectUrl: string;
    enabled: boolean;
}

export interface TokenFormData {
    name: string;
    type: "ACCESS" | "REFRESH" | "API" | "SERVICE";
    scopes: string[];
    expiresAt?: string;
}
