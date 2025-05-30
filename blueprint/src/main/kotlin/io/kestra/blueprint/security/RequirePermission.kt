package io.kestra.blueprint.security

import io.micronaut.aop.Around
import io.micronaut.context.annotation.Type
import kotlin.annotation.AnnotationRetention
import kotlin.annotation.AnnotationTarget

/**
 * 权限要求注解
 * 用于方法级别的权限控制
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Around
@Type(PermissionInterceptor::class)
annotation class RequirePermission(
    /**
     * 所需权限列表
     */
    val permissions: Array<String>,
    
    /**
     * 权限检查模式
     * ALL: 需要所有权限
     * ANY: 需要任意一个权限
     */
    val mode: PermissionMode = PermissionMode.ALL
)

/**
 * 权限检查模式
 */
enum class PermissionMode {
    ALL,  // 需要所有权限
    ANY   // 需要任意一个权限
}