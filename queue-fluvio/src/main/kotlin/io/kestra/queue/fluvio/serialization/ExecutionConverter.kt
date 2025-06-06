package io.kestra.queue.fluvio.serialization

import io.kestra.core.models.executions.Execution
import io.kestra.core.models.flows.State
import io.kestra.queue.fluvio.proto.ExecutionProto
import io.kestra.queue.fluvio.proto.ExecutionStateProto
import io.kestra.queue.fluvio.proto.StateType
import java.time.Instant

/**
 * Converter between Kestra Execution and Protocol Buffers ExecutionProto
 */
object ExecutionConverter {
    
    fun toProto(execution: Execution): ExecutionProto {
        val builder = ExecutionProto.newBuilder()
            .setId(execution.id)
            .setNamespace(execution.namespace)
            .setFlowId(execution.flowId)
            .setFlowRevision(execution.flowRevision ?: 0)
            .setDeleted(execution.isDeleted)
        
        // Optional fields
        execution.tenantId?.let { builder.tenantId = it }
        execution.originalId?.let { builder.originalId = it }
        
        // State conversion
        execution.state?.let { state ->
            builder.state = convertStateToProto(state)
        }
        
        // Task runs
        execution.taskRunList?.let { taskRuns ->
            taskRuns.forEach { taskRun ->
                builder.addTaskRuns(TaskRunConverter.toProto(taskRun))
            }
        }
        
        // Inputs and variables
        execution.inputs?.let { inputs ->
            inputs.forEach { (key, value) ->
                builder.putInputs(key, value?.toString() ?: "")
            }
        }
        
        execution.variables?.let { variables ->
            variables.forEach { (key, value) ->
                builder.putVariables(key, value?.toString() ?: "")
            }
        }
        
        // Timestamps
        execution.createdDate?.let { 
            builder.createdDate = it.toEpochMilli()
        }
        execution.updatedDate?.let { 
            builder.updatedDate = it.toEpochMilli()
        }
        execution.scheduleDate?.let { 
            builder.scheduleDate = it.toEpochMilli()
        }
        
        // Labels
        execution.labels?.let { labels ->
            labels.forEach { label ->
                builder.addLabels(
                    io.kestra.queue.fluvio.proto.LabelProto.newBuilder()
                        .setKey(label.key)
                        .setValue(label.value)
                        .build()
                )
            }
        }
        
        // Metadata
        execution.metadata?.let { metadata ->
            val metadataBuilder = io.kestra.queue.fluvio.proto.ExecutionMetadataProto.newBuilder()
                .setAttemptNumber(metadata.attemptNumber ?: 0)
            
            metadata.originalCreatedDate?.let { 
                metadataBuilder.originalCreatedDate = it.toString()
            }
            
            builder.metadata = metadataBuilder.build()
        }
        
        return builder.build()
    }
    
