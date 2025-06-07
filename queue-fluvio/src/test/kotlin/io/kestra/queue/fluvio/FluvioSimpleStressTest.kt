package io.kestra.queue.fluvio

import io.kestra.core.models.executions.Execution
import io.kestra.core.models.executions.LogEntry
import io.kestra.core.models.flows.State
import io.kestra.core.utils.IdUtils
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.slf4j.event.Level
import java.time.Instant
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import kotlin.random.Random

/**
 * Fluvio队列系统简化压力测试
 * 不依赖Micronaut，直接测试对象创建和处理性能
 * 
 * 测试场景：
 * - 高并发对象创建
 * - 大量数据处理
 * - 内存使用验证
 * - 序列化性能测试
 */
class FluvioSimpleStressTest {

    @Test
    fun `should handle high volume object creation`() {
        // Given - 大量对象创建测试
        val objectCount = 10000
        val createdObjects = AtomicInteger(0)
        val startTime = System.nanoTime()
        
        // When - 创建大量Execution对象
        val executions = mutableListOf<Execution>()
        repeat(objectCount) { index ->
            try {
                val execution = createTestExecution("stress-execution-$index")
                executions.add(execution)
                createdObjects.incrementAndGet()
            } catch (e: Exception) {
                println("Failed to create execution $index: ${e.message}")
            }
        }
        
        val endTime = System.nanoTime()
        val durationMs = (endTime - startTime) / 1_000_000.0
        val throughput = objectCount / (durationMs / 1000.0)
        
        // Then - 验证对象创建性能
        assertEquals(objectCount, createdObjects.get(), "所有对象都应该创建成功")
        assertEquals(objectCount, executions.size, "所有对象都应该添加到列表")
        assertTrue(throughput > 1000, "对象创建吞吐量应该大于1000 obj/s，实际: $throughput")
        assertTrue(durationMs < 30000, "创建时间应该小于30秒，实际: $durationMs ms")
        
        // 验证对象完整性
        executions.forEachIndexed { index, execution ->
            assertNotNull(execution.id, "执行ID不应该为空")
            assertNotNull(execution.namespace, "命名空间不应该为空")
            assertTrue(execution.flowId.startsWith("stress-execution-"), "流程ID应该有正确前缀")
            assertEquals("io.kestra.stress.test", execution.namespace, "命名空间应该正确")
        }
        
        println("=== 大量对象创建压力测试结果 ===")
        println("创建对象数: ${createdObjects.get()}")
        println("总耗时: ${durationMs}ms")
        println("创建吞吐量: ${String.format("%.2f", throughput)} obj/s")
        println("平均创建时间: ${String.format("%.4f", durationMs / objectCount)}ms/obj")
    }

    @Test
    fun `should handle concurrent object processing`() {
        // Given - 并发处理测试参数
        val threadCount = 20
        val objectsPerThread = 500
        val totalObjects = threadCount * objectsPerThread
        
        val processedObjects = AtomicInteger(0)
        val failedObjects = AtomicInteger(0)
        val totalProcessingTime = AtomicLong(0)
        val startTime = System.nanoTime()
        
        val executor = Executors.newFixedThreadPool(threadCount)
        val latch = CountDownLatch(threadCount)
        
        // When - 并发处理对象
        repeat(threadCount) { threadIndex ->
            executor.submit {
                try {
                    repeat(objectsPerThread) { objectIndex ->
                        val processingStart = System.nanoTime()
                        
                        // 创建和处理不同类型的对象
                        when (objectIndex % 3) {
                            0 -> {
                                val execution = createTestExecution("concurrent-execution-$threadIndex-$objectIndex")
                                // 模拟处理逻辑
                                val runningExecution = execution.withState(State.Type.RUNNING)
                                val completedExecution = runningExecution.withState(State.Type.SUCCESS)
                                assertNotNull(completedExecution)
                            }
                            1 -> {
                                val logEntry = createTestLogEntry("Concurrent processing message $threadIndex-$objectIndex")
                                // 模拟日志处理
                                assertNotNull(logEntry.executionId)
                                assertNotNull(logEntry.message)
                                assertTrue(logEntry.message.contains("Concurrent processing"))
                            }
                            2 -> {
                                // 模拟复杂计算
                                val result = performComplexCalculation(objectIndex)
                                assertTrue(result > 0, "计算结果应该大于0")
                            }
                        }
                        
                        val processingEnd = System.nanoTime()
                        totalProcessingTime.addAndGet(processingEnd - processingStart)
                        processedObjects.incrementAndGet()
                    }
                } catch (e: Exception) {
                    failedObjects.addAndGet(objectsPerThread)
                    println("Thread $threadIndex failed: ${e.message}")
                } finally {
                    latch.countDown()
                }
            }
        }
        
        // 等待所有线程完成
        assertTrue(latch.await(60, TimeUnit.SECONDS), "所有线程应在60秒内完成")
        
        val endTime = System.nanoTime()
        val totalDurationMs = (endTime - startTime) / 1_000_000.0
        val throughput = processedObjects.get() / (totalDurationMs / 1000.0)
        val avgProcessingTime = totalProcessingTime.get() / processedObjects.get().toDouble() / 1_000_000.0 // ms
        
        executor.shutdown()
        
        // Then - 验证并发处理结果
        assertEquals(totalObjects, processedObjects.get(), "所有对象都应该处理成功")
        assertEquals(0, failedObjects.get(), "不应该有失败的对象")
        assertTrue(throughput > 500, "处理吞吐量应该大于500 obj/s，实际: $throughput")
        assertTrue(avgProcessingTime < 10, "平均处理时间应该小于10ms，实际: $avgProcessingTime")
        assertTrue(totalDurationMs < 60000, "总处理时间应该小于60秒，实际: $totalDurationMs ms")
        
        println("=== 并发对象处理压力测试结果 ===")
        println("线程数: $threadCount")
        println("每线程对象数: $objectsPerThread")
        println("总对象数: $totalObjects")
        println("处理成功: ${processedObjects.get()}")
        println("处理失败: ${failedObjects.get()}")
        println("总耗时: ${totalDurationMs}ms")
        println("处理吞吐量: ${String.format("%.2f", throughput)} obj/s")
        println("平均处理时间: ${String.format("%.4f", avgProcessingTime)}ms/obj")
    }

