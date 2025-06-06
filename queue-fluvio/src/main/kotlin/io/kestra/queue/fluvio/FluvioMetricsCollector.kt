package io.kestra.queue.fluvio

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import jakarta.inject.Singleton

/**
 * Fluvio队列性能指标收集器
 * 提供详细的性能监控和指标收集功能
 */
@Singleton
class FluvioMetricsCollector(
    private val meterRegistry: MeterRegistry
) {

    private val logger = LoggerFactory.getLogger(FluvioMetricsCollector::class.java)

    // 基础计数器
    private val messagesSent = AtomicLong(0)
    private val messagesReceived = AtomicLong(0)
    private val messagesProcessed = AtomicLong(0)
    private val messagesFailed = AtomicLong(0)
    
    // 性能指标
    private val totalLatency = AtomicLong(0)
    private val latencyCount = AtomicLong(0) // 记录延迟的消息数量
    private val maxLatency = AtomicLong(0)
    private val minLatency = AtomicLong(Long.MAX_VALUE)
    
    // 队列特定指标
    private val queueMetrics = ConcurrentHashMap<String, QueueMetrics>()
    
    // 连接状态
    private val connectionStatus = AtomicReference(ConnectionStatus.DISCONNECTED)
    private val lastConnectionTime = AtomicReference<Instant>()
    
    // Micrometer指标
    private val sendCounter: Counter
    private val receiveCounter: Counter
    private val errorCounter: Counter
    private val latencyTimer: Timer
    
    init {
        // 初始化Micrometer指标
        sendCounter = Counter.builder("fluvio.queue.messages.sent")
            .description("Total number of messages sent to Fluvio")
            .register(meterRegistry)
            
        receiveCounter = Counter.builder("fluvio.queue.messages.received")
            .description("Total number of messages received from Fluvio")
            .register(meterRegistry)
            
        errorCounter = Counter.builder("fluvio.queue.errors")
            .description("Total number of errors in Fluvio operations")
            .register(meterRegistry)
            
        latencyTimer = Timer.builder("fluvio.queue.latency")
            .description("Message processing latency")
            .register(meterRegistry)
            
        // 注册Gauge指标
        registerGauges()
    }

    /**
     * 记录消息发送
     */
    fun recordMessageSent(queueType: String, consumerGroup: String?, latencyMs: Long = 0) {
        messagesSent.incrementAndGet()
        sendCounter.increment()
        
        if (latencyMs > 0) {
            recordLatency(latencyMs)
        }
        
        getQueueMetrics(queueType, consumerGroup).recordSent()
        
        logger.debug("Message sent to queue: {} group: {} latency: {}ms", queueType, consumerGroup, latencyMs)
    }

    /**
     * 记录消息接收
     */
    fun recordMessageReceived(queueType: String, consumerGroup: String?, latencyMs: Long = 0) {
        messagesReceived.incrementAndGet()
        receiveCounter.increment()
        
        if (latencyMs > 0) {
            recordLatency(latencyMs)
        }
        
        getQueueMetrics(queueType, consumerGroup).recordReceived()
        
        logger.debug("Message received from queue: {} group: {} latency: {}ms", queueType, consumerGroup, latencyMs)
    }

    /**
     * 记录消息处理完成
     */
    fun recordMessageProcessed(queueType: String, consumerGroup: String?, processingTimeMs: Long) {
        messagesProcessed.incrementAndGet()
        latencyTimer.record(Duration.ofMillis(processingTimeMs))
        
        getQueueMetrics(queueType, consumerGroup).recordProcessed(processingTimeMs)
        
        logger.debug("Message processed for queue: {} group: {} time: {}ms", queueType, consumerGroup, processingTimeMs)
    }

    /**
     * 记录错误
     */
    fun recordError(queueType: String, consumerGroup: String?, errorType: String, exception: Throwable?) {
        messagesFailed.incrementAndGet()
        errorCounter.increment()
        
        getQueueMetrics(queueType, consumerGroup).recordError(errorType)
        
        logger.warn("Error in queue: {} group: {} type: {}", queueType, consumerGroup, errorType, exception)
    }

    /**
     * 记录连接状态变化
     */
    fun recordConnectionStatus(status: ConnectionStatus) {
        val previousStatus = connectionStatus.getAndSet(status)
        
        if (status == ConnectionStatus.CONNECTED) {
            lastConnectionTime.set(Instant.now())
        }
        
        if (previousStatus != status) {
            logger.info("Fluvio connection status changed: {} -> {}", previousStatus, status)
            
            meterRegistry.counter("fluvio.connection.status.changes", 
                "from", previousStatus.name.lowercase(),
                "to", status.name.lowercase()
            ).increment()
        }
    }

    /**
     * 记录延迟
     */
    private fun recordLatency(latencyMs: Long) {
        totalLatency.addAndGet(latencyMs)
        latencyCount.incrementAndGet()

        // 更新最大延迟
        var currentMax = maxLatency.get()
        while (latencyMs > currentMax && !maxLatency.compareAndSet(currentMax, latencyMs)) {
            currentMax = maxLatency.get()
        }

        // 更新最小延迟
        var currentMin = minLatency.get()
        while (latencyMs < currentMin && !minLatency.compareAndSet(currentMin, latencyMs)) {
            currentMin = minLatency.get()
        }
    }

    /**
     * 获取队列特定指标
     */
    private fun getQueueMetrics(queueType: String, consumerGroup: String?): QueueMetrics {
        val key = "${queueType}:${consumerGroup ?: "default"}"
        return queueMetrics.computeIfAbsent(key) { QueueMetrics(queueType, consumerGroup) }
    }

    /**
     * 注册Gauge指标
     */
    private fun registerGauges() {
        // 使用简单的Gauge注册方式
        meterRegistry.gauge("fluvio.queue.messages.sent.total", this) { it.messagesSent.get().toDouble() }
        meterRegistry.gauge("fluvio.queue.messages.received.total", this) { it.messagesReceived.get().toDouble() }
        meterRegistry.gauge("fluvio.queue.messages.processed.total", this) { it.messagesProcessed.get().toDouble() }
        meterRegistry.gauge("fluvio.queue.messages.failed.total", this) { it.messagesFailed.get().toDouble() }
        meterRegistry.gauge("fluvio.queue.latency.max", this) { it.maxLatency.get().toDouble() }
        meterRegistry.gauge("fluvio.queue.latency.min", this) {
            val min = it.minLatency.get()
            if (min == Long.MAX_VALUE) 0.0 else min.toDouble()
        }
        meterRegistry.gauge("fluvio.queue.latency.avg", this) {
            val count = it.latencyCount.get()
            if (count > 0) it.totalLatency.get().toDouble() / count else 0.0
        }
        meterRegistry.gauge("fluvio.connection.status", this) {
            if (it.connectionStatus.get() == ConnectionStatus.CONNECTED) 1.0 else 0.0
        }
        meterRegistry.gauge("fluvio.queue.types.count", this) { it.queueMetrics.size.toDouble() }
    }

    /**
     * 获取性能统计信息
     */
    fun getPerformanceStatistics(): PerformanceStatistics {
        val sent = messagesSent.get()
        val received = messagesReceived.get()
        val processed = messagesProcessed.get()
        val failed = messagesFailed.get()
        
        val avgLatency = if (latencyCount.get() > 0) totalLatency.get().toDouble() / latencyCount.get() else 0.0
        val successRate = if (sent > 0) ((sent - failed).toDouble() / sent * 100) else 100.0
        
        return PerformanceStatistics(
            messagesSent = sent,
            messagesReceived = received,
            messagesProcessed = processed,
            messagesFailed = failed,
            averageLatencyMs = avgLatency,
            maxLatencyMs = maxLatency.get(),
            minLatencyMs = if (minLatency.get() == Long.MAX_VALUE) 0 else minLatency.get(),
            successRate = successRate,
            connectionStatus = connectionStatus.get(),
            lastConnectionTime = lastConnectionTime.get(),
            queueCount = queueMetrics.size,
            queueMetrics = queueMetrics.values.toList()
        )
    }

    /**
     * 重置统计信息
     */
    fun resetStatistics() {
        messagesSent.set(0)
        messagesReceived.set(0)
        messagesProcessed.set(0)
        messagesFailed.set(0)
        totalLatency.set(0)
        latencyCount.set(0)
        maxLatency.set(0)
        minLatency.set(Long.MAX_VALUE)
        queueMetrics.clear()
        
        logger.info("Fluvio metrics statistics reset")
    }

    /**
     * 获取队列健康状态
     */
    fun getHealthStatus(): HealthStatus {
        val stats = getPerformanceStatistics()
        
        val isHealthy = stats.connectionStatus == ConnectionStatus.CONNECTED &&
                       stats.successRate > 95.0 &&
                       stats.averageLatencyMs < 100.0
        
        val issues = mutableListOf<String>()
        
        if (stats.connectionStatus != ConnectionStatus.CONNECTED) {
            issues.add("Fluvio connection not established")
        }
        
        if (stats.successRate <= 95.0) {
            issues.add("Success rate too low: ${String.format("%.2f", stats.successRate)}%")
        }
        
        if (stats.averageLatencyMs >= 100.0) {
            issues.add("Average latency too high: ${String.format("%.2f", stats.averageLatencyMs)}ms")
        }
        
        return HealthStatus(
            healthy = isHealthy,
            issues = issues,
            lastCheck = Instant.now()
        )
    }
}

