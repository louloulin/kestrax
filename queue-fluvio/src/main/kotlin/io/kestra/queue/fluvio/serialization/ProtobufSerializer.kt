package io.kestra.queue.fluvio.serialization

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.kestra.core.exceptions.DeserializationException
import io.kestra.core.models.executions.Execution
import io.kestra.core.models.executions.TaskRun
import io.kestra.core.models.executions.LogEntry
import io.kestra.core.models.executions.MetricEntry
import io.kestra.queue.fluvio.proto.*
import jakarta.inject.Singleton
import org.slf4j.LoggerFactory

/**
 * Protocol Buffers serializer for Fluvio queue messages
 * Provides efficient binary serialization using Protocol Buffers with JSON fallback
 */
@Singleton
class FluvioProtobufSerializer {

    private val logger = LoggerFactory.getLogger(FluvioProtobufSerializer::class.java)
    private val objectMapper = ObjectMapper().apply {
        registerModule(JavaTimeModule())
    }

    /**
     * Serialize an object to byte array
     * Currently uses JSON serialization, Protocol Buffers optimization will be added later
     */
    fun <T> serialize(obj: T): ByteArray {
        return try {
            objectMapper.writeValueAsBytes(obj)
        } catch (e: Exception) {
            logger.error("Failed to serialize object: {}", obj, e)
            throw RuntimeException("Serialization failed", e)
        }
    }

    /**
     * Deserialize byte array to object
     * Currently uses JSON deserialization, Protocol Buffers optimization will be added later
     */
    fun <T> deserialize(data: ByteArray, clazz: Class<T>): T {
        return try {
            objectMapper.readValue(data, clazz)
        } catch (e: Exception) {
            logger.error("Failed to deserialize data to class: {}", clazz.name, e)
            throw DeserializationException(e, String(data))
        }
    }
}