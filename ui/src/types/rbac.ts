// RBAC Type Definitions
export interface User {
    id: string;
    username: string;
    email: string;
    firstName: string;
    lastName: string;
    enabled: boolean;
    roles: string[];
    groups: string[];
    createdAt: string;
    lastLoginAt?: string;
}

export interface Role {
    id: string;
    name: string;
    displayName: string;
    description: string;
    permissions: string[];
    userCount: number;
    enabled: boolean;
    createdAt: string;
}

export interface Group {
    id: string;
    name: string;
    displayName: string;
    description: string;
    memberCount: number;
    roles: string[];
    enabled: boolean;
    autoAssign: boolean;
    createdAt: string;
}

export interface PermissionBinding {
    id: string;
    name: string;
    subjectType: "user" | "group";
    subjectName: string;
    roleRef: string;
    scope: "global" | "namespace";
    namespace?: string;
    createdAt: string;
}

export interface Permission {
    id: string;
    name: string;
    resource: string;
    action: string;
    description: string;
}

export interface UserFormData {
    username: string;
    email: string;
    firstName: string;
    lastName: string;
    password?: string;
    enabled: boolean;
    roles: string[];
    groups: string[];
}

export interface RoleFormData {
    name: string;
    displayName: string;
    description: string;
    permissions: string[];
    enabled: boolean;
}

export interface GroupFormData {
    name: string;
    displayName: string;
    description: string;
    roles: string[];
    enabled: boolean;
    autoAssign: boolean;
}

export interface BindingFormData {
    name: string;
    subjectType: "user" | "group";
    subjectName: string;
    roleRef: string;
    scope: "global" | "namespace";
    namespace?: string;
}
