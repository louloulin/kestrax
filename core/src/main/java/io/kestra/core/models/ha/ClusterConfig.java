package io.kestra.core.models.ha;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.micronaut.core.annotation.Introspected;
import lombok.Builder;
import lombok.Value;
import lombok.With;
import lombok.extern.jackson.Jacksonized;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Min;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * High Availability Cluster Configuration for DataFlare
 * Defines cluster topology, node roles, and failover strategies
 */
@Value
@Builder(toBuilder = true)
@Jacksonized
@Introspected
@With
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ClusterConfig {

    /**
     * Unique cluster identifier
     */
    @NotNull
    String clusterId;

    /**
     * Cluster name for display purposes
     */
    @NotNull
    String clusterName;

    /**
     * Cluster topology configuration
     */
    @NotNull
    @Builder.Default
    ClusterTopology topology = ClusterTopology.ACTIVE_PASSIVE;

    /**
     * Node configuration
     */
    @NotNull
    NodeConfig nodeConfig;

    /**
     * Load balancer configuration
     */
    LoadBalancerConfig loadBalancer;

    /**
     * Failover configuration
     */
    @NotNull
    FailoverConfig failover;

    /**
     * Health check configuration
     */
    @NotNull
    HealthCheckConfig healthCheck;

    /**
     * Data replication configuration
     */
    ReplicationConfig replication;

    /**
     * Cluster metadata
     */
    @Builder.Default
    Map<String, String> metadata = Map.of();

    /**
     * Whether cluster is enabled
     */
    @Builder.Default
    boolean enabled = false;

    /**
     * Cluster topology types
     */
    public enum ClusterTopology {
        /**
         * Active-Passive: One active node, others standby
         */
        ACTIVE_PASSIVE,

        /**
         * Active-Active: Multiple active nodes with load balancing
         */
        ACTIVE_ACTIVE,

        /**
         * Master-Slave: One master for writes, slaves for reads
         */
        MASTER_SLAVE,

        /**
         * Mesh: All nodes can handle all operations
         */
        MESH
    }

    /**
     * Node configuration within cluster
     */
    @Value
    @Builder(toBuilder = true)
    @Jacksonized
    @Introspected
    @With
    public static class NodeConfig {

        /**
         * Minimum number of nodes required for cluster operation
         */
        @Min(1)
        @Builder.Default
        int minNodes = 1;

        /**
         * Maximum number of nodes in cluster
         */
        @Positive
        @Builder.Default
        int maxNodes = 10;

        /**
         * Node roles configuration
         */
        @NotNull
        @Builder.Default
        Set<NodeRole> allowedRoles = Set.of(NodeRole.EXECUTOR, NodeRole.SCHEDULER);

        /**
         * Node discovery configuration
         */
        @NotNull
        NodeDiscoveryConfig discovery;

        /**
         * Node communication configuration
         */
        @NotNull
        NodeCommunicationConfig communication;
    }

    /**
     * Node roles in cluster
     */
    public enum NodeRole {
        /**
         * Executes workflows and tasks
         */
        EXECUTOR,

        /**
         * Schedules workflows
         */
        SCHEDULER,

        /**
         * Serves web UI and API
         */
        WEBSERVER,

        /**
         * Manages cluster coordination
         */
        COORDINATOR,

        /**
         * Handles indexing and search
         */
        INDEXER
    }

    /**
     * Node discovery configuration
     */
    @Value
    @Builder(toBuilder = true)
    @Jacksonized
    @Introspected
    @With
    public static class NodeDiscoveryConfig {

        /**
         * Discovery method
         */
        @NotNull
        @Builder.Default
        DiscoveryMethod method = DiscoveryMethod.STATIC;

        /**
         * Static node list (for STATIC discovery)
         */
        List<String> staticNodes;

        /**
         * Kubernetes service configuration (for KUBERNETES discovery)
         */
        KubernetesConfig kubernetes;

        /**
         * Consul configuration (for CONSUL discovery)
         */
        ConsulConfig consul;

        /**
         * Discovery interval
         */
        @NotNull
        @Builder.Default
        Duration discoveryInterval = Duration.ofSeconds(30);
    }

    /**
     * Discovery methods
     */
    public enum DiscoveryMethod {
        STATIC,
        KUBERNETES,
        CONSUL,
        ETCD,
        ZOOKEEPER
    }

    /**
     * Kubernetes discovery configuration
     */
    @Value
    @Builder(toBuilder = true)
    @Jacksonized
    @Introspected
    @With
    public static class KubernetesConfig {

        /**
         * Kubernetes namespace
         */
        @NotNull
        String namespace;

        /**
         * Service name
         */
        @NotNull
        String serviceName;

        /**
         * Label selector
         */
        Map<String, String> labelSelector;
    }

    /**
     * Consul discovery configuration
     */
    @Value
    @Builder(toBuilder = true)
    @Jacksonized
    @Introspected
    @With
    public static class ConsulConfig {

        /**
         * Consul host
         */
        @NotNull
        String host;

        /**
         * Consul port
         */
        @Positive
        @Builder.Default
        int port = 8500;

        /**
         * Service name in Consul
         */
        @NotNull
        String serviceName;

        /**
         * Consul datacenter
         */
        String datacenter;
    }

    /**
     * Node communication configuration
     */
    @Value
    @Builder(toBuilder = true)
    @Jacksonized
    @Introspected
    @With
    public static class NodeCommunicationConfig {

        /**
         * Communication protocol
         */
        @NotNull
        @Builder.Default
        CommunicationProtocol protocol = CommunicationProtocol.HTTP;

        /**
         * Communication port
         */
        @Positive
        @Builder.Default
        int port = 8080;

        /**
         * SSL/TLS configuration
         */
        SslConfig ssl;

        /**
         * Connection timeout
         */
        @NotNull
        @Builder.Default
        Duration connectionTimeout = Duration.ofSeconds(10);

        /**
         * Read timeout
         */
        @NotNull
        @Builder.Default
        Duration readTimeout = Duration.ofSeconds(30);
    }

    /**
     * Communication protocols
     */
    public enum CommunicationProtocol {
        HTTP,
        HTTPS,
        GRPC,
        GRPC_TLS
    }

    /**
     * SSL configuration
     */
    @Value
    @Builder(toBuilder = true)
    @Jacksonized
    @Introspected
    @With
    public static class SslConfig {

        /**
         * Whether SSL is enabled
         */
        @Builder.Default
        boolean enabled = false;

        /**
         * Keystore path
         */
        String keystorePath;

        /**
         * Keystore password
         */
        String keystorePassword;

        /**
         * Truststore path
         */
        String truststorePath;

        /**
         * Truststore password
         */
        String truststorePassword;
    }

    /**
     * Load balancer configuration
     */
    @Value
    @Builder(toBuilder = true)
    @Jacksonized
    @Introspected
    @With
    public static class LoadBalancerConfig {

        /**
         * Load balancing algorithm
         */
        @NotNull
        @Builder.Default
        LoadBalancingAlgorithm algorithm = LoadBalancingAlgorithm.ROUND_ROBIN;

        /**
         * Health check configuration for load balancer
         */
        @NotNull
        HealthCheckConfig healthCheck;

        /**
         * Session affinity configuration
         */
        SessionAffinityConfig sessionAffinity;
    }

    /**
     * Load balancing algorithms
     */
    public enum LoadBalancingAlgorithm {
        ROUND_ROBIN,
        LEAST_CONNECTIONS,
        WEIGHTED_ROUND_ROBIN,
        IP_HASH,
        RANDOM
    }

    /**
     * Session affinity configuration
     */
    @Value
    @Builder(toBuilder = true)
    @Jacksonized
    @Introspected
    @With
    public static class SessionAffinityConfig {

        /**
         * Whether session affinity is enabled
         */
        @Builder.Default
        boolean enabled = false;

        /**
         * Session timeout
         */
        @NotNull
        @Builder.Default
        Duration sessionTimeout = Duration.ofMinutes(30);

        /**
         * Cookie name for session affinity
         */
        @Builder.Default
        String cookieName = "DATAFLARE_SESSION";
    }

    /**
     * Failover configuration
     */
    @Value
    @Builder(toBuilder = true)
    @Jacksonized
    @Introspected
    @With
    public static class FailoverConfig {

        /**
         * Failover strategy
         */
        @NotNull
        @Builder.Default
        FailoverStrategy strategy = FailoverStrategy.AUTOMATIC;

        /**
         * Maximum failover time
         */
        @NotNull
        @Builder.Default
        Duration maxFailoverTime = Duration.ofMinutes(5);

        /**
         * Number of retries before failover
         */
        @Min(0)
        @Builder.Default
        int retryCount = 3;

        /**
         * Retry interval
         */
        @NotNull
        @Builder.Default
        Duration retryInterval = Duration.ofSeconds(10);

        /**
         * Whether to enable automatic failback
         */
        @Builder.Default
        boolean autoFailback = true;

        /**
         * Failback delay
         */
        @NotNull
        @Builder.Default
        Duration failbackDelay = Duration.ofMinutes(2);
    }

    /**
     * Failover strategies
     */
    public enum FailoverStrategy {
        AUTOMATIC,
        MANUAL,
        PRIORITY_BASED
    }

    /**
     * Health check configuration
     */
    @Value
    @Builder(toBuilder = true)
    @Jacksonized
    @Introspected
    @With
    public static class HealthCheckConfig {

        /**
         * Health check interval
         */
        @NotNull
        @Builder.Default
        Duration interval = Duration.ofSeconds(30);

        /**
         * Health check timeout
         */
        @NotNull
        @Builder.Default
        Duration timeout = Duration.ofSeconds(10);

        /**
         * Number of consecutive failures before marking unhealthy
         */
        @Min(1)
        @Builder.Default
        int failureThreshold = 3;

        /**
         * Number of consecutive successes before marking healthy
         */
        @Min(1)
        @Builder.Default
        int successThreshold = 1;

        /**
         * Health check endpoint path
         */
        @NotNull
        @Builder.Default
        String healthCheckPath = "/health";

        /**
         * Expected HTTP status codes for healthy response
         */
        @NotNull
        @Builder.Default
        Set<Integer> expectedStatusCodes = Set.of(200);
    }

    /**
     * Data replication configuration
     */
    @Value
    @Builder(toBuilder = true)
    @Jacksonized
    @Introspected
    @With
    public static class ReplicationConfig {

        /**
         * Replication strategy
         */
        @NotNull
        @Builder.Default
        ReplicationStrategy strategy = ReplicationStrategy.ASYNC;

        /**
         * Replication factor (number of replicas)
         */
        @Min(1)
        @Builder.Default
        int replicationFactor = 2;

        /**
         * Consistency level
         */
        @NotNull
        @Builder.Default
        ConsistencyLevel consistencyLevel = ConsistencyLevel.EVENTUAL;

        /**
         * Replication timeout
         */
        @NotNull
        @Builder.Default
        Duration replicationTimeout = Duration.ofSeconds(30);
    }

    /**
     * Replication strategies
     */
    public enum ReplicationStrategy {
        SYNC,
        ASYNC,
        SEMI_SYNC
    }

    /**
     * Consistency levels
     */
    public enum ConsistencyLevel {
        STRONG,
        EVENTUAL,
        WEAK
    }
}
