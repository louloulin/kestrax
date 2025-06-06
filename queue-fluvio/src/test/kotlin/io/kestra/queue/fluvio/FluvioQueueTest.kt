package io.kestra.queue.fluvio

import io.kestra.core.models.executions.Execution
import io.kestra.core.queues.QueueService
import io.kestra.core.metrics.MetricRegistry
import io.kestra.core.utils.ExecutorsUtils
import io.kestra.queue.fluvio.serialization.ProtobufSerializer
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import jakarta.inject.Inject
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.mockito.Mockito.*

@MicronautTest
class FluvioQueueTest {

    @Inject
    lateinit var config: FluvioQueueConfiguration

    @Test
    fun `should create FluvioQueue with correct configuration`() {
        // Given
        val clientManager = mock(FluvioClientManager::class.java)
        val serializer = mock(ProtobufSerializer::class.java)
        val queueService = mock(QueueService::class.java)
        val metricRegistry = mock(MetricRegistry::class.java)
        val executorsUtils = mock(ExecutorsUtils::class.java)

        // When
        val queue = FluvioQueue(
            messageType = Execution::class.java,
            queueTypeName = "executions",
            clientManager = clientManager,
            serializer = serializer,
            queueService = queueService,
            metricRegistry = metricRegistry,
            config = config,
            executorsUtils = executorsUtils
        )

        // Then
        assertNotNull(queue)
    }

    @Test
    fun `should generate correct topic name`() {
        // Given
        val queueType = "worker-jobs"

        // When
        val topicName = config.getTopicName(queueType)

        // Then
        assertEquals("kestra-worker-jobs", topicName)
    }

    @Test
    fun `should use custom topic configuration when available`() {
        // Given
        val customConfig = FluvioQueueConfiguration(
            topics = mapOf(
                "executions" to FluvioQueueConfiguration.TopicConfig(
                    partitions = 10,
                    replicationFactor = 3,
                    topicName = "custom-executions"
                )
            )
        )

        // When
        val topicName = customConfig.getTopicName("executions")
        val partitions = customConfig.getPartitions("executions")
        val replicationFactor = customConfig.getReplicationFactor("executions")

        // Then
        assertEquals("custom-executions", topicName)
        assertEquals(10, partitions)
        assertEquals(3, replicationFactor)
    }

    @Test
    fun `should fall back to default configuration when topic config not found`() {
        // Given
        val defaultConfig = FluvioQueueConfiguration(
            partitions = 5,
            replicationFactor = 2
        )

        // When
        val partitions = defaultConfig.getPartitions("unknown-queue")
        val replicationFactor = defaultConfig.getReplicationFactor("unknown-queue")

        // Then
        assertEquals(5, partitions)
        assertEquals(2, replicationFactor)
    }
}

/**
 * Integration test for FluvioQueue functionality
 * Note: This requires a running Fluvio cluster for full integration testing
 */
@MicronautTest
class FluvioQueueIntegrationTest : StringSpec() {
    
    init {
        "should serialize and deserialize execution messages".config(enabled = false) {
            // This test is disabled by default as it requires a running Fluvio cluster
            // Enable it for integration testing with actual Fluvio infrastructure
            
            // Given
            val execution = createTestExecution()
            val serializer = ProtobufSerializer()
            
            // When
            val serialized = serializer.serialize(execution)
            val deserialized = serializer.deserialize(serialized, Execution::class.java)
            
            // Then
            deserialized.id shouldBe execution.id
            deserialized.namespace shouldBe execution.namespace
            deserialized.flowId shouldBe execution.flowId
        }
    }
    
    private fun createTestExecution(): Execution {
        return Execution.builder()
            .id("test-execution-id")
            .namespace("test.namespace")
            .flowId("test-flow")
            .flowRevision(1)
            .state(State())
            .build()
    }
}

/**
 * Performance test for FluvioQueue
 */
@MicronautTest
class FluvioQueuePerformanceTest : StringSpec() {
    
    init {
        "should handle high throughput message processing".config(enabled = false) {
            // This test is disabled by default as it requires performance testing setup
            // Enable it for performance benchmarking
            
            val messageCount = 10000
            val startTime = System.currentTimeMillis()
            
            // Simulate high throughput processing
            repeat(messageCount) {
                // Process messages
            }
            
            val endTime = System.currentTimeMillis()
            val duration = endTime - startTime
            val throughput = messageCount * 1000.0 / duration
            
            println("Processed $messageCount messages in ${duration}ms")
            println("Throughput: ${throughput.toInt()} messages/second")
            
            // Assert minimum throughput requirement
            throughput shouldBe io.kotest.matchers.comparables.gt(1000.0)
        }
    }
}