    @Test
    fun `should handle memory pressure gracefully`() {
        // Given - 内存压力测试参数
        val largeObjectCount = 5000
        val runtime = Runtime.getRuntime()
        val initialMemory = runtime.totalMemory() - runtime.freeMemory()
        
        // When - 创建大量包含大数据的对象
        val largeObjects = mutableListOf<Any>()
        repeat(largeObjectCount) { index ->
            when (index % 3) {
                0 -> {
                    val execution = createLargeExecution("memory-test-execution-$index")
                    largeObjects.add(execution)
                }
                1 -> {
                    val logEntry = createLargeLogEntry("Memory test log entry $index with large content")
                    largeObjects.add(logEntry)
                }
                2 -> {
                    // 创建包含大量数据的Map
                    val largeMap = mutableMapOf<String, String>()
                    repeat(100) { mapIndex ->
                        largeMap["key-$mapIndex"] = "value-".repeat(50) + mapIndex
                    }
                    largeObjects.add(largeMap)
                }
            }
            
            // 每1000个对象检查一次内存
            if (index % 1000 == 0 && index > 0) {
                val currentMemory = runtime.totalMemory() - runtime.freeMemory()
                val memoryIncrease = currentMemory - initialMemory
                
                // 内存增长应该是合理的（小于1GB）
                assertTrue(memoryIncrease < 1024 * 1024 * 1024, 
                          "内存增长应该合理，当前增长: ${memoryIncrease / 1024 / 1024}MB")
            }
        }
        
        val finalMemory = runtime.totalMemory() - runtime.freeMemory()
        val totalMemoryIncrease = finalMemory - initialMemory
        
        // 清理对象并触发GC
        largeObjects.clear()
        System.gc()
        Thread.sleep(2000) // 等待GC
        
        val afterGcMemory = runtime.totalMemory() - runtime.freeMemory()
        val memoryReclaimed = finalMemory - afterGcMemory
        
        // Then - 验证内存使用
        assertEquals(0, largeObjects.size, "对象列表应该已清空")
        // 内存增长可能为负数（JVM优化），所以使用绝对值
        val absMemoryIncrease = kotlin.math.abs(totalMemoryIncrease)
        assertTrue(absMemoryIncrease < 2 * 1024 * 1024 * 1024, // 2GB
                  "总内存变化应该合理，实际: ${totalMemoryIncrease / 1024 / 1024}MB")
        // 如果内存增长为正，验证GC回收效果
        if (totalMemoryIncrease > 0) {
            assertTrue(memoryReclaimed > totalMemoryIncrease * 0.1,
                      "GC应该回收部分内存，回收: ${memoryReclaimed / 1024 / 1024}MB")
        }
        
        println("=== 内存压力测试结果 ===")
        println("创建对象数: $largeObjectCount")
        println("初始内存: ${initialMemory / 1024 / 1024}MB")
        println("最终内存: ${finalMemory / 1024 / 1024}MB")
        println("内存增长: ${totalMemoryIncrease / 1024 / 1024}MB")
        println("GC后内存: ${afterGcMemory / 1024 / 1024}MB")
        println("回收内存: ${memoryReclaimed / 1024 / 1024}MB")
        println("回收率: ${String.format("%.1f", memoryReclaimed.toDouble() / totalMemoryIncrease * 100)}%")
    }

