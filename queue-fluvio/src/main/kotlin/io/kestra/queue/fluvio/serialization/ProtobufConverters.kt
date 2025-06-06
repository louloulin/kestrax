package io.kestra.queue.fluvio.serialization

import io.kestra.core.models.executions.Execution
import io.kestra.core.models.executions.TaskRun
import io.kestra.core.models.executions.LogEntry
import io.kestra.core.models.executions.MetricEntry
import io.kestra.core.models.flows.State
import io.kestra.core.models.Label
import io.kestra.queue.fluvio.proto.*
import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.ZoneId

/**
 * 高性能Protocol Buffers转换器
 * 实现Kestra核心模型与Protocol Buffers的高效转换
 * 目标：实现3-4倍序列化性能提升
 */
object ProtobufConverters {
    
    private val logger = LoggerFactory.getLogger(ProtobufConverters::class.java)

    /**
     * 将Execution转换为Protocol Buffers
     * 优化：直接字段映射，避免反射和中间对象
     */
    fun convertExecutionToProto(execution: Execution): ExecutionProto {
        val builder = ExecutionProto.newBuilder()

        // 基础字段 - 直接映射，性能最优
        execution.id?.let { builder.setId(it) }
        execution.namespace?.let { builder.setNamespace(it) }
        execution.flowId?.let { builder.setFlowId(it) }
        execution.flowRevision?.let { builder.setFlowRevision(it) }
        builder.setDeleted(execution.isDeleted)

        // 可选字段
        execution.tenantId?.let { builder.setTenantId(it) }
        execution.originalId?.let { builder.setOriginalId(it) }

        // 状态转换 - 优化的枚举映射
        execution.state?.let { state ->
            builder.setState(convertStateToExecutionStateProto(state))
        }

        // TaskRun列表 - 批量转换优化
        execution.taskRunList?.let { taskRuns ->
            if (taskRuns.isNotEmpty()) {
                taskRuns.forEach { taskRun ->
                    builder.addTaskRuns(convertTaskRunToProto(taskRun))
                }
            }
        }

        // 输入参数 - Map优化
        execution.inputs?.let { inputs ->
            if (inputs.isNotEmpty()) {
                inputs.forEach { (key, value) ->
                    builder.putInputs(key, value?.toString() ?: "")
                }
            }
        }

        // 变量 - Map优化
        execution.variables?.let { variables ->
            if (variables.isNotEmpty()) {
                variables.forEach { (key, value) ->
                    builder.putVariables(key, value?.toString() ?: "")
                }
            }
        }

        // 时间戳 - 高效时间转换
        execution.scheduleDate?.let { date ->
            builder.setScheduleDate(date.toEpochMilli().toString())
        }

        // 标签 - 批量转换
        execution.labels?.let { labels ->
            if (labels.isNotEmpty()) {
                labels.forEach { label ->
                    builder.addLabels(
                        LabelProto.newBuilder()
                            .setKey(label.key() ?: "")
                            .setValue(label.value() ?: "")
                            .build()
                    )
                }
            }
        }

        // 元数据
        execution.metadata?.let { metadata ->
            val metadataBuilder = ExecutionMetadataProto.newBuilder()
            metadata.attemptNumber?.let { metadataBuilder.setAttemptNumber(it) }
            metadata.originalCreatedDate?.let { 
                metadataBuilder.setOriginalCreatedDate(it.toEpochMilli())
            }
            builder.setMetadata(metadataBuilder.build())
        }

        return builder.build()
    }

