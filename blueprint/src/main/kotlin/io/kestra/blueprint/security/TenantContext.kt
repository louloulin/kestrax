package io.kestra.blueprint.security

import io.micronaut.context.annotation.Context
import jakarta.inject.Singleton

/**
 * 租户上下文
 * 用于在请求处理过程中传递租户信息
 */
@Singleton
@Context
class TenantContext {
    
    private val tenantIdThreadLocal = ThreadLocal<String>()
    private val namespaceIdThreadLocal = ThreadLocal<String>()
    private val userIdThreadLocal = ThreadLocal<String>()
    
    /**
     * 设置当前租户ID
     */
    fun setTenantId(tenantId: String) {
        tenantIdThreadLocal.set(tenantId)
    }
    
    /**
     * 获取当前租户ID
     */
    fun getTenantId(): String? {
        return tenantIdThreadLocal.get()
    }
    
    /**
     * 设置当前命名空间ID
     */
    fun setNamespaceId(namespaceId: String) {
        namespaceIdThreadLocal.set(namespaceId)
    }
    
    /**
     * 获取当前命名空间ID
     */
    fun getNamespaceId(): String? {
        return namespaceIdThreadLocal.get()
    }
    
    /**
     * 设置当前用户ID
     */
    fun setUserId(userId: String) {
        userIdThreadLocal.set(userId)
    }
    
    /**
     * 获取当前用户ID
     */
    fun getUserId(): String? {
        return userIdThreadLocal.get()
    }
    
    /**
     * 清理当前线程的上下文信息
     */
    fun clear() {
        tenantIdThreadLocal.remove()
        namespaceIdThreadLocal.remove()
        userIdThreadLocal.remove()
    }
    
    /**
     * 检查是否有有效的租户上下文
     */
    fun hasValidContext(): Boolean {
        return getTenantId() != null && getNamespaceId() != null && getUserId() != null
    }
}