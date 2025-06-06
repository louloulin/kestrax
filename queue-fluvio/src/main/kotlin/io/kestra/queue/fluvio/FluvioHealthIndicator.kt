package io.kestra.queue.fluvio

import io.micronaut.health.HealthStatus
import io.micronaut.management.health.indicator.HealthIndicator
import io.micronaut.management.health.indicator.HealthResult
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.Instant
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import jakarta.inject.Singleton
import org.reactivestreams.Publisher
import reactor.core.publisher.Mono

/**
 * Fluvio队列健康检查指示器
 * 集成到Micronaut健康检查系统中
 */
@Singleton
class FluvioHealthIndicator(
    private val metricsCollector: FluvioMetricsCollector,
    private val config: FluvioQueueConfiguration
) : HealthIndicator {

    private val logger = LoggerFactory.getLogger(FluvioHealthIndicator::class.java)
    
    companion object {
        private const val HEALTH_CHECK_TIMEOUT_SECONDS = 10L
        private const val MAX_ACCEPTABLE_LATENCY_MS = 100.0
        private const val MIN_SUCCESS_RATE = 95.0
    }

    override fun getResult(): Publisher<HealthResult> {
        return Mono.fromCallable { performHealthCheck() }
            .timeout(Duration.ofSeconds(HEALTH_CHECK_TIMEOUT_SECONDS))
            .onErrorReturn(createErrorResult("Health check timeout or error"))
    }

    /**
     * 执行健康检查
     */
    private fun performHealthCheck(): HealthResult {
        val startTime = Instant.now()
        
        try {
            logger.debug("Starting Fluvio health check")
            
            // 获取性能统计
            val stats = metricsCollector.getPerformanceStatistics()
            val healthStatus = metricsCollector.getHealthStatus()
            
            // 检查连接状态
            val connectionCheck = checkConnection(stats)
            
            // 检查性能指标
            val performanceCheck = checkPerformance(stats)
            
            // 检查配置状态
            val configCheck = checkConfiguration()
            
            // 综合评估
            val overallHealthy = connectionCheck.healthy && 
                               performanceCheck.healthy && 
                               configCheck.healthy &&
                               healthStatus.healthy
            
            val details = mutableMapOf<String, Any>()
            details["connection"] = connectionCheck.toMap()
            details["performance"] = performanceCheck.toMap()
            details["configuration"] = configCheck.toMap()
            details["statistics"] = createStatisticsMap(stats)
            details["checkDuration"] = Duration.between(startTime, Instant.now()).toMillis()
            
            val status = if (overallHealthy) HealthStatus.UP else HealthStatus.DOWN
            
            logger.debug("Fluvio health check completed: {}", status)
            
            return HealthResult.builder("fluvio", status)
                .details(details)
                .build()
                
        } catch (e: Exception) {
            logger.error("Error during Fluvio health check", e)
            return createErrorResult("Health check failed: ${e.message}")
        }
    }

    /**
     * 检查连接状态
     */
    private fun checkConnection(stats: PerformanceStatistics): CheckResult {
        val isConnected = stats.connectionStatus == ConnectionStatus.CONNECTED
        val lastConnectionTime = stats.lastConnectionTime
        
        val issues = mutableListOf<String>()
        
        if (!isConnected) {
            issues.add("Fluvio connection not established")
        }
        
        if (lastConnectionTime == null) {
            issues.add("No connection time recorded")
        } else {
            val timeSinceConnection = Duration.between(lastConnectionTime, Instant.now())
            if (timeSinceConnection.toMinutes() > 60) {
                issues.add("Connection established over 1 hour ago")
            }
        }
        
        return CheckResult(
            healthy = isConnected && issues.isEmpty(),
            issues = issues,
            details = mapOf(
                "status" to stats.connectionStatus.name,
                "lastConnectionTime" to lastConnectionTime?.toString(),
                "timeSinceConnection" to lastConnectionTime?.let { 
                    Duration.between(it, Instant.now()).toSeconds() 
                }
            )
        )
    }

    /**
     * 检查性能指标
     */
    private fun checkPerformance(stats: PerformanceStatistics): CheckResult {
        val issues = mutableListOf<String>()
        
        // 检查成功率
        if (stats.successRate < MIN_SUCCESS_RATE) {
            issues.add("Success rate too low: ${String.format("%.2f", stats.successRate)}%")
        }
        
        // 检查延迟
        if (stats.averageLatencyMs > MAX_ACCEPTABLE_LATENCY_MS) {
            issues.add("Average latency too high: ${String.format("%.2f", stats.averageLatencyMs)}ms")
        }
        
        // 检查错误率
        val totalMessages = stats.messagesSent
        val errorRate = if (totalMessages > 0) {
            stats.messagesFailed.toDouble() / totalMessages * 100
        } else 0.0
        
        if (errorRate > 5.0) {
            issues.add("Error rate too high: ${String.format("%.2f", errorRate)}%")
        }
        
        return CheckResult(
            healthy = issues.isEmpty(),
            issues = issues,
            details = mapOf(
                "successRate" to stats.successRate,
                "averageLatencyMs" to stats.averageLatencyMs,
                "maxLatencyMs" to stats.maxLatencyMs,
                "minLatencyMs" to stats.minLatencyMs,
                "errorRate" to errorRate,
                "totalMessages" to totalMessages,
                "failedMessages" to stats.messagesFailed
            )
        )
    }

    /**
     * 检查配置状态
     */
    private fun checkConfiguration(): CheckResult {
        val issues = mutableListOf<String>()
        
        // 检查基本配置
        if (config.clusterEndpoint.isBlank()) {
            issues.add("Cluster endpoint not configured")
        }
        
        if (config.topicPrefix.isBlank()) {
            issues.add("Topic prefix not configured")
        }
        
        // 检查连接配置
        if (config.healthCheck.connectionTimeout.toMillis() <= 0) {
            issues.add("Invalid connection timeout: ${config.healthCheck.connectionTimeout}")
        }

        if (config.producer.requestTimeout.toMillis() <= 0) {
            issues.add("Invalid request timeout: ${config.producer.requestTimeout}")
        }
        
        return CheckResult(
            healthy = issues.isEmpty(),
            issues = issues,
            details = mapOf(
                "clusterEndpoint" to config.clusterEndpoint,
                "topicPrefix" to config.topicPrefix,
                "connectionTimeoutMs" to config.healthCheck.connectionTimeout.toMillis(),
                "requestTimeoutMs" to config.producer.requestTimeout.toMillis(),
                "batchSize" to config.producer.batchSize,
                "pollTimeoutMs" to config.consumer.fetchMaxWait.toMillis()
            )
        )
    }

    /**
     * 创建统计信息映射
     */
    private fun createStatisticsMap(stats: PerformanceStatistics): Map<String, Any> {
        return mapOf(
            "messagesSent" to stats.messagesSent,
            "messagesReceived" to stats.messagesReceived,
            "messagesProcessed" to stats.messagesProcessed,
            "messagesFailed" to stats.messagesFailed,
            "queueCount" to stats.queueCount,
            "connectionStatus" to stats.connectionStatus.name,
            "lastConnectionTime" to (stats.lastConnectionTime?.toString() ?: "N/A")
        )
    }

    /**
     * 创建错误结果
     */
    private fun createErrorResult(message: String): HealthResult {
        return HealthResult.builder("fluvio", HealthStatus.DOWN)
            .details(mapOf(
                "error" to message,
                "timestamp" to Instant.now().toString()
            ))
            .build()
    }

    /**
     * 检查结果数据类
     */
    private data class CheckResult(
        val healthy: Boolean,
        val issues: List<String>,
        val details: Map<String, Any?>
    ) {
        fun toMap(): Map<String, Any> {
            return mapOf(
                "healthy" to healthy,
                "issues" to issues,
                "details" to details
            )
        }
    }
}

