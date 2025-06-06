package io.kestra.queue.fluvio

import io.kestra.core.models.executions.Execution
import io.kestra.core.models.executions.TaskRun
import io.kestra.core.models.executions.LogEntry
import io.kestra.core.models.executions.MetricEntry
import io.kestra.core.queues.QueueService
import io.kestra.core.metrics.MetricRegistry
import io.kestra.core.utils.ExecutorsUtils
import io.kestra.queue.fluvio.serialization.FluvioProtobufSerializer
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Assertions.*
import org.mockito.Mockito.*
import java.time.Instant

/**
 * Integration tests for FluvioQueue with Kestra core components
 * 
 * These tests verify that FluvioQueue integrates correctly with Kestra's
 * execution engine, serialization, and monitoring systems.
 */
class FluvioQueueIntegrationTest {
    
    private val config = FluvioQueueConfiguration()
    
    @Test
    fun `should serialize and deserialize Execution objects correctly`() {
        // Given
        val serializer = FluvioProtobufSerializer()
        val execution = createTestExecution()
        
        // When
        val serialized = serializer.serialize(execution)
        val deserialized = serializer.deserialize(serialized, Execution::class.java)
        
        // Then
        assertNotNull(deserialized)
        assertEquals(execution.id, deserialized.id)
        assertEquals(execution.namespace, deserialized.namespace)
        assertEquals(execution.flowId, deserialized.flowId)
        assertEquals(execution.flowRevision, deserialized.flowRevision)
        assertEquals(execution.inputs, deserialized.inputs)
        assertEquals(execution.variables, deserialized.variables)
    }
    
    @Test
    fun `should serialize and deserialize TaskRun objects correctly`() {
        // Given
        val serializer = FluvioProtobufSerializer()
        val taskRun = createTestTaskRun()
        
        // When
        val serialized = serializer.serialize(taskRun)
        val deserialized = serializer.deserialize(serialized, TaskRun::class.java)
        
        // Then
        assertNotNull(deserialized)
        assertEquals(taskRun.id, deserialized.id)
        assertEquals(taskRun.executionId, deserialized.executionId)
        assertEquals(taskRun.namespace, deserialized.namespace)
        assertEquals(taskRun.flowId, deserialized.flowId)
        assertEquals(taskRun.taskId, deserialized.taskId)
        assertEquals(taskRun.outputs, deserialized.outputs)
    }
    
    @Test
    fun `should serialize and deserialize LogEntry objects correctly`() {
        // Given
        val serializer = FluvioProtobufSerializer()
        val logEntry = createTestLogEntry()
        
        // When
        val serialized = serializer.serialize(logEntry)
        val deserialized = serializer.deserialize(serialized, LogEntry::class.java)
        
        // Then
        assertNotNull(deserialized)
        assertEquals(logEntry.namespace, deserialized.namespace)
        assertEquals(logEntry.flowId, deserialized.flowId)
        assertEquals(logEntry.executionId, deserialized.executionId)
        assertEquals(logEntry.message, deserialized.message)
        assertEquals(logEntry.level, deserialized.level)
    }
    
    @Test
    fun `should serialize and deserialize MetricEntry objects correctly`() {
        // Given
        val serializer = FluvioProtobufSerializer()
        val metricEntry = createTestMetricEntry()
        
        // When
        val serialized = serializer.serialize(metricEntry)
        val deserialized = serializer.deserialize(serialized, MetricEntry::class.java)
        
        // Then
        assertNotNull(deserialized)
        assertEquals(metricEntry.namespace, deserialized.namespace)
        assertEquals(metricEntry.flowId, deserialized.flowId)
        assertEquals(metricEntry.executionId, deserialized.executionId)
        assertEquals(metricEntry.name, deserialized.name)
        assertEquals(metricEntry.value, deserialized.value)
        assertEquals(metricEntry.tags, deserialized.tags)
    }
    
    @Test
    fun `should handle queue configuration correctly`() {
        // Given
        val customConfig = FluvioQueueConfiguration().apply {
            topicPrefix = "test-kestra"
            partitions = 5
            replicationFactor = 2
            topics = mapOf(
                "executions" to FluvioQueueConfiguration.TopicConfig().apply {
                    partitions = 10
                    replicationFactor = 3
                    topicName = "custom-executions"
                }
            )
        }
        
        // When & Then
        assertEquals("custom-executions", customConfig.getTopicName("executions"))
        assertEquals(10, customConfig.getPartitions("executions"))
        assertEquals(3, customConfig.getReplicationFactor("executions"))
        
        // Test fallback for non-configured topics
        assertEquals("test-kestra-task-runs", customConfig.getTopicName("task-runs"))
        assertEquals(5, customConfig.getPartitions("task-runs"))
        assertEquals(2, customConfig.getReplicationFactor("task-runs"))
    }
    
