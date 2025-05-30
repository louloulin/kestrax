package io.kestra.blueprint.health

import io.kestra.blueprint.repository.BlueprintRepository
import io.kestra.blueprint.repository.NamespaceRepository
import io.micronaut.health.HealthStatus
import io.micronaut.management.health.indicator.HealthIndicator
import io.micronaut.management.health.indicator.HealthResult
import jakarta.inject.Singleton
import org.reactivestreams.Publisher
import reactor.core.publisher.Mono
import java.time.Duration

/**
 * 蓝图模块健康检查指示器
 * 检查数据库连接和基本功能是否正常
 */
@Singleton
class BlueprintHealthIndicator(
    private val blueprintRepository: BlueprintRepository,
    private val namespaceRepository: NamespaceRepository
) : HealthIndicator {
    
    override fun getResult(): Publisher<HealthResult> {
        return Mono.fromCallable {
            try {
                // 检查数据库连接
                val blueprintCount = blueprintRepository.count()
                val namespaceCount = namespaceRepository.count()
                
                // 构建健康检查结果
                val details = mapOf(
                    "blueprintCount" to blueprintCount,
                    "namespaceCount" to namespaceCount,
                    "database" to "connected",
                    "timestamp" to System.currentTimeMillis()
                )
                
                HealthResult.builder("blueprint")
                    .status(HealthStatus.UP)
                    .details(details)
                    .build()
                    
            } catch (e: Exception) {
                // 数据库连接失败或其他错误
                val details = mapOf(
                    "error" to e.message,
                    "database" to "disconnected",
                    "timestamp" to System.currentTimeMillis()
                )
                
                HealthResult.builder("blueprint")
                    .status(HealthStatus.DOWN)
                    .details(details)
                    .build()
            }
        }
        .timeout(Duration.ofSeconds(5)) // 5秒超时
        .onErrorReturn(
            HealthResult.builder("blueprint")
                .status(HealthStatus.DOWN)
                .details(mapOf(
                    "error" to "Health check timeout",
                    "timestamp" to System.currentTimeMillis()
                ))
                .build()
        )
    }
}