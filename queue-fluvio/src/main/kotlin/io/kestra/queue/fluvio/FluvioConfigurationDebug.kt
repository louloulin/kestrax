package io.kestra.queue.fluvio

import io.micronaut.context.ApplicationContext
import io.micronaut.context.annotation.Bean
import io.micronaut.context.annotation.Factory
import io.micronaut.context.annotation.Value
import io.micronaut.context.event.ApplicationEventListener
import io.micronaut.context.event.StartupEvent
import jakarta.inject.Singleton
import org.slf4j.LoggerFactory
import java.util.*

/**
 * Debug bean to verify configuration loading
 * This bean is ALWAYS created to help debug configuration issues
 */
@Factory
class FluvioConfigurationDebug : ApplicationEventListener<StartupEvent> {

    private val logger = LoggerFactory.getLogger(FluvioConfigurationDebug::class.java)

    init {
        logger.error("🔍 FluvioConfigurationDebug factory created! This should be visible!")
    }

    override fun onApplicationEvent(event: StartupEvent) {
        logger.error("🔍 FluvioConfigurationDebug: Application started!")
    }

    @Bean
    @Singleton
    fun debugConfiguration(
        @Value("\${kestra.queue.type:unknown}") queueType: String,
        @Value("\${kestra.queue.fluvio.cluster-endpoint:not-set}") clusterEndpoint: String,
        applicationContext: ApplicationContext
    ): String {
        logger.error("🔍 Configuration Debug:")
        logger.error("  - kestra.queue.type = '{}'", queueType)
        logger.error("  - kestra.queue.fluvio.cluster-endpoint = '{}'", clusterEndpoint)

        // 检查所有相关的配置属性
        val allQueueProps = applicationContext.environment.getProperties("kestra.queue")
        logger.error("🔍 All kestra.queue properties:")
        allQueueProps.forEach { (key, value) ->
            logger.error("  - {} = '{}'", key, value)
        }

        // 检查系统属性
        val systemQueueType = System.getProperty("kestra.queue.type")
        logger.error("🔍 System property kestra.queue.type = '{}'", systemQueueType)

        // 检查环境变量
        val envQueueType = System.getenv("KESTRA_QUEUE_TYPE")
        logger.error("🔍 Environment variable KESTRA_QUEUE_TYPE = '{}'", envQueueType)

        // 检查是否存在 FluvioQueueConfiguration bean
        try {
            val configExists = applicationContext.containsBean(FluvioQueueConfiguration::class.java)
            logger.error("🔍 FluvioQueueConfiguration bean exists: {}", configExists)

            if (configExists) {
                val config = applicationContext.getBean(FluvioQueueConfiguration::class.java)
                logger.error("🔍 FluvioQueueConfiguration loaded successfully:")
                logger.error("  - cluster-endpoint: {}", config.clusterEndpoint)
                logger.error("  - topic-prefix: {}", config.topicPrefix)
            }
        } catch (e: Exception) {
            logger.error("🔍 Error checking FluvioQueueConfiguration: {}", e.message)
        }

        return "debug-config"
    }
}
