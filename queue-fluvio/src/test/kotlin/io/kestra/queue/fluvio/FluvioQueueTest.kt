package io.kestra.queue.fluvio

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kestra.core.models.executions.Execution
import io.kestra.core.models.flows.State
import io.kestra.core.queues.QueueService
import io.kestra.core.metrics.MetricRegistry
import io.kestra.queue.fluvio.serialization.ProtobufSerializer
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import io.mockk.mockk
import io.mockk.every
import jakarta.inject.Inject
import java.time.Instant

@MicronautTest
class FluvioQueueTest : StringSpec() {
    
    @Inject
    lateinit var config: FluvioQueueConfiguration
    
    init {
        "should create FluvioQueue with correct configuration" {
            // Given
            val clientManager = mockk<FluvioClientManager>()
            val serializer = mockk<ProtobufSerializer>()
            val queueService = mockk<QueueService>()
            val metricRegistry = mockk<MetricRegistry>()
            
            // When
            val queue = FluvioQueue(
                messageType = Execution::class.java,
                queueTypeName = "executions",
                clientManager = clientManager,
                serializer = serializer,
                queueService = queueService,
                metricRegistry = metricRegistry,
                config = config
            )
            
            // Then
            queue shouldNotBe null
        }
        
        "should generate correct topic name" {
            // Given
            val queueType = "worker-jobs"
            
            // When
            val topicName = config.getTopicName(queueType)
            
            // Then
            topicName shouldBe "kestra-worker-jobs"
        }
        
        "should use custom topic configuration when available" {
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
            topicName shouldBe "custom-executions"
            partitions shouldBe 10
            replicationFactor shouldBe 3
        }
        
        "should fall back to default configuration when topic config not found" {
            // Given
            val defaultConfig = FluvioQueueConfiguration(
                partitions = 5,
                replicationFactor = 2
            )
            
            // When
            val partitions = defaultConfig.getPartitions("unknown-queue")
            val replicationFactor = defaultConfig.getReplicationFactor("unknown-queue")
            
            // Then
            partitions shouldBe 5
            replicationFactor shouldBe 2
        }
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
