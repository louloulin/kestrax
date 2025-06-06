package io.kestra.queue.fluvio.serialization

import io.kestra.core.models.executions.LogEntry
import io.kestra.core.models.executions.MetricEntry
import io.kestra.core.models.executions.TaskRun
import io.kestra.core.runners.WorkerTask
import io.kestra.core.runners.WorkerTaskResult
import io.kestra.queue.fluvio.proto.*

/**
 * Placeholder converters for other Kestra types
 * These should be implemented based on the actual structure of each type
 */

object TaskRunConverter {
    fun toProto(taskRun: TaskRun): TaskRunProto {
        return TaskRunProto.newBuilder()
            .setId(taskRun.id)
            .setExecutionId(taskRun.executionId)
            .setNamespace(taskRun.namespace)
            .setFlowId(taskRun.flowId)
            .setTaskId(taskRun.taskId)
            .setValue(taskRun.value ?: "")
            .build()
    }
    
    fun fromProto(proto: TaskRunProto): TaskRun {
        return TaskRun.builder()
            .id(proto.id)
            .executionId(proto.executionId)
            .namespace(proto.namespace)
            .flowId(proto.flowId)
            .taskId(proto.taskId)
            .value(if (proto.value.isEmpty()) null else proto.value)
            .build()
    }
}

object WorkerTaskConverter {
    fun toProto(workerTask: WorkerTask): WorkerTaskProto {
        return WorkerTaskProto.newBuilder()
            .setId(workerTask.taskRun.id)
            .setType(workerTask.task.type)
            .setTaskRun(TaskRunConverter.toProto(workerTask.taskRun))
            .build()
    }
    
    fun fromProto(proto: WorkerTaskProto): WorkerTask {
        // This is a simplified implementation
        // The actual implementation would need to reconstruct the full WorkerTask
        throw NotImplementedError("WorkerTask deserialization not yet implemented")
    }
}

object WorkerTaskResultConverter {
    fun toProto(workerTaskResult: WorkerTaskResult): WorkerTaskResultProto {
        return WorkerTaskResultProto.newBuilder()
            .setTaskRun(TaskRunConverter.toProto(workerTaskResult.taskRun))
            .build()
    }
    
    fun fromProto(proto: WorkerTaskResultProto): WorkerTaskResult {
        return WorkerTaskResult.builder()
            .taskRun(TaskRunConverter.fromProto(proto.taskRun))
            .build()
    }
}

object LogEntryConverter {
    fun toProto(logEntry: LogEntry): LogEntryProto {
        val builder = LogEntryProto.newBuilder()
            .setTimestamp(logEntry.timestamp.toEpochMilli())
            .setLevel(convertLogLevel(logEntry.level))
            .setMessage(logEntry.message)
        
        logEntry.namespace?.let { builder.namespace = it }
        logEntry.flowId?.let { builder.flowId = it }
        logEntry.taskId?.let { builder.taskId = it }
        logEntry.executionId?.let { builder.executionId = it }
        logEntry.taskRunId?.let { builder.taskRunId = it }
        logEntry.thread?.let { builder.thread = it }
        logEntry.loggerName?.let { builder.loggerName = it }
        
        return builder.build()
    }
    
    fun fromProto(proto: LogEntryProto): LogEntry {
        return LogEntry.builder()
            .namespace(if (proto.namespace.isEmpty()) null else proto.namespace)
            .flowId(if (proto.flowId.isEmpty()) null else proto.flowId)
            .taskId(if (proto.taskId.isEmpty()) null else proto.taskId)
            .executionId(if (proto.executionId.isEmpty()) null else proto.executionId)
            .taskRunId(if (proto.taskRunId.isEmpty()) null else proto.taskRunId)
            .timestamp(java.time.Instant.ofEpochMilli(proto.timestamp))
            .level(convertLogLevel(proto.level))
            .message(proto.message)
            .thread(if (proto.thread.isEmpty()) null else proto.thread)
            .loggerName(if (proto.loggerName.isEmpty()) null else proto.loggerName)
            .build()
    }
    
    private fun convertLogLevel(level: org.slf4j.event.Level): LogLevel {
        return when (level) {
            org.slf4j.event.Level.TRACE -> LogLevel.TRACE
            org.slf4j.event.Level.DEBUG -> LogLevel.DEBUG
            org.slf4j.event.Level.INFO -> LogLevel.INFO
            org.slf4j.event.Level.WARN -> LogLevel.WARN
            org.slf4j.event.Level.ERROR -> LogLevel.ERROR
        }
    }
    
    private fun convertLogLevel(level: LogLevel): org.slf4j.event.Level {
        return when (level) {
            LogLevel.TRACE -> org.slf4j.event.Level.TRACE
            LogLevel.DEBUG -> org.slf4j.event.Level.DEBUG
            LogLevel.INFO -> org.slf4j.event.Level.INFO
            LogLevel.WARN -> org.slf4j.event.Level.WARN
            LogLevel.ERROR -> org.slf4j.event.Level.ERROR
            LogLevel.UNRECOGNIZED -> org.slf4j.event.Level.INFO
        }
    }
}

object MetricEntryConverter {
    fun toProto(metricEntry: MetricEntry): MetricEntryProto {
        val builder = MetricEntryProto.newBuilder()
            .setTimestamp(metricEntry.timestamp.toEpochMilli())
            .setName(metricEntry.name)
            .setType(metricEntry.type)
            .setValue(metricEntry.value)
        
        metricEntry.namespace?.let { builder.namespace = it }
        metricEntry.flowId?.let { builder.flowId = it }
        metricEntry.taskId?.let { builder.taskId = it }
        metricEntry.executionId?.let { builder.executionId = it }
        metricEntry.taskRunId?.let { builder.taskRunId = it }
        
        metricEntry.tags?.forEach { (key, value) ->
            builder.putTags(key, value)
        }
        
        return builder.build()
    }
    
    fun fromProto(proto: MetricEntryProto): MetricEntry {
        return MetricEntry.builder()
            .namespace(if (proto.namespace.isEmpty()) null else proto.namespace)
            .flowId(if (proto.flowId.isEmpty()) null else proto.flowId)
            .taskId(if (proto.taskId.isEmpty()) null else proto.taskId)
            .executionId(if (proto.executionId.isEmpty()) null else proto.executionId)
            .taskRunId(if (proto.taskRunId.isEmpty()) null else proto.taskRunId)
            .timestamp(java.time.Instant.ofEpochMilli(proto.timestamp))
            .name(proto.name)
            .type(proto.type)
            .value(proto.value)
            .tags(if (proto.tagsCount > 0) proto.tagsMap else null)
            .build()
    }
}