/**
 * 连接状态枚举
 */
enum class ConnectionStatus {
    CONNECTED,
    DISCONNECTED,
    CONNECTING,
    ERROR
}

/**
 * 队列特定指标
 */
data class QueueMetrics(
    val queueType: String,
    val consumerGroup: String?
) {
    private val sent = AtomicLong(0)
    private val received = AtomicLong(0)
    private val processed = AtomicLong(0)
    private val errors = ConcurrentHashMap<String, AtomicLong>()
    private val totalProcessingTime = AtomicLong(0)
    
    fun recordSent() = sent.incrementAndGet()
    fun recordReceived() = received.incrementAndGet()
    fun recordProcessed(processingTimeMs: Long) {
        processed.incrementAndGet()
        totalProcessingTime.addAndGet(processingTimeMs)
    }
    fun recordError(errorType: String) {
        errors.computeIfAbsent(errorType) { AtomicLong(0) }.incrementAndGet()
    }
    
    fun getSent() = sent.get()
    fun getReceived() = received.get()
    fun getProcessed() = processed.get()
    fun getErrors() = errors.mapValues { it.value.get() }
    fun getAverageProcessingTime() = if (processed.get() > 0) totalProcessingTime.get().toDouble() / processed.get() else 0.0
}

/**
 * 性能统计信息
 */
data class PerformanceStatistics(
    val messagesSent: Long,
    val messagesReceived: Long,
    val messagesProcessed: Long,
    val messagesFailed: Long,
    val averageLatencyMs: Double,
    val maxLatencyMs: Long,
    val minLatencyMs: Long,
    val successRate: Double,
    val connectionStatus: ConnectionStatus,
    val lastConnectionTime: Instant?,
    val queueCount: Int,
    val queueMetrics: List<QueueMetrics>
)

/**
 * 健康状态
 */
data class HealthStatus(
    val healthy: Boolean,
    val issues: List<String>,
    val lastCheck: Instant
)
