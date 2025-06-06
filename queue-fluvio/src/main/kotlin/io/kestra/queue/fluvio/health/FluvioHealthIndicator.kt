package io.kestra.queue.fluvio.health

import io.kestra.queue.fluvio.FluvioClientManager
import io.kestra.queue.fluvio.FluvioQueueConfiguration
import io.micronaut.health.HealthStatus
import io.micronaut.management.health.indicator.HealthIndicator
import io.micronaut.management.health.indicator.HealthResult
import jakarta.inject.Singleton
import mu.KotlinLogging
import org.reactivestreams.Publisher
import reactor.core.publisher.Mono
import java.time.Duration

private val logger = KotlinLogging.logger {}

/**
 * Health indicator for Fluvio queue system
 */
@Singleton
class FluvioHealthIndicator(
    private val clientManager: FluvioClientManager,
    private val config: FluvioQueueConfiguration
) : HealthIndicator {
    
    companion object {
        const val NAME = "fluvio-queue"
    }
    
    override fun getResult(): Publisher<HealthResult> {
        return Mono.fromCallable { checkHealth() }
            .timeout(config.healthCheck.connectionTimeout)
            .onErrorReturn(createUnhealthyResult("Health check timeout or error"))
    }
    
    private fun checkHealth(): HealthResult {
        return try {
            if (!config.healthCheck.enabled) {
                return HealthResult.builder(NAME, HealthStatus.UP)
                    .details(mapOf("status" to "Health checks disabled"))
                    .build()
            }
            
            val isHealthy = clientManager.isHealthy()
            
            if (isHealthy) {
                createHealthyResult()
            } else {
                createUnhealthyResult("Fluvio cluster is not accessible")
            }
            
        } catch (e: Exception) {
            logger.error(e) { "Fluvio health check failed" }
            createUnhealthyResult("Health check failed: ${e.message}")
        }
    }
    
    private fun createHealthyResult(): HealthResult {
        val details = mutableMapOf<String, Any>(
            "cluster-endpoint" to config.clusterEndpoint,
            "status" to "Connected",
            "topics-prefix" to config.topicPrefix
        )
        
        try {
            // Add additional cluster information if available
            val admin = clientManager.getAdmin()
            val topics = admin.listTopics()
            details["topics-count"] = topics.size
            details["kestra-topics"] = topics.filter { it.name.startsWith(config.topicPrefix) }.size
        } catch (e: Exception) {
            logger.debug(e) { "Could not retrieve additional cluster information" }
        }
        
        return HealthResult.builder(NAME, HealthStatus.UP)
            .details(details)
            .build()
    }
    
    private fun createUnhealthyResult(reason: String): HealthResult {
        val details = mapOf(
            "cluster-endpoint" to config.clusterEndpoint,
            "status" to "Disconnected",
            "reason" to reason,
            "auto-fallback" to config.healthCheck.autoFallback
        )
        
        return HealthResult.builder(NAME, HealthStatus.DOWN)
            .details(details)
            .build()
    }
}
