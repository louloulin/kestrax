package io.kestra.queue.fluvio.serialization

import com.fasterxml.jackson.databind.ObjectMapper
import io.kestra.core.exceptions.DeserializationException
import jakarta.inject.Singleton
import org.slf4j.LoggerFactory

/**
 * Simplified serializer for Fluvio queue messages
 * Uses JSON serialization as a fallback until Protocol Buffers are properly configured
 */
@Singleton
class ProtobufSerializer {

    private val logger = LoggerFactory.getLogger(ProtobufSerializer::class.java)
    private val objectMapper = ObjectMapper()

    /**
     * Serialize an object to byte array
     * Currently uses JSON serialization as a temporary implementation
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
     * Currently uses JSON deserialization as a temporary implementation
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