    @Test
    fun `should create FluvioQueue instances with correct configuration`() {
        // Given
        val clientManager = mock(FluvioClientManager::class.java)
        val serializer = FluvioProtobufSerializer()
        val queueService = mock(QueueService::class.java)
        val metricRegistry = mock(MetricRegistry::class.java)
        val executorsUtils = mock(ExecutorsUtils::class.java)
        
        // Mock the ExecutorsUtils to return non-null values
        `when`(executorsUtils.cachedThreadPool(anyString())).thenReturn(java.util.concurrent.Executors.newCachedThreadPool())
        `when`(executorsUtils.maxCachedThreadPool(anyInt(), anyString())).thenReturn(java.util.concurrent.Executors.newCachedThreadPool())
        
        // When
        val executionQueue = FluvioQueue(
            messageType = Execution::class.java,
            queueTypeName = "executions",
            clientManager = clientManager,
            serializer = serializer,
            queueService = queueService,
            metricRegistry = metricRegistry,
            config = config,
            executorsUtils = executorsUtils
        )
        
        val taskRunQueue = FluvioQueue(
            messageType = TaskRun::class.java,
            queueTypeName = "task-runs",
            clientManager = clientManager,
            serializer = serializer,
            queueService = queueService,
            metricRegistry = metricRegistry,
            config = config,
            executorsUtils = executorsUtils
        )
        
        // Then
        assertNotNull(executionQueue)
        assertNotNull(taskRunQueue)
    }
    
    @Test
    fun `should handle serialization errors gracefully`() {
        // Given
        val serializer = FluvioProtobufSerializer()
        val invalidObject = object {
            val circularRef: Any = this // This will cause serialization to fail
        }
        
        // When & Then
        assertThrows(RuntimeException::class.java) {
            serializer.serialize(invalidObject)
        }
    }
    
    @Test
    fun `should handle deserialization errors gracefully`() {
        // Given
        val serializer = FluvioProtobufSerializer()
        val invalidData = "invalid json data".toByteArray()
        
        // When & Then
        assertThrows(Exception::class.java) {
            serializer.deserialize(invalidData, Execution::class.java)
        }
    }
    
    @Test
    fun `should demonstrate performance characteristics`() {
        // Given
        val serializer = FluvioProtobufSerializer()
        val execution = createTestExecution()
        val iterations = 1000
        
        // When
        val startTime = System.currentTimeMillis()
        
        repeat(iterations) {
            val serialized = serializer.serialize(execution)
            val deserialized = serializer.deserialize(serialized, Execution::class.java)
            assertNotNull(deserialized)
        }
        
        val endTime = System.currentTimeMillis()
        val totalTime = endTime - startTime
        val throughput = iterations * 1000.0 / totalTime
        
        // Then
        println("Serialization performance test:")
        println("- Processed $iterations round-trips in ${totalTime}ms")
        println("- Throughput: ${throughput.toInt()} operations/second")
        
        // Assert reasonable performance (should be much faster than database operations)
        assertTrue(throughput > 100, "Throughput should be > 100 ops/sec, was: $throughput")
        assertTrue(totalTime < 10000, "Total time should be < 10 seconds, was: ${totalTime}ms")
    }
    
    // Helper methods to create test objects
    private fun createTestExecution(): Execution {
        return Execution.builder()
            .id("test-execution-id")
            .namespace("test.namespace")
            .flowId("test-flow")
            .flowRevision(1)
            .inputs(mapOf("input1" to "value1", "input2" to "value2"))
            .variables(mapOf("var1" to "varValue1", "var2" to "varValue2"))
            .build()
    }
    
    private fun createTestTaskRun(): TaskRun {
        return TaskRun.builder()
            .id("test-taskrun-id")
            .executionId("test-execution-id")
            .namespace("test.namespace")
            .flowId("test-flow")
            .taskId("test-task")
            .outputs(null)
            .build()
    }
    
    private fun createTestLogEntry(): LogEntry {
        return LogEntry.builder()
            .namespace("test.namespace")
            .flowId("test-flow")
            .executionId("test-execution-id")
            .message("Test log message")
            .level(org.slf4j.event.Level.INFO)
            .timestamp(Instant.now())
            .build()
    }
    
    private fun createTestMetricEntry(): MetricEntry {
        return MetricEntry.builder()
            .namespace("test.namespace")
            .flowId("test-flow")
            .executionId("test-execution-id")
            .name("test.metric")
            .value(42.0)
            .tags(mapOf("tag1" to "value1", "tag2" to "value2"))
            .timestamp(Instant.now())
            .build()
    }
}
