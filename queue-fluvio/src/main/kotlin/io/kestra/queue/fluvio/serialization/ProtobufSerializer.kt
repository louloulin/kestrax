package io.kestra.queue.fluvio.serialization

import com.google.protobuf.InvalidProtocolBufferException
import io.kestra.core.exceptions.DeserializationException
import io.kestra.core.models.executions.Execution
import io.kestra.core.models.executions.LogEntry
import io.kestra.core.models.executions.MetricEntry
import io.kestra.core.models.executions.TaskRun
import io.kestra.core.runners.WorkerTask
import io.kestra.core.runners.WorkerTaskResult
import io.kestra.queue.fluvio.proto.*
import jakarta.inject.Singleton
import mu.KotlinLogging
import java.util.*

private val logger = KotlinLogging.logger {}

/**
 * Protocol Buffers serializer for Kestra queue messages
 */
@Singleton
class ProtobufSerializer {
    
    /**
     * Serialize a Kestra object to Protocol Buffers bytes
     */
    fun serialize(message: Any): ByteArray {
        try {
            val builder = QueueMessage.newBuilder()
                .setMessageId(generateMessageId())
                .setTimestamp(System.currentTimeMillis())
            
            when (message) {
                is Execution -> {
                    builder.messageType = "execution"
                    builder.tenantId = message.tenantId ?: ""
                    builder.execution = convertToProto(message)
                }
                is TaskRun -> {
                    builder.messageType = "task_run"
                    builder.tenantId = "" // TaskRun doesn't have tenantId directly
                    builder.taskRun = convertToProto(message)
                }
                is WorkerTask -> {
                    builder.messageType = "worker_task"
                    builder.tenantId = "" // Extract from context if needed
                    builder.workerTask = convertToProto(message)
                }
                is WorkerTaskResult -> {
                    builder.messageType = "worker_task_result"
                    builder.tenantId = "" // Extract from context if needed
                    builder.workerTaskResult = convertToProto(message)
                }
                is LogEntry -> {
                    builder.messageType = "log_entry"
                    builder.tenantId = "" // LogEntry doesn't have tenantId directly
                    builder.logEntry = convertToProto(message)
                }
                is MetricEntry -> {
                    builder.messageType = "metric_entry"
                    builder.tenantId = "" // MetricEntry doesn't have tenantId directly
                    builder.metricEntry = convertToProto(message)
                }
                else -> {
                    throw DeserializationException("Unsupported message type: ${message::class.java.name}")
                }
            }
            
            return builder.build().toByteArray()
        } catch (e: Exception) {
            logger.error(e) { "Failed to serialize message of type ${message::class.java.simpleName}" }
            throw DeserializationException("Failed to serialize message", e)
        }
    }
    
    /**
     * Deserialize Protocol Buffers bytes to a Kestra object
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> deserialize(data: ByteArray, messageType: Class<T>): T {
        try {
            val queueMessage = QueueMessage.parseFrom(data)
            
            return when (queueMessage.messageType) {
                "execution" -> {
                    if (messageType.isAssignableFrom(Execution::class.java)) {
                        convertFromProto(queueMessage.execution) as T
                    } else {
                        throw DeserializationException("Expected ${messageType.name} but got execution")
                    }
                }
                "task_run" -> {
                    if (messageType.isAssignableFrom(TaskRun::class.java)) {
                        convertFromProto(queueMessage.taskRun) as T
                    } else {
                        throw DeserializationException("Expected ${messageType.name} but got task_run")
                    }
                }
                "worker_task" -> {
                    if (messageType.isAssignableFrom(WorkerTask::class.java)) {
                        convertFromProto(queueMessage.workerTask) as T
                    } else {
                        throw DeserializationException("Expected ${messageType.name} but got worker_task")
                    }
                }
                "worker_task_result" -> {
                    if (messageType.isAssignableFrom(WorkerTaskResult::class.java)) {
                        convertFromProto(queueMessage.workerTaskResult) as T
                    } else {
                        throw DeserializationException("Expected ${messageType.name} but got worker_task_result")
                    }
                }
                "log_entry" -> {
                    if (messageType.isAssignableFrom(LogEntry::class.java)) {
                        convertFromProto(queueMessage.logEntry) as T
                    } else {
                        throw DeserializationException("Expected ${messageType.name} but got log_entry")
                    }
                }
                "metric_entry" -> {
                    if (messageType.isAssignableFrom(MetricEntry::class.java)) {
                        convertFromProto(queueMessage.metricEntry) as T
                    } else {
                        throw DeserializationException("Expected ${messageType.name} but got metric_entry")
                    }
                }
                else -> throw DeserializationException("Unknown message type: ${queueMessage.messageType}")
            }
        } catch (e: InvalidProtocolBufferException) {
            logger.error(e) { "Failed to parse protobuf message" }
            throw DeserializationException("Failed to parse protobuf message", e)
        } catch (e: Exception) {
            logger.error(e) { "Failed to deserialize message to ${messageType.simpleName}" }
            throw DeserializationException("Failed to deserialize message", e)
        }
    }
    
    // Conversion methods - these will be implemented in separate converter classes
    private fun convertToProto(execution: Execution): ExecutionProto {
        return ExecutionConverter.toProto(execution)
    }
    
    private fun convertFromProto(proto: ExecutionProto): Execution {
        return ExecutionConverter.fromProto(proto)
    }
    
    private fun convertToProto(taskRun: TaskRun): TaskRunProto {
        return TaskRunConverter.toProto(taskRun)
    }
    
    private fun convertFromProto(proto: TaskRunProto): TaskRun {
        return TaskRunConverter.fromProto(proto)
    }
    
    private fun convertToProto(workerTask: WorkerTask): WorkerTaskProto {
        return WorkerTaskConverter.toProto(workerTask)
    }
    
    private fun convertFromProto(proto: WorkerTaskProto): WorkerTask {
        return WorkerTaskConverter.fromProto(proto)
    }
    
    private fun convertToProto(workerTaskResult: WorkerTaskResult): WorkerTaskResultProto {
        return WorkerTaskResultConverter.toProto(workerTaskResult)
    }
    
    private fun convertFromProto(proto: WorkerTaskResultProto): WorkerTaskResult {
        return WorkerTaskResultConverter.fromProto(proto)
    }
    
    private fun convertToProto(logEntry: LogEntry): LogEntryProto {
        return LogEntryConverter.toProto(logEntry)
    }
    
    private fun convertFromProto(proto: LogEntryProto): LogEntry {
        return LogEntryConverter.fromProto(proto)
    }
    
    private fun convertToProto(metricEntry: MetricEntry): MetricEntryProto {
        return MetricEntryConverter.toProto(metricEntry)
    }
    
    private fun convertFromProto(proto: MetricEntryProto): MetricEntry {
        return MetricEntryConverter.fromProto(proto)
    }
    
    private fun generateMessageId(): String = UUID.randomUUID().toString()
}
