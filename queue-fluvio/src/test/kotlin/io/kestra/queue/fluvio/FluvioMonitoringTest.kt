package io.kestra.queue.fluvio

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import java.time.Instant

/**
 * Fluvio监控功能测试
 * 验证指标收集、健康检查和监控功能
 */
class FluvioMonitoringTest : StringSpec({

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

    "should create health checker" {
        val meterRegistry = SimpleMeterRegistry()
        val metricsCollector = FluvioMetricsCollector(meterRegistry)
        val config = FluvioQueueConfiguration()
        val healthChecker = FluvioHealthChecker(metricsCollector, config)

        healthChecker shouldNotBe null
    }

    "should perform deep health check" {
        val meterRegistry = SimpleMeterRegistry()
        val metricsCollector = FluvioMetricsCollector(meterRegistry)
        val config = FluvioQueueConfiguration()
        val healthChecker = FluvioHealthChecker(metricsCollector, config)

        val result = healthChecker.performDeepHealthCheck()

        result shouldNotBe null
        result.basicHealth shouldNotBe null
        result.connectionTest shouldNotBe null
        result.performanceTest shouldNotBe null
        result.configValidation shouldNotBe null
        result.checkDuration shouldNotBe null
    }

    "should create queue monitor" {
        val meterRegistry = SimpleMeterRegistry()
        val metricsCollector = FluvioMetricsCollector(meterRegistry)
        val config = FluvioQueueConfiguration()
        val healthChecker = FluvioHealthChecker(metricsCollector, config)
        val monitorConfig = MonitorConfig(
            healthCheckIntervalSeconds = 10, // 较长间隔避免测试中的竞争条件
            performanceCheckIntervalSeconds = 10,
            alertCheckIntervalSeconds = 10
        )
        val monitor = FluvioQueueMonitor(metricsCollector, healthChecker, meterRegistry, monitorConfig)

        monitor shouldNotBe null
        
        // 验证初始状态
        val status = monitor.getMonitoringStatus()
        status.isRunning shouldBe false
        status.config shouldBe monitorConfig
    }

    "should get real-time statistics" {
        val meterRegistry = SimpleMeterRegistry()
        val metricsCollector = FluvioMetricsCollector(meterRegistry)
        val config = FluvioQueueConfiguration()
        val healthChecker = FluvioHealthChecker(metricsCollector, config)
        val monitor = FluvioQueueMonitor(metricsCollector, healthChecker, meterRegistry)

        // 记录一些数据
        metricsCollector.recordMessageSent("test", "group", 25)
        metricsCollector.recordMessageReceived("test", "group", 30)

        // 获取实时统计
        val realTimeStats = monitor.getRealTimeStatistics()

        realTimeStats shouldNotBe null
        realTimeStats.timestamp shouldNotBe null
        realTimeStats.performanceStats shouldNotBe null
        realTimeStats.healthStatus shouldNotBe null
        realTimeStats.monitoringStatus shouldNotBe null
        realTimeStats.activeAlerts shouldNotBe null

        // 验证性能统计
        realTimeStats.performanceStats.messagesSent shouldBe 1
        realTimeStats.performanceStats.messagesReceived shouldBe 1
    }

    "should validate connection status enum" {
        val statuses = ConnectionStatus.values()
        statuses.contains(ConnectionStatus.CONNECTED) shouldBe true
        statuses.contains(ConnectionStatus.DISCONNECTED) shouldBe true
        statuses.contains(ConnectionStatus.CONNECTING) shouldBe true
        statuses.contains(ConnectionStatus.ERROR) shouldBe true
    }

    "should validate alert types and severities" {
        // 验证告警类型枚举
        val alertTypes = AlertType.values()
        alertTypes.contains(AlertType.HIGH_LATENCY) shouldBe true
        alertTypes.contains(AlertType.LOW_SUCCESS_RATE) shouldBe true
        alertTypes.contains(AlertType.UNHEALTHY) shouldBe true
        alertTypes.contains(AlertType.CONNECTION_FAILED) shouldBe true

        // 验证告警严重程度枚举
        val severities = AlertSeverity.values()
        severities.contains(AlertSeverity.INFO) shouldBe true
        severities.contains(AlertSeverity.WARNING) shouldBe true
        severities.contains(AlertSeverity.CRITICAL) shouldBe true
    }

    "should handle alert equality correctly" {
        val alert1 = Alert(
            type = AlertType.HIGH_LATENCY,
            message = "High latency detected",
            severity = AlertSeverity.WARNING,
            timestamp = Instant.now()
        )

        val alert2 = Alert(
            type = AlertType.HIGH_LATENCY,
            message = "Different message",
            severity = AlertSeverity.WARNING,
            timestamp = Instant.now().plusSeconds(10)
        )

        val alert3 = Alert(
            type = AlertType.LOW_SUCCESS_RATE,
            message = "Low success rate",
            severity = AlertSeverity.CRITICAL,
            timestamp = Instant.now()
        )

        // 相同类型和严重程度的告警应该相等（用于去重）
        alert1 shouldBe alert2
        alert1.hashCode() shouldBe alert2.hashCode()

        // 不同类型或严重程度的告警应该不相等
        (alert1 == alert3) shouldBe false
    }

    "should handle concurrent access" {
        val meterRegistry = SimpleMeterRegistry()
        val collector = FluvioMetricsCollector(meterRegistry)

        // 并发记录消息
        val threads = (1..5).map { threadIndex ->
            Thread {
                repeat(50) { messageIndex ->
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
        stats.messagesSent shouldBe 250 // 5 threads * 50 messages
        stats.messagesReceived shouldBe 250
        stats.messagesFailed shouldBe 25 // 5 threads * 5 errors
        stats.queueCount shouldBe 5 // 5 different groups
    }
})
