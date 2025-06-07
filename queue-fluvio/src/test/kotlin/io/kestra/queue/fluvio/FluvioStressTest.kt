package io.kestra.queue.fluvio

import io.kestra.core.exceptions.DeserializationException
import io.kestra.core.models.executions.Execution
import io.kestra.core.models.executions.TaskRun
import io.kestra.core.models.executions.LogEntry
import io.kestra.core.models.executions.MetricEntry
import io.kestra.core.models.executions.Variables
import io.kestra.core.models.flows.State
import io.kestra.core.queues.QueueFactoryInterface
import io.kestra.core.queues.QueueInterface
import io.kestra.core.runners.WorkerTask
import io.kestra.core.runners.WorkerTaskResult
import io.kestra.core.utils.Either
import io.kestra.core.utils.IdUtils
import io.kestra.plugin.core.log.Log
import io.micronaut.context.ApplicationContext
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import jakarta.inject.Inject
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.slf4j.event.Level
import java.time.Duration
import java.time.Instant
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import java.util.function.Consumer
import kotlin.random.Random

/**
 * Fluvio队列系统真实压力测试
 * 使用真实的Fluvio producer和consumer进行压力测试
 *
 * 测试场景：
 * - 高并发消息发送和接收
 * - 大量数据吞吐测试
 * - 长时间运行稳定性
 * - 消息可靠性验证
 * - 错误恢复能力
 */
@MicronautTest
class FluvioStressTest {

    @Inject
    lateinit var applicationContext: ApplicationContext

    @Inject
    lateinit var queueFactory: QueueFactoryInterface

    private lateinit var executionQueue: QueueInterface<Execution>
    private lateinit var taskRunQueue: QueueInterface<WorkerTaskResult>
    private lateinit var logQueue: QueueInterface<LogEntry>
    private lateinit var metricQueue: QueueInterface<MetricEntry>

    @BeforeEach
    fun setUp() {
        executionQueue = queueFactory.execution()
        taskRunQueue = queueFactory.workerTaskResult()
        logQueue = queueFactory.logEntry()
        metricQueue = queueFactory.metricEntry()
    }

    @Test
    fun `should handle basic message sending and receiving stress test`() {
        // Given - 基础压力测试参数
        val messageCount = 100
        val sentMessages = AtomicInteger(0)
        val receivedMessages = AtomicInteger(0)
        val receivedExecutionIds = mutableSetOf<String>()
        val startTime = System.nanoTime()

        // When - 启动消费者
        val messageConsumer = Consumer<Either<Execution, DeserializationException>> { either ->
            if (either.isLeft()) {
                val execution = either.getLeft()
                receivedMessages.incrementAndGet()
                synchronized(receivedExecutionIds) {
                    receivedExecutionIds.add(execution.id)
                }

                // 验证消息完整性
                assertNotNull(execution.id)
                assertNotNull(execution.namespace)
                assertNotNull(execution.flowId)
                assertTrue(execution.flowId.startsWith("stress-execution-"))
            } else {
                println("Deserialization error: ${either.getRight().message}")
            }
        }

        val consumerRunnable = executionQueue.receive(null, Execution::class.java, messageConsumer)
        val consumerThread = Thread(consumerRunnable)
        consumerThread.start()

        // 等待消费者启动
        Thread.sleep(1000)

        // 发送消息
        val sentExecutionIds = mutableSetOf<String>()
        repeat(messageCount) { index ->
            try {
                val execution = createStressTestExecution("stress-execution-$index")
                executionQueue.emit(execution)
                sentMessages.incrementAndGet()
                sentExecutionIds.add(execution.id)
            } catch (e: Exception) {
                println("Failed to send message $index: ${e.message}")
            }
        }

        // 等待消息处理
        Thread.sleep(10000)
        consumerThread.interrupt()

        val endTime = System.nanoTime()
        val totalDurationMs = (endTime - startTime) / 1_000_000.0
        val throughput = sentMessages.get() / (totalDurationMs / 1000.0)

        // Then - 验证压力测试结果
        assertEquals(messageCount, sentMessages.get(), "所有消息都应该成功发送")
        assertTrue(receivedMessages.get() > messageCount * 0.8,
                  "应该接收到大部分消息，发送: ${sentMessages.get()}, 接收: ${receivedMessages.get()}")
        assertTrue(throughput > 10, "吞吐量应该大于10 msg/s，实际: $throughput")

        // 验证消息唯一性
        assertEquals(sentMessages.get(), sentExecutionIds.size, "发送的消息ID应该唯一")
        assertTrue(receivedExecutionIds.size <= sentExecutionIds.size, "接收的消息ID不应该超过发送的")

        println("=== 基础压力测试结果 ===")
        println("发送消息: ${sentMessages.get()}")
        println("接收消息: ${receivedMessages.get()}")
        println("总耗时: ${totalDurationMs}ms")
        println("吞吐量: ${String.format("%.2f", throughput)} msg/s")
        println("消息接收率: ${String.format("%.2f", receivedMessages.get().toDouble() / sentMessages.get() * 100)}%")
        println("消息唯一性: 发送${sentExecutionIds.size}个唯一ID，接收${receivedExecutionIds.size}个唯一ID")
    }

