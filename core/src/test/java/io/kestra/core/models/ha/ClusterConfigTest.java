package io.kestra.core.models.ha;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.Duration;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ClusterConfig Tests")
class ClusterConfigTest {

    @Test
    @DisplayName("Should create cluster config with default values")
    void shouldCreateClusterConfigWithDefaults() {
        ClusterConfig config = ClusterConfig.builder()
            .clusterId("test-cluster")
            .clusterName("Test Cluster")
            .nodeConfig(createDefaultNodeConfig())
            .failover(createDefaultFailoverConfig())
            .healthCheck(createDefaultHealthCheckConfig())
            .build();

        assertNotNull(config);
        assertEquals("test-cluster", config.getClusterId());
        assertEquals("Test Cluster", config.getClusterName());
        assertEquals(ClusterConfig.ClusterTopology.ACTIVE_PASSIVE, config.getTopology());
        assertFalse(config.isEnabled());
        assertTrue(config.getMetadata().isEmpty());
    }

    @Test
    @DisplayName("Should create cluster config with custom topology")
    void shouldCreateClusterConfigWithCustomTopology() {
        ClusterConfig config = ClusterConfig.builder()
            .clusterId("active-active-cluster")
            .clusterName("Active-Active Cluster")
            .topology(ClusterConfig.ClusterTopology.ACTIVE_ACTIVE)
            .nodeConfig(createDefaultNodeConfig())
            .failover(createDefaultFailoverConfig())
            .healthCheck(createDefaultHealthCheckConfig())
            .enabled(true)
            .build();

        assertEquals(ClusterConfig.ClusterTopology.ACTIVE_ACTIVE, config.getTopology());
        assertTrue(config.isEnabled());
    }

    @Test
    @DisplayName("Should create node config with discovery settings")
    void shouldCreateNodeConfigWithDiscovery() {
        ClusterConfig.NodeDiscoveryConfig discovery = ClusterConfig.NodeDiscoveryConfig.builder()
            .method(ClusterConfig.DiscoveryMethod.KUBERNETES)
            .kubernetes(ClusterConfig.KubernetesConfig.builder()
                .namespace("dataflare")
                .serviceName("dataflare-service")
                .labelSelector(Map.of("app", "dataflare"))
                .build())
            .discoveryInterval(Duration.ofSeconds(15))
            .build();

        ClusterConfig.NodeConfig nodeConfig = ClusterConfig.NodeConfig.builder()
            .minNodes(2)
            .maxNodes(5)
            .allowedRoles(Set.of(ClusterConfig.NodeRole.EXECUTOR, ClusterConfig.NodeRole.SCHEDULER))
            .discovery(discovery)
            .communication(createDefaultCommunicationConfig())
            .build();

        assertEquals(2, nodeConfig.getMinNodes());
        assertEquals(5, nodeConfig.getMaxNodes());
        assertEquals(2, nodeConfig.getAllowedRoles().size());
        assertEquals(ClusterConfig.DiscoveryMethod.KUBERNETES, nodeConfig.getDiscovery().getMethod());
        assertEquals("dataflare", nodeConfig.getDiscovery().getKubernetes().getNamespace());
        assertEquals(Duration.ofSeconds(15), nodeConfig.getDiscovery().getDiscoveryInterval());
    }

    @Test
    @DisplayName("Should create consul discovery config")
    void shouldCreateConsulDiscoveryConfig() {
        ClusterConfig.ConsulConfig consul = ClusterConfig.ConsulConfig.builder()
            .host("consul.example.com")
            .port(8500)
            .serviceName("dataflare")
            .datacenter("dc1")
            .build();

        ClusterConfig.NodeDiscoveryConfig discovery = ClusterConfig.NodeDiscoveryConfig.builder()
            .method(ClusterConfig.DiscoveryMethod.CONSUL)
            .consul(consul)
            .build();

        assertEquals(ClusterConfig.DiscoveryMethod.CONSUL, discovery.getMethod());
        assertEquals("consul.example.com", discovery.getConsul().getHost());
        assertEquals(8500, discovery.getConsul().getPort());
        assertEquals("dataflare", discovery.getConsul().getServiceName());
        assertEquals("dc1", discovery.getConsul().getDatacenter());
    }