    /**
     * 将Protocol Buffers转换为Execution
     * 优化：直接构建器模式，避免中间对象创建
     */
    fun convertExecutionFromProto(proto: ExecutionProto): Execution {
        val builder = Execution.builder()

        // 基础字段恢复
        if (proto.id.isNotEmpty()) builder.id(proto.id)
        if (proto.namespace.isNotEmpty()) builder.namespace(proto.namespace)
        if (proto.flowId.isNotEmpty()) builder.flowId(proto.flowId)
        if (proto.flowRevision != 0) builder.flowRevision(proto.flowRevision)
        builder.deleted(proto.deleted)

        // 可选字段恢复
        if (proto.tenantId.isNotEmpty()) builder.tenantId(proto.tenantId)
        if (proto.originalId.isNotEmpty()) builder.originalId(proto.originalId)

        // 状态恢复
        if (proto.hasState()) {
            builder.state(convertStateFromProto(proto.state))
        }

        // TaskRun列表恢复 - 批量处理优化
        if (proto.taskRunsCount > 0) {
            val taskRuns = ArrayList<TaskRun>(proto.taskRunsCount) // 预分配容量
            proto.taskRunsList.forEach { taskRunProto ->
                taskRuns.add(convertTaskRunFromProto(taskRunProto))
            }
            builder.taskRunList(taskRuns)
        }

        // 输入参数恢复
        if (proto.inputsCount > 0) {
            builder.inputs(HashMap(proto.inputsMap)) // 直接使用HashMap
        }

        // 变量恢复
        if (proto.variablesCount > 0) {
            builder.variables(HashMap(proto.variablesMap))
        }

        // 时间戳恢复
        if (proto.scheduleDate != 0L) {
            val instant = Instant.ofEpochMilli(proto.scheduleDate)
            builder.scheduleDate(instant)
        }

        // 标签恢复 - 批量处理
        if (proto.labelsCount > 0) {
            val labels = ArrayList<Label>(proto.labelsCount)
            proto.labelsList.forEach { labelProto ->
                labels.add(Label(labelProto.key, labelProto.value))
            }
            builder.labels(labels)
        }

        // 元数据恢复
        if (proto.hasMetadata()) {
            val metadata = Execution.Metadata.builder()
            if (proto.metadata.attemptNumber != 0) {
                metadata.attemptNumber(proto.metadata.attemptNumber)
            }
            if (proto.metadata.originalCreatedDate != 0L) {
                val instant = Instant.ofEpochMilli(proto.metadata.originalCreatedDate)
                metadata.originalCreatedDate(instant.atZone(ZoneId.systemDefault()))
            }
            builder.metadata(metadata.build())
        }

        return builder.build()
    }

    /**
     * 将TaskRun转换为Protocol Buffers
     * 优化：精简字段映射，高效状态转换
     */
    fun convertTaskRunToProto(taskRun: TaskRun): TaskRunProto {
        val builder = TaskRunProto.newBuilder()

        // 基础字段
        taskRun.id?.let { builder.setId(it) }
        taskRun.executionId?.let { builder.setExecutionId(it) }
        taskRun.namespace?.let { builder.setNamespace(it) }
        taskRun.flowId?.let { builder.setFlowId(it) }
        taskRun.taskId?.let { builder.setTaskId(it) }
        taskRun.value?.let { builder.setValue(it) }

        // 可选字段
        taskRun.parentTaskRunId?.let { builder.setParentTaskRunId(it) }
        taskRun.iteration?.let { builder.setIteration(it) }

        // 状态
        taskRun.state?.let { state ->
            builder.setState(convertStateToProto(state))
        }

        // 输出 - Map优化
        taskRun.outputs?.let { outputs ->
            if (outputs.isNotEmpty()) {
                outputs.forEach { (key, value) ->
                    builder.putOutputs(key, value?.toString() ?: "")
                }
            }
        }

        // 尝试记录 - 批量处理
        taskRun.attempts?.let { attempts ->
            if (attempts.isNotEmpty()) {
                attempts.forEach { attempt ->
                    val attemptBuilder = TaskRunAttemptProto.newBuilder()
                    attempt.state?.let { attemptBuilder.setState(convertStateToProto(it)) }
                    attempt.metrics?.forEach { (key, value) ->
                        attemptBuilder.putMetrics(key, value?.toString() ?: "")
                    }
                    builder.addAttempts(attemptBuilder.build())
                }
            }
        }

        return builder.build()
    }

    /**
     * 将Protocol Buffers转换为TaskRun
     */
    fun convertTaskRunFromProto(proto: TaskRunProto): TaskRun {
        val builder = TaskRun.builder()

        // 基础字段恢复
        if (proto.id.isNotEmpty()) builder.id(proto.id)
        if (proto.executionId.isNotEmpty()) builder.executionId(proto.executionId)
        if (proto.namespace.isNotEmpty()) builder.namespace(proto.namespace)
        if (proto.flowId.isNotEmpty()) builder.flowId(proto.flowId)
        if (proto.taskId.isNotEmpty()) builder.taskId(proto.taskId)
        if (proto.value.isNotEmpty()) builder.value(proto.value)

        // 可选字段恢复
        if (proto.parentTaskRunId.isNotEmpty()) builder.parentTaskRunId(proto.parentTaskRunId)
        if (proto.iteration != 0) builder.iteration(proto.iteration)

        // 状态恢复
        if (proto.hasState()) {
            builder.state(convertStateFromProto(proto.state))
        }

        // 输出恢复
        if (proto.outputsCount > 0) {
            builder.outputs(HashMap(proto.outputsMap))
        }

        // 尝试记录恢复
        if (proto.attemptsCount > 0) {
            val attempts = ArrayList<TaskRun.TaskRunAttempt>(proto.attemptsCount)
            proto.attemptsList.forEach { attemptProto ->
                val attemptBuilder = TaskRun.TaskRunAttempt.builder()
                if (attemptProto.hasState()) {
                    attemptBuilder.state(convertStateFromProto(attemptProto.state))
                }
                if (attemptProto.metricsCount > 0) {
                    attemptBuilder.metrics(HashMap(attemptProto.metricsMap))
                }
                attempts.add(attemptBuilder.build())
            }
            builder.attempts(attempts)
        }

        return builder.build()
    }

