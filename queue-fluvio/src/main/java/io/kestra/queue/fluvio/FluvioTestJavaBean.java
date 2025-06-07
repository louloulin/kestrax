package io.kestra.queue.fluvio;

import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple Java test bean to verify that Fluvio module is loaded
 */
@Singleton
public class FluvioTestJavaBean {
    
    private static final Logger logger = LoggerFactory.getLogger(FluvioTestJavaBean.class);
    
    public FluvioTestJavaBean() {
        logger.info("🚀 FluvioTestJavaBean created - Fluvio module is loaded!");
    }
    
    public String test() {
        return "Fluvio module is working!";
    }
}
