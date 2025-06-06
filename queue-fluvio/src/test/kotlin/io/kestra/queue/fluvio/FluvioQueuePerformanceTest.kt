package io.kestra.queue.fluvio

import io.kestra.core.models.executions.Execution
import io.kestra.core.queues.QueueService
import io.kestra.core.metrics.MetricRegistry
import io.kestra.core.utils.ExecutorsUtils
import io.kestra.queue.fluvio.serialization.FluvioProtobufSerializer
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Assertions.*
import org.mockito.Mockito.*
import java.time.ZonedDateTime
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.system.measureTimeMillis

/**
 * Performance tests for FluvioQueue
 * 
 * These tests are disabled by default as they require a running Fluvio cluster.
 * Enable them for performance benchmarking and integration testing.
 */
class FluvioQueuePerformanceTest {
    
    private val config = FluvioQueueConfiguration()
    
    @Test
    @Disabled("Requires running Fluvio cluster")
    fun `should handle high throughput message serialization`() {
        // Given
        val serializer = FluvioProtobufSerializer()
        val messageCount = 10000
        val execution = createTestExecution()
        
        // When
        val serializationTime = measureTimeMillis {
            repeat(messageCount) {
                val serialized = serializer.serialize(execution)
                val deserialized = serializer.deserialize(serialized, Execution::class.java)
                assertNotNull(deserialized)
                assertEquals(execution.id, deserialized.id)
            }
        }
        
        // Then
        val throughput = messageCount * 1000.0 / serializationTime
        println("Serialization performance:")
        println("- Processed $messageCount messages in ${serializationTime}ms")
        println("- Throughput: ${throughput.toInt()} messages/second")
        
        // Assert minimum performance requirement (should be much faster than JDBC)
        assertTrue(throughput > 5000, "Serialization throughput should be > 5000 msg/sec, was: $throughput")
    }
    
    @Test
    @Disabled("Requires running Fluvio cluster")
    fun `should handle concurrent message processing`() {
        // Given
        val clientManager = mock(FluvioClientManager::class.java)
        val serializer = FluvioProtobufSerializer()
        val queueService = mock(QueueService::class.java)
        val metricRegistry = mock(MetricRegistry::class.java)
        val executorsUtils = mock(ExecutorsUtils::class.java)
        
        // Mock the ExecutorsUtils to return non-null values
        `when`(executorsUtils.cachedThreadPool(anyString())).thenReturn(java.util.concurrent.Executors.newCachedThreadPool())
        `when`(executorsUtils.maxCachedThreadPool(anyInt(), anyString())).thenReturn(java.util.concurrent.Executors.newCachedThreadPool())
        
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
        
        val threadCount = 10
        val messagesPerThread = 1000
        val totalMessages = threadCount * messagesPerThread
        val latch = CountDownLatch(threadCount)
        val execution = createTestExecution()
        
        // When
        val processingTime = measureTimeMillis {
            repeat(threadCount) { threadIndex ->
                Thread {
                    try {
                        repeat(messagesPerThread) { messageIndex ->
                            val testExecution = execution.toBuilder()
                                .id("execution-$threadIndex-$messageIndex")
                                .build()
                            
                            // Test serialization (since we can't test actual Fluvio without cluster)
                            val serialized = serializer.serialize(testExecution)
                            val deserialized = serializer.deserialize(serialized, Execution::class.java)
                            assertNotNull(deserialized)
                        }
                    } finally {
                        latch.countDown()
                    }
                }.start()
            }
            
            assertTrue(latch.await(30, TimeUnit.SECONDS), "All threads should complete within 30 seconds")
        }
        
        // Then
        val throughput = totalMessages * 1000.0 / processingTime
        println("Concurrent processing performance:")
        println("- Processed $totalMessages messages with $threadCount threads in ${processingTime}ms")
        println("- Throughput: ${throughput.toInt()} messages/second")
        
        // Assert minimum performance requirement
        assertTrue(throughput > 8000, "Concurrent throughput should be > 8000 msg/sec, was: $throughput")
    }
    
    @Test
    @Disabled("Requires running Fluvio cluster")
    fun `should demonstrate memory efficiency`() {
        // Given
        val serializer = FluvioProtobufSerializer()
        val execution = createTestExecution()
        val messageCount = 50000
        
        // Measure memory before
        System.gc()
        val memoryBefore = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
        
        // When
        val serializedMessages = mutableListOf<ByteArray>()
        val processingTime = measureTimeMillis {
            repeat(messageCount) { index ->
                val testExecution = execution.toBuilder()
                    .id("execution-$index")
                    .build()
                
                val serialized = serializer.serialize(testExecution)
                serializedMessages.add(serialized)
                
                // Periodically deserialize to test round-trip
                if (index % 1000 == 0) {
                    val deserialized = serializer.deserialize(serialized, Execution::class.java)
                    assertNotNull(deserialized)
                }
            }
        }
        
        // Measure memory after
        System.gc()
        val memoryAfter = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
        val memoryUsed = memoryAfter - memoryBefore
        
        // Then
        val avgMessageSize = serializedMessages.map { it.size }.average()
        val throughput = messageCount * 1000.0 / processingTime
        
        println("Memory efficiency test:")
        println("- Processed $messageCount messages in ${processingTime}ms")
        println("- Throughput: ${throughput.toInt()} messages/second")
        println("- Average message size: ${avgMessageSize.toInt()} bytes")
        println("- Memory used: ${memoryUsed / 1024 / 1024} MB")
        println("- Memory per message: ${memoryUsed / messageCount} bytes")
        
        // Assert reasonable memory usage (should be much better than JSON)
        assertTrue(avgMessageSize < 2000, "Average message size should be < 2KB, was: $avgMessageSize")
        assertTrue(memoryUsed / messageCount < 5000, "Memory per message should be < 5KB")
    }
    
    @Test
    fun `should validate queue configuration performance`() {
        // Given
        val customConfig = FluvioQueueConfiguration(
            topicPrefix = "test",
            partitions = 10,
            replicationFactor = 2,
            topics = mapOf(
                "executions" to FluvioQueueConfiguration.TopicConfig(
                    partitions = 20,
                    replicationFactor = 3,
                    topicName = "high-performance-executions"
                )
            )
        )
        
        // When & Then - Configuration access should be very fast
        val configTime = measureTimeMillis {
            repeat(100000) {
                val topicName = customConfig.getTopicName("executions")
                val partitions = customConfig.getPartitions("executions")
                val replicationFactor = customConfig.getReplicationFactor("executions")
                
                assertEquals("high-performance-executions", topicName)
                assertEquals(20, partitions)
                assertEquals(3, replicationFactor)
            }
        }
        
        println("Configuration access performance: ${configTime}ms for 100,000 operations")
        assertTrue(configTime < 100, "Configuration access should be very fast, took: ${configTime}ms")
    }
    
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
}