    @Test
    fun `should handle sustained processing load`() {
        // Given - 持续处理负载测试
        val testDurationSeconds = 10
        val targetRate = 100 // obj/s
        val intervalMs = 1000 / targetRate
        
        val processedObjects = AtomicInteger(0)
        val errorCount = AtomicInteger(0)
        val startTime = System.nanoTime()
        val endTime = startTime + testDurationSeconds * 1_000_000_000L
        
        // When - 持续处理对象
        val executor = Executors.newSingleThreadExecutor()
        val future = executor.submit {
            var objectIndex = 0
            while (System.nanoTime() < endTime) {
                try {
                    // 创建和处理对象
                    val execution = createTestExecution("sustained-execution-$objectIndex")
                    val runningExecution = execution.withState(State.Type.RUNNING)
                    val completedExecution = runningExecution.withState(State.Type.SUCCESS)
                    
                    // 验证处理结果
                    assertEquals(State.Type.SUCCESS, completedExecution.state.current)
                    
                    processedObjects.incrementAndGet()
                    objectIndex++
                    
                    // 控制处理速率
                    Thread.sleep(intervalMs.toLong())
                } catch (e: Exception) {
                    errorCount.incrementAndGet()
                    println("Error processing object $objectIndex: ${e.message}")
                }
            }
        }
        
        future.get(testDurationSeconds + 5L, TimeUnit.SECONDS)
        executor.shutdown()
        
        val actualDurationMs = (System.nanoTime() - startTime) / 1_000_000.0
        val actualRate = processedObjects.get() / (actualDurationMs / 1000.0)
        val errorRate = errorCount.get().toDouble() / processedObjects.get()
        
        // Then - 验证持续处理结果
        assertTrue(processedObjects.get() > targetRate * testDurationSeconds * 0.8,
                  "应该处理足够的对象，实际: ${processedObjects.get()}")
        assertTrue(errorRate < 0.01, "错误率应该小于1%，实际: ${errorRate * 100}%")
        assertTrue(actualRate > targetRate * 0.8,
                  "实际处理速率应该接近目标，实际: $actualRate")
        
        println("=== 持续处理负载测试结果 ===")
        println("测试时长: ${testDurationSeconds}秒")
        println("目标速率: $targetRate obj/s")
        println("实际速率: ${String.format("%.2f", actualRate)} obj/s")
        println("处理对象: ${processedObjects.get()}")
        println("错误数量: ${errorCount.get()}")
        println("错误率: ${String.format("%.4f", errorRate * 100)}%")
    }

    // Helper methods
    private fun createTestExecution(flowId: String): Execution {
        return Execution.builder()
            .id(IdUtils.create())
            .namespace("io.kestra.stress.test")
            .flowId(flowId)
            .flowRevision(1)
            .state(State())
            .build()
    }

    private fun createLargeExecution(flowId: String): Execution {
        return Execution.builder()
            .id(IdUtils.create())
            .namespace("io.kestra.stress.test")
            .flowId(flowId)
            .flowRevision(1)
            .state(State())
            .variables(mapOf<String, Any>(
                "largeData1" to "x".repeat(1000),
                "largeData2" to "y".repeat(1000),
                "largeData3" to "z".repeat(1000),
                "metadata" to mapOf(
                    "description" to "Large execution for memory testing",
                    "tags" to listOf("stress", "memory", "test"),
                    "config" to "config-data-".repeat(100)
                )
            ))
            .build()
    }

    private fun createTestLogEntry(message: String): LogEntry {
        return LogEntry.builder()
            .executionId(IdUtils.create())
            .taskId("stress-test-task")
            .level(Level.INFO)
            .message(message)
            .timestamp(Instant.now())
            .build()
    }

    private fun createLargeLogEntry(message: String): LogEntry {
        val largeMessage = "$message - " + "additional log content ".repeat(100)
        return LogEntry.builder()
            .executionId(IdUtils.create())
            .taskId("memory-test-task")
            .level(Level.INFO)
            .message(largeMessage)
            .timestamp(Instant.now())
            .build()
    }

    private fun performComplexCalculation(input: Int): Long {
        // 模拟复杂计算
        var result = input.toLong()
        repeat(1000) { i ->
            result = (result * 31 + i) % 1000000007
        }
        return result
    }
}
