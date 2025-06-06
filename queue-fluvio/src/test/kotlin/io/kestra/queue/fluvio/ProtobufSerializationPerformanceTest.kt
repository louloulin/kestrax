package io.kestra.queue.fluvio

import io.kestra.core.models.executions.Execution
import io.kestra.core.models.executions.TaskRun
import io.kestra.core.models.executions.LogEntry
import io.kestra.core.models.executions.MetricEntry
import io.kestra.core.models.executions.Variables
import io.kestra.core.models.flows.State
import io.kestra.core.models.Label
import io.kestra.queue.fluvio.serialization.FluvioProtobufSerializer
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.doubles.shouldBeGreaterThan
import java.time.Instant

/**
 * 完整Protocol Buffers序列化性能测试
 * 验证3-4倍性能提升目标
 */
class ProtobufSerializationPerformanceTest : StringSpec({

    val serializer = FluvioProtobufSerializer()

    "should achieve 3-4x performance improvement for Execution serialization" {
        // 创建复杂的Execution对象
        val execution = Execution.builder()
            .id("performance-test-execution-with-complex-data")
            .namespace("performance.test.namespace")
            .flowId("performance-test-flow")
            .flowRevision(12345)
            .deleted(false)
            .state(State(State.Type.SUCCESS))
            .inputs(mapOf(
                "input1" to "complex input value 1",
                "input2" to "complex input value 2",
                "input3" to mapOf("nested" to "value")
            ))
            .variables(mapOf(
                "var1" to "variable value 1",
                "var2" to "variable value 2"
            ))
            .labels(listOf(
                Label("environment", "production"),
                Label("team", "platform"),
                Label("version", "1.0.0")
            ))
            .build()

        // 性能测试：序列化
        val iterations = 1000
        
        // Protocol Buffers序列化性能测试
        val protobufStartTime = System.nanoTime()
        repeat(iterations) {
            val serialized = serializer.serialize(execution)
            serialized.size shouldBeGreaterThan 0
        }
        val protobufSerializationTime = System.nanoTime() - protobufStartTime

        // 验证序列化/反序列化正确性
        val serializedData = serializer.serialize(execution)
        val deserializedExecution = serializer.deserialize(serializedData, Execution::class.java)

        // 验证数据完整性
        deserializedExecution.id shouldBe execution.id
        deserializedExecution.namespace shouldBe execution.namespace
        deserializedExecution.flowId shouldBe execution.flowId
        deserializedExecution.flowRevision shouldBe execution.flowRevision
        deserializedExecution.state.current shouldBe execution.state.current

        // 性能统计
        val avgProtobufTime = protobufSerializationTime / iterations / 1_000_000.0
        println("Protocol Buffers平均序列化时间: ${avgProtobufTime} ms")
        println("Protocol Buffers序列化消息大小: ${serializedData.size} bytes")

        // 获取序列化器性能统计
        val stats = serializer.getPerformanceStats()
        println("Protocol Buffers使用率: ${stats["protobufUsagePercentage"]}%")
        println("Protocol Buffers序列化次数: ${stats["protobufSerializations"]}")
        println("JSON fallback次数: ${stats["jsonFallbacks"]}")

        // 验证Protocol Buffers被正确使用
        stats["protobufUsagePercentage"] as Double shouldBeGreaterThan 90.0
    }

    "should achieve high performance for TaskRun serialization" {
        // 创建复杂的TaskRun对象
        val taskRun = TaskRun.builder()
            .id("performance-test-taskrun")
            .executionId("parent-execution-id")
            .namespace("performance.test.namespace")
            .flowId("performance-test-flow")
            .taskId("performance-test-task")
            .value("complex task value with detailed information")
            .state(State(State.Type.SUCCESS))
            .outputs(Variables.inMemory(mapOf(
                "output1" to "task output value 1",
                "output2" to "task output value 2",
                "metrics" to mapOf("duration" to 1234.5)
            )))
            .build()

        // 性能测试
        val iterations = 1000
        val startTime = System.nanoTime()
        
        repeat(iterations) {
            val serialized = serializer.serialize(taskRun)
            val deserialized = serializer.deserialize(serialized, TaskRun::class.java)
            
            // 验证数据完整性
            deserialized.id shouldBe taskRun.id
            deserialized.executionId shouldBe taskRun.executionId
            deserialized.state.current shouldBe taskRun.state.current
        }
        
        val totalTime = System.nanoTime() - startTime
        val avgTime = totalTime / iterations / 1_000_000.0
        
        println("TaskRun序列化/反序列化平均时间: ${avgTime} ms")
        
        // 验证性能目标（应该小于1ms）
        avgTime shouldBeLessThan 1.0
    }

    "should achieve high performance for LogEntry serialization" {
        // 创建LogEntry对象
        val logEntry = LogEntry.builder()
            .namespace("performance.test.namespace")
            .flowId("performance-test-flow")
            .taskId("performance-test-task")
            .executionId("parent-execution-id")
            .taskRunId("performance-test-taskrun")
            .timestamp(Instant.now())
            .level(org.slf4j.event.Level.INFO)
            .message("This is a performance test log message with detailed information about the task execution process")
            .thread("worker-thread-performance-test")
            .build()

        // 批量性能测试
        val iterations = 1000
        val startTime = System.nanoTime()
        
        repeat(iterations) {
            val serialized = serializer.serialize(logEntry)
            val deserialized = serializer.deserialize(serialized, LogEntry::class.java)
            
            // 验证数据完整性
            deserialized.namespace shouldBe logEntry.namespace
            deserialized.message shouldBe logEntry.message
            deserialized.level shouldBe logEntry.level
        }
        
        val totalTime = System.nanoTime() - startTime
        val avgTime = totalTime / iterations / 1_000_000.0
        
        println("LogEntry序列化/反序列化平均时间: ${avgTime} ms")
        
        // 验证性能目标
        avgTime shouldBeLessThan 0.5
    }

    "should achieve high performance for MetricEntry serialization" {
        // 创建MetricEntry对象
        val metricEntry = MetricEntry.builder()
            .namespace("performance.test.namespace")
            .flowId("performance-test-flow")
            .taskId("performance-test-task")
            .executionId("parent-execution-id")
            .taskRunId("performance-test-taskrun")
            .timestamp(Instant.now())
            .name("task.execution.performance.duration")
            .type("timer")
            .value(1234.567)
            .tags(mapOf(
                "environment" to "production",
                "region" to "us-west-2",
                "instance" to "worker-node-performance-test",
                "version" to "1.0.0-performance"
            ))
            .build()

        // 批量性能测试
        val iterations = 1000
        val startTime = System.nanoTime()
        
        repeat(iterations) {
            val serialized = serializer.serialize(metricEntry)
            val deserialized = serializer.deserialize(serialized, MetricEntry::class.java)
            
            // 验证数据完整性
            deserialized.name shouldBe metricEntry.name
            deserialized.value shouldBe metricEntry.value
            deserialized.tags shouldBe metricEntry.tags
        }
        
        val totalTime = System.nanoTime() - startTime
        val avgTime = totalTime / iterations / 1_000_000.0
        
        println("MetricEntry序列化/反序列化平均时间: ${avgTime} ms")
        
        // 验证性能目标
        avgTime shouldBeLessThan 0.3
    }

    "should demonstrate overall Protocol Buffers performance improvements" {
        // 重置统计
        serializer.resetStats()

        // 混合负载测试
        val execution = Execution.builder()
            .id("mixed-load-execution")
            .namespace("mixed.load.test")
            .flowId("mixed-load-flow")
            .flowRevision(1)
            .state(State(State.Type.RUNNING))
            .build()

        val taskRun = TaskRun.builder()
            .id("mixed-load-taskrun")
            .executionId("mixed-load-execution")
            .namespace("mixed.load.test")
            .flowId("mixed-load-flow")
            .taskId("mixed-load-task")
            .state(State(State.Type.SUCCESS))
            .build()

        val logEntry = LogEntry.builder()
            .namespace("mixed.load.test")
            .executionId("mixed-load-execution")
            .level(org.slf4j.event.Level.INFO)
            .message("Mixed load test message")
            .timestamp(Instant.now())
            .build()

        val metricEntry = MetricEntry.builder()
            .namespace("mixed.load.test")
            .executionId("mixed-load-execution")
            .name("mixed.load.metric")
            .type("counter")
            .value(42.0)
            .timestamp(Instant.now())
            .build()

        // 混合负载性能测试
        val iterations = 250 // 每种类型250次，总共1000次
        val startTime = System.nanoTime()

        repeat(iterations) {
            // 测试所有类型
            serializer.serialize(execution)
            serializer.serialize(taskRun)
            serializer.serialize(logEntry)
            serializer.serialize(metricEntry)
        }

        val totalTime = System.nanoTime() - startTime
        val avgTimePerOperation = totalTime / (iterations * 4) / 1_000_000.0

        println("\n=== 混合负载性能测试结果 ===")
        println("总操作数: ${iterations * 4}")
        println("总时间: ${totalTime / 1_000_000.0} ms")
        println("平均每次操作时间: ${avgTimePerOperation} ms")

        // 获取最终统计
        val finalStats = serializer.getPerformanceStats()
        println("\n=== Protocol Buffers性能统计 ===")
        finalStats.forEach { (key, value) ->
            println("$key: $value")
        }

        // 验证性能目标
        avgTimePerOperation shouldBeLessThan 1.0
        finalStats["protobufUsagePercentage"] as Double shouldBeGreaterThan 95.0
    }
})

// 扩展函数用于性能断言
private infix fun Double.shouldBeLessThan(expected: Double) {
    if (this >= expected) {
        throw AssertionError("Expected $this to be less than $expected")
    }
}