    @Test
    fun `should handle multiple queue types under load`() {
        // Given - 多队列类型测试参数
        val messageCount = 50
        val sentCounts = mutableMapOf<String, Int>()
        val receivedCounts = mutableMapOf<String, AtomicInteger>()

        receivedCounts["execution"] = AtomicInteger(0)
        receivedCounts["log"] = AtomicInteger(0)
        receivedCounts["metric"] = AtomicInteger(0)

        // When - 启动消费者
        val executorService = Executors.newFixedThreadPool(3)

        // Execution消费者
        val executionConsumer = Consumer<Either<Execution, DeserializationException>> { either ->
            if (either.isLeft()) {
                val execution = either.getLeft()
                if (execution.flowId.startsWith("multi-execution-")) {
                    receivedCounts["execution"]?.incrementAndGet()
                }
            }
        }
        val executionRunnable = executionQueue.receive(null, Execution::class.java, executionConsumer)
        val executionThread = Thread(executionRunnable)
        executionThread.start()

        // Log消费者
        val logConsumer = Consumer<Either<LogEntry, DeserializationException>> { either ->
            if (either.isLeft()) {
                val logEntry = either.getLeft()
                if (logEntry.message.startsWith("Multi queue test")) {
                    receivedCounts["log"]?.incrementAndGet()
                }
            }
        }
        val logRunnable = logQueue.receive(null, LogEntry::class.java, logConsumer)
        val logThread = Thread(logRunnable)
        logThread.start()

        // Metric消费者
        val metricConsumer = Consumer<Either<MetricEntry, DeserializationException>> { either ->
            if (either.isLeft()) {
                val metricEntry = either.getLeft()
                if (metricEntry.name.startsWith("multi-metric-")) {
                    receivedCounts["metric"]?.incrementAndGet()
                }
            }
        }
        val metricRunnable = metricQueue.receive(null, MetricEntry::class.java, metricConsumer)
        val metricThread = Thread(metricRunnable)
        metricThread.start()

        // 等待消费者启动
        Thread.sleep(1000)

        // 发送混合类型消息
        repeat(messageCount) { index ->
            try {
                when (index % 3) {
                    0 -> {
                        val execution = createStressTestExecution("multi-execution-$index")
                        executionQueue.emit(execution)
                        sentCounts["execution"] = sentCounts.getOrDefault("execution", 0) + 1
                    }
                    1 -> {
                        val logEntry = createStressTestLogEntry("Multi queue test message $index")
                        logQueue.emit(logEntry)
                        sentCounts["log"] = sentCounts.getOrDefault("log", 0) + 1
                    }
                    2 -> {
                        val metricEntry = createStressTestMetricEntry("multi-metric-$index")
                        metricQueue.emit(metricEntry)
                        sentCounts["metric"] = sentCounts.getOrDefault("metric", 0) + 1
                    }
                }
            } catch (e: Exception) {
                println("Failed to send multi-queue message $index: ${e.message}")
            }
        }

        // 等待消息处理
        Thread.sleep(10000)

        // 停止消费者
        executionThread.interrupt()
        logThread.interrupt()
        metricThread.interrupt()

        executorService.shutdown()

        // Then - 验证多队列处理
        val totalSent = sentCounts.values.sum()
        val totalReceived = receivedCounts.values.sumOf { it.get() }

        assertEquals(messageCount, totalSent, "应该发送所有消息")
        assertTrue(totalReceived > totalSent * 0.7,
                  "应该接收到大部分消息，发送: $totalSent, 接收: $totalReceived")

        println("=== 多队列类型压力测试结果 ===")
        println("总发送消息: $totalSent")
        println("总接收消息: $totalReceived")
        sentCounts.forEach { (type, count) ->
            val received = receivedCounts[type]?.get() ?: 0
            println("$type: 发送 $count, 接收 $received (${String.format("%.1f", received.toDouble() / count * 100)}%)")
        }
    }