    @Test
    @DisplayName("Should create SSL configuration")
    void shouldCreateSslConfiguration() {
        ClusterConfig.SslConfig ssl = ClusterConfig.SslConfig.builder()
            .enabled(true)
            .keystorePath("/path/to/keystore.jks")
            .keystorePassword("keystore-password")
            .truststorePath("/path/to/truststore.jks")
            .truststorePassword("truststore-password")
            .build();

        ClusterConfig.NodeCommunicationConfig communication = ClusterConfig.NodeCommunicationConfig.builder()
            .protocol(ClusterConfig.CommunicationProtocol.HTTPS)
            .port(8443)
            .ssl(ssl)
            .connectionTimeout(Duration.ofSeconds(15))
            .readTimeout(Duration.ofSeconds(45))
            .build();

        assertTrue(communication.getSsl().isEnabled());
        assertEquals("/path/to/keystore.jks", communication.getSsl().getKeystorePath());
        assertEquals("keystore-password", communication.getSsl().getKeystorePassword());
        assertEquals(ClusterConfig.CommunicationProtocol.HTTPS, communication.getProtocol());
        assertEquals(8443, communication.getPort());
    }

    @Test
    @DisplayName("Should create load balancer configuration")
    void shouldCreateLoadBalancerConfiguration() {
        ClusterConfig.SessionAffinityConfig sessionAffinity = ClusterConfig.SessionAffinityConfig.builder()
            .enabled(true)
            .sessionTimeout(Duration.ofMinutes(45))
            .cookieName("DATAFLARE_LB_SESSION")
            .build();

        ClusterConfig.LoadBalancerConfig loadBalancer = ClusterConfig.LoadBalancerConfig.builder()
            .algorithm(ClusterConfig.LoadBalancingAlgorithm.LEAST_CONNECTIONS)
            .healthCheck(createDefaultHealthCheckConfig())
            .sessionAffinity(sessionAffinity)
            .build();

        assertEquals(ClusterConfig.LoadBalancingAlgorithm.LEAST_CONNECTIONS, loadBalancer.getAlgorithm());
        assertTrue(loadBalancer.getSessionAffinity().isEnabled());
        assertEquals(Duration.ofMinutes(45), loadBalancer.getSessionAffinity().getSessionTimeout());
        assertEquals("DATAFLARE_LB_SESSION", loadBalancer.getSessionAffinity().getCookieName());
    }

    @Test
    @DisplayName("Should create failover configuration")
    void shouldCreateFailoverConfiguration() {
        ClusterConfig.FailoverConfig failover = ClusterConfig.FailoverConfig.builder()
            .strategy(ClusterConfig.FailoverStrategy.PRIORITY_BASED)
            .maxFailoverTime(Duration.ofMinutes(10))
            .retryCount(5)
            .retryInterval(Duration.ofSeconds(15))
            .autoFailback(false)
            .failbackDelay(Duration.ofMinutes(5))
            .build();

        assertEquals(ClusterConfig.FailoverStrategy.PRIORITY_BASED, failover.getStrategy());
        assertEquals(Duration.ofMinutes(10), failover.getMaxFailoverTime());
        assertEquals(5, failover.getRetryCount());
        assertEquals(Duration.ofSeconds(15), failover.getRetryInterval());
        assertFalse(failover.isAutoFailback());
        assertEquals(Duration.ofMinutes(5), failover.getFailbackDelay());
    }

