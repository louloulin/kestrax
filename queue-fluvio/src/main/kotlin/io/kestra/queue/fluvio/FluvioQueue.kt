package io.kestra.queue.fluvio

import com.infinyon.fluvio.FluvioConsumer
import com.infinyon.fluvio.FluvioProducer
import com.infinyon.fluvio.Record
import io.kestra.core.exceptions.DeserializationException
import io.kestra.core.metrics.MetricRegistry
import io.kestra.core.queues.QueueException
import io.kestra.core.queues.QueueInterface
import io.kestra.core.queues.QueueService
import io.kestra.core.utils.Either
import io.kestra.queue.fluvio.serialization.ProtobufSerializer
import kotlinx.coroutines.*
import mu.KotlinLogging
import java.io.IOException
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.function.Consumer

private val logger = KotlinLogging.logger {}

/**
 * Fluvio implementation of Kestra's QueueInterface
 * 
 * This implementation provides full compatibility with the existing QueueInterface
 * while leveraging Fluvio's high-performance streaming capabilities.
 */
class FluvioQueue<T>(
    private val messageType: Class<T>,
    private val queueTypeName: String,
    private val clientManager: FluvioClientManager,
    private val serializer: ProtobufSerializer,
    private val queueService: QueueService,
    private val metricRegistry: MetricRegistry,
    private val config: FluvioQueueConfiguration
) : QueueInterface<T> {
    
    private val isClosed = AtomicBoolean(false)
    private val isPaused = AtomicBoolean(false)
    private val topicName = config.getTopicName(queueTypeName)
    
    // Coroutine scope for async operations
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    /**
     * Emit a message to the queue
     * Fully compatible with existing QueueInterface.emit()
     */
    override fun emit(consumerGroup: String?, message: T) {
        if (isClosed.get() || isPaused.get()) {
            throw QueueException("Queue is closed or paused")
        }
        
        try {
            val key = queueService.key(message) ?: generateKey()
            val data = serializer.serialize(message!!)
            val producer = getProducer()
            
            // Send message synchronously to maintain compatibility
            val future = producer.send(key, data)
            future.get(config.producer.requestTimeout.toMillis(), TimeUnit.MILLISECONDS)
            
            // Update metrics
            metricRegistry.counter("queue.emit.success", "queue_type", queueTypeName).increment()
            
            logger.debug { "Successfully emitted message to topic $topicName with key $key" }
            
        } catch (e: Exception) {
            metricRegistry.counter("queue.emit.error", "queue_type", queueTypeName).increment()
            logger.error(e) { "Failed to emit message to topic $topicName" }
            throw QueueException("Failed to emit to Fluvio", e)
        }
    }
    
    /**
     * Emit a message asynchronously
     * Fully compatible with existing QueueInterface.emitAsync()
     */
    override fun emitAsync(consumerGroup: String?, message: T) {
        if (isClosed.get() || isPaused.get()) {
            throw QueueException("Queue is closed or paused")
        }
        
        scope.launch {
            try {
                emit(consumerGroup, message)
            } catch (e: QueueException) {
                logger.error(e) { "Async emit failed for topic $topicName" }
            }
        }
    }
    
    /**
     * Delete operation - no-op for Fluvio as messages are automatically cleaned up
     * Maintains compatibility with existing QueueInterface.delete()
     */
    override fun delete(consumerGroup: String?, message: T) {
        // No-op for Fluvio - messages are automatically cleaned up based on retention policy
        // This maintains compatibility with the existing JDBC queue behavior
        logger.debug { "Delete operation called for topic $topicName (no-op in Fluvio)" }
    }
    
    /**
     * Receive messages from the queue
     * Fully compatible with existing QueueInterface.receive()
     */
    override fun receive(
        consumerGroup: String?,
        consumer: Consumer<Either<T, DeserializationException>>,
        forUpdate: Boolean
    ): Runnable {
        val actualTopicName = buildTopicName(consumerGroup)
        
        return Runnable {
            scope.launch {
                try {
                    val fluvioConsumer = getConsumer(actualTopicName, consumerGroup)
                    
                    // Start consuming messages
                    while (!isClosed.get() && !isPaused.get()) {
                        try {
                            val records = fluvioConsumer.poll(config.consumer.fetchMaxWait.toMillis())
                            
                            records.forEach { record ->
                                processRecord(record, consumer)
                            }
                            
                        } catch (e: Exception) {
                            if (!isClosed.get()) {
                                logger.error(e) { "Error polling messages from topic $actualTopicName" }
                                delay(1000) // Brief delay before retrying
                            }
                        }
                    }
                } catch (e: Exception) {
                    logger.error(e) { "Consumer error for topic $actualTopicName" }
                }
            }
        }
    }
    
    /**
     * Receive messages by queue type (compatibility method)
     */
    override fun receive(
        consumerGroup: String?,
        queueType: Class<*>?,
        consumer: Consumer<Either<T, DeserializationException>>,
        forUpdate: Boolean
    ): Runnable {
        // Use the queue type name if provided, otherwise use the configured topic name
        val topicName = queueType?.let { config.getTopicName(it.simpleName) } ?: this.topicName
        
        return Runnable {
            scope.launch {
                try {
                    val fluvioConsumer = getConsumer(topicName, consumerGroup)
                    
                    while (!isClosed.get() && !isPaused.get()) {
                        try {
                            val records = fluvioConsumer.poll(config.consumer.fetchMaxWait.toMillis())
                            
                            records.forEach { record ->
                                processRecord(record, consumer)
                            }
                            
                        } catch (e: Exception) {
                            if (!isClosed.get()) {
                                logger.error(e) { "Error polling messages from topic $topicName" }
                                delay(1000)
                            }
                        }
                    }
                } catch (e: Exception) {
                    logger.error(e) { "Consumer error for topic $topicName" }
                }
            }
        }
    }
    
    /**
     * Batch receive method for compatibility with JdbcQueue
     */
    fun receiveBatch(
        consumerGroup: String?,
        consumer: Consumer<List<Either<T, DeserializationException>>>
    ): Runnable {
        val actualTopicName = buildTopicName(consumerGroup)
        
        return Runnable {
            scope.launch {
                try {
                    val fluvioConsumer = getConsumer(actualTopicName, consumerGroup)
                    
                    while (!isClosed.get() && !isPaused.get()) {
                        try {
                            val records = fluvioConsumer.poll(config.consumer.fetchMaxWait.toMillis())
                            
                            if (records.isNotEmpty()) {
                                val batch = records.map { record ->
                                    try {
                                        val message = serializer.deserialize(record.value(), messageType)
                                        Either.left<T, DeserializationException>(message)
                                    } catch (e: Exception) {
                                        metricRegistry.counter("queue.receive.error", "queue_type", queueTypeName).increment()
                                        Either.right<T, DeserializationException>(DeserializationException(e))
                                    }
                                }
                                
                                consumer.accept(batch)
                                metricRegistry.counter("queue.receive.batch.success", "queue_type", queueTypeName).increment()
                            }
                            
                        } catch (e: Exception) {
                            if (!isClosed.get()) {
                                logger.error(e) { "Error in batch polling from topic $actualTopicName" }
                                delay(1000)
                            }
                        }
                    }
                } catch (e: Exception) {
                    logger.error(e) { "Batch consumer error for topic $actualTopicName" }
                }
            }
        }
    }
    
    /**
     * Pause the queue
     */
    override fun pause() {
        isPaused.set(true)
        logger.info { "Paused queue for topic $topicName" }
    }
    
    /**
     * Resume the queue
     */
    override fun resume() {
        isPaused.set(false)
        logger.info { "Resumed queue for topic $topicName" }
    }
    
    /**
     * Close the queue and cleanup resources
     */
    override fun close() {
        if (isClosed.compareAndSet(false, true)) {
            logger.info { "Closing queue for topic $topicName" }
            
            // Cancel all coroutines
            scope.cancel()
            
            logger.info { "Queue closed for topic $topicName" }
        }
    }
    
    private fun getProducer(): FluvioProducer {
        return clientManager.getProducer(topicName)
    }
    
    private fun getConsumer(topicName: String, consumerGroup: String?): FluvioConsumer {
        return clientManager.getConsumer(topicName, consumerGroup)
    }
    
    private fun buildTopicName(consumerGroup: String?): String {
        return if (consumerGroup != null) {
            "${topicName}-${consumerGroup}"
        } else {
            topicName
        }
    }
    
    private fun processRecord(record: Record, consumer: Consumer<Either<T, DeserializationException>>) {
        try {
            val message = serializer.deserialize(record.value(), messageType)
            consumer.accept(Either.left(message))
            metricRegistry.counter("queue.receive.success", "queue_type", queueTypeName).increment()
        } catch (e: Exception) {
            metricRegistry.counter("queue.receive.error", "queue_type", queueTypeName).increment()
            consumer.accept(Either.right(DeserializationException(e)))
        }
    }
    
    private fun generateKey(): String = java.util.UUID.randomUUID().toString()
}
