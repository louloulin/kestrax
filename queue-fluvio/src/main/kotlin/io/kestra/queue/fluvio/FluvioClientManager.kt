package io.kestra.queue.fluvio

import com.infinyon.fluvio.Fluvio
import com.infinyon.fluvio.FluvioAdmin
import com.infinyon.fluvio.FluvioConfig
import com.infinyon.fluvio.FluvioProducer
import com.infinyon.fluvio.FluvioConsumer
import com.infinyon.fluvio.TopicSpec
import io.micronaut.context.annotation.Bean
import io.micronaut.context.annotation.Factory
import io.micronaut.context.event.ApplicationEventListener
import io.micronaut.context.event.StartupEvent
import io.micronaut.context.event.ShutdownEvent
import jakarta.inject.Singleton
import mu.KotlinLogging
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

private val logger = KotlinLogging.logger {}

/**
 * Manages Fluvio client connections and topic creation
 */
@Singleton
class FluvioClientManager(
    private val config: FluvioQueueConfiguration
) : ApplicationEventListener<StartupEvent>, ApplicationEventListener<ShutdownEvent> {
    
    private val isInitialized = AtomicBoolean(false)
    private val isShutdown = AtomicBoolean(false)
    
    private lateinit var fluvio: Fluvio
    private lateinit var admin: FluvioAdmin
    private val producers = ConcurrentHashMap<String, FluvioProducer>()
    private val consumers = ConcurrentHashMap<String, FluvioConsumer>()
    
    /**
     * Initialize Fluvio connection on application startup
     */
    override fun onApplicationEvent(event: StartupEvent) {
        if (isInitialized.compareAndSet(false, true)) {
            try {
                logger.info { "Initializing Fluvio client manager..." }
                initializeFluvio()
                createTopics()
                logger.info { "Fluvio client manager initialized successfully" }
            } catch (e: Exception) {
                logger.error(e) { "Failed to initialize Fluvio client manager" }
                isInitialized.set(false)
                throw e
            }
        }
    }
    
    /**
     * Cleanup resources on application shutdown
     */
    override fun onApplicationEvent(event: ShutdownEvent) {
        if (isShutdown.compareAndSet(false, true)) {
            logger.info { "Shutting down Fluvio client manager..." }
            cleanup()
            logger.info { "Fluvio client manager shutdown completed" }
        }
    }
    
    /**
     * Get or create a producer for the specified topic
     */
    fun getProducer(topicName: String): FluvioProducer {
        ensureInitialized()
        return producers.computeIfAbsent(topicName) { topic ->
            logger.debug { "Creating producer for topic: $topic" }
            fluvio.producer(topic)
        }
    }
    
    /**
     * Get or create a consumer for the specified topic
     */
    fun getConsumer(topicName: String, consumerGroup: String? = null): FluvioConsumer {
        ensureInitialized()
        val consumerKey = if (consumerGroup != null) "$topicName:$consumerGroup" else topicName
        
        return consumers.computeIfAbsent(consumerKey) { _ ->
            logger.debug { "Creating consumer for topic: $topicName, group: $consumerGroup" }
            if (consumerGroup != null) {
                fluvio.consumerWithConfig(topicName) {
                    groupId(consumerGroup)
                    autoOffsetReset(config.consumer.autoOffsetReset)
                    sessionTimeout(config.consumer.sessionTimeout.toMillis().toInt())
                    heartbeatInterval(config.consumer.heartbeatInterval.toMillis().toInt())
                }
            } else {
                fluvio.consumer(topicName)
            }
        }
    }
    
    /**
     * Check if Fluvio cluster is healthy
     */
    fun isHealthy(): Boolean {
        return try {
            if (!isInitialized.get() || isShutdown.get()) {
                false
            } else {
                // Simple health check by listing topics
                admin.listTopics().isNotEmpty() || admin.listTopics().isEmpty()
                true
            }
        } catch (e: Exception) {
            logger.warn(e) { "Fluvio health check failed" }
            false
        }
    }
    
    /**
     * Get admin client for topic management
     */
    fun getAdmin(): FluvioAdmin {
        ensureInitialized()
        return admin
    }
    
    private fun initializeFluvio() {
        val fluvioConfig = FluvioConfig.builder()
            .endpoint(config.clusterEndpoint)
            .build()
        
        fluvio = Fluvio.connect(fluvioConfig)
        admin = fluvio.admin()
        
        logger.info { "Connected to Fluvio cluster at ${config.clusterEndpoint}" }
    }
    
    private fun createTopics() {
        val requiredTopics = listOf(
            "executions",
            "worker-jobs", 
            "worker-task-results",
            "worker-trigger-results",
            "logs",
            "metrics",
            "flows",
            "templates",
            "execution-killed",
            "worker-instances",
            "worker-job-running",
            "triggers",
            "subflow-execution-results",
            "cluster-events",
            "subflow-execution-end"
        )
        
        val existingTopics = admin.listTopics().map { it.name }.toSet()
        
        requiredTopics.forEach { queueType ->
            val topicName = config.getTopicName(queueType)
            
            if (topicName !in existingTopics) {
                logger.info { "Creating topic: $topicName" }
                
                val topicSpec = TopicSpec.builder()
                    .partitions(config.getPartitions(queueType))
                    .replicationFactor(config.getReplicationFactor(queueType))
                    .build()
                
                try {
                    admin.createTopic(topicName, topicSpec)
                    logger.info { "Successfully created topic: $topicName" }
                } catch (e: Exception) {
                    logger.error(e) { "Failed to create topic: $topicName" }
                    throw e
                }
            } else {
                logger.debug { "Topic already exists: $topicName" }
            }
        }
    }
    
    private fun cleanup() {
        try {
            // Close all producers
            producers.values.forEach { producer ->
                try {
                    producer.close()
                } catch (e: Exception) {
                    logger.warn(e) { "Error closing producer" }
                }
            }
            producers.clear()
            
            // Close all consumers
            consumers.values.forEach { consumer ->
                try {
                    consumer.close()
                } catch (e: Exception) {
                    logger.warn(e) { "Error closing consumer" }
                }
            }
            consumers.clear()
            
            // Close admin client
            if (::admin.isInitialized) {
                admin.close()
            }
            
            // Close main Fluvio connection
            if (::fluvio.isInitialized) {
                fluvio.close()
            }
            
        } catch (e: Exception) {
            logger.error(e) { "Error during Fluvio cleanup" }
        }
    }
    
    private fun ensureInitialized() {
        if (!isInitialized.get()) {
            throw IllegalStateException("Fluvio client manager is not initialized")
        }
        if (isShutdown.get()) {
            throw IllegalStateException("Fluvio client manager has been shutdown")
        }
    }
}