    /**
     * 将LogEntry转换为Protocol Buffers
     * 优化：直接枚举映射，避免字符串转换
     */
    fun convertLogEntryToProto(logEntry: LogEntry): LogEntryProto {
        val builder = LogEntryProto.newBuilder()

        // 基础字段
        logEntry.namespace?.let { builder.setNamespace(it) }
        logEntry.flowId?.let { builder.setFlowId(it) }
        logEntry.taskId?.let { builder.setTaskId(it) }
        logEntry.executionId?.let { builder.setExecutionId(it) }
        logEntry.taskRunId?.let { builder.setTaskRunId(it) }
        logEntry.timestamp?.let { builder.setTimestamp(it.toEpochMilli()) }
        logEntry.message?.let { builder.setMessage(it) }
        logEntry.thread?.let { builder.setThread(it) }

        // 日志级别 - 优化的枚举映射
        logEntry.level?.let { level ->
            val protoLevel = when (level) {
                org.slf4j.event.Level.TRACE -> LogLevel.TRACE
                org.slf4j.event.Level.DEBUG -> LogLevel.DEBUG
                org.slf4j.event.Level.INFO -> LogLevel.INFO
                org.slf4j.event.Level.WARN -> LogLevel.WARN
                org.slf4j.event.Level.ERROR -> LogLevel.ERROR
            }
            builder.setLevel(protoLevel)
        }

        return builder.build()
    }

    /**
     * 将Protocol Buffers转换为LogEntry
     */
    fun convertLogEntryFromProto(proto: LogEntryProto): LogEntry {
        // 优化的枚举映射
        val level = when (proto.level) {
            LogLevel.TRACE -> org.slf4j.event.Level.TRACE
            LogLevel.DEBUG -> org.slf4j.event.Level.DEBUG
            LogLevel.INFO -> org.slf4j.event.Level.INFO
            LogLevel.WARN -> org.slf4j.event.Level.WARN
            LogLevel.ERROR -> org.slf4j.event.Level.ERROR
            LogLevel.UNRECOGNIZED -> org.slf4j.event.Level.INFO
        }

        return LogEntry.builder()
            .namespace(proto.namespace.takeIf { it.isNotEmpty() })
            .flowId(proto.flowId.takeIf { it.isNotEmpty() })
            .taskId(proto.taskId.takeIf { it.isNotEmpty() })
            .executionId(proto.executionId.takeIf { it.isNotEmpty() })
            .taskRunId(proto.taskRunId.takeIf { it.isNotEmpty() })
            .timestamp(if (proto.timestamp != 0L) Instant.ofEpochMilli(proto.timestamp) else null)
            .level(level)
            .message(proto.message.takeIf { it.isNotEmpty() })
            .thread(proto.thread.takeIf { it.isNotEmpty() })
            .build()
    }

    /**
     * 将MetricEntry转换为Protocol Buffers
     */
    fun convertMetricEntryToProto(metricEntry: MetricEntry): MetricEntryProto {
        val builder = MetricEntryProto.newBuilder()

        // 基础字段
        metricEntry.namespace?.let { builder.setNamespace(it) }
        metricEntry.flowId?.let { builder.setFlowId(it) }
        metricEntry.taskId?.let { builder.setTaskId(it) }
        metricEntry.executionId?.let { builder.setExecutionId(it) }
        metricEntry.taskRunId?.let { builder.setTaskRunId(it) }
        metricEntry.timestamp?.let { builder.setTimestamp(it.toEpochMilli()) }
        metricEntry.name?.let { builder.setName(it) }
        metricEntry.type?.let { builder.setType(it) }
        metricEntry.value?.let { builder.setValue(it) }

        // 标签 - Map优化
        metricEntry.tags?.let { tags ->
            if (tags.isNotEmpty()) {
                tags.forEach { (key, value) ->
                    builder.putTags(key, value ?: "")
                }
            }
        }

        return builder.build()
    }

