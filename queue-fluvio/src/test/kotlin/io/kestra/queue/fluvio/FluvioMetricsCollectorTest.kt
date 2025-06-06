package io.kestra.queue.fluvio

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import java.time.Instant

/**
 * Fluvio指标收集器测试
 * 验证性能监控和指标收集功能
 */
class FluvioMetricsCollectorTest : StringSpec({

    "should create metrics collector with meter registry" {
        val meterRegistry = SimpleMeterRegistry()
        val collector = FluvioMetricsCollector(meterRegistry)

        collector shouldNotBe null
        
        // 验证初始统计信息
        val stats = collector.getPerformanceStatistics()
        stats.messagesSent shouldBe 0
        stats.messagesReceived shouldBe 0
        stats.messagesProcessed shouldBe 0
        stats.messagesFailed shouldBe 0
    }

    "should record message sent correctly" {
        val meterRegistry = SimpleMeterRegistry()
        val collector = FluvioMetricsCollector(meterRegistry)

        // 记录发送的消息
        collector.recordMessageSent("execution", "test-group", 25)
        collector.recordMessageSent("logEntry", "log-group", 15)
        collector.recordMessageSent("execution", "test-group", 35)

        val stats = collector.getPerformanceStatistics()
        stats.messagesSent shouldBe 3
        stats.averageLatencyMs shouldBeGreaterThan 0.0
        stats.maxLatencyMs shouldBe 35
        stats.minLatencyMs shouldBe 15

        // 验证Micrometer指标
        val sendCounter = meterRegistry.find("fluvio.queue.messages.sent").counter()
        sendCounter shouldNotBe null
        sendCounter.count() shouldBe 3.0
    }

    "should record message received correctly" {
        val meterRegistry = SimpleMeterRegistry()
        val collector = FluvioMetricsCollector(meterRegistry)

        // 记录接收的消息
        collector.recordMessageReceived("execution", "test-group", 20)
        collector.recordMessageReceived("logEntry", null, 30)

        val stats = collector.getPerformanceStatistics()
        stats.messagesReceived shouldBe 2
        stats.averageLatencyMs shouldBe 25.0

        // 验证Micrometer指标
        val receiveCounter = meterRegistry.find("fluvio.queue.messages.received").counter()
        receiveCounter shouldNotBe null
        receiveCounter.count() shouldBe 2.0
    }

    "should record message processed correctly" {
        val meterRegistry = SimpleMeterRegistry()
        val collector = FluvioMetricsCollector(meterRegistry)

        // 记录处理的消息
        collector.recordMessageProcessed("execution", "test-group", 50)
        collector.recordMessageProcessed("execution", "test-group", 75)

        val stats = collector.getPerformanceStatistics()
        stats.messagesProcessed shouldBe 2

        // 验证延迟计时器
        val latencyTimer = meterRegistry.find("fluvio.queue.latency").timer()
        latencyTimer shouldNotBe null
        latencyTimer.count() shouldBe 2L
    }

    "should record errors correctly" {
        val meterRegistry = SimpleMeterRegistry()
        val collector = FluvioMetricsCollector(meterRegistry)

        // 记录错误
        collector.recordError("execution", "test-group", "CONNECTION_ERROR", RuntimeException("Test error"))
        collector.recordError("logEntry", null, "SERIALIZATION_ERROR", null)

        val stats = collector.getPerformanceStatistics()
        stats.messagesFailed shouldBe 2

        // 验证错误计数器
        val errorCounter = meterRegistry.find("fluvio.queue.errors").counter()
        errorCounter shouldNotBe null
        errorCounter.count() shouldBe 2.0
    }

    "should track connection status changes" {
        val meterRegistry = SimpleMeterRegistry()
        val collector = FluvioMetricsCollector(meterRegistry)

        // 初始状态应该是DISCONNECTED
        val initialStats = collector.getPerformanceStatistics()
        initialStats.connectionStatus shouldBe ConnectionStatus.DISCONNECTED

        // 更改连接状态
        collector.recordConnectionStatus(ConnectionStatus.CONNECTING)
        collector.recordConnectionStatus(ConnectionStatus.CONNECTED)

        val stats = collector.getPerformanceStatistics()
        stats.connectionStatus shouldBe ConnectionStatus.CONNECTED
        stats.lastConnectionTime shouldNotBe null

        // 验证连接状态变化计数器
        val statusChangeCounter = meterRegistry.find("fluvio.connection.status.changes").counter()
        statusChangeCounter shouldNotBe null
        statusChangeCounter.count() shouldBeGreaterThan 0.0
    }

    "should calculate success rate correctly" {
        val meterRegistry = SimpleMeterRegistry()
        val collector = FluvioMetricsCollector(meterRegistry)

        // 发送10条消息，2条失败
        repeat(10) {
            collector.recordMessageSent("test", "group")
        }
        repeat(2) {
            collector.recordError("test", "group", "ERROR", null)
        }

        val stats = collector.getPerformanceStatistics()
        stats.messagesSent shouldBe 10
        stats.messagesFailed shouldBe 2
        stats.successRate shouldBe 80.0 // (10-2)/10 * 100
    }

    "should track queue-specific metrics" {
        val meterRegistry = SimpleMeterRegistry()
        val collector = FluvioMetricsCollector(meterRegistry)

        // 记录不同队列类型的消息
        collector.recordMessageSent("execution", "group1")
        collector.recordMessageSent("logEntry", "group2")
        collector.recordMessageSent("metricEntry", null)

        val stats = collector.getPerformanceStatistics()
        stats.queueCount shouldBe 3 // execution:group1, logEntry:group2, metricEntry:default

        // 验证队列数量Gauge
        val queueCountGauge = meterRegistry.find("fluvio.queue.types.count").gauge()
        queueCountGauge shouldNotBe null
        queueCountGauge.value() shouldBe 3.0
    }

    "should provide health status" {
        val meterRegistry = SimpleMeterRegistry()
        val collector = FluvioMetricsCollector(meterRegistry)

        // 初始健康状态
        val initialHealth = collector.getHealthStatus()
        initialHealth.healthy shouldBe false // 因为没有连接
        initialHealth.issues.size shouldBeGreaterThan 0

        // 设置连接状态并发送一些成功消息
        collector.recordConnectionStatus(ConnectionStatus.CONNECTED)
        repeat(100) {
            collector.recordMessageSent("test", "group", 10) // 低延迟
        }

        val healthyStatus = collector.getHealthStatus()
        healthyStatus.healthy shouldBe true
        healthyStatus.issues.size shouldBe 0
    }

    "should reset statistics correctly" {
        val meterRegistry = SimpleMeterRegistry()
        val collector = FluvioMetricsCollector(meterRegistry)

        // 记录一些数据
        collector.recordMessageSent("test", "group", 50)
        collector.recordMessageReceived("test", "group", 30)
        collector.recordError("test", "group", "ERROR", null)

        // 验证数据存在
        val statsBefore = collector.getPerformanceStatistics()
        statsBefore.messagesSent shouldBe 1
        statsBefore.messagesReceived shouldBe 1
        statsBefore.messagesFailed shouldBe 1

        // 重置统计信息
        collector.resetStatistics()

        // 验证数据被重置
        val statsAfter = collector.getPerformanceStatistics()
        statsAfter.messagesSent shouldBe 0
        statsAfter.messagesReceived shouldBe 0
        statsAfter.messagesFailed shouldBe 0
        statsAfter.queueCount shouldBe 0
    }

    "should handle concurrent access" {
        val meterRegistry = SimpleMeterRegistry()
        val collector = FluvioMetricsCollector(meterRegistry)

        // 并发记录消息
        val threads = (1..10).map { threadIndex ->
            Thread {
                repeat(100) { messageIndex ->
                    collector.recordMessageSent("concurrent-test", "group-$threadIndex", 10)
                    collector.recordMessageReceived("concurrent-test", "group-$threadIndex", 15)
                    
                    if (messageIndex % 10 == 0) {
                        collector.recordError("concurrent-test", "group-$threadIndex", "TEST_ERROR", null)
                    }
                }
            }
        }

        // 启动所有线程
        threads.forEach { it.start() }

        // 等待所有线程完成
        threads.forEach { it.join() }

        // 验证统计信息
        val stats = collector.getPerformanceStatistics()
        stats.messagesSent shouldBe 1000 // 10 threads * 100 messages
        stats.messagesReceived shouldBe 1000
        stats.messagesFailed shouldBe 100 // 10 threads * 10 errors
        stats.queueCount shouldBe 10 // 10 different groups
    }

    "should track latency statistics correctly" {
        val meterRegistry = SimpleMeterRegistry()
        val collector = FluvioMetricsCollector(meterRegistry)

        // 记录不同延迟的消息
        val latencies = listOf(10L, 20L, 30L, 40L, 50L)
        latencies.forEach { latency ->
            collector.recordMessageSent("test", "group", latency)
        }

        val stats = collector.getPerformanceStatistics()
        stats.minLatencyMs shouldBe 10
        stats.maxLatencyMs shouldBe 50
        stats.averageLatencyMs shouldBe 30.0 // (10+20+30+40+50)/5

        // 验证Gauge指标
        val maxLatencyGauge = meterRegistry.find("fluvio.queue.latency.max").gauge()
        maxLatencyGauge shouldNotBe null
        maxLatencyGauge.value() shouldBe 50.0

        val minLatencyGauge = meterRegistry.find("fluvio.queue.latency.min").gauge()
        minLatencyGauge shouldNotBe null
        minLatencyGauge.value() shouldBe 10.0

        val avgLatencyGauge = meterRegistry.find("fluvio.queue.latency.avg").gauge()
        avgLatencyGauge shouldNotBe null
        avgLatencyGauge.value() shouldBe 30.0
    }

    "should handle edge cases" {
        val meterRegistry = SimpleMeterRegistry()
        val collector = FluvioMetricsCollector(meterRegistry)

        // 测试空统计
        val emptyStats = collector.getPerformanceStatistics()
        emptyStats.averageLatencyMs shouldBe 0.0
        emptyStats.successRate shouldBe 100.0 // 没有消息时成功率为100%

        // 测试只有错误的情况
        collector.recordError("test", "group", "ERROR", null)
        val errorOnlyStats = collector.getPerformanceStatistics()
        errorOnlyStats.successRate shouldBe 100.0 // 没有发送消息，所以成功率仍为100%

        // 测试null消费者组
        collector.recordMessageSent("test", null, 25)
        val nullGroupStats = collector.getPerformanceStatistics()
        nullGroupStats.messagesSent shouldBe 1
    }

    "should validate connection status enum" {
        val statuses = ConnectionStatus.values()
        statuses.contains(ConnectionStatus.CONNECTED) shouldBe true
        statuses.contains(ConnectionStatus.DISCONNECTED) shouldBe true
        statuses.contains(ConnectionStatus.CONNECTING) shouldBe true
        statuses.contains(ConnectionStatus.ERROR) shouldBe true
    }
})
