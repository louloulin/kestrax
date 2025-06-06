package io.kestra.queue.fluvio

import io.micrometer.core.instrument.MeterRegistry
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.Instant
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import jakarta.inject.Singleton
import jakarta.annotation.PreDestroy

/**
 * Fluvio队列监控器
 * 提供实时监控、告警和自动恢复功能
 */
@Singleton
class FluvioQueueMonitor(
    private val metricsCollector: FluvioMetricsCollector,
    private val healthChecker: FluvioHealthChecker,
    private val meterRegistry: MeterRegistry,
    private val config: MonitorConfig = MonitorConfig()
) {

    private val logger = LoggerFactory.getLogger(FluvioQueueMonitor::class.java)
    private val scheduler: ScheduledExecutorService = Executors.newScheduledThreadPool(3)
    private val isRunning = AtomicBoolean(false)
    private val lastHealthCheck = AtomicReference<Instant>()
    private val alertManager = AlertManager(meterRegistry)

    /**
     * 启动监控
     */
    fun startMonitoring() {
        if (isRunning.compareAndSet(false, true)) {
            logger.info("Starting Fluvio queue monitoring with config: {}", config)
            
            // 健康检查任务
            scheduler.scheduleAtFixedRate(
                { performHealthCheck() },
                0,
                config.healthCheckIntervalSeconds,
                TimeUnit.SECONDS
            )
            
            // 性能监控任务
            scheduler.scheduleAtFixedRate(
                { performPerformanceMonitoring() },
                config.performanceCheckDelaySeconds,
                config.performanceCheckIntervalSeconds,
                TimeUnit.SECONDS
            )
            
            // 告警检查任务
            scheduler.scheduleAtFixedRate(
                { performAlertCheck() },
                config.alertCheckDelaySeconds,
                config.alertCheckIntervalSeconds,
                TimeUnit.SECONDS
            )
            
            logger.info("Fluvio queue monitoring started successfully")
        } else {
            logger.warn("Fluvio queue monitoring is already running")
        }
    }

    /**
     * 停止监控
     */
    @PreDestroy
    fun stopMonitoring() {
        if (isRunning.compareAndSet(true, false)) {
            logger.info("Stopping Fluvio queue monitoring")
            
            scheduler.shutdown()
            try {
                if (!scheduler.awaitTermination(30, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow()
                }
            } catch (e: InterruptedException) {
                scheduler.shutdownNow()
                Thread.currentThread().interrupt()
            }
            
            logger.info("Fluvio queue monitoring stopped")
        }
    }

    /**
     * 执行健康检查
     */
    private fun performHealthCheck() {
        try {
            logger.debug("Performing scheduled health check")
            
            val healthResult = healthChecker.performDeepHealthCheck()
            lastHealthCheck.set(Instant.now())
            
            // 记录健康状态指标
            meterRegistry.gauge("fluvio.monitor.health.status", 
                if (healthResult.healthy) 1.0 else 0.0)
            
            meterRegistry.gauge("fluvio.monitor.health.check.duration", 
                healthResult.checkDuration.toMillis().toDouble())
            
            // 处理健康检查结果
            if (!healthResult.healthy) {
                handleUnhealthyState(healthResult)
            } else {
                handleHealthyState(healthResult)
            }
            
        } catch (e: Exception) {
            logger.error("Error during scheduled health check", e)
            meterRegistry.counter("fluvio.monitor.health.check.errors").increment()
        }
    }

    /**
     * 执行性能监控
     */
    private fun performPerformanceMonitoring() {
        try {
            logger.debug("Performing performance monitoring")
            
            val stats = metricsCollector.getPerformanceStatistics()
            
            // 记录性能指标
            recordPerformanceMetrics(stats)
            
            // 检查性能阈值
            checkPerformanceThresholds(stats)
            
        } catch (e: Exception) {
            logger.error("Error during performance monitoring", e)
            meterRegistry.counter("fluvio.monitor.performance.errors").increment()
        }
    }

    /**
     * 执行告警检查
     */
    private fun performAlertCheck() {
        try {
            logger.debug("Performing alert check")
            
            val stats = metricsCollector.getPerformanceStatistics()
            val healthStatus = metricsCollector.getHealthStatus()
            
            // 检查各种告警条件
            alertManager.checkAlerts(stats, healthStatus, config)
            
        } catch (e: Exception) {
            logger.error("Error during alert check", e)
            meterRegistry.counter("fluvio.monitor.alert.check.errors").increment()
        }
    }

    /**
     * 处理不健康状态
     */
    private fun handleUnhealthyState(healthResult: DeepHealthCheckResult) {
        logger.warn("Fluvio queue is unhealthy: {}", healthResult.basicHealth.issues)
        
        // 记录不健康事件
        meterRegistry.counter("fluvio.monitor.unhealthy.events").increment()
        
        // 尝试自动恢复
        if (config.autoRecoveryEnabled) {
            attemptAutoRecovery(healthResult)
        }
        
        // 发送告警
        alertManager.sendHealthAlert(healthResult)
    }

    /**
     * 处理健康状态
     */
    private fun handleHealthyState(healthResult: DeepHealthCheckResult) {
        logger.debug("Fluvio queue is healthy")
        
        // 清除之前的告警
        alertManager.clearHealthAlerts()
    }

    /**
     * 尝试自动恢复
     */
    private fun attemptAutoRecovery(healthResult: DeepHealthCheckResult) {
        logger.info("Attempting auto recovery for unhealthy Fluvio queue")
        
        try {
            // 重置连接状态
            if (!healthResult.connectionTest.success) {
                logger.info("Attempting to reset connection")
                metricsCollector.recordConnectionStatus(ConnectionStatus.CONNECTING)
                // 这里可以实现实际的连接重置逻辑
            }
            
            // 清理指标
            if (config.resetMetricsOnRecovery) {
                logger.info("Resetting metrics as part of recovery")
                metricsCollector.resetStatistics()
            }
            
            meterRegistry.counter("fluvio.monitor.recovery.attempts").increment()
            
        } catch (e: Exception) {
            logger.error("Auto recovery failed", e)
            meterRegistry.counter("fluvio.monitor.recovery.failures").increment()
        }
    }

    /**
     * 记录性能指标
     */
    private fun recordPerformanceMetrics(stats: PerformanceStatistics) {
        meterRegistry.gauge("fluvio.monitor.messages.sent.rate", 
            calculateRate(stats.messagesSent))
        
        meterRegistry.gauge("fluvio.monitor.messages.received.rate", 
            calculateRate(stats.messagesReceived))
        
        meterRegistry.gauge("fluvio.monitor.success.rate", stats.successRate)
        
        meterRegistry.gauge("fluvio.monitor.latency.avg", stats.averageLatencyMs)
        meterRegistry.gauge("fluvio.monitor.latency.max", stats.maxLatencyMs.toDouble())
        
        meterRegistry.gauge("fluvio.monitor.queue.count", stats.queueCount.toDouble())
    }

    /**
     * 检查性能阈值
     */
    private fun checkPerformanceThresholds(stats: PerformanceStatistics) {
        // 检查延迟阈值
        if (stats.averageLatencyMs > config.latencyThresholdMs) {
            logger.warn("Average latency {} exceeds threshold {}", 
                stats.averageLatencyMs, config.latencyThresholdMs)
            meterRegistry.counter("fluvio.monitor.threshold.latency.violations").increment()
        }
        
        // 检查成功率阈值
        if (stats.successRate < config.successRateThreshold) {
            logger.warn("Success rate {} below threshold {}", 
                stats.successRate, config.successRateThreshold)
            meterRegistry.counter("fluvio.monitor.threshold.success.violations").increment()
        }
        
        // 检查错误率
        val errorRate = if (stats.messagesSent > 0) {
            stats.messagesFailed.toDouble() / stats.messagesSent * 100
        } else 0.0
        
        if (errorRate > config.errorRateThreshold) {
            logger.warn("Error rate {} exceeds threshold {}", 
                errorRate, config.errorRateThreshold)
            meterRegistry.counter("fluvio.monitor.threshold.error.violations").increment()
        }
    }

    /**
     * 计算速率（简化实现）
     */
    private fun calculateRate(total: Long): Double {
        // 这里可以实现更复杂的速率计算逻辑
        return total.toDouble()
    }

    /**
     * 获取监控状态
     */
    fun getMonitoringStatus(): MonitoringStatus {
        return MonitoringStatus(
            isRunning = isRunning.get(),
            lastHealthCheck = lastHealthCheck.get(),
            config = config,
            alertsActive = alertManager.getActiveAlerts().size
        )
    }

    /**
     * 强制执行健康检查
     */
    fun forceHealthCheck(): DeepHealthCheckResult {
        logger.info("Forcing health check")
        return healthChecker.performDeepHealthCheck()
    }

    /**
     * 获取实时统计信息
     */
    fun getRealTimeStatistics(): RealTimeStatistics {
        val stats = metricsCollector.getPerformanceStatistics()
        val healthStatus = metricsCollector.getHealthStatus()
        
        return RealTimeStatistics(
            timestamp = Instant.now(),
            performanceStats = stats,
            healthStatus = healthStatus,
            monitoringStatus = getMonitoringStatus(),
            activeAlerts = alertManager.getActiveAlerts()
        )
    }
}

/**
 * 告警管理器
 */
class AlertManager(private val meterRegistry: MeterRegistry) {
    
    private val logger = LoggerFactory.getLogger(AlertManager::class.java)
    private val activeAlerts = mutableSetOf<Alert>()
    
    fun checkAlerts(stats: PerformanceStatistics, healthStatus: io.kestra.queue.fluvio.HealthStatus, config: MonitorConfig) {
        // 检查延迟告警
        if (stats.averageLatencyMs > config.latencyAlertThresholdMs) {
            addAlert(Alert(
                type = AlertType.HIGH_LATENCY,
                message = "High average latency: ${stats.averageLatencyMs}ms",
                severity = AlertSeverity.WARNING,
                timestamp = Instant.now()
            ))
        }
        
        // 检查健康状态告警
        if (!healthStatus.healthy) {
            addAlert(Alert(
                type = AlertType.UNHEALTHY,
                message = "Queue unhealthy: ${healthStatus.issues.joinToString(", ")}",
                severity = AlertSeverity.CRITICAL,
                timestamp = Instant.now()
            ))
        }
    }
    
    fun sendHealthAlert(healthResult: DeepHealthCheckResult) {
        addAlert(Alert(
            type = AlertType.HEALTH_CHECK_FAILED,
            message = "Health check failed: ${healthResult.basicHealth.issues.joinToString(", ")}",
            severity = AlertSeverity.CRITICAL,
            timestamp = Instant.now()
        ))
    }
    
    fun clearHealthAlerts() {
        activeAlerts.removeIf { it.type == AlertType.UNHEALTHY || it.type == AlertType.HEALTH_CHECK_FAILED }
    }
    
    private fun addAlert(alert: Alert) {
        if (activeAlerts.add(alert)) {
            logger.warn("New alert: {}", alert)
            meterRegistry.counter("fluvio.monitor.alerts", 
                "type", alert.type.name.lowercase(),
                "severity", alert.severity.name.lowercase()
            ).increment()
        }
    }
    
    fun getActiveAlerts(): Set<Alert> = activeAlerts.toSet()
}

/**
 * 监控配置
 */
data class MonitorConfig(
    val healthCheckIntervalSeconds: Long = 30,
    val performanceCheckIntervalSeconds: Long = 15,
    val performanceCheckDelaySeconds: Long = 10,
    val alertCheckIntervalSeconds: Long = 60,
    val alertCheckDelaySeconds: Long = 30,
    val autoRecoveryEnabled: Boolean = true,
    val resetMetricsOnRecovery: Boolean = false,
    val latencyThresholdMs: Double = 100.0,
    val latencyAlertThresholdMs: Double = 200.0,
    val successRateThreshold: Double = 95.0,
    val errorRateThreshold: Double = 5.0
)

/**
 * 监控状态
 */
data class MonitoringStatus(
    val isRunning: Boolean,
    val lastHealthCheck: Instant?,
    val config: MonitorConfig,
    val alertsActive: Int
)

/**
 * 实时统计信息
 */
data class RealTimeStatistics(
    val timestamp: Instant,
    val performanceStats: PerformanceStatistics,
    val healthStatus: io.kestra.queue.fluvio.HealthStatus,
    val monitoringStatus: MonitoringStatus,
    val activeAlerts: Set<Alert>
)

/**
 * 告警类型
 */
enum class AlertType {
    HIGH_LATENCY,
    LOW_SUCCESS_RATE,
    HIGH_ERROR_RATE,
    UNHEALTHY,
    CONNECTION_FAILED,
    HEALTH_CHECK_FAILED
}

/**
 * 告警严重程度
 */
enum class AlertSeverity {
    INFO,
    WARNING,
    CRITICAL
}

/**
 * 告警
 */
data class Alert(
    val type: AlertType,
    val message: String,
    val severity: AlertSeverity,
    val timestamp: Instant
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Alert) return false
        return type == other.type && severity == other.severity
    }
    
    override fun hashCode(): Int {
        return type.hashCode() * 31 + severity.hashCode()
    }
}
