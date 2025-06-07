package io.kestra.queue.fluvio

import io.micronaut.context.ApplicationContext
import io.micronaut.context.annotation.Bean
import io.micronaut.context.annotation.Factory
import io.micronaut.context.annotation.Value
import jakarta.inject.Singleton
import org.slf4j.LoggerFactory
import java.util.*

/**
 * Debug bean to verify configuration loading
 * This bean is ALWAYS created to help debug configuration issues
 */
@Factory
class FluvioConfigurationDebug {

    private val logger = LoggerFactory.getLogger(FluvioConfigurationDebug::class.java)

    init {
        logger.info("🔍 FluvioConfigurationDebug factory created!")
    }

    @Bean
    @Singleton
    fun debugConfiguration(
        @Value("\${kestra.queue.type:unknown}") queueType: String,
        @Value("\${kestra.queue.fluvio.cluster-endpoint:not-set}") clusterEndpoint: String,
        applicationContext: ApplicationContext
    ): String {
        logger.info("🔍 Configuration Debug:")
        logger.info("  - kestra.queue.type = '{}'", queueType)
        logger.info("  - kestra.queue.fluvio.cluster-endpoint = '{}'", clusterEndpoint)

        // 检查所有相关的配置属性
        val allQueueProps = applicationContext.environment.getProperties("kestra.queue")
        logger.info("🔍 All kestra.queue properties:")
        allQueueProps.forEach { (key, value) ->
            logger.info("  - {} = '{}'", key, value)
        }

        // 检查环境变量
        val environments = applicationContext.environment.activeNames
        logger.info("🔍 Active environments: {}", environments)

        if (queueType == "fluvio") {
            logger.info("✅ Fluvio queue type detected!")
        } else {
            logger.warn("❌ Queue type is '{}', not 'fluvio'", queueType)
        }

        return "debug-config"
    }
}