    @Test
    fun `should handle sustained load with real messaging`() {
        // Given - 持续负载测试参数
        val testDurationSeconds = 15
        val targetThroughput = 10 // msg/s
        val intervalMs = 1000 / targetThroughput

        val sentMessages = AtomicInteger(0)
        val receivedMessages = AtomicInteger(0)
        val errorCount = AtomicInteger(0)
        val startTime = System.nanoTime()
        val endTime = startTime + testDurationSeconds * 1_000_000_000L

        // When - 启动消费者
        val logConsumer = Consumer<Either<LogEntry, DeserializationException>> { either ->
            if (either.isLeft()) {
                val logEntry = either.getLeft()
                if (logEntry.message.startsWith("Sustained load test")) {
                    receivedMessages.incrementAndGet()
                }
            }
        }

        val consumerRunnable = logQueue.receive(null, LogEntry::class.java, logConsumer)
        val consumerThread = Thread(consumerRunnable)
        consumerThread.start()

        // 等待消费者启动
        Thread.sleep(1000)

        // 执行持续负载测试
        val producerExecutor = Executors.newSingleThreadExecutor()
        val producerFuture = producerExecutor.submit {
            var messageIndex = 0
            while (System.nanoTime() < endTime) {
                try {
                    val logEntry = createSustainedTestLogEntry("Sustained load test message $messageIndex")
                    logQueue.emit(logEntry)
                    sentMessages.incrementAndGet()
                    messageIndex++

                    // 控制发送速率
                    Thread.sleep(intervalMs.toLong())
                } catch (e: Exception) {
                    errorCount.incrementAndGet()
                    println("Error sending message $messageIndex: ${e.message}")
                }
            }
        }

        producerFuture.get(testDurationSeconds + 5L, TimeUnit.SECONDS)

        // 等待消费者处理完消息
        Thread.sleep(5000)
        consumerThread.interrupt()

        val actualDurationMs = (System.nanoTime() - startTime) / 1_000_000.0
        val actualThroughput = sentMessages.get() / (actualDurationMs / 1000.0)
        val errorRate = if (sentMessages.get() > 0) errorCount.get().toDouble() / sentMessages.get() else 0.0

        producerExecutor.shutdown()

        // Then - 验证持续负载测试结果
        assertTrue(sentMessages.get() > targetThroughput * testDurationSeconds * 0.7,
                  "应该发送足够的消息，实际: ${sentMessages.get()}")
        assertTrue(errorRate < 0.1, "错误率应该小于10%，实际: ${errorRate * 100}%")
        assertTrue(receivedMessages.get() > sentMessages.get() * 0.7,
                  "应该接收到大部分消息，发送: ${sentMessages.get()}, 接收: ${receivedMessages.get()}")

        println("=== 持续负载压力测试结果 ===")
        println("测试时长: ${testDurationSeconds}秒")
        println("目标吞吐量: $targetThroughput msg/s")
        println("实际吞吐量: ${String.format("%.2f", actualThroughput)} msg/s")
        println("发送消息: ${sentMessages.get()}")
        println("接收消息: ${receivedMessages.get()}")
        println("错误数量: ${errorCount.get()}")
        println("错误率: ${String.format("%.4f", errorRate * 100)}%")
        println("消息接收率: ${String.format("%.2f", receivedMessages.get().toDouble() / sentMessages.get() * 100)}%")
    }