    @Test
    @DisplayName("Should create health check configuration")
    void shouldCreateHealthCheckConfiguration() {
        ClusterConfig.HealthCheckConfig healthCheck = ClusterConfig.HealthCheckConfig.builder()
            .interval(Duration.ofSeconds(15))
            .timeout(Duration.ofSeconds(5))
            .failureThreshold(5)
            .successThreshold(2)
            .healthCheckPath("/api/health")
            .expectedStatusCodes(Set.of(200, 204))
            .build();

        assertEquals(Duration.ofSeconds(15), healthCheck.getInterval());
        assertEquals(Duration.ofSeconds(5), healthCheck.getTimeout());
        assertEquals(5, healthCheck.getFailureThreshold());
        assertEquals(2, healthCheck.getSuccessThreshold());
        assertEquals("/api/health", healthCheck.getHealthCheckPath());
        assertTrue(healthCheck.getExpectedStatusCodes().contains(200));
        assertTrue(healthCheck.getExpectedStatusCodes().contains(204));
    }

    @Test
    @DisplayName("Should create replication configuration")
    void shouldCreateReplicationConfiguration() {
        ClusterConfig.ReplicationConfig replication = ClusterConfig.ReplicationConfig.builder()
            .strategy(ClusterConfig.ReplicationStrategy.SEMI_SYNC)
            .replicationFactor(3)
            .consistencyLevel(ClusterConfig.ConsistencyLevel.STRONG)
            .replicationTimeout(Duration.ofSeconds(45))
            .build();

        assertEquals(ClusterConfig.ReplicationStrategy.SEMI_SYNC, replication.getStrategy());
        assertEquals(3, replication.getReplicationFactor());
        assertEquals(ClusterConfig.ConsistencyLevel.STRONG, replication.getConsistencyLevel());
        assertEquals(Duration.ofSeconds(45), replication.getReplicationTimeout());
    }

    @Test
    @DisplayName("Should create complete cluster configuration")
    void shouldCreateCompleteClusterConfiguration() {
        ClusterConfig config = ClusterConfig.builder()
            .clusterId("production-cluster")
            .clusterName("Production DataFlare Cluster")
            .topology(ClusterConfig.ClusterTopology.ACTIVE_ACTIVE)
            .nodeConfig(createCompleteNodeConfig())
            .loadBalancer(createCompleteLoadBalancerConfig())
            .failover(createCompleteFailoverConfig())
            .healthCheck(createCompleteHealthCheckConfig())
            .replication(createCompleteReplicationConfig())
            .metadata(Map.of(
                "environment", "production",
                "region", "us-west-2",
                "version", "1.0.0"
            ))
            .enabled(true)
            .build();

        assertNotNull(config);
        assertEquals("production-cluster", config.getClusterId());
        assertEquals("Production DataFlare Cluster", config.getClusterName());
        assertEquals(ClusterConfig.ClusterTopology.ACTIVE_ACTIVE, config.getTopology());
        assertTrue(config.isEnabled());
        assertEquals("production", config.getMetadata().get("environment"));
        assertEquals("us-west-2", config.getMetadata().get("region"));
        assertEquals("1.0.0", config.getMetadata().get("version"));
        
        // Verify all components are properly configured
        assertNotNull(config.getNodeConfig());
        assertNotNull(config.getLoadBalancer());
        assertNotNull(config.getFailover());
        assertNotNull(config.getHealthCheck());
        assertNotNull(config.getReplication());
    }

    @Test
    @DisplayName("Should validate cluster config builder pattern")
    void shouldValidateClusterConfigBuilderPattern() {
        ClusterConfig original = ClusterConfig.builder()
            .clusterId("test-cluster")
            .clusterName("Test Cluster")
            .nodeConfig(createDefaultNodeConfig())
            .failover(createDefaultFailoverConfig())
            .healthCheck(createDefaultHealthCheckConfig())
            .enabled(false)
            .build();

        ClusterConfig modified = original.toBuilder()
            .enabled(true)
            .topology(ClusterConfig.ClusterTopology.MESH)
            .metadata(Map.of("updated", "true"))
            .build();

        // Original should be unchanged
        assertFalse(original.isEnabled());
        assertEquals(ClusterConfig.ClusterTopology.ACTIVE_PASSIVE, original.getTopology());
        assertTrue(original.getMetadata().isEmpty());

        // Modified should have new values
        assertTrue(modified.isEnabled());
        assertEquals(ClusterConfig.ClusterTopology.MESH, modified.getTopology());
        assertEquals("true", modified.getMetadata().get("updated"));
        
        // Common fields should be the same
        assertEquals(original.getClusterId(), modified.getClusterId());
        assertEquals(original.getClusterName(), modified.getClusterName());
    }

