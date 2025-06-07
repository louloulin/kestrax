package io.kestra.queue.fluvio

import io.micronaut.context.ApplicationContext
import io.micronaut.context.annotation.Factory
import io.micronaut.context.env.Environment
import io.micronaut.context.event.ApplicationEventListener
import io.micronaut.context.event.StartupEvent
import org.slf4j.LoggerFactory

/**
 * Force Fluvio configuration to be loaded
 * This is a temporary workaround to ensure Fluvio queue is used
 */
@Factory
class FluvioConfigurationForcer : ApplicationEventListener<StartupEvent> {

    private val logger = LoggerFactory.getLogger(FluvioConfigurationForcer::class.java)

    init {
        // 在类初始化时就强制设置系统属性
        logger.error("🔧 FluvioConfigurationForcer: Initializing with forced queue type")
        System.setProperty("kestra.queue.type", "fluvio")
    }

    override fun onApplicationEvent(event: StartupEvent) {
        logger.error("🔧 FluvioConfigurationForcer: Application startup event received")

        // 最后一次强制设置
        System.setProperty("kestra.queue.type", "fluvio")

        // 验证配置是否正确加载
        val applicationContext = event.source as ApplicationContext
        val environment = applicationContext.getBean(Environment::class.java)
        val queueType = environment.getProperty("kestra.queue.type", String::class.java).orElse("unknown")
        logger.error("🔧 Final queue type at startup: {}", queueType)

        if (queueType != "fluvio") {
            logger.error("🚨 WARNING: Queue type is not 'fluvio'! Current: {}", queueType)

            // 记录所有相关配置以便调试
            val allProps = environment.getProperties("kestra.queue")
            logger.error("🔧 All queue properties:")
            allProps.forEach { (key, value) ->
                logger.error("  - {} = {}", key, value)
            }

            // 检查系统属性
            val systemProp = System.getProperty("kestra.queue.type")
            logger.error("🔧 System property kestra.queue.type = {}", systemProp)

            // 检查环境变量
            val envVar = System.getenv("KESTRA_QUEUE_TYPE")
            logger.error("🔧 Environment variable KESTRA_QUEUE_TYPE = {}", envVar)
        } else {
            logger.error("✅ SUCCESS: Queue type is correctly set to 'fluvio'")
        }
    }
}