    fun fromProto(proto: ExecutionProto): Execution {
        val builder = Execution.builder()
            .id(proto.id)
            .namespace(proto.namespace)
            .flowId(proto.flowId)
            .flowRevision(if (proto.flowRevision == 0) null else proto.flowRevision)
            .deleted(proto.deleted)
        
        // Optional fields
        if (proto.tenantId.isNotEmpty()) {
            builder.tenantId(proto.tenantId)
        }
        if (proto.originalId.isNotEmpty()) {
            builder.originalId(proto.originalId)
        }
        
        // State conversion
        if (proto.hasState()) {
            builder.state(convertStateFromProto(proto.state))
        }
        
        // Task runs
        if (proto.taskRunsCount > 0) {
            val taskRuns = proto.taskRunsList.map { taskRunProto ->
                TaskRunConverter.fromProto(taskRunProto)
            }
            builder.taskRunList(taskRuns)
        }
        
        // Inputs and variables
        if (proto.inputsCount > 0) {
            builder.inputs(proto.inputsMap)
        }
        
        if (proto.variablesCount > 0) {
            builder.variables(proto.variablesMap)
        }
        
        // Timestamps
        if (proto.createdDate != 0L) {
            builder.createdDate(Instant.ofEpochMilli(proto.createdDate))
        }
        if (proto.updatedDate != 0L) {
            builder.updatedDate(Instant.ofEpochMilli(proto.updatedDate))
        }
        if (proto.scheduleDate != 0L) {
            builder.scheduleDate(Instant.ofEpochMilli(proto.scheduleDate))
        }
        
        // Labels
        if (proto.labelsCount > 0) {
            val labels = proto.labelsList.map { labelProto ->
                io.kestra.core.models.Label(labelProto.key, labelProto.value)
            }
            builder.labels(labels)
        }
        
        // Metadata
        if (proto.hasMetadata()) {
            val metadata = io.kestra.core.models.executions.ExecutionMetadata.builder()
                .attemptNumber(if (proto.metadata.attemptNumber == 0) null else proto.metadata.attemptNumber)
            
            if (proto.metadata.originalCreatedDate.isNotEmpty()) {
                metadata.originalCreatedDate(proto.metadata.originalCreatedDate)
            }
            
            builder.metadata(metadata.build())
        }
        
        return builder.build()
    }
    
    private fun convertStateToProto(state: State): ExecutionStateProto {
        val builder = ExecutionStateProto.newBuilder()
            .setCurrent(convertStateTypeToProto(state.current))
        
        state.histories?.let { histories ->
            histories.forEach { history ->
                builder.addHistories(
                    io.kestra.queue.fluvio.proto.StateHistoryProto.newBuilder()
                        .setState(convertStateTypeToProto(history.state))
                        .setDate(history.date.toEpochMilli())
                        .build()
                )
            }
        }
        
        return builder.build()
    }
    
    private fun convertStateFromProto(proto: ExecutionStateProto): State {
        val builder = State.builder()
            .current(convertStateTypeFromProto(proto.current))
        
        if (proto.historiesCount > 0) {
            val histories = proto.historiesList.map { historyProto ->
                State.History(
                    convertStateTypeFromProto(historyProto.state),
                    Instant.ofEpochMilli(historyProto.date)
                )
            }
            builder.histories(histories)
        }
        
        return builder.build()
    }
    
    private fun convertStateTypeToProto(stateType: State.Type): StateType {
        return when (stateType) {
            State.Type.CREATED -> StateType.CREATED
            State.Type.RUNNING -> StateType.RUNNING
            State.Type.PAUSED -> StateType.PAUSED
            State.Type.RESTARTED -> StateType.RESTARTED
            State.Type.KILLING -> StateType.KILLING
            State.Type.KILLED -> StateType.KILLED
            State.Type.SUCCESS -> StateType.SUCCESS
            State.Type.WARNING -> StateType.WARNING
            State.Type.FAILED -> StateType.FAILED
            State.Type.CANCELLED -> StateType.CANCELLED
            State.Type.SKIPPED -> StateType.SKIPPED
        }
    }
    
    private fun convertStateTypeFromProto(stateType: StateType): State.Type {
        return when (stateType) {
            StateType.CREATED -> State.Type.CREATED
            StateType.RUNNING -> State.Type.RUNNING
            StateType.PAUSED -> State.Type.PAUSED
            StateType.RESTARTED -> State.Type.RESTARTED
            StateType.KILLING -> State.Type.KILLING
            StateType.KILLED -> State.Type.KILLED
            StateType.SUCCESS -> State.Type.SUCCESS
            StateType.WARNING -> State.Type.WARNING
            StateType.FAILED -> State.Type.FAILED
            StateType.CANCELLED -> State.Type.CANCELLED
            StateType.SKIPPED -> State.Type.SKIPPED
            StateType.UNRECOGNIZED -> State.Type.CREATED // 默认值
        }
    }
}
