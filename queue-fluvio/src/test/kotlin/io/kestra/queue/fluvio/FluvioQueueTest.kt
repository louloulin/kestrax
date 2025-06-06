package io.kestra.queue.fluvio

import io.kestra.core.models.executions.Execution
import io.kestra.core.queues.QueueService
import io.kestra.core.metrics.MetricRegistry
import io.kestra.core.utils.ExecutorsUtils
import io.kestra.queue.fluvio.serialization.ProtobufSerializer
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.mockito.Mockito.*

class FluvioQueueTest {

    private val config = FluvioQueueConfiguration()

    @Test
    fun `should create FluvioQueue with correct configuration`() {
        // Given
        val clientManager = mock(FluvioClientManager::class.java)
        val serializer = mock(ProtobufSerializer::class.java)
        val queueService = mock(QueueService::class.java)
        val metricRegistry = mock(MetricRegistry::class.java)
        val executorsUtils = mock(ExecutorsUtils::class.java)

        // Mock the ExecutorsUtils to return non-null values
        `when`(executorsUtils.cachedThreadPool(anyString())).thenReturn(java.util.concurrent.Executors.newCachedThreadPool())

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
