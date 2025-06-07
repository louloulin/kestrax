package io.kestra.queue.fluvio

import io.kestra.core.metrics.MetricRegistry
import io.kestra.core.models.executions.*
import io.kestra.core.models.flows.FlowInterface
import io.kestra.core.models.templates.Template
import io.kestra.core.models.triggers.Trigger
import io.kestra.core.queues.QueueFactoryInterface
import io.kestra.core.queues.QueueInterface
import io.kestra.core.queues.QueueService
import io.kestra.core.queues.WorkerJobQueueInterface
import io.kestra.core.queues.WorkerTriggerResultQueueInterface
import io.kestra.core.runners.*
import io.kestra.core.server.ClusterEvent
import io.kestra.queue.fluvio.serialization.FluvioProtobufSerializer
import io.micronaut.context.annotation.Bean
import io.micronaut.context.annotation.Factory
import io.micronaut.context.annotation.Replaces
import io.micronaut.context.annotation.Requires
import io.micronaut.context.annotation.Value
import io.micronaut.context.condition.Condition
import io.micronaut.context.condition.ConditionContext
import jakarta.inject.Named
import jakarta.inject.Singleton
import org.slf4j.LoggerFactory
import java.util.*

/**
 * Fluvio implementation of QueueFactoryInterface
 *
 * This factory creates Fluvio-based queues that are fully compatible with
 * Kestra's existing queue system while providing superior performance.
 *
 * Uses @Replaces to replace JdbcQueueFactory when Fluvio is configured.
 */
@Factory
@Requires(condition = FluvioEnabledCondition::class)
@Replaces(bean = QueueFactoryInterface::class)
class FluvioQueueFactory(
    private val clientManager: FluvioClientManager,
    private val serializer: FluvioProtobufSerializer,
    private val queueService: QueueService,
    private val metricRegistry: MetricRegistry,
    private val config: FluvioQueueConfiguration,
    private val executorsUtils: io.kestra.core.utils.ExecutorsUtils
) : QueueFactoryInterface {

    private val logger = LoggerFactory.getLogger(FluvioQueueFactory::class.java)

    init {
        logger.info("🚀 FluvioQueueFactory created successfully!")
        logger.info("📊 Configuration: cluster-endpoint={}, topic-prefix={}",
            config.clusterEndpoint, config.topicPrefix)
    }

    private fun <T> createFluvioQueue(messageType: Class<T>, queueTypeName: String): FluvioQueue<T> {
        return FluvioQueue(
            messageType = messageType,
            queueTypeName = queueTypeName,
            clientManager = clientManager,
            serializer = serializer,
            queueService = queueService,
            metricRegistry = metricRegistry,
            config = config,
            executorsUtils = executorsUtils
        )
    }

    @Bean
    @Singleton
    @Named(QueueFactoryInterface.EXECUTION_NAMED)
    override fun execution(): QueueInterface<Execution> {
        return createFluvioQueue(Execution::class.java, "executions")
    }
    
    @Bean
    @Singleton
    @Named(QueueFactoryInterface.EXECUTOR_NAMED)
    override fun executor(): QueueInterface<Executor> {
        return createFluvioQueue(Executor::class.java, "executors")
    }

    @Bean
    @Singleton
    @Named(QueueFactoryInterface.WORKERJOB_NAMED)
    override fun workerJob(): QueueInterface<WorkerJob> {
        return createFluvioQueue(WorkerJob::class.java, "worker-jobs")
    }

    @Bean
    @Singleton
    @Named(QueueFactoryInterface.WORKERTASKRESULT_NAMED)
    override fun workerTaskResult(): QueueInterface<WorkerTaskResult> {
        return createFluvioQueue(WorkerTaskResult::class.java, "worker-task-results")
    }

    @Bean
    @Singleton
    @Named(QueueFactoryInterface.WORKERTRIGGERRESULT_NAMED)
    override fun workerTriggerResult(): QueueInterface<WorkerTriggerResult> {
        return createFluvioQueue(WorkerTriggerResult::class.java, "worker-trigger-results")
    }
    
    @Bean
    @Singleton
    @Named(QueueFactoryInterface.WORKERTASKLOG_NAMED)
    override fun logEntry(): QueueInterface<LogEntry> {
        return createFluvioQueue(LogEntry::class.java, "logs")
    }

    @Bean
    @Singleton
    @Named(QueueFactoryInterface.METRIC_QUEUE)
    override fun metricEntry(): QueueInterface<MetricEntry> {
        return createFluvioQueue(MetricEntry::class.java, "metrics")
    }

    @Bean
    @Singleton
    @Named(QueueFactoryInterface.FLOW_NAMED)
    override fun flow(): QueueInterface<FlowInterface> {
        return createFluvioQueue(FlowInterface::class.java, "flows")
    }

    @Bean
    @Singleton
    @Named(QueueFactoryInterface.KILL_NAMED)
    override fun kill(): QueueInterface<ExecutionKilled> {
        return createFluvioQueue(ExecutionKilled::class.java, "execution-killed")
    }
    
    @Bean
    @Singleton
    @Named(QueueFactoryInterface.TEMPLATE_NAMED)
    override fun template(): QueueInterface<Template> {
        return createFluvioQueue(Template::class.java, "templates")
    }

    @Bean
    @Singleton
    @Named(QueueFactoryInterface.WORKERINSTANCE_NAMED)
    override fun workerInstance(): QueueInterface<WorkerInstance> {
        return createFluvioQueue(WorkerInstance::class.java, "worker-instances")
    }

    @Bean
    @Singleton
    @Named(QueueFactoryInterface.WORKERJOBRUNNING_NAMED)
    override fun workerJobRunning(): QueueInterface<WorkerJobRunning> {
        return createFluvioQueue(WorkerJobRunning::class.java, "worker-job-running")
    }

    @Bean
    @Singleton
    @Named(QueueFactoryInterface.TRIGGER_NAMED)
    override fun trigger(): QueueInterface<Trigger> {
        return createFluvioQueue(Trigger::class.java, "triggers")
    }
    
    @Bean
    @Singleton
    @Named(QueueFactoryInterface.SUBFLOWEXECUTIONRESULT_NAMED)
    override fun subflowExecutionResult(): QueueInterface<SubflowExecutionResult> {
        return createFluvioQueue(SubflowExecutionResult::class.java, "subflow-execution-results")
    }

    @Bean
    @Singleton
    @Named(QueueFactoryInterface.SUBFLOWEXECUTIONEND_NAMED)
    override fun subflowExecutionEnd(): QueueInterface<SubflowExecutionEnd> {
        return createFluvioQueue(SubflowExecutionEnd::class.java, "subflow-execution-end")
    }

    @Bean
    @Singleton
    @Named(QueueFactoryInterface.CLUSTER_EVENT_NAMED)
    fun clusterEvent(): QueueInterface<ClusterEvent> {
        return createFluvioQueue(ClusterEvent::class.java, "cluster-events")
    }

    // Special queue interfaces that extend the base QueueInterface
    // For now, we'll use the standard FluvioQueue implementation

    @Bean
    @Singleton
    override fun workerJobQueue(): WorkerJobQueueInterface {
        val fluvioQueue = createFluvioQueue(WorkerJob::class.java, "worker-jobs")
        return FluvioWorkerJobQueue(fluvioQueue)
    }

    @Bean
    @Singleton
    override fun workerTriggerResultQueue(): WorkerTriggerResultQueueInterface {
        val fluvioQueue = createFluvioQueue(WorkerTriggerResult::class.java, "worker-trigger-results")
        return FluvioWorkerTriggerResultQueue(fluvioQueue)
    }

}
