// RBAC Store - Mock implementation for testing
export const useRBACStore = () => {
    return {
        fetchUsers: async (_params: any = {}) => {
            return {
                data: [
                    {
                        id: "1",
                        username: "admin",
                        email: "admin@dataflare.com",
                        firstName: "Admin",
                        lastName: "User",
                        enabled: true,
                        roles: ["admin"],
                        groups: ["administrators"],
                        createdAt: "2024-01-01T00:00:00Z",
                        lastLoginAt: "2024-01-15T10:30:00Z"
                    },
                    {
                        id: "2",
                        username: "developer",
                        email: "dev@dataflare.com",
                        firstName: "Developer",
                        lastName: "User",
                        enabled: true,
                        roles: ["developer"],
                        groups: ["developers"],
                        createdAt: "2024-01-02T00:00:00Z",
                        lastLoginAt: "2024-01-15T09:15:00Z"
                    }
                ],
                total: 2
            };
        },
        
        fetchRoles: async (_params: any = {}) => {
            return {
                data: [
                    {
                        id: "1",
                        name: "admin",
                        displayName: "Administrator",
                        description: "Full system access",
                        permissions: ["*"],
                        userCount: 1,
                        enabled: true,
                        createdAt: "2024-01-01T00:00:00Z"
                    },
                    {
                        id: "2",
                        name: "developer",
                        displayName: "Developer",
                        description: "Development access",
                        permissions: ["FLOW_READ", "FLOW_CREATE", "FLOW_UPDATE"],
                        userCount: 5,
                        enabled: true,
                        createdAt: "2024-01-01T00:00:00Z"
                    }
                ],
                total: 2
            };
        },
        
        fetchGroups: async (_params: any = {}) => {
            return {
                data: [
                    {
                        id: "1",
                        name: "administrators",
                        displayName: "Administrators",
                        description: "System administrators group",
                        memberCount: 2,
                        roles: ["admin"],
                        enabled: true,
                        autoAssign: false,
                        createdAt: "2024-01-01T00:00:00Z"
                    }
                ],
                total: 1
            };
        },
        
        fetchBindings: async (_params: any = {}) => {
            return {
                data: [
                    {
                        id: "1",
                        name: "admin-global-binding",
                        subjectType: "user",
                        subjectName: "admin",
                        roleRef: "admin",
                        scope: "global",
                        namespace: null,
                        createdAt: "2024-01-01T00:00:00Z"
                    }
                ],
                total: 1
            };
        },
        
        // CRUD operations
        createUser: async (userData: any) => ({id: Date.now().toString(), ...userData}),
        updateUser: async (_id: string, userData: any) => ({id: _id, ...userData}),
        deleteUser: async (_id: string) => ({success: true}),
        
        createRole: async (roleData: any) => ({id: Date.now().toString(), ...roleData}),
        updateRole: async (_id: string, roleData: any) => ({id: _id, ...roleData}),
        deleteRole: async (_id: string) => ({success: true}),
        
        createGroup: async (groupData: any) => ({id: Date.now().toString(), ...groupData}),
        updateGroup: async (_id: string, groupData: any) => ({id: _id, ...groupData}),
        deleteGroup: async (_id: string) => ({success: true}),
        
        createBinding: async (bindingData: any) => ({id: Date.now().toString(), ...bindingData}),
        updateBinding: async (_id: string, bindingData: any) => ({id: _id, ...bindingData}),
        deleteBinding: async (_id: string) => ({success: true})
    };
};
