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
import io.kestra.queue.fluvio.serialization.ProtobufSerializer
import io.micronaut.context.annotation.Bean
import io.micronaut.context.annotation.ConditionalOnProperty
import io.micronaut.context.annotation.Factory
import io.micronaut.context.annotation.Replaces
import jakarta.inject.Named
import jakarta.inject.Singleton

/**
 * Fluvio implementation of QueueFactoryInterface
 * 
 * This factory creates Fluvio-based queues that are fully compatible with
 * Kestra's existing queue system while providing superior performance.
 */
@Factory
@ConditionalOnProperty(name = "kestra.queue.type", value = "fluvio")
class FluvioQueueFactory(
    private val clientManager: FluvioClientManager,
    private val serializer: ProtobufSerializer,
    private val queueService: QueueService,
    private val metricRegistry: MetricRegistry,
    private val config: FluvioQueueConfiguration
) : QueueFactoryInterface {
    
    @Bean
    @Singleton
    @Named(QueueFactoryInterface.EXECUTION_NAMED)
    override fun execution(): QueueInterface<Execution> {
        return FluvioQueue(
            messageType = Execution::class.java,
            queueTypeName = "executions",
            clientManager = clientManager,
            serializer = serializer,
            queueService = queueService,
            metricRegistry = metricRegistry,
            config = config
        )
    }
    
    @Bean
    @Singleton
    @Named(QueueFactoryInterface.EXECUTOR_NAMED)
    override fun executor(): QueueInterface<Executor> {
        return FluvioQueue(
            messageType = Executor::class.java,
            queueTypeName = "executors",
            clientManager = clientManager,
            serializer = serializer,
            queueService = queueService,
            metricRegistry = metricRegistry,
            config = config
        )
    }
    
    @Bean
    @Singleton
    @Named(QueueFactoryInterface.WORKERJOB_NAMED)
    override fun workerJob(): QueueInterface<WorkerJob> {
        return FluvioQueue(
            messageType = WorkerJob::class.java,
            queueTypeName = "worker-jobs",
            clientManager = clientManager,
            serializer = serializer,
            queueService = queueService,
            metricRegistry = metricRegistry,
            config = config
        )
    }
    
    @Bean
    @Singleton
    @Named(QueueFactoryInterface.WORKERTASKRESULT_NAMED)
    override fun workerTaskResult(): QueueInterface<WorkerTaskResult> {
        return FluvioQueue(
            messageType = WorkerTaskResult::class.java,
            queueTypeName = "worker-task-results",
            clientManager = clientManager,
            serializer = serializer,
            queueService = queueService,
            metricRegistry = metricRegistry,
            config = config
        )
    }
    
    @Bean
    @Singleton
    @Named(QueueFactoryInterface.WORKERTRIGGERRESULT_NAMED)
    override fun workerTriggerResult(): QueueInterface<WorkerTriggerResult> {
        return FluvioQueue(
            messageType = WorkerTriggerResult::class.java,
            queueTypeName = "worker-trigger-results",
            clientManager = clientManager,
            serializer = serializer,
            queueService = queueService,
            metricRegistry = metricRegistry,
            config = config
        )
    }
    
    @Bean
    @Singleton
    @Named(QueueFactoryInterface.WORKERTASKLOG_NAMED)
    override fun logEntry(): QueueInterface<LogEntry> {
        return FluvioQueue(
            messageType = LogEntry::class.java,
            queueTypeName = "logs",
            clientManager = clientManager,
            serializer = serializer,
            queueService = queueService,
            metricRegistry = metricRegistry,
            config = config
        )
    }
    
    @Bean
    @Singleton
    @Named(QueueFactoryInterface.METRIC_QUEUE)
    override fun metricEntry(): QueueInterface<MetricEntry> {
        return FluvioQueue(
            messageType = MetricEntry::class.java,
            queueTypeName = "metrics",
            clientManager = clientManager,
            serializer = serializer,
            queueService = queueService,
            metricRegistry = metricRegistry,
            config = config
        )
    }
    
    @Bean
    @Singleton
    @Named(QueueFactoryInterface.FLOW_NAMED)
    override fun flow(): QueueInterface<FlowInterface> {
        return FluvioQueue(
            messageType = FlowInterface::class.java,
            queueTypeName = "flows",
            clientManager = clientManager,
            serializer = serializer,
            queueService = queueService,
            metricRegistry = metricRegistry,
            config = config
        )
    }
    
    @Bean
    @Singleton
    @Named(QueueFactoryInterface.KILL_NAMED)
    override fun kill(): QueueInterface<ExecutionKilled> {
        return FluvioQueue(
            messageType = ExecutionKilled::class.java,
            queueTypeName = "execution-killed",
            clientManager = clientManager,
            serializer = serializer,
            queueService = queueService,
            metricRegistry = metricRegistry,
            config = config
        )
    }
    
    @Bean
    @Singleton
    @Named(QueueFactoryInterface.TEMPLATE_NAMED)
    override fun template(): QueueInterface<Template> {
        return FluvioQueue(
            messageType = Template::class.java,
            queueTypeName = "templates",
            clientManager = clientManager,
            serializer = serializer,
            queueService = queueService,
            metricRegistry = metricRegistry,
            config = config
        )
    }
    
    @Bean
    @Singleton
    @Named(QueueFactoryInterface.WORKERINSTANCE_NAMED)
    override fun workerInstance(): QueueInterface<WorkerInstance> {
        return FluvioQueue(
            messageType = WorkerInstance::class.java,
            queueTypeName = "worker-instances",
            clientManager = clientManager,
            serializer = serializer,
            queueService = queueService,
            metricRegistry = metricRegistry,
            config = config
        )
    }
    
    @Bean
    @Singleton
    @Named(QueueFactoryInterface.WORKERJOBRUNNING_NAMED)
    override fun workerJobRunning(): QueueInterface<WorkerJobRunning> {
        return FluvioQueue(
            messageType = WorkerJobRunning::class.java,
            queueTypeName = "worker-job-running",
            clientManager = clientManager,
            serializer = serializer,
            queueService = queueService,
            metricRegistry = metricRegistry,
            config = config
        )
    }
    
    @Bean
    @Singleton
    @Named(QueueFactoryInterface.TRIGGER_NAMED)
    override fun trigger(): QueueInterface<Trigger> {
        return FluvioQueue(
            messageType = Trigger::class.java,
            queueTypeName = "triggers",
            clientManager = clientManager,
            serializer = serializer,
            queueService = queueService,
            metricRegistry = metricRegistry,
            config = config
        )
    }
    
    @Bean
    @Singleton
    @Named(QueueFactoryInterface.SUBFLOWEXECUTIONRESULT_NAMED)
    override fun subflowExecutionResult(): QueueInterface<SubflowExecutionResult> {
        return FluvioQueue(
            messageType = SubflowExecutionResult::class.java,
            queueTypeName = "subflow-execution-results",
            clientManager = clientManager,
            serializer = serializer,
            queueService = queueService,
            metricRegistry = metricRegistry,
            config = config
        )
    }
    
    @Bean
    @Singleton
    @Named(QueueFactoryInterface.SUBFLOWEXECUTIONEND_NAMED)
    override fun subflowExecutionEnd(): QueueInterface<SubflowExecutionEnd> {
        return FluvioQueue(
            messageType = SubflowExecutionEnd::class.java,
            queueTypeName = "subflow-execution-end",
            clientManager = clientManager,
            serializer = serializer,
            queueService = queueService,
            metricRegistry = metricRegistry,
            config = config
        )
    }
    
    // Special queue interfaces that extend the base QueueInterface
    
    @Bean
    @Singleton
    override fun workerJobQueue(): WorkerJobQueueInterface {
        return FluvioWorkerJobQueue(
            clientManager = clientManager,
            serializer = serializer,
            queueService = queueService,
            metricRegistry = metricRegistry,
            config = config
        )
    }
    
    @Bean
    @Singleton
    override fun workerTriggerResultQueue(): WorkerTriggerResultQueueInterface {
        return FluvioWorkerTriggerResultQueue(
            clientManager = clientManager,
            serializer = serializer,
            queueService = queueService,
            metricRegistry = metricRegistry,
            config = config
        )
    }
}
