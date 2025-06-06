package io.kestra.queue.fluvio

import com.infinyon.fluvio.*
import io.micronaut.context.event.ApplicationEventListener
import io.micronaut.context.event.StartupEvent
import io.micronaut.context.event.ShutdownEvent
import jakarta.inject.Singleton
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

private val logger = LoggerFactory.getLogger(FluvioClientManager::class.java)

/**
 * Manages Fluvio client connections and topic creation
 */
@Singleton
class FluvioClientManager(
    private val config: FluvioQueueConfiguration
) : ApplicationEventListener<StartupEvent> {

    private val isInitialized = AtomicBoolean(false)
    private val isShutdown = AtomicBoolean(false)

    private lateinit var fluvio: Fluvio
    private val producers = ConcurrentHashMap<String, TopicProducer>()
    private val consumers = ConcurrentHashMap<String, PartitionConsumer>()
    
    /**
     * Initialize Fluvio connection on application startup
     */
    override fun onApplicationEvent(event: StartupEvent) {
        if (isInitialized.compareAndSet(false, true)) {
            try {
                logger.info("Initializing Fluvio client manager...")
                initializeFluvio()
                createTopics()
                logger.info("Fluvio client manager initialized successfully")
            } catch (e: Exception) {
                logger.error("Failed to initialize Fluvio client manager", e)
                isInitialized.set(false)
                throw e
            }
        }
    }
    
    /**
     * Cleanup resources on application shutdown
     */
    fun shutdown() {
        if (isShutdown.compareAndSet(false, true)) {
            logger.info("Shutting down Fluvio client manager...")
            cleanup()
            logger.info("Fluvio client manager shutdown completed")
        }
    }
    
    /**
     * Get or create a producer for the specified topic
     */
    fun getProducer(topicName: String): TopicProducer {
        ensureInitialized()
        return producers.computeIfAbsent(topicName) { topic ->
            logger.debug("Creating producer for topic: {}", topic)
            fluvio.producer(topic)
        }
    }

    /**
     * Get or create a consumer for the specified topic
     * Note: Fluvio Java client uses partition-based consumers
     */
    fun getConsumer(topicName: String, consumerGroup: String? = null): PartitionConsumer {
        ensureInitialized()
        val consumerKey = if (consumerGroup != null) "$topicName:$consumerGroup" else topicName

        return consumers.computeIfAbsent(consumerKey) { _ ->
            logger.debug("Creating consumer for topic: {}, group: {}", topicName, consumerGroup)
            // Fluvio Java client uses partition-based consumers
            // For simplicity, we'll use partition 0 by default
            fluvio.consumer(topicName, 0)
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
                admin.listTopics()
                true
            }
        } catch (e: Exception) {
            logger.warn("Fluvio health check failed", e)
            false
        }
    }
    
    /**
     * Get Fluvio client for topic management
     * Note: Fluvio Java client doesn't have a separate admin client
     */
    fun getFluvio(): Fluvio {
        ensureInitialized()
        return fluvio
    }
    
    private fun initializeFluvio() {
        // Fluvio Java client uses a simple connect method
        fluvio = Fluvio.connect()

        logger.info("Connected to Fluvio cluster")
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
        
        // Note: Fluvio Java client doesn't provide topic management APIs
        // Topics need to be created externally using the Fluvio CLI
        // This is a limitation of the current Java client
        logger.info("Topic creation is handled externally via Fluvio CLI")
        logger.info("Required topics: {}", requiredTopics.joinToString(", "))
    }
    
    private fun cleanup() {
        try {
            // Close all producers
            producers.values.forEach { producer ->
                try {
                    producer.close()
                } catch (e: Exception) {
                    logger.warn("Error closing producer", e)
                }
            }
            producers.clear()

            // Close all consumers
            consumers.values.forEach { consumer ->
                try {
                    consumer.close()
                } catch (e: Exception) {
                    logger.warn("Error closing consumer", e)
                }
            }
            consumers.clear()

            // Close main Fluvio connection
            if (::fluvio.isInitialized) {
                fluvio.close()
            }

        } catch (e: Exception) {
            logger.error("Error during Fluvio cleanup", e)
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
