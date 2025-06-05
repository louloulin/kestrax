package io.kestra.blueprint.security

import io.micronaut.aop.MethodInterceptor
import io.micronaut.aop.MethodInvocationContext
import io.micronaut.http.HttpStatus
import io.micronaut.http.exceptions.HttpStatusException
import io.micronaut.security.authentication.Authentication
import io.micronaut.security.utils.SecurityService
import jakarta.inject.Singleton
import org.slf4j.LoggerFactory

/**
 * 权限拦截器
 * 实现方法级别的权限控制
 */
@Singleton
class PermissionInterceptor(
    private val tenantContext: TenantContext
) : MethodInterceptor<Any, Any> {
    
    private val logger = LoggerFactory.getLogger(PermissionInterceptor::class.java)
    
    override fun intercept(context: MethodInvocationContext<Any, Any>): Any? {
        // 租户上下文由TenantFilter设置，这里只需要验证权限
        return context.proceed()
    }


    
    /**
     * 检查是否拥有所有权限
     */
    private fun hasAllPermissions(authentication: Authentication, permissions: Array<String>): Boolean {
        val userPermissions = getUserPermissions(authentication)
        return permissions.all { permission ->
            userPermissions.contains(permission) || userPermissions.contains("*")
        }
    }
    
    /**
     * 检查是否拥有任意权限
     */
    private fun hasAnyPermission(authentication: Authentication, permissions: Array<String>): Boolean {
        val userPermissions = getUserPermissions(authentication)
        return userPermissions.contains("*") || permissions.any { permission ->
            userPermissions.contains(permission)
        }
    }
    
    /**
     * 获取用户权限列表
     */
    private fun getUserPermissions(authentication: Authentication): Set<String> {
        val roles = authentication.roles
        val permissions = mutableSetOf<String>()
        
        // 从角色中提取权限
        roles.forEach { role ->
            when (role) {
                "ADMIN" -> permissions.add("*") // 管理员拥有所有权限
                "BLUEPRINT_READ" -> permissions.add("blueprint:read")
                "BLUEPRINT_WRITE" -> {
                    permissions.add("blueprint:read")
                    permissions.add("blueprint:write")
                }
                "BLUEPRINT_DELETE" -> {
                    permissions.add("blueprint:read")
                    permissions.add("blueprint:write")
                    permissions.add("blueprint:delete")
                }
            }
        }
        
        // 从属性中获取额外权限
        val additionalPermissions = authentication.attributes["permissions"] as? List<String>
        additionalPermissions?.let { permissions.addAll(it) }
        
        return permissions
    }
    
    /**
     * 设置租户上下文
     */
    private fun setTenantContext(authentication: Authentication) {
        val tenantId = authentication.attributes["tenantId"] as? String
        val namespaceId = authentication.attributes["namespaceId"] as? String
        val userId = authentication.name
        
        tenantId?.let { tenantContext.setTenantId(it) }
        namespaceId?.let { tenantContext.setNamespaceId(it) }
        userId?.let { tenantContext.setUserId(it) }
    }
}