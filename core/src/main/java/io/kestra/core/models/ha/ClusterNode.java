package io.kestra.core.models.ha;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.micronaut.core.annotation.Introspected;
import lombok.Builder;
import lombok.Value;
import lombok.With;
import lombok.extern.jackson.Jacksonized;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Instant;
import java.util.Map;
import java.util.Set;

/**
 * Represents a node in the DataFlare cluster
 * Contains node metadata, status, and capabilities
 */
@Value
@Builder(toBuilder = true)
@Jacksonized
@Introspected
@With
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ClusterNode {

    /**
     * Unique node identifier
     */
    @NotNull
    String nodeId;

    /**
     * Node name for display purposes
     */
    @NotNull
    String nodeName;

    /**
     * Cluster this node belongs to
     */
    @NotNull
    String clusterId;

    /**
     * Node host/IP address
     */
    @NotNull
    String host;

    /**
     * Node port
     */
    @Positive
    int port;

    /**
     * Node roles
     */
    @NotNull
    Set<ClusterConfig.NodeRole> roles;

    /**
     * Current node status
     */
    @NotNull
    @Builder.Default
    NodeStatus status = NodeStatus.STARTING;

    /**
     * Node health information
     */
    @NotNull
    NodeHealth health;

    /**
     * Node capabilities and resources
     */
    @NotNull
    NodeCapabilities capabilities;

    /**
     * Node configuration
     */
    @NotNull
    NodeConfiguration configuration;

    /**
     * Node metadata
     */
    @Builder.Default
    Map<String, String> metadata = Map.of();

    /**
     * Node version information
     */
    @NotNull
    NodeVersion version;

    /**
     * When the node was created
     */
    @NotNull
    @Builder.Default
    Instant createdAt = Instant.now();

    /**
     * When the node was last updated
     */
    @NotNull
    @Builder.Default
    Instant updatedAt = Instant.now();

    /**
     * When the node last sent a heartbeat
     */
    @NotNull
    @Builder.Default
    Instant lastHeartbeat = Instant.now();

    /**
     * Node status enumeration
     */
    public enum NodeStatus {
        /**
         * Node is starting up
         */
        STARTING,

        /**
         * Node is healthy and active
         */
        HEALTHY,

        /**
         * Node is unhealthy but still running
         */
        UNHEALTHY,

        /**
         * Node is draining (not accepting new work)
         */
        DRAINING,

        /**
         * Node is offline
         */
        OFFLINE,

        /**
         * Node has failed
         */
        FAILED,

        /**
         * Node is in maintenance mode
         */
        MAINTENANCE
    }

    /**
     * Node health information
     */
    @Value
    @Builder(toBuilder = true)
    @Jacksonized
    @Introspected
    @With
    public static class NodeHealth {

        /**
         * Overall health status
         */
        @NotNull
        @Builder.Default
        HealthStatus overallStatus = HealthStatus.UNKNOWN;

        /**
         * CPU usage percentage (0-100)
         */
        @Builder.Default
        double cpuUsage = 0.0;

        /**
         * Memory usage percentage (0-100)
         */
        @Builder.Default
        double memoryUsage = 0.0;

        /**
         * Disk usage percentage (0-100)
         */
        @Builder.Default
        double diskUsage = 0.0;

        /**
         * Network latency in milliseconds
         */
        @Builder.Default
        long networkLatency = 0;

        /**
         * Number of active connections
         */
        @Builder.Default
        int activeConnections = 0;

        /**
         * Number of active executions
         */
        @Builder.Default
        int activeExecutions = 0;

        /**
         * Load average (1 minute)
         */
        @Builder.Default
        double loadAverage = 0.0;

        /**
         * JVM heap usage percentage (0-100)
         */
        @Builder.Default
        double heapUsage = 0.0;

        /**
         * Last health check timestamp
         */
        @NotNull
        @Builder.Default
        Instant lastHealthCheck = Instant.now();

        /**
         * Health check details
         */
        @Builder.Default
        Map<String, Object> healthDetails = Map.of();
    }

    /**
     * Health status enumeration
     */
    public enum HealthStatus {
        HEALTHY,
        DEGRADED,
        UNHEALTHY,
        UNKNOWN
    }

    /**
     * Node capabilities and resource information
     */
    @Value
    @Builder(toBuilder = true)
    @Jacksonized
    @Introspected
    @With
    public static class NodeCapabilities {

        /**
         * Number of CPU cores
         */
        @Positive
        @Builder.Default
        int cpuCores = 1;

        /**
         * Total memory in MB
         */
        @Positive
        @Builder.Default
        long totalMemoryMB = 1024;

        /**
         * Available memory in MB
         */
        @Builder.Default
        long availableMemoryMB = 512;

        /**
         * Total disk space in MB
         */
        @Positive
        @Builder.Default
        long totalDiskMB = 10240;

        /**
         * Available disk space in MB
         */
        @Builder.Default
        long availableDiskMB = 5120;

        /**
         * Maximum concurrent executions
         */
        @Positive
        @Builder.Default
        int maxConcurrentExecutions = 10;

        /**
         * Current concurrent executions
         */
        @Builder.Default
        int currentConcurrentExecutions = 0;

        /**
         * Supported task types
         */
        @Builder.Default
        Set<String> supportedTaskTypes = Set.of();

        /**
         * Node tags for scheduling
         */
        @Builder.Default
        Set<String> tags = Set.of();

        /**
         * Node priority (higher = more preferred)
         */
        @Builder.Default
        int priority = 100;

        /**
         * Whether node can be used for scheduling
         */
        @Builder.Default
        boolean schedulable = true;
    }

    /**
     * Node configuration
     */
    @Value
    @Builder(toBuilder = true)
    @Jacksonized
    @Introspected
    @With
    public static class NodeConfiguration {

        /**
         * Node environment (dev, staging, prod)
         */
        @NotNull
        @Builder.Default
        String environment = "dev";

        /**
         * Node region
         */
        String region;

        /**
         * Node availability zone
         */
        String availabilityZone;

        /**
         * Node data center
         */
        String dataCenter;

        /**
         * JVM configuration
         */
        JvmConfiguration jvm;

        /**
         * Network configuration
         */
        NetworkConfiguration network;

        /**
         * Storage configuration
         */
        StorageConfiguration storage;

        /**
         * Security configuration
         */
        SecurityConfiguration security;
    }

    /**
     * JVM configuration
     */
    @Value
    @Builder(toBuilder = true)
    @Jacksonized
    @Introspected
    @With
    public static class JvmConfiguration {

        /**
         * JVM version
         */
        String version;

        /**
         * JVM vendor
         */
        String vendor;

        /**
         * Maximum heap size in MB
         */
        @Positive
        @Builder.Default
        long maxHeapSizeMB = 1024;

        /**
         * Initial heap size in MB
         */
        @Positive
        @Builder.Default
        long initialHeapSizeMB = 256;

        /**
         * GC algorithm
         */
        String gcAlgorithm;

        /**
         * JVM arguments
         */
        @Builder.Default
        Set<String> jvmArgs = Set.of();
    }

    /**
     * Network configuration
     */
    @Value
    @Builder(toBuilder = true)
    @Jacksonized
    @Introspected
    @With
    public static class NetworkConfiguration {

        /**
         * Network interface
         */
        String networkInterface;

        /**
         * Bind address
         */
        String bindAddress;

        /**
         * Public address (for external access)
         */
        String publicAddress;

        /**
         * SSL/TLS enabled
         */
        @Builder.Default
        boolean sslEnabled = false;

        /**
         * Connection pool size
         */
        @Positive
        @Builder.Default
        int connectionPoolSize = 10;
    }

    /**
     * Storage configuration
     */
    @Value
    @Builder(toBuilder = true)
    @Jacksonized
    @Introspected
    @With
    public static class StorageConfiguration {

        /**
         * Data directory path
         */
        @NotNull
        String dataDirectory;

        /**
         * Temporary directory path
         */
        @NotNull
        String tempDirectory;

        /**
         * Log directory path
         */
        @NotNull
        String logDirectory;

        /**
         * Storage type
         */
        @NotNull
        @Builder.Default
        StorageType storageType = StorageType.LOCAL;

        /**
         * Storage encryption enabled
         */
        @Builder.Default
        boolean encryptionEnabled = false;
    }

    /**
     * Storage types
     */
    public enum StorageType {
        LOCAL,
        NFS,
        S3,
        GCS,
        AZURE_BLOB
    }

    /**
     * Security configuration
     */
    @Value
    @Builder(toBuilder = true)
    @Jacksonized
    @Introspected
    @With
    public static class SecurityConfiguration {

        /**
         * Authentication enabled
         */
        @Builder.Default
        boolean authenticationEnabled = true;

        /**
         * Authorization enabled
         */
        @Builder.Default
        boolean authorizationEnabled = true;

        /**
         * Audit logging enabled
         */
        @Builder.Default
        boolean auditLoggingEnabled = true;

        /**
         * Allowed client certificates
         */
        @Builder.Default
        Set<String> allowedClientCertificates = Set.of();

        /**
         * Security policies
         */
        @Builder.Default
        Set<String> securityPolicies = Set.of();
    }

    /**
     * Node version information
     */
    @Value
    @Builder(toBuilder = true)
    @Jacksonized
    @Introspected
    @With
    public static class NodeVersion {

        /**
         * DataFlare version
         */
        @NotNull
        String dataFlareVersion;

        /**
         * Build timestamp
         */
        @NotNull
        String buildTimestamp;

        /**
         * Git commit hash
         */
        String gitCommit;

        /**
         * Build number
         */
        String buildNumber;

        /**
         * Operating system
         */
        String operatingSystem;

        /**
         * Architecture
         */
        String architecture;
    }

    /**
     * Check if node is healthy
     */
    public boolean isHealthy() {
        return status == NodeStatus.HEALTHY &&
               health.getOverallStatus() == HealthStatus.HEALTHY;
    }

    /**
     * Check if node is available for scheduling
     */
    public boolean isAvailable() {
        return isHealthy() &&
               capabilities.isSchedulable() &&
               status != NodeStatus.DRAINING &&
               status != NodeStatus.MAINTENANCE;
    }

    /**
     * Check if node has capacity for more executions
     */
    public boolean hasCapacity() {
        return capabilities.getCurrentConcurrentExecutions() <
               capabilities.getMaxConcurrentExecutions();
    }

    /**
     * Get node load percentage
     */
    public double getLoadPercentage() {
        if (capabilities.getMaxConcurrentExecutions() == 0) {
            return 0.0;
        }
        return (double) capabilities.getCurrentConcurrentExecutions() /
               capabilities.getMaxConcurrentExecutions() * 100.0;
    }

    /**
     * Calculate node score for load balancing
     * Higher score = better candidate
     */
    public double calculateScore() {
        if (!isAvailable()) {
            return 0.0;
        }

        double loadScore = 1.0 - (getLoadPercentage() / 100.0);
        double healthScore = calculateHealthScore();
        double priorityScore = capabilities.getPriority() / 100.0;

        return (loadScore * 0.4) + (healthScore * 0.4) + (priorityScore * 0.2);
    }

    /**
     * Calculate health score based on resource usage
     */
    private double calculateHealthScore() {
        double cpuScore = 1.0 - (health.getCpuUsage() / 100.0);
        double memoryScore = 1.0 - (health.getMemoryUsage() / 100.0);
        double diskScore = 1.0 - (health.getDiskUsage() / 100.0);

        return (cpuScore + memoryScore + diskScore) / 3.0;
    }
}
