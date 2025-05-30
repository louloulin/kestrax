package io.kestra.blueprint.metrics

import io.kestra.blueprint.repository.BlueprintRepository
import io.kestra.blueprint.repository.NamespaceRepository
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import io.micronaut.context.event.ApplicationEventListener
import io.micronaut.runtime.event.ApplicationStartupEvent
import jakarta.inject.Singleton
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicLong

/**
 * 蓝图模块指标收集器
 * 使用Micrometer收集业务指标
 */
@Singleton
class BlueprintMetrics(
    private val meterRegistry: MeterRegistry,
    private val blueprintRepository: BlueprintRepository,
    private val namespaceRepository: NamespaceRepository
) : ApplicationEventListener<ApplicationStartupEvent> {
    
    private val logger = LoggerFactory.getLogger(BlueprintMetrics::class.java)
    
    // 计数器
    private val blueprintCreatedCounter: Counter
    private val blueprintUpdatedCounter: Counter
    private val blueprintDeletedCounter: Counter
    private val blueprintViewedCounter: Counter
    private val templateRenderedCounter: Counter
    
    // 计时器
    private val blueprintCreateTimer: Timer
    private val blueprintUpdateTimer: Timer
    private val blueprintDeleteTimer: Timer
    private val templateRenderTimer: Timer
    
    // 原子计数器（用于Gauge）
    private val activeBlueprintsCount = AtomicLong(0)
    private val activeNamespacesCount = AtomicLong(0)
    private val publicBlueprintsCount = AtomicLong(0)
    private val templateBlueprintsCount = AtomicLong(0)
    
    init {
        // 初始化计数器
        blueprintCreatedCounter = Counter.builder("blueprint.created.total")
            .description("Total number of blueprints created")
            .register(meterRegistry)
            
        blueprintUpdatedCounter = Counter.builder("blueprint.updated.total")
            .description("Total number of blueprints updated")
            .register(meterRegistry)
            
        blueprintDeletedCounter = Counter.builder("blueprint.deleted.total")
            .description("Total number of blueprints deleted")
            .register(meterRegistry)
            
        blueprintViewedCounter = Counter.builder("blueprint.viewed.total")
            .description("Total number of blueprint views")
            .register(meterRegistry)
            
        templateRenderedCounter = Counter.builder("blueprint.template.rendered.total")
            .description("Total number of templates rendered")
            .register(meterRegistry)
        
        // 初始化计时器
        blueprintCreateTimer = Timer.builder("blueprint.create.duration")
            .description("Time taken to create a blueprint")
            .register(meterRegistry)
            
        blueprintUpdateTimer = Timer.builder("blueprint.update.duration")
            .description("Time taken to update a blueprint")
            .register(meterRegistry)
            
        blueprintDeleteTimer = Timer.builder("blueprint.delete.duration")
            .description("Time taken to delete a blueprint")
            .register(meterRegistry)
            
        templateRenderTimer = Timer.builder("blueprint.template.render.duration")
            .description("Time taken to render a template")
            .register(meterRegistry)
        
        // 初始化Gauge
        Gauge.builder("blueprint.active.count")
            .description("Current number of active blueprints")
            .register(meterRegistry) { activeBlueprintsCount.get().toDouble() }
            
        Gauge.builder("blueprint.namespace.count")
            .description("Current number of active namespaces")
            .register(meterRegistry) { activeNamespacesCount.get().toDouble() }
            
        Gauge.builder("blueprint.public.count")
            .description("Current number of public blueprints")
            .register(meterRegistry) { publicBlueprintsCount.get().toDouble() }
            
        Gauge.builder("blueprint.template.count")
            .description("Current number of template blueprints")
            .register(meterRegistry) { templateBlueprintsCount.get().toDouble() }
    }
    
    override fun onApplicationEvent(event: ApplicationStartupEvent) {
        // 应用启动时更新指标
        updateGaugeMetrics()
        logger.info("Blueprint metrics initialized")
    }
    
    /**
     * 记录蓝图创建事件
     */
    fun recordBlueprintCreated(namespaceId: String) {
        blueprintCreatedCounter.increment(
            "namespace", namespaceId
        )
        updateGaugeMetrics()
    }
    
    /**
     * 记录蓝图更新事件
     */
    fun recordBlueprintUpdated(namespaceId: String) {
        blueprintUpdatedCounter.increment(
            "namespace", namespaceId
        )
    }
    
    /**
     * 记录蓝图删除事件
     */
    fun recordBlueprintDeleted(namespaceId: String) {
        blueprintDeletedCounter.increment(
            "namespace", namespaceId
        )
        updateGaugeMetrics()
    }
    
    /**
     * 记录蓝图查看事件
     */
    fun recordBlueprintViewed(blueprintId: String, namespaceId: String) {
        blueprintViewedCounter.increment(
            "blueprint", blueprintId,
            "namespace", namespaceId
        )
    }
    
    /**
     * 记录模板渲染事件
     */
    fun recordTemplateRendered(success: Boolean) {
        templateRenderedCounter.increment(
            "success", success.toString()
        )
    }
    
    /**
     * 获取蓝图创建计时器
     */
    fun getBlueprintCreateTimer(): Timer {
        return blueprintCreateTimer
    }
    
    /**
     * 获取蓝图更新计时器
     */
    fun getBlueprintUpdateTimer(): Timer {
        return blueprintUpdateTimer
    }
    
    /**
     * 获取蓝图删除计时器
     */
    fun getBlueprintDeleteTimer(): Timer {
        return blueprintDeleteTimer
    }
    
    /**
     * 获取模板渲染计时器
     */
    fun getTemplateRenderTimer(): Timer {
        return templateRenderTimer
    }
    
    /**
     * 更新Gauge指标
     */
    private fun updateGaugeMetrics() {
        try {
            activeBlueprintsCount.set(blueprintRepository.count())
            activeNamespacesCount.set(namespaceRepository.count())
            
            // 这里可以添加更多复杂的查询来获取公开蓝图和模板蓝图的数量
            // 由于Repository接口限制，暂时使用简化实现
            
        } catch (e: Exception) {
            logger.error("Failed to update gauge metrics", e)
        }
    }
}