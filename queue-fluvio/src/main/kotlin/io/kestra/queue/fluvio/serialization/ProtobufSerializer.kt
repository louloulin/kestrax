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
 * Currently uses JSON serialization with Protocol Buffers infrastructure ready for future optimization
 * TODO: Implement full Protocol Buffers serialization for 3-4x performance improvement
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
            // For now, use JSON serialization for all types
            // TODO: Implement Protocol Buffers serialization for core types
            logger.debug("Serializing object of type: {}", obj?.javaClass?.simpleName)
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
            // For now, use JSON deserialization for all types
            // TODO: Implement Protocol Buffers deserialization for core types
            logger.debug("Deserializing to class: {}", clazz.name)
            objectMapper.readValue(data, clazz)
        } catch (e: Exception) {
            logger.error("Failed to deserialize data to class: {}", clazz.name, e)
            throw DeserializationException(e, String(data))
        }
    }
}