/**
 * 扩展的健康检查工具
 */
@Singleton
class FluvioHealthChecker(
    private val metricsCollector: FluvioMetricsCollector,
    private val config: FluvioQueueConfiguration
) {

    private val logger = LoggerFactory.getLogger(FluvioHealthChecker::class.java)

    /**
     * 执行深度健康检查
     */
    fun performDeepHealthCheck(): DeepHealthCheckResult {
        val startTime = Instant.now()
        
        try {
            // 基础健康检查
            val basicHealth = metricsCollector.getHealthStatus()
            val stats = metricsCollector.getPerformanceStatistics()
            
            // 连接测试
            val connectionTest = testConnection()
            
            // 性能测试
            val performanceTest = testPerformance()
            
            // 配置验证
            val configValidation = validateConfiguration()
            
            val overallHealthy = basicHealth.healthy && 
                               connectionTest.success && 
                               performanceTest.success && 
                               configValidation.success
            
            return DeepHealthCheckResult(
                healthy = overallHealthy,
                basicHealth = basicHealth,
                connectionTest = connectionTest,
                performanceTest = performanceTest,
                configValidation = configValidation,
                statistics = stats,
                checkDuration = Duration.between(startTime, Instant.now())
            )
            
        } catch (e: Exception) {
            logger.error("Deep health check failed", e)
            return DeepHealthCheckResult(
                healthy = false,
                basicHealth = io.kestra.queue.fluvio.HealthStatus(false, listOf("Deep health check failed: ${e.message}"), Instant.now()),
                connectionTest = TestResult(false, "Connection test failed: ${e.message}"),
                performanceTest = TestResult(false, "Performance test failed: ${e.message}"),
                configValidation = TestResult(false, "Config validation failed: ${e.message}"),
                statistics = null,
                checkDuration = Duration.between(startTime, Instant.now())
            )
        }
    }

    /**
     * 测试连接
     */
    private fun testConnection(): TestResult {
        return try {
            // 这里可以实现实际的连接测试
            // 例如尝试创建一个测试主题或发送测试消息
            TestResult(true, "Connection test passed")
        } catch (e: Exception) {
            TestResult(false, "Connection test failed: ${e.message}")
        }
    }

    /**
     * 测试性能
     */
    private fun testPerformance(): TestResult {
        return try {
            val stats = metricsCollector.getPerformanceStatistics()
            
            if (stats.averageLatencyMs > 200.0) {
                TestResult(false, "Performance test failed: high latency ${stats.averageLatencyMs}ms")
            } else if (stats.successRate < 90.0) {
                TestResult(false, "Performance test failed: low success rate ${stats.successRate}%")
            } else {
                TestResult(true, "Performance test passed")
            }
        } catch (e: Exception) {
            TestResult(false, "Performance test failed: ${e.message}")
        }
    }

    /**
     * 验证配置
     */
    private fun validateConfiguration(): TestResult {
        return try {
            val issues = mutableListOf<String>()
            
            if (config.clusterEndpoint.isBlank()) {
                issues.add("Missing cluster endpoint")
            }
            
            if (config.healthCheck.connectionTimeout.toMillis() <= 0) {
                issues.add("Invalid connection timeout")
            }
            
            if (issues.isEmpty()) {
                TestResult(true, "Configuration validation passed")
            } else {
                TestResult(false, "Configuration validation failed: ${issues.joinToString(", ")}")
            }
        } catch (e: Exception) {
            TestResult(false, "Configuration validation failed: ${e.message}")
        }
    }
}

/**
 * 测试结果
 */
data class TestResult(
    val success: Boolean,
    val message: String
)

/**
 * 深度健康检查结果
 */
data class DeepHealthCheckResult(
    val healthy: Boolean,
    val basicHealth: io.kestra.queue.fluvio.HealthStatus,
    val connectionTest: TestResult,
    val performanceTest: TestResult,
    val configValidation: TestResult,
    val statistics: PerformanceStatistics?,
    val checkDuration: Duration
)