    /**
     * 将Protocol Buffers转换为MetricEntry
     */
    fun convertMetricEntryFromProto(proto: MetricEntryProto): MetricEntry {
        return MetricEntry.builder()
            .namespace(proto.namespace.takeIf { it.isNotEmpty() })
            .flowId(proto.flowId.takeIf { it.isNotEmpty() })
            .taskId(proto.taskId.takeIf { it.isNotEmpty() })
            .executionId(proto.executionId.takeIf { it.isNotEmpty() })
            .taskRunId(proto.taskRunId.takeIf { it.isNotEmpty() })
            .timestamp(if (proto.timestamp != 0L) Instant.ofEpochMilli(proto.timestamp) else null)
            .name(proto.name.takeIf { it.isNotEmpty() })
            .type(proto.type.takeIf { it.isNotEmpty() })
            .value(proto.value.takeIf { it != 0.0 })
            .tags(if (proto.tagsCount > 0) HashMap(proto.tagsMap) else null)
            .build()
    }

    // ========== 高性能状态转换辅助方法 ==========

    /**
     * 将State转换为Protocol Buffers
     * 优化：直接状态映射，避免中间对象
     */
    private fun convertStateToProto(state: State): StateProto {
        val builder = StateProto.newBuilder()
            .setCurrent(convertStateTypeToProto(state.current))

        // 历史记录 - 批量处理优化
        state.histories?.let { histories ->
            if (histories.isNotEmpty()) {
                histories.forEach { history ->
                    builder.addHistories(
                        StateHistoryProto.newBuilder()
                            .setState(convertStateTypeToProto(history.state))
                            .setDate(history.date.toInstant().toEpochMilli())
                            .build()
                    )
                }
            }
        }

        return builder.build()
    }

    /**
     * 将Protocol Buffers转换为State
     */
    private fun convertStateFromProto(proto: StateProto): State {
        val current = convertStateTypeFromProto(proto.current)

        val histories = if (proto.historiesCount > 0) {
            val historyList = ArrayList<State.History>(proto.historiesCount) // 预分配容量
            proto.historiesList.forEach { historyProto ->
                val instant = Instant.ofEpochMilli(historyProto.date)
                historyList.add(
                    State.History(
                        convertStateTypeFromProto(historyProto.state),
                        instant.atZone(ZoneId.systemDefault())
                    )
                )
            }
            historyList
        } else {
            null
        }

        return State(current, histories)
    }

    /**
     * 高性能状态类型转换 - 使用查找表优化
     */
    private val stateTypeToProtoMap = mapOf(
        State.Type.CREATED to StateType.CREATED,
        State.Type.RUNNING to StateType.RUNNING,
        State.Type.PAUSED to StateType.PAUSED,
        State.Type.RESTARTED to StateType.RESTARTED,
        State.Type.KILLING to StateType.KILLING,
        State.Type.KILLED to StateType.KILLED,
        State.Type.FAILED to StateType.FAILED,
        State.Type.WARNING to StateType.WARNING,
        State.Type.SUCCESS to StateType.SUCCESS
    )

    private val protoToStateTypeMap = mapOf(
        StateType.CREATED to State.Type.CREATED,
        StateType.RUNNING to State.Type.RUNNING,
        StateType.PAUSED to State.Type.PAUSED,
        StateType.RESTARTED to State.Type.RESTARTED,
        StateType.KILLING to State.Type.KILLING,
        StateType.KILLED to State.Type.KILLED,
        StateType.FAILED to State.Type.FAILED,
        StateType.WARNING to State.Type.WARNING,
        StateType.SUCCESS to State.Type.SUCCESS,
        StateType.CANCELLED to State.Type.KILLED, // 映射到最接近的类型
        StateType.SKIPPED to State.Type.SUCCESS // 映射到最接近的类型
    )

    private fun convertStateTypeToProto(stateType: State.Type): StateType {
        return stateTypeToProtoMap[stateType] ?: StateType.CREATED
    }

    private fun convertStateTypeFromProto(stateType: StateType): State.Type {
        return protoToStateTypeMap[stateType] ?: State.Type.FAILED
    }

    // ========== 性能监控和统计 ==========

    /**
     * 获取转换性能统计信息
     */
    fun getConversionStats(): Map<String, Any> {
        return mapOf(
            "stateTypeMapSize" to stateTypeToProtoMap.size,
            "protoStateMapSize" to protoToStateTypeMap.size,
            "optimizationsEnabled" to listOf(
                "directFieldMapping",
                "preAllocatedCollections",
                "lookupTableStateConversion",
                "batchProcessing",
                "avoidReflection"
            )
        )
    }
}
