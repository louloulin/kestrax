package io.kestra.blueprint.security

import io.micronaut.http.HttpRequest
import io.micronaut.http.MutableHttpResponse
import io.micronaut.http.annotation.Filter
import io.micronaut.http.filter.HttpServerFilter
import io.micronaut.http.filter.ServerFilterChain
import org.reactivestreams.Publisher
import reactor.core.publisher.Mono
import jakarta.inject.Singleton
import org.slf4j.LoggerFactory

/**
 * 租户过滤器
 * 从HTTP请求头中提取租户信息并设置到TenantContext中
 */
@Filter("/**")
@Singleton
class TenantFilter(
    private val tenantContext: TenantContext
) : HttpServerFilter {
    
    private val logger = LoggerFactory.getLogger(TenantFilter::class.java)
    
    companion object {
        const val HEADER_TENANT_ID = "X-Tenant-Id"
        const val HEADER_NAMESPACE_ID = "X-Namespace-Id"
        const val HEADER_USER_ID = "X-User-Id"
    }
    
    override fun doFilter(request: HttpRequest<*>, chain: ServerFilterChain): Publisher<MutableHttpResponse<*>> {
        // 从请求头中提取租户信息
        val tenantId = request.headers.get(HEADER_TENANT_ID) ?: "default-tenant"
        val namespaceId = request.headers.get(HEADER_NAMESPACE_ID) ?: "official"
        val userId = request.headers.get(HEADER_USER_ID) ?: "system"
        
        // 设置租户上下文
        tenantContext.setTenantId(tenantId)
        tenantContext.setNamespaceId(namespaceId)
        tenantContext.setUserId(userId)
        
        logger.debug("设置租户上下文 - tenantId: {}, namespaceId: {}, userId: {}", tenantId, namespaceId, userId)
        
        return Mono.fromCallable {
            chain.proceed(request)
        }.flatMapMany { publisher ->
            publisher
        }.doFinally {
            // 请求处理完成后清理上下文
            tenantContext.clear()
        }
    }
}