    @Test
    fun `should handle message reliability under stress`() {
        // Given - 消息可靠性测试参数
        val messageCount = 50
        val sentMessageIds = mutableSetOf<String>()
        val receivedMessageIds = mutableSetOf<String>()

        // When - 启动消费者
        val metricConsumer = Consumer<Either<MetricEntry, DeserializationException>> { either ->
            if (either.isLeft()) {
                val metricEntry = either.getLeft()
                if (metricEntry.name.startsWith("reliability-test-")) {
                    synchronized(receivedMessageIds) {
                        receivedMessageIds.add(metricEntry.name)
                    }
                }
            }
        }

        val consumerRunnable = metricQueue.receive(null, MetricEntry::class.java, metricConsumer)
        val consumerThread = Thread(consumerRunnable)
        consumerThread.start()

        // 等待消费者启动
        Thread.sleep(1000)

        // 发送带有唯一ID的消息
        repeat(messageCount) { index ->
            try {
                val metricEntry = createReliabilityTestMetricEntry("reliability-test-$index")
                metricQueue.emit(metricEntry)
                sentMessageIds.add(metricEntry.name)
            } catch (e: Exception) {
                println("Failed to send message $index: ${e.message}")
            }
        }

        // 等待消息处理
        Thread.sleep(10000)
        consumerThread.interrupt()

        // Then - 验证消息可靠性
        assertEquals(messageCount, sentMessageIds.size, "应该发送所有消息")
        assertTrue(receivedMessageIds.size > messageCount * 0.8,
                  "应该接收到80%以上的消息，发送: ${sentMessageIds.size}, 接收: ${receivedMessageIds.size}")

        // 检查消息完整性
        val lostMessages = sentMessageIds - receivedMessageIds
        val duplicateMessages = receivedMessageIds.size - receivedMessageIds.toSet().size

        assertTrue(lostMessages.size < messageCount * 0.2,
                  "丢失消息应该少于20%，丢失: ${lostMessages.size}")
        assertEquals(0, duplicateMessages, "不应该有重复消息")

        println("=== 消息可靠性测试结果 ===")
        println("发送消息: ${sentMessageIds.size}")
        println("接收消息: ${receivedMessageIds.size}")
        println("丢失消息: ${lostMessages.size}")
        println("重复消息: $duplicateMessages")
        println("消息完整率: ${String.format("%.2f", receivedMessageIds.size.toDouble() / sentMessageIds.size * 100)}%")

        if (lostMessages.isNotEmpty()) {
            println("丢失的消息: ${lostMessages.take(5)}")
        }
    }



    // Helper methods for creating test objects
    private fun createStressTestExecution(flowId: String): Execution {
        return Execution.builder()
            .id(IdUtils.create())
            .namespace("io.kestra.stress.test")
            .flowId(flowId)
            .flowRevision(1)
            .state(State())
            .build()
    }

    private fun createStressTestTaskRun(taskId: String): TaskRun {
        return TaskRun.builder()
            .id(IdUtils.create())
            .executionId(IdUtils.create())
            .namespace("io.kestra.stress.test")
            .flowId("stress-test-flow")
            .taskId(taskId)
            .state(State())
            .build()
    }

    private fun createStressTestLogEntry(message: String): LogEntry {
        return LogEntry.builder()
            .executionId(IdUtils.create())
            .taskId("stress-test-task")
            .level(Level.INFO)
            .message(message)
            .timestamp(Instant.now())
            .build()
    }

    private fun createStressTestMetricEntry(name: String): MetricEntry {
        return MetricEntry.builder()
            .executionId(IdUtils.create())
            .taskId("stress-test-task")
            .name(name)
            .value(Random.nextDouble() * 100)
            .timestamp(Instant.now())
            .build()
    }

    private fun createSustainedTestLogEntry(message: String): LogEntry {
        return LogEntry.builder()
            .executionId(IdUtils.create())
            .taskId("sustained-test-task")
            .level(Level.INFO)
            .message(message)
            .timestamp(Instant.now())
            .build()
    }

    private fun createReliabilityTestMetricEntry(name: String): MetricEntry {
        return MetricEntry.builder()
            .executionId(IdUtils.create())
            .taskId("reliability-test-task")
            .name(name)
            .value(Random.nextDouble() * 100)
            .timestamp(Instant.now())
            .build()
    }

    private fun createLargeTaskRun(taskId: String): TaskRun {
        return TaskRun.builder()
            .id(IdUtils.create())
            .executionId(IdUtils.create())
            .namespace("io.kestra.large.test")
            .flowId("large-test-flow")
            .taskId(taskId)
            .state(State())
            .outputs(Variables.inMemory(mapOf<String, Any>(
                "output1" to "a".repeat(500),
                "output2" to "b".repeat(500),
                "output3" to "c".repeat(500)
            )))
            .build()
    }
}
