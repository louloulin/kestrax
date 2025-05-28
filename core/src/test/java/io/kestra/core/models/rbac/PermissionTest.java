package io.kestra.core.models.rbac;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PermissionTest {

    @Test
    void testPermissionCode() {
        assertEquals("flow:create", Permission.FLOW_CREATE.getCode());
        assertEquals("user:read", Permission.USER_READ.getCode());
        assertEquals("system:admin", Permission.SYSTEM_ADMIN.getCode());
    }

    @Test
    void testPermissionDescription() {
        assertEquals("Create flows", Permission.FLOW_CREATE.getDescription());
        assertEquals("Read users", Permission.USER_READ.getDescription());
        assertEquals("System administration", Permission.SYSTEM_ADMIN.getDescription());
    }

    @Test
    void testFromCode() {
        assertEquals(Permission.FLOW_CREATE, Permission.fromCode("flow:create"));
        assertEquals(Permission.USER_READ, Permission.fromCode("user:read"));
        assertEquals(Permission.SYSTEM_ADMIN, Permission.fromCode("system:admin"));
    }

    @Test
    void testFromCodeInvalid() {
        assertThrows(IllegalArgumentException.class, () -> {
            Permission.fromCode("invalid:permission");
        });
    }

    @Test
    void testImplies() {
        // System admin implies all permissions
        assertTrue(Permission.SYSTEM_ADMIN.implies(Permission.FLOW_CREATE));
        assertTrue(Permission.SYSTEM_ADMIN.implies(Permission.USER_DELETE));
        assertTrue(Permission.SYSTEM_ADMIN.implies(Permission.EXECUTION_READ));

        // Same permission implies itself
        assertTrue(Permission.FLOW_READ.implies(Permission.FLOW_READ));
        assertTrue(Permission.USER_CREATE.implies(Permission.USER_CREATE));

        // Update permissions imply read permissions
        assertTrue(Permission.FLOW_UPDATE.implies(Permission.FLOW_READ));
        assertTrue(Permission.USER_UPDATE.implies(Permission.USER_READ));
        assertTrue(Permission.EXECUTION_UPDATE.implies(Permission.EXECUTION_READ));

        // Delete permissions imply read permissions
        assertTrue(Permission.FLOW_DELETE.implies(Permission.FLOW_READ));
        assertTrue(Permission.USER_DELETE.implies(Permission.USER_READ));
        assertTrue(Permission.EXECUTION_DELETE.implies(Permission.EXECUTION_READ));

        // Read permissions don't imply write permissions
        assertFalse(Permission.FLOW_READ.implies(Permission.FLOW_UPDATE));
        assertFalse(Permission.USER_READ.implies(Permission.USER_DELETE));
        assertFalse(Permission.EXECUTION_READ.implies(Permission.EXECUTION_CREATE));
    }

    @Test
    void testGetResourceType() {
        assertEquals("flow", Permission.FLOW_CREATE.getResourceType());
        assertEquals("user", Permission.USER_READ.getResourceType());
        assertEquals("execution", Permission.EXECUTION_DELETE.getResourceType());
        assertEquals("system", Permission.SYSTEM_ADMIN.getResourceType());
    }

    @Test
    void testGetAction() {
        assertEquals("create", Permission.FLOW_CREATE.getAction());
        assertEquals("read", Permission.USER_READ.getAction());
        assertEquals("delete", Permission.EXECUTION_DELETE.getAction());
        assertEquals("admin", Permission.SYSTEM_ADMIN.getAction());
    }
}
