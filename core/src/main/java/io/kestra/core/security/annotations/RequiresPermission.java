package io.kestra.core.security.annotations;

import io.kestra.core.models.rbac.Permission;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to specify required permissions for accessing a method or class.
 * Can be applied to controller methods or entire controller classes.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiresPermission {
    
    /**
     * Required permissions for accessing the annotated method/class.
     * User must have ALL specified permissions (AND logic).
     */
    Permission[] value();
    
    /**
     * Optional namespace context for permission checking.
     * If not specified, will try to extract from request parameters or path variables.
     */
    String namespace() default "";
    
    /**
     * Whether to use OR logic instead of AND logic for multiple permissions.
     * Default is false (AND logic - user must have ALL permissions).
     * If true, user needs only ONE of the specified permissions.
     */
    boolean anyOf() default false;
    
    /**
     * Custom error message when permission check fails.
     * If not specified, a default message will be used.
     */
    String message() default "";
    
    /**
     * Whether to skip permission check for system administrators.
     * Default is true - system admins bypass all permission checks.
     */
    boolean allowSystemAdmin() default true;
}
