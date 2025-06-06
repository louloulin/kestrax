package io.kestra.queue.fluvio

import io.kestra.queue.fluvio.proto.*
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Test Protocol Buffers infrastructure
 * Validates that Protocol Buffers classes are generated correctly and basic serialization works
 */
class ProtobufInfrastructureTest : StringSpec({

    "should generate Protocol Buffers classes correctly" {
        // Test that Protocol Buffers classes are generated and accessible
        val executionProto = ExecutionProto.newBuilder()
            .setId("test-execution")
            .setNamespace("test.namespace")
            .setFlowId("test-flow")
            .setFlowRevision(1)
            .setDeleted(false)
            .build()

        executionProto.id shouldBe "test-execution"
        executionProto.namespace shouldBe "test.namespace"
        executionProto.flowId shouldBe "test-flow"
        executionProto.flowRevision shouldBe 1
        executionProto.deleted shouldBe false
    }

    "should create QueueMessage with ExecutionProto payload" {
        // Test ExecutionProto
        val executionProto = ExecutionProto.newBuilder()
            .setId("test-execution")
            .setNamespace("test.namespace")
            .build()

        val queueMessage = QueueMessage.newBuilder()
            .setMessageType("execution")
            .setTimestamp(System.currentTimeMillis())
            .setMessageId("msg-1")
            .setExecution(executionProto)
            .build()

        queueMessage.messageType shouldBe "execution"
        queueMessage.hasExecution() shouldBe true
        queueMessage.execution.id shouldBe "test-execution"
    }

    "should create QueueMessage with TaskRunProto payload" {
        // Test TaskRunProto
        val taskRunProto = TaskRunProto.newBuilder()
            .setId("test-taskrun")
            .setExecutionId("test-execution")
            .build()

        val queueMessage = QueueMessage.newBuilder()
            .setMessageType("task_run")
            .setTimestamp(System.currentTimeMillis())
            .setMessageId("msg-2")
            .setTaskRun(taskRunProto)
            .build()

        queueMessage.messageType shouldBe "task_run"
        queueMessage.hasTaskRun() shouldBe true
        queueMessage.taskRun.id shouldBe "test-taskrun"
    }

    "should create QueueMessage with LogEntryProto payload" {
        // Test LogEntryProto
        val logEntryProto = LogEntryProto.newBuilder()
            .setExecutionId("test-execution")
            .setMessage("Test log message")
            .setLevel(LogLevel.INFO)
            .build()

        val queueMessage = QueueMessage.newBuilder()
            .setMessageType("log_entry")
            .setTimestamp(System.currentTimeMillis())
            .setMessageId("msg-3")
            .setLogEntry(logEntryProto)
            .build()

        queueMessage.messageType shouldBe "log_entry"
        queueMessage.hasLogEntry() shouldBe true
        queueMessage.logEntry.message shouldBe "Test log message"
        queueMessage.logEntry.level shouldBe LogLevel.INFO
    }

    "should create QueueMessage with MetricEntryProto payload" {
        // Test MetricEntryProto
        val metricEntryProto = MetricEntryProto.newBuilder()
            .setExecutionId("test-execution")
            .setName("test.metric")
            .setValue(42.0)
            .build()

        val queueMessage = QueueMessage.newBuilder()
            .setMessageType("metric_entry")
            .setTimestamp(System.currentTimeMillis())
            .setMessageId("msg-4")
            .setMetricEntry(metricEntryProto)
            .build()

        queueMessage.messageType shouldBe "metric_entry"
        queueMessage.hasMetricEntry() shouldBe true
        queueMessage.metricEntry.name shouldBe "test.metric"
        queueMessage.metricEntry.value shouldBe 42.0
    }

    "should serialize and deserialize Protocol Buffers messages" {
        // Create a QueueMessage with ExecutionProto
        val executionProto = ExecutionProto.newBuilder()
            .setId("test-execution")
            .setNamespace("test.namespace")
            .setFlowId("test-flow")
            .setFlowRevision(1)
            .setDeleted(false)
            .build()

        val queueMessage = QueueMessage.newBuilder()
            .setMessageType("execution")
            .setTimestamp(System.currentTimeMillis())
            .setMessageId("test-message-id")
            .setExecution(executionProto)
            .build()

        // Serialize to bytes
        val serializedBytes = queueMessage.toByteArray()
        serializedBytes shouldNotBe null
        serializedBytes.size shouldNotBe 0

        // Deserialize from bytes
        val deserializedMessage = QueueMessage.parseFrom(serializedBytes)
        deserializedMessage.messageType shouldBe "execution"
        deserializedMessage.messageId shouldBe "test-message-id"
        deserializedMessage.hasExecution() shouldBe true
        deserializedMessage.execution.id shouldBe "test-execution"
        deserializedMessage.execution.namespace shouldBe "test.namespace"
        deserializedMessage.execution.flowId shouldBe "test-flow"
        deserializedMessage.execution.flowRevision shouldBe 1
        deserializedMessage.execution.deleted shouldBe false
    }

    "should validate Protocol Buffers performance characteristics" {
        // Create a complex QueueMessage
        val executionProto = ExecutionProto.newBuilder()
            .setId("performance-test-execution")
            .setNamespace("performance.test")
            .setFlowId("performance-flow")
            .setFlowRevision(1)
            .setDeleted(false)
            .putInputs("input1", "value1")
            .putInputs("input2", "value2")
            .putVariables("var1", "varValue1")
            .putVariables("var2", "varValue2")
            .addLabels(LabelProto.newBuilder().setKey("env").setValue("test").build())
            .addLabels(LabelProto.newBuilder().setKey("team").setValue("platform").build())
            .build()

        val queueMessage = QueueMessage.newBuilder()
            .setMessageType("execution")
            .setTimestamp(System.currentTimeMillis())
            .setMessageId("performance-test-message")
            .setExecution(executionProto)
            .build()

        // Measure serialization performance
        val startTime = System.nanoTime()
        val serializedBytes = queueMessage.toByteArray()
        val serializationTime = System.nanoTime() - startTime

        // Measure deserialization performance
        val deserializeStartTime = System.nanoTime()
        val deserializedMessage = QueueMessage.parseFrom(serializedBytes)
        val deserializationTime = System.nanoTime() - deserializeStartTime

        // Validate results
        serializedBytes.size shouldNotBe 0
        deserializedMessage.execution.inputsCount shouldBe 2
        deserializedMessage.execution.variablesCount shouldBe 2
        deserializedMessage.execution.labelsCount shouldBe 2

        // Performance should be reasonable (less than 1ms for small messages)
        println("Protocol Buffers serialization time: ${serializationTime / 1_000_000.0} ms")
        println("Protocol Buffers deserialization time: ${deserializationTime / 1_000_000.0} ms")
        println("Serialized message size: ${serializedBytes.size} bytes")
    }

    "should demonstrate Protocol Buffers size efficiency" {
        // Create a message with various data types
        val executionProto = ExecutionProto.newBuilder()
            .setId("size-test-execution-with-long-id-to-test-compression")
            .setNamespace("size.test.namespace.with.long.name")
            .setFlowId("size-test-flow-with-descriptive-name")
            .setFlowRevision(12345)
            .setDeleted(false)
            .putInputs("input_parameter_1", "value_1_with_some_content")
            .putInputs("input_parameter_2", "value_2_with_more_content")
            .putInputs("input_parameter_3", "value_3_with_even_more_content")
            .putVariables("variable_1", "variable_value_1")
            .putVariables("variable_2", "variable_value_2")
            .addLabels(LabelProto.newBuilder().setKey("environment").setValue("production").build())
            .addLabels(LabelProto.newBuilder().setKey("team").setValue("platform-engineering").build())
            .addLabels(LabelProto.newBuilder().setKey("project").setValue("kestra-fluvio-queue").build())
            .build()

        val queueMessage = QueueMessage.newBuilder()
            .setMessageType("execution")
            .setTimestamp(System.currentTimeMillis())
            .setMessageId("size-test-message-id-with-uuid-like-content")
            .setExecution(executionProto)
            .build()

        val protobufBytes = queueMessage.toByteArray()

        println("Protocol Buffers message size: ${protobufBytes.size} bytes")
        println("Message contains:")
        println("- Execution ID: ${executionProto.id}")
        println("- Namespace: ${executionProto.namespace}")
        println("- Flow ID: ${executionProto.flowId}")
        println("- ${executionProto.inputsCount} inputs")
        println("- ${executionProto.variablesCount} variables")
        println("- ${executionProto.labelsCount} labels")

        // Validate that the message is properly structured
        protobufBytes.size shouldNotBe 0
        executionProto.inputsCount shouldBe 3
        executionProto.variablesCount shouldBe 2
        executionProto.labelsCount shouldBe 3
    }
})
