package io.kestra.queue.fluvio

import io.micronaut.context.condition.Condition
import io.micronaut.context.condition.ConditionContext

/**
 * 共享的 Fluvio 启用条件类
 * 
 * 这个条件类会检查：
 * 1. 系统属性 kestra.queue.type
 * 2. 环境变量 KESTRA_QUEUE_TYPE
 * 3. 配置属性 kestra.queue.type
 * 
 * 只要其中任何一个设置为 "fluvio"，就启用 Fluvio 相关的 bean
 */
class FluvioEnabledCondition : Condition {
    override fun matches(context: ConditionContext<*>): Boolean {
        // 1. 检查系统属性
        val systemProperty = System.getProperty("kestra.queue.type")
        if (systemProperty == "fluvio") {
            return true
        }
        
        // 2. 检查环境变量
        val envVar = System.getenv("KESTRA_QUEUE_TYPE")
        if (envVar == "fluvio") {
            return true
        }
        
        // 3. 检查配置属性
        val configProperty = context.getProperty("kestra.queue.type", String::class.java)
        if (configProperty.isPresent && configProperty.get() == "fluvio") {
            return true
        }
        
        // 默认情况下不启用
        return false
    }
}