    // Helper methods for creating test configurations

    private ClusterConfig.NodeConfig createDefaultNodeConfig() {
        return ClusterConfig.NodeConfig.builder()
            .discovery(ClusterConfig.NodeDiscoveryConfig.builder()
                .method(ClusterConfig.DiscoveryMethod.STATIC)
                .build())
            .communication(createDefaultCommunicationConfig())
            .build();
    }

    private ClusterConfig.NodeCommunicationConfig createDefaultCommunicationConfig() {
        return ClusterConfig.NodeCommunicationConfig.builder()
            .protocol(ClusterConfig.CommunicationProtocol.HTTP)
            .port(8080)
            .build();
    }

    private ClusterConfig.FailoverConfig createDefaultFailoverConfig() {
        return ClusterConfig.FailoverConfig.builder().build();
    }

    private ClusterConfig.HealthCheckConfig createDefaultHealthCheckConfig() {
        return ClusterConfig.HealthCheckConfig.builder().build();
    }

    private ClusterConfig.NodeConfig createCompleteNodeConfig() {
        return ClusterConfig.NodeConfig.builder()
            .minNodes(3)
            .maxNodes(10)
            .allowedRoles(Set.of(
                ClusterConfig.NodeRole.EXECUTOR,
                ClusterConfig.NodeRole.SCHEDULER,
                ClusterConfig.NodeRole.WEBSERVER
            ))
            .discovery(ClusterConfig.NodeDiscoveryConfig.builder()
                .method(ClusterConfig.DiscoveryMethod.KUBERNETES)
                .kubernetes(ClusterConfig.KubernetesConfig.builder()
                    .namespace("dataflare-prod")
                    .serviceName("dataflare-cluster")
                    .build())
                .build())
            .communication(ClusterConfig.NodeCommunicationConfig.builder()
                .protocol(ClusterConfig.CommunicationProtocol.HTTPS)
                .port(8443)
                .ssl(ClusterConfig.SslConfig.builder()
                    .enabled(true)
                    .build())
                .build())
            .build();
    }

    private ClusterConfig.LoadBalancerConfig createCompleteLoadBalancerConfig() {
        return ClusterConfig.LoadBalancerConfig.builder()
            .algorithm(ClusterConfig.LoadBalancingAlgorithm.WEIGHTED_ROUND_ROBIN)
            .healthCheck(createCompleteHealthCheckConfig())
            .sessionAffinity(ClusterConfig.SessionAffinityConfig.builder()
                .enabled(true)
                .build())
            .build();
    }

    private ClusterConfig.FailoverConfig createCompleteFailoverConfig() {
        return ClusterConfig.FailoverConfig.builder()
            .strategy(ClusterConfig.FailoverStrategy.AUTOMATIC)
            .maxFailoverTime(Duration.ofMinutes(3))
            .retryCount(3)
            .autoFailback(true)
            .build();
    }

    private ClusterConfig.HealthCheckConfig createCompleteHealthCheckConfig() {
        return ClusterConfig.HealthCheckConfig.builder()
            .interval(Duration.ofSeconds(20))
            .timeout(Duration.ofSeconds(8))
            .failureThreshold(3)
            .successThreshold(1)
            .build();
    }

    private ClusterConfig.ReplicationConfig createCompleteReplicationConfig() {
        return ClusterConfig.ReplicationConfig.builder()
            .strategy(ClusterConfig.ReplicationStrategy.ASYNC)
            .replicationFactor(2)
            .consistencyLevel(ClusterConfig.ConsistencyLevel.EVENTUAL)
            .build();
    }
}
