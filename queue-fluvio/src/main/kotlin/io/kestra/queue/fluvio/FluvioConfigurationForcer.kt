package io.kestra.queue.fluvio

import io.micronaut.context.annotation.Bean
import io.micronaut.context.annotation.Factory
import io.micronaut.context.annotation.Primary
import io.micronaut.context.annotation.Replaces
import io.micronaut.context.env.Environment
import jakarta.inject.Singleton
import org.slf4j.LoggerFactory

/**
 * Force Fluvio configuration to be loaded
 * This is a temporary workaround to ensure Fluvio queue is used
 */
@Factory
class FluvioConfigurationForcer {

    private val logger = LoggerFactory.getLogger(FluvioConfigurationForcer::class.java)

    @Bean
    @Singleton
    @Primary
    fun forceFluvioQueueType(environment: Environment): String {
        logger.info("🔧 FluvioConfigurationForcer: Forcing queue type to 'fluvio'")
        
        // 强制设置队列类型
        System.setProperty("kestra.queue.type", "fluvio")
        
        // 记录所有相关配置
        val queueType = environment.getProperty("kestra.queue.type", String::class.java).orElse("not-found")
        logger.info("🔧 Current queue type: {}", queueType)
        
        val allProps = environment.getProperties("kestra.queue")
        logger.info("🔧 All queue properties:")
        allProps.forEach { (key, value) ->
            logger.info("  - {} = {}", key, value)
        }
        
        return "fluvio-forced"
    }
}
