package io.kestra.queue.fluvio

import jakarta.inject.Singleton
import org.slf4j.LoggerFactory

/**
 * Simple test bean to verify that Fluvio module is loaded
 */
@Singleton
class FluvioTestBean {
    
    private val logger = LoggerFactory.getLogger(FluvioTestBean::class.java)
    
    init {
        logger.info("🚀 FluvioTestBean created - Fluvio module is loaded!")
    }
    
    fun test(): String {
        return "Fluvio module is working!"
    }
}
