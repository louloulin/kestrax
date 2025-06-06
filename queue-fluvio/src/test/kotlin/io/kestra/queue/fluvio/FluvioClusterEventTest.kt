package io.kestra.queue.fluvio

import io.kestra.core.server.ClusterEvent
import io.kestra.queue.fluvio.serialization.FluvioProtobufSerializer
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import java.time.LocalDateTime

/**
 * ClusterEvent序列化功能测试
 * 验证ClusterEvent的序列化和反序列化功能
 */
class FluvioClusterEventTest : StringSpec({

    "should verify ClusterEvent queue factory method exists" {
        // 验证ClusterEvent队列工厂方法存在
        // 这里我们只验证方法存在，不实际创建实例
        val factoryClass = FluvioQueueFactory::class.java
        val clusterEventMethod = factoryClass.getDeclaredMethod("clusterEvent")

        clusterEventMethod shouldNotBe null
        clusterEventMethod.returnType shouldBe io.kestra.core.queues.QueueInterface::class.java
    }

    "should serialize and deserialize ClusterEvent objects" {
        // 创建测试用的ClusterEvent对象
        val clusterEvent = ClusterEvent(
            ClusterEvent.EventType.MAINTENANCE_ENTER,
            LocalDateTime.now(),
            "Test maintenance message"
        )

        val serializer = FluvioProtobufSerializer()
        
        // 测试序列化
        val serializedData = serializer.serialize(clusterEvent)
        serializedData shouldNotBe null
        serializedData.size shouldBeGreaterThan 0

        // 测试反序列化
        val deserializedEvent = serializer.deserialize(serializedData, ClusterEvent::class.java)
        deserializedEvent shouldNotBe null
        deserializedEvent.eventType shouldBe ClusterEvent.EventType.MAINTENANCE_ENTER
        deserializedEvent.message shouldBe "Test maintenance message"
    }

    "should handle different ClusterEvent types" {
        // 测试不同类型的ClusterEvent
        val eventTypes = listOf(
            ClusterEvent.EventType.MAINTENANCE_ENTER,
            ClusterEvent.EventType.MAINTENANCE_EXIT,
            ClusterEvent.EventType.PLUGINS_SYNC_REQUESTED
        )

        val serializer = FluvioProtobufSerializer()

        eventTypes.forEach { eventType ->
            val clusterEvent = ClusterEvent(
                eventType,
                LocalDateTime.now(),
                "Test message for ${eventType.name.lowercase()}"
            )

            // 序列化和反序列化测试
            val serializedData = serializer.serialize(clusterEvent)
            val deserializedEvent = serializer.deserialize(serializedData, ClusterEvent::class.java)

            deserializedEvent.eventType shouldBe eventType
            deserializedEvent.message shouldBe "Test message for ${eventType.name.lowercase()}"
        }
    }

    "should support ClusterEvent queue operations verification" {
        // 验证ClusterEvent队列方法存在
        val factoryClass = FluvioQueueFactory::class.java
        val clusterEventMethod = factoryClass.getDeclaredMethod("clusterEvent")

        // 验证方法签名正确
        clusterEventMethod shouldNotBe null
        clusterEventMethod.parameterCount shouldBe 0
    }

    "should create ClusterEvent with custom UID" {
        // 测试包含自定义UID的ClusterEvent
        val customEvent = ClusterEvent(
            "custom-uid-123",
            ClusterEvent.EventType.PLUGINS_SYNC_REQUESTED,
            LocalDateTime.now(),
            "Custom UID test event"
        )

        val serializer = FluvioProtobufSerializer()
        
        // 序列化和反序列化测试
        val serializedData = serializer.serialize(customEvent)
        val deserializedEvent = serializer.deserialize(serializedData, ClusterEvent::class.java)

        deserializedEvent.eventType shouldBe ClusterEvent.EventType.PLUGINS_SYNC_REQUESTED
        deserializedEvent.uid shouldBe "custom-uid-123"
        deserializedEvent.message shouldBe "Custom UID test event"
    }

    "should verify ClusterEvent queue factory integration" {
        // 验证ClusterEvent队列与QueueFactory的集成
        val factoryClass = FluvioQueueFactory::class.java

        // 验证ClusterEvent队列方法存在并且有正确的注解
        val clusterEventMethod = factoryClass.getDeclaredMethod("clusterEvent")
        clusterEventMethod shouldNotBe null

        // 验证方法返回类型
        clusterEventMethod.returnType shouldBe io.kestra.core.queues.QueueInterface::class.java
    }

    "should support ClusterEvent serialization performance" {
        // 性能测试：验证ClusterEvent序列化性能
        val clusterEvent = ClusterEvent(
            ClusterEvent.EventType.MAINTENANCE_ENTER,
            LocalDateTime.now(),
            "Performance test event"
        )

        val serializer = FluvioProtobufSerializer()
        
        // 批量序列化测试
        val iterations = 50
        val startTime = System.nanoTime()

        repeat(iterations) {
            val serializedData = serializer.serialize(clusterEvent)
            val deserializedEvent = serializer.deserialize(serializedData, ClusterEvent::class.java)
            
            // 验证数据完整性
            deserializedEvent.eventType shouldBe ClusterEvent.EventType.MAINTENANCE_ENTER
        }

        val endTime = System.nanoTime()
        val avgTime = (endTime - startTime) / iterations / 1_000_000.0

        println("ClusterEvent平均序列化/反序列化时间: ${avgTime} ms")
        
        // 性能应该在合理范围内（小于2ms）
        avgTime shouldBeLessThan 2.0
    }
})

// 扩展函数用于性能断言
private infix fun Double.shouldBeLessThan(expected: Double) {
    if (this >= expected) {
        throw AssertionError("Expected $this to be less than $expected")
    }
}

private infix fun Int.shouldBeGreaterThan(expected: Int) {
    if (this <= expected) {
        throw AssertionError("Expected $this to be greater than $expected")
    }
}
