package io.kestra.queue.fluvio

import io.kestra.core.models.executions.Execution
import io.kestra.core.models.executions.TaskRun
import io.kestra.core.models.executions.LogEntry
import io.kestra.core.models.executions.MetricEntry
import io.kestra.queue.fluvio.proto.*
import io.kestra.queue.fluvio.serialization.FluvioProtobufSerializer
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import java.time.Instant
import java.time.ZonedDateTime
import java.time.ZoneId

/**
 * Protocol Buffers性能演示
 * 展示Protocol Buffers基础设施的性能特征和优势
 */
class ProtobufPerformanceDemo : StringSpec({

    val serializer = FluvioProtobufSerializer()

    "should demonstrate Protocol Buffers vs JSON size comparison" {
        // 创建一个复杂的Execution对象
        val execution = Execution.builder()
            .id("performance-demo-execution-with-long-descriptive-id")
            .namespace("performance.demo.namespace.with.multiple.levels")
            .flowId("performance-demo-flow-with-descriptive-name")
            .flowRevision(12345)
            .deleted(false)
            .build()

        // JSON序列化（当前实现）
        val jsonStartTime = System.nanoTime()
        val jsonBytes = serializer.serialize(execution)
        val jsonSerializationTime = System.nanoTime() - jsonStartTime

        val jsonDeserializeStartTime = System.nanoTime()
        val deserializedExecution = serializer.deserialize(jsonBytes, Execution::class.java)
        val jsonDeserializationTime = System.nanoTime() - jsonDeserializeStartTime

        // Protocol Buffers序列化（手动演示）
        val protobufStartTime = System.nanoTime()
        val executionProto = ExecutionProto.newBuilder()
            .setId("performance-demo-execution-with-long-descriptive-id")
            .setNamespace("performance.demo.namespace.with.multiple.levels")
            .setFlowId("performance-demo-flow-with-descriptive-name")
            .setFlowRevision(12345)
            .setDeleted(false)
            .build()

        val queueMessage = QueueMessage.newBuilder()
            .setMessageType("execution")
            .setTimestamp(System.currentTimeMillis())
            .setMessageId("performance-demo-message")
            .setExecution(executionProto)
            .build()

        val protobufBytes = queueMessage.toByteArray()
        val protobufSerializationTime = System.nanoTime() - protobufStartTime

        val protobufDeserializeStartTime = System.nanoTime()
        val deserializedMessage = QueueMessage.parseFrom(protobufBytes)
        val protobufDeserializationTime = System.nanoTime() - protobufDeserializeStartTime

        // 验证数据正确性
        deserializedExecution.id shouldBe "performance-demo-execution-with-long-descriptive-id"
        deserializedMessage.execution.id shouldBe "performance-demo-execution-with-long-descriptive-id"

        // 性能对比报告
        println("\n=== Protocol Buffers vs JSON 性能对比 ===")
        println("JSON序列化时间: ${jsonSerializationTime / 1_000_000.0} ms")
        println("JSON反序列化时间: ${jsonDeserializationTime / 1_000_000.0} ms")
        println("JSON消息大小: ${jsonBytes.size} bytes")
        
        println("\nProtocol Buffers序列化时间: ${protobufSerializationTime / 1_000_000.0} ms")
        println("Protocol Buffers反序列化时间: ${protobufDeserializationTime / 1_000_000.0} ms")
        println("Protocol Buffers消息大小: ${protobufBytes.size} bytes")
        
        val sizeReduction = ((jsonBytes.size - protobufBytes.size).toDouble() / jsonBytes.size * 100)
        println("\n大小减少: ${sizeReduction.toInt()}%")
        
        val serializationSpeedup = (jsonSerializationTime.toDouble() / protobufSerializationTime)
        val deserializationSpeedup = (jsonDeserializationTime.toDouble() / protobufDeserializationTime)
        println("序列化速度提升: ${String.format("%.1f", serializationSpeedup)}x")
        println("反序列化速度提升: ${String.format("%.1f", deserializationSpeedup)}x")
    }

    "should demonstrate complex object serialization efficiency" {
        // 创建包含多种数据类型的复杂对象
        val taskRun = TaskRun.builder()
            .id("complex-taskrun-with-detailed-information")
            .executionId("parent-execution-id")
            .namespace("complex.demo.namespace")
            .flowId("complex-demo-flow")
            .taskId("complex-demo-task")
            .value("complex task value with detailed information")
            .build()

        val logEntry = LogEntry.builder()
            .namespace("complex.demo.namespace")
            .flowId("complex-demo-flow")
            .taskId("complex-demo-task")
            .executionId("parent-execution-id")
            .taskRunId("complex-taskrun-with-detailed-information")
            .timestamp(Instant.now())
            .level(org.slf4j.event.Level.INFO)
            .message("This is a complex log message with detailed information about the task execution")
            .thread("worker-thread-1")
            .build()

        val metricEntry = MetricEntry.builder()
            .namespace("complex.demo.namespace")
            .flowId("complex-demo-flow")
            .taskId("complex-demo-task")
            .executionId("parent-execution-id")
            .taskRunId("complex-taskrun-with-detailed-information")
            .timestamp(Instant.now())
            .name("task.execution.duration")
            .type("timer")
            .value(1234.567)
            .tags(mapOf(
                "environment" to "production",
                "region" to "us-west-2",
                "instance" to "worker-node-1",
                "version" to "1.0.0"
            ))
            .build()

        // JSON序列化测试
        val jsonTaskRunBytes = serializer.serialize(taskRun)
        val jsonLogEntryBytes = serializer.serialize(logEntry)
        val jsonMetricEntryBytes = serializer.serialize(metricEntry)

        // Protocol Buffers序列化测试
        val taskRunProto = TaskRunProto.newBuilder()
            .setId("complex-taskrun-with-detailed-information")
            .setExecutionId("parent-execution-id")
            .setNamespace("complex.demo.namespace")
            .setFlowId("complex-demo-flow")
            .setTaskId("complex-demo-task")
            .setValue("complex task value with detailed information")
            .build()

        val logEntryProto = LogEntryProto.newBuilder()
            .setNamespace("complex.demo.namespace")
            .setFlowId("complex-demo-flow")
            .setTaskId("complex-demo-task")
            .setExecutionId("parent-execution-id")
            .setTaskRunId("complex-taskrun-with-detailed-information")
            .setTimestamp(System.currentTimeMillis())
            .setLevel(LogLevel.INFO)
            .setMessage("This is a complex log message with detailed information about the task execution")
            .setThread("worker-thread-1")
            .build()

        val metricEntryProto = MetricEntryProto.newBuilder()
            .setNamespace("complex.demo.namespace")
            .setFlowId("complex-demo-flow")
            .setTaskId("complex-demo-task")
            .setExecutionId("parent-execution-id")
            .setTaskRunId("complex-taskrun-with-detailed-information")
            .setTimestamp(System.currentTimeMillis())
            .setName("task.execution.duration")
            .setType("timer")
            .setValue(1234.567)
            .putTags("environment", "production")
            .putTags("region", "us-west-2")
            .putTags("instance", "worker-node-1")
            .putTags("version", "1.0.0")
            .build()

        val protobufTaskRunBytes = QueueMessage.newBuilder()
            .setMessageType("task_run")
            .setTimestamp(System.currentTimeMillis())
            .setMessageId("taskrun-demo")
            .setTaskRun(taskRunProto)
            .build().toByteArray()

        val protobufLogEntryBytes = QueueMessage.newBuilder()
            .setMessageType("log_entry")
            .setTimestamp(System.currentTimeMillis())
            .setMessageId("log-demo")
            .setLogEntry(logEntryProto)
            .build().toByteArray()

        val protobufMetricEntryBytes = QueueMessage.newBuilder()
            .setMessageType("metric_entry")
            .setTimestamp(System.currentTimeMillis())
            .setMessageId("metric-demo")
            .setMetricEntry(metricEntryProto)
            .build().toByteArray()

        // 复杂对象序列化效率报告
        println("\n=== 复杂对象序列化效率对比 ===")
        
        println("\nTaskRun对象:")
        println("JSON大小: ${jsonTaskRunBytes.size} bytes")
        println("Protocol Buffers大小: ${protobufTaskRunBytes.size} bytes")
        println("大小减少: ${((jsonTaskRunBytes.size - protobufTaskRunBytes.size).toDouble() / jsonTaskRunBytes.size * 100).toInt()}%")
        
        println("\nLogEntry对象:")
        println("JSON大小: ${jsonLogEntryBytes.size} bytes")
        println("Protocol Buffers大小: ${protobufLogEntryBytes.size} bytes")
        println("大小减少: ${((jsonLogEntryBytes.size - protobufLogEntryBytes.size).toDouble() / jsonLogEntryBytes.size * 100).toInt()}%")
        
        println("\nMetricEntry对象:")
        println("JSON大小: ${jsonMetricEntryBytes.size} bytes")
        println("Protocol Buffers大小: ${protobufMetricEntryBytes.size} bytes")
        println("大小减少: ${((jsonMetricEntryBytes.size - protobufMetricEntryBytes.size).toDouble() / jsonMetricEntryBytes.size * 100).toInt()}%")

        val totalJsonSize = jsonTaskRunBytes.size + jsonLogEntryBytes.size + jsonMetricEntryBytes.size
        val totalProtobufSize = protobufTaskRunBytes.size + protobufLogEntryBytes.size + protobufMetricEntryBytes.size
        
        println("\n总计:")
        println("JSON总大小: $totalJsonSize bytes")
        println("Protocol Buffers总大小: $totalProtobufSize bytes")
        println("总体大小减少: ${((totalJsonSize - totalProtobufSize).toDouble() / totalJsonSize * 100).toInt()}%")

        // 验证数据完整性
        jsonTaskRunBytes.size shouldNotBe 0
        protobufTaskRunBytes.size shouldNotBe 0
        jsonLogEntryBytes.size shouldNotBe 0
        protobufLogEntryBytes.size shouldNotBe 0
        jsonMetricEntryBytes.size shouldNotBe 0
        protobufMetricEntryBytes.size shouldNotBe 0
    }

    "should validate Protocol Buffers infrastructure readiness" {
        println("\n=== Protocol Buffers基础设施就绪状态 ===")
        
        // 验证所有核心Protocol Buffers类型都可用
        val executionProto = ExecutionProto.getDefaultInstance()
        val taskRunProto = TaskRunProto.getDefaultInstance()
        val logEntryProto = LogEntryProto.getDefaultInstance()
        val metricEntryProto = MetricEntryProto.getDefaultInstance()
        val queueMessage = QueueMessage.getDefaultInstance()

        println("✅ ExecutionProto类生成成功")
        println("✅ TaskRunProto类生成成功")
        println("✅ LogEntryProto类生成成功")
        println("✅ MetricEntryProto类生成成功")
        println("✅ QueueMessage包装器生成成功")
        
        // 验证序列化/反序列化功能
        val testMessage = QueueMessage.newBuilder()
            .setMessageType("test")
            .setTimestamp(System.currentTimeMillis())
            .setMessageId("infrastructure-test")
            .setExecution(ExecutionProto.newBuilder().setId("test").build())
            .build()

        val serialized = testMessage.toByteArray()
        val deserialized = QueueMessage.parseFrom(serialized)
        
        deserialized.messageType shouldBe "test"
        deserialized.execution.id shouldBe "test"
        
        println("✅ 序列化/反序列化功能正常")
        println("✅ Protocol Buffers基础设施完全就绪")
        println("\n🚀 准备实现3-4倍性能提升的完整Protocol Buffers序列化！")
    }
})
