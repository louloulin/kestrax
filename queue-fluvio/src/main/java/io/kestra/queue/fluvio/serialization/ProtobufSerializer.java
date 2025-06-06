package io.kestra.queue.fluvio.serialization;

import com.google.protobuf.InvalidProtocolBufferException;
import io.kestra.core.exceptions.DeserializationException;
import io.kestra.core.models.executions.Execution;
import io.kestra.core.models.executions.LogEntry;
import io.kestra.core.models.executions.MetricEntry;
import io.kestra.core.models.executions.TaskRun;
import io.kestra.core.runners.WorkerTask;
import io.kestra.core.runners.WorkerTaskResult;
import io.kestra.queue.fluvio.proto.*;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

/**
 * Protocol Buffers serializer for Kestra queue messages
 */
@Singleton
@Slf4j
public class ProtobufSerializer {
    
    /**
     * Serialize a Kestra object to Protocol Buffers bytes
     */
    public byte[] serialize(Object message) throws DeserializationException {
        try {
            QueueMessage.Builder builder = QueueMessage.newBuilder()
                .setMessageId(generateMessageId())
                .setTimestamp(System.currentTimeMillis());
            
            if (message instanceof Execution execution) {
                builder.setMessageType("execution")
                    .setTenantId(execution.getTenantId() != null ? execution.getTenantId() : "")
                    .setExecution(convertToProto(execution));
            } else if (message instanceof TaskRun taskRun) {
                builder.setMessageType("task_run")
                    .setTenantId("") // TaskRun doesn't have tenantId directly
                    .setTaskRun(convertToProto(taskRun));
            } else if (message instanceof WorkerTask workerTask) {
                builder.setMessageType("worker_task")
                    .setTenantId("") // Extract from context if needed
                    .setWorkerTask(convertToProto(workerTask));
            } else if (message instanceof WorkerTaskResult workerTaskResult) {
                builder.setMessageType("worker_task_result")
                    .setTenantId("") // Extract from context if needed
                    .setWorkerTaskResult(convertToProto(workerTaskResult));
            } else if (message instanceof LogEntry logEntry) {
                builder.setMessageType("log_entry")
                    .setTenantId("") // LogEntry doesn't have tenantId directly
                    .setLogEntry(convertToProto(logEntry));
            } else if (message instanceof MetricEntry metricEntry) {
                builder.setMessageType("metric_entry")
                    .setTenantId("") // MetricEntry doesn't have tenantId directly
                    .setMetricEntry(convertToProto(metricEntry));
            } else {
                throw new DeserializationException("Unsupported message type: " + message.getClass().getName());
            }
            
            return builder.build().toByteArray();
        } catch (Exception e) {
            throw new DeserializationException("Failed to serialize message", e);
        }
    }
    
    /**
     * Deserialize Protocol Buffers bytes to a Kestra object
     */
    @SuppressWarnings("unchecked")
    public <T> T deserialize(byte[] data, Class<T> messageType) throws DeserializationException {
        try {
            QueueMessage queueMessage = QueueMessage.parseFrom(data);
            
            return switch (queueMessage.getMessageType()) {
                case "execution" -> {
                    if (messageType.isAssignableFrom(Execution.class)) {
                        yield (T) convertFromProto(queueMessage.getExecution());
                    }
                    throw new DeserializationException("Expected " + messageType.getName() + " but got execution");
                }
                case "task_run" -> {
                    if (messageType.isAssignableFrom(TaskRun.class)) {
                        yield (T) convertFromProto(queueMessage.getTaskRun());
                    }
                    throw new DeserializationException("Expected " + messageType.getName() + " but got task_run");
                }
                case "worker_task" -> {
                    if (messageType.isAssignableFrom(WorkerTask.class)) {
                        yield (T) convertFromProto(queueMessage.getWorkerTask());
                    }
                    throw new DeserializationException("Expected " + messageType.getName() + " but got worker_task");
                }
                case "worker_task_result" -> {
                    if (messageType.isAssignableFrom(WorkerTaskResult.class)) {
                        yield (T) convertFromProto(queueMessage.getWorkerTaskResult());
                    }
                    throw new DeserializationException("Expected " + messageType.getName() + " but got worker_task_result");
                }
                case "log_entry" -> {
                    if (messageType.isAssignableFrom(LogEntry.class)) {
                        yield (T) convertFromProto(queueMessage.getLogEntry());
                    }
                    throw new DeserializationException("Expected " + messageType.getName() + " but got log_entry");
                }
                case "metric_entry" -> {
                    if (messageType.isAssignableFrom(MetricEntry.class)) {
                        yield (T) convertFromProto(queueMessage.getMetricEntry());
                    }
                    throw new DeserializationException("Expected " + messageType.getName() + " but got metric_entry");
                }
                default -> throw new DeserializationException("Unknown message type: " + queueMessage.getMessageType());
            };
        } catch (InvalidProtocolBufferException e) {
            throw new DeserializationException("Failed to parse protobuf message", e);
        } catch (Exception e) {
            throw new DeserializationException("Failed to deserialize message", e);
        }
    }
    
    // Conversion methods - these will be implemented in separate converter classes
    private ExecutionProto convertToProto(Execution execution) {
        return ExecutionConverter.toProto(execution);
    }
    
    private Execution convertFromProto(ExecutionProto proto) {
        return ExecutionConverter.fromProto(proto);
    }
    
    private TaskRunProto convertToProto(TaskRun taskRun) {
        return TaskRunConverter.toProto(taskRun);
    }
    
    private TaskRun convertFromProto(TaskRunProto proto) {
        return TaskRunConverter.fromProto(proto);
    }
    
    private WorkerTaskProto convertToProto(WorkerTask workerTask) {
        return WorkerTaskConverter.toProto(workerTask);
    }
    
    private WorkerTask convertFromProto(WorkerTaskProto proto) {
        return WorkerTaskConverter.fromProto(proto);
    }
    
    private WorkerTaskResultProto convertToProto(WorkerTaskResult workerTaskResult) {
        return WorkerTaskResultConverter.toProto(workerTaskResult);
    }
    
    private WorkerTaskResult convertFromProto(WorkerTaskResultProto proto) {
        return WorkerTaskResultConverter.fromProto(proto);
    }
    
    private LogEntryProto convertToProto(LogEntry logEntry) {
        return LogEntryConverter.toProto(logEntry);
    }
    
    private LogEntry convertFromProto(LogEntryProto proto) {
        return LogEntryConverter.fromProto(proto);
    }
    
    private MetricEntryProto convertToProto(MetricEntry metricEntry) {
        return MetricEntryConverter.toProto(metricEntry);
    }
    
    private MetricEntry convertFromProto(MetricEntryProto proto) {
        return MetricEntryConverter.fromProto(proto);
    }
    
    private String generateMessageId() {
        return java.util.UUID.randomUUID().toString();
    }
}
