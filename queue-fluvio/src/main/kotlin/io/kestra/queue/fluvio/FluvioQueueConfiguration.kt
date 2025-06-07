package io.kestra.queue.fluvio

import io.micronaut.context.annotation.ConfigurationProperties
import io.micronaut.context.annotation.Requires
import java.time.Duration

@ConfigurationProperties("kestra.queue.fluvio")
@Requires(property = "kestra.queue.type", value = "fluvio")
data class FluvioQueueConfiguration(
    /**
     * Fluvio cluster endpoint
     */
    var clusterEndpoint: String = "fluvio-sc:9003",
    
    /**
     * Topic prefix for all Kestra topics
     */
    var topicPrefix: String = "kestra",
    
    /**
     * Default replication factor for topics
     */
    var replicationFactor: Int = 2,
    
    /**
     * Default number of partitions for topics
     */
    var partitions: Int = 3,
    
    /**
     * Topic retention configuration
     */
    var retention: RetentionConfig = RetentionConfig(),
    
    /**
     * Producer configuration
     */
    var producer: ProducerConfig = ProducerConfig(),
    
    /**
     * Consumer configuration
     */
    var consumer: ConsumerConfig = ConsumerConfig(),
    
    /**
     * Health check configuration
     */
    var healthCheck: HealthCheckConfig = HealthCheckConfig(),
    
    /**
     * Topic-specific configurations
     */
    var topics: Map<String, TopicConfig> = emptyMap()
) {
    
    data class RetentionConfig(
        /**
         * Time-based retention
         */
        var time: Duration = Duration.ofDays(7),
        
        /**
         * Size-based retention in bytes
         */
        var size: Long = 10L * 1024 * 1024 * 1024 // 10GB
    )
    
    data class ProducerConfig(
        /**
         * Batch size for batching messages
         */
        var batchSize: Int = 100,
        
        /**
         * Linger time in milliseconds
         */
        var lingerMs: Int = 10,
        
        /**
         * Compression type
         */
        var compression: String = "lz4",
        
        /**
         * Request timeout
         */
        var requestTimeout: Duration = Duration.ofSeconds(30),
        
        /**
         * Retry attempts
         */
        var retryAttempts: Int = 3,
        
        /**
         * Retry backoff
         */
        var retryBackoff: Duration = Duration.ofMillis(100)
    )
    
    data class ConsumerConfig(
        /**
         * Minimum bytes to fetch
         */
        var fetchMinBytes: Int = 1024,
        
        /**
         * Maximum wait time for fetch
         */
        var fetchMaxWait: Duration = Duration.ofMillis(500),
        
        /**
         * Maximum records per poll
         */
        var maxPollRecords: Int = 100,
        
        /**
         * Consumer group session timeout
         */
        var sessionTimeout: Duration = Duration.ofSeconds(30),
        
        /**
         * Consumer group heartbeat interval
         */
        var heartbeatInterval: Duration = Duration.ofSeconds(3),
        
        /**
         * Auto offset reset policy
         */
        var autoOffsetReset: String = "latest"
    )
    
    data class HealthCheckConfig(
        /**
         * Enable health checks
         */
        var enabled: Boolean = true,
        
        /**
         * Health check interval
         */
        var interval: Duration = Duration.ofSeconds(30),
        
        /**
         * Failure threshold before marking as unhealthy
         */
        var failureThreshold: Int = 3,
        
        /**
         * Enable automatic fallback to JDBC
         */
        var autoFallback: Boolean = true,
        
        /**
         * Connection timeout for health checks
         */
        var connectionTimeout: Duration = Duration.ofSeconds(5)
    )
    
    data class TopicConfig(
        /**
         * Number of partitions for this topic
         */
        var partitions: Int? = null,
        
        /**
         * Replication factor for this topic
         */
        var replicationFactor: Int? = null,
        
        /**
         * Topic-specific retention time
         */
        var retentionTime: Duration? = null,
        
        /**
         * Topic-specific retention size
         */
        var retentionSize: Long? = null,
        
        /**
         * Custom topic name (if different from default naming)
         */
        var topicName: String? = null
    )
    
    /**
     * Get the full topic name for a queue type
     */
    fun getTopicName(queueType: String): String {
        val topicConfig = topics[queueType]
        return topicConfig?.topicName 
            ?: "$topicPrefix-${queueType.lowercase().replace("_", "-")}"
    }
    
    /**
     * Get partitions for a specific queue type
     */
    fun getPartitions(queueType: String): Int {
        return topics[queueType]?.partitions ?: partitions
    }
    
    /**
     * Get replication factor for a specific queue type
     */
    fun getReplicationFactor(queueType: String): Int {
        return topics[queueType]?.replicationFactor ?: replicationFactor
    }
}
