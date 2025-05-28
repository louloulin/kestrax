package io.kestra.core.models.ha;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.Instant;
import java.time.Duration;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ClusterNode Tests")
class ClusterNodeTest {

    @Test
    @DisplayName("Should create cluster node with default values")
    void shouldCreateClusterNodeWithDefaults() {
        ClusterNode node = ClusterNode.builder()
            .nodeId("node-1")
            .nodeName("Test Node 1")
            .clusterId("test-cluster")
            .host("localhost")
            .port(8080)
            .roles(Set.of(ClusterConfig.NodeRole.EXECUTOR))
            .health(createDefaultHealth())
            .capabilities(createDefaultCapabilities())
            .configuration(createDefaultConfiguration())
            .version(createDefaultVersion())
            .build();

        assertNotNull(node);
        assertEquals("node-1", node.getNodeId());
        assertEquals("Test Node 1", node.getNodeName());
        assertEquals("test-cluster", node.getClusterId());
        assertEquals("localhost", node.getHost());
        assertEquals(8080, node.getPort());
        assertEquals(ClusterNode.NodeStatus.STARTING, node.getStatus());
        assertTrue(node.getMetadata().isEmpty());
        assertNotNull(node.getCreatedAt());
        assertNotNull(node.getUpdatedAt());
        assertNotNull(node.getLastHeartbeat());
    }

    @Test
    @DisplayName("Should create cluster node with multiple roles")
    void shouldCreateClusterNodeWithMultipleRoles() {
        Set<ClusterConfig.NodeRole> roles = Set.of(
            ClusterConfig.NodeRole.EXECUTOR,
            ClusterConfig.NodeRole.SCHEDULER,
            ClusterConfig.NodeRole.WEBSERVER
        );

        ClusterNode node = ClusterNode.builder()
            .nodeId("multi-role-node")
            .nodeName("Multi-Role Node")
            .clusterId("test-cluster")
            .host("192.168.1.100")
            .port(8080)
            .roles(roles)
            .status(ClusterNode.NodeStatus.HEALTHY)
            .health(createHealthyHealth())
            .capabilities(createDefaultCapabilities())
            .configuration(createDefaultConfiguration())
            .version(createDefaultVersion())
            .build();

        assertEquals(3, node.getRoles().size());
        assertTrue(node.getRoles().contains(ClusterConfig.NodeRole.EXECUTOR));
        assertTrue(node.getRoles().contains(ClusterConfig.NodeRole.SCHEDULER));
        assertTrue(node.getRoles().contains(ClusterConfig.NodeRole.WEBSERVER));
        assertEquals(ClusterNode.NodeStatus.HEALTHY, node.getStatus());
    }

    @Test
    @DisplayName("Should create node health with resource usage")
    void shouldCreateNodeHealthWithResourceUsage() {
        ClusterNode.NodeHealth health = ClusterNode.NodeHealth.builder()
            .overallStatus(ClusterNode.HealthStatus.HEALTHY)
            .cpuUsage(45.5)
            .memoryUsage(67.2)
            .diskUsage(23.8)
            .networkLatency(15)
            .activeConnections(25)
            .activeExecutions(8)
            .loadAverage(1.2)
            .heapUsage(55.0)
            .healthDetails(Map.of(
                "database_connections", 10,
                "cache_hit_rate", 0.95
            ))
            .build();

        assertEquals(ClusterNode.HealthStatus.HEALTHY, health.getOverallStatus());
        assertEquals(45.5, health.getCpuUsage(), 0.01);
        assertEquals(67.2, health.getMemoryUsage(), 0.01);
        assertEquals(23.8, health.getDiskUsage(), 0.01);
        assertEquals(15, health.getNetworkLatency());
        assertEquals(25, health.getActiveConnections());
        assertEquals(8, health.getActiveExecutions());
        assertEquals(1.2, health.getLoadAverage(), 0.01);
        assertEquals(55.0, health.getHeapUsage(), 0.01);
        assertEquals(10, health.getHealthDetails().get("database_connections"));
        assertEquals(0.95, health.getHealthDetails().get("cache_hit_rate"));
    }

    @Test
    @DisplayName("Should create node capabilities with resource limits")
    void shouldCreateNodeCapabilitiesWithResourceLimits() {
        ClusterNode.NodeCapabilities capabilities = ClusterNode.NodeCapabilities.builder()
            .cpuCores(8)
            .totalMemoryMB(16384)
            .availableMemoryMB(8192)
            .totalDiskMB(102400)
            .availableDiskMB(51200)
            .maxConcurrentExecutions(20)
            .currentConcurrentExecutions(5)
            .supportedTaskTypes(Set.of("bash", "python", "sql"))
            .tags(Set.of("gpu", "high-memory", "ssd"))
            .priority(150)
            .schedulable(true)
            .build();

        assertEquals(8, capabilities.getCpuCores());
        assertEquals(16384, capabilities.getTotalMemoryMB());
        assertEquals(8192, capabilities.getAvailableMemoryMB());
        assertEquals(102400, capabilities.getTotalDiskMB());
        assertEquals(51200, capabilities.getAvailableDiskMB());
        assertEquals(20, capabilities.getMaxConcurrentExecutions());
        assertEquals(5, capabilities.getCurrentConcurrentExecutions());
        assertEquals(3, capabilities.getSupportedTaskTypes().size());
        assertTrue(capabilities.getSupportedTaskTypes().contains("python"));
        assertEquals(3, capabilities.getTags().size());
        assertTrue(capabilities.getTags().contains("gpu"));
        assertEquals(150, capabilities.getPriority());
        assertTrue(capabilities.isSchedulable());
    }

    @Test
    @DisplayName("Should create node configuration with environment settings")
    void shouldCreateNodeConfigurationWithEnvironmentSettings() {
        ClusterNode.JvmConfiguration jvm = ClusterNode.JvmConfiguration.builder()
            .version("17.0.2")
            .vendor("Eclipse Adoptium")
            .maxHeapSizeMB(4096)
            .initialHeapSizeMB(1024)
            .gcAlgorithm("G1GC")
            .jvmArgs(Set.of("-XX:+UseG1GC", "-XX:MaxGCPauseMillis=200"))
            .build();

        ClusterNode.NetworkConfiguration network = ClusterNode.NetworkConfiguration.builder()
            .networkInterface("eth0")
            .bindAddress("0.0.0.0")
            .publicAddress("203.0.113.1")
            .sslEnabled(true)
            .connectionPoolSize(50)
            .build();

        ClusterNode.StorageConfiguration storage = ClusterNode.StorageConfiguration.builder()
            .dataDirectory("/opt/dataflare/data")
            .tempDirectory("/tmp/dataflare")
            .logDirectory("/var/log/dataflare")
            .storageType(ClusterNode.StorageType.S3)
            .encryptionEnabled(true)
            .build();

        ClusterNode.SecurityConfiguration security = ClusterNode.SecurityConfiguration.builder()
            .authenticationEnabled(true)
            .authorizationEnabled(true)
            .auditLoggingEnabled(true)
            .allowedClientCertificates(Set.of("client1.crt", "client2.crt"))
            .securityPolicies(Set.of("strict-tls", "rbac-enabled"))
            .build();

        ClusterNode.NodeConfiguration config = ClusterNode.NodeConfiguration.builder()
            .environment("production")
            .region("us-west-2")
            .availabilityZone("us-west-2a")
            .dataCenter("dc1")
            .jvm(jvm)
            .network(network)
            .storage(storage)
            .security(security)
            .build();

        assertEquals("production", config.getEnvironment());
        assertEquals("us-west-2", config.getRegion());
        assertEquals("us-west-2a", config.getAvailabilityZone());
        assertEquals("dc1", config.getDataCenter());
        
        // Verify JVM config
        assertEquals("17.0.2", config.getJvm().getVersion());
        assertEquals("Eclipse Adoptium", config.getJvm().getVendor());
        assertEquals(4096, config.getJvm().getMaxHeapSizeMB());
        
        // Verify network config
        assertEquals("eth0", config.getNetwork().getNetworkInterface());
        assertTrue(config.getNetwork().isSslEnabled());
        
        // Verify storage config
        assertEquals(ClusterNode.StorageType.S3, config.getStorage().getStorageType());
        assertTrue(config.getStorage().isEncryptionEnabled());
        
        // Verify security config
        assertTrue(config.getSecurity().isAuthenticationEnabled());
        assertEquals(2, config.getSecurity().getAllowedClientCertificates().size());
    }

    @Test
    @DisplayName("Should create node version information")
    void shouldCreateNodeVersionInformation() {
        ClusterNode.NodeVersion version = ClusterNode.NodeVersion.builder()
            .dataFlareVersion("1.2.3")
            .buildTimestamp("2024-01-15T10:30:00Z")
            .gitCommit("abc123def456")
            .buildNumber("456")
            .operatingSystem("Linux")
            .architecture("x86_64")
            .build();

        assertEquals("1.2.3", version.getDataFlareVersion());
        assertEquals("2024-01-15T10:30:00Z", version.getBuildTimestamp());
        assertEquals("abc123def456", version.getGitCommit());
        assertEquals("456", version.getBuildNumber());
        assertEquals("Linux", version.getOperatingSystem());
        assertEquals("x86_64", version.getArchitecture());
    }

    @Test
    @DisplayName("Should check if node is healthy")
    void shouldCheckIfNodeIsHealthy() {
        ClusterNode healthyNode = createTestNode()
            .toBuilder()
            .status(ClusterNode.NodeStatus.HEALTHY)
            .health(createHealthyHealth())
            .build();

        ClusterNode unhealthyNode = createTestNode()
            .toBuilder()
            .status(ClusterNode.NodeStatus.UNHEALTHY)
            .health(createUnhealthyHealth())
            .build();

        assertTrue(healthyNode.isHealthy());
        assertFalse(unhealthyNode.isHealthy());
    }

    @Test
    @DisplayName("Should check if node is available for scheduling")
    void shouldCheckIfNodeIsAvailableForScheduling() {
        ClusterNode availableNode = createTestNode()
            .toBuilder()
            .status(ClusterNode.NodeStatus.HEALTHY)
            .health(createHealthyHealth())
            .capabilities(createDefaultCapabilities().toBuilder()
                .schedulable(true)
                .build())
            .build();

        ClusterNode drainingNode = createTestNode()
            .toBuilder()
            .status(ClusterNode.NodeStatus.DRAINING)
            .health(createHealthyHealth())
            .build();

        ClusterNode unschedulableNode = createTestNode()
            .toBuilder()
            .status(ClusterNode.NodeStatus.HEALTHY)
            .health(createHealthyHealth())
            .capabilities(createDefaultCapabilities().toBuilder()
                .schedulable(false)
                .build())
            .build();

        assertTrue(availableNode.isAvailable());
        assertFalse(drainingNode.isAvailable());
        assertFalse(unschedulableNode.isAvailable());
    }

    @Test
    @DisplayName("Should check if node has capacity")
    void shouldCheckIfNodeHasCapacity() {
        ClusterNode nodeWithCapacity = createTestNode()
            .toBuilder()
            .capabilities(createDefaultCapabilities().toBuilder()
                .maxConcurrentExecutions(10)
                .currentConcurrentExecutions(5)
                .build())
            .build();

        ClusterNode nodeAtCapacity = createTestNode()
            .toBuilder()
            .capabilities(createDefaultCapabilities().toBuilder()
                .maxConcurrentExecutions(10)
                .currentConcurrentExecutions(10)
                .build())
            .build();

        assertTrue(nodeWithCapacity.hasCapacity());
        assertFalse(nodeAtCapacity.hasCapacity());
    }

    @Test
    @DisplayName("Should calculate node load percentage")
    void shouldCalculateNodeLoadPercentage() {
        ClusterNode node = createTestNode()
            .toBuilder()
            .capabilities(createDefaultCapabilities().toBuilder()
                .maxConcurrentExecutions(20)
                .currentConcurrentExecutions(15)
                .build())
            .build();

        assertEquals(75.0, node.getLoadPercentage(), 0.01);
    }

    @Test
    @DisplayName("Should calculate node score for load balancing")
    void shouldCalculateNodeScoreForLoadBalancing() {
        ClusterNode highScoreNode = createTestNode()
            .toBuilder()
            .status(ClusterNode.NodeStatus.HEALTHY)
            .health(ClusterNode.NodeHealth.builder()
                .overallStatus(ClusterNode.HealthStatus.HEALTHY)
                .cpuUsage(20.0)
                .memoryUsage(30.0)
                .diskUsage(25.0)
                .build())
            .capabilities(createDefaultCapabilities().toBuilder()
                .maxConcurrentExecutions(10)
                .currentConcurrentExecutions(2)
                .priority(150)
                .schedulable(true)
                .build())
            .build();

        ClusterNode lowScoreNode = createTestNode()
            .toBuilder()
            .status(ClusterNode.NodeStatus.HEALTHY)
            .health(ClusterNode.NodeHealth.builder()
                .overallStatus(ClusterNode.HealthStatus.HEALTHY)
                .cpuUsage(90.0)
                .memoryUsage(85.0)
                .diskUsage(80.0)
                .build())
            .capabilities(createDefaultCapabilities().toBuilder()
                .maxConcurrentExecutions(10)
                .currentConcurrentExecutions(9)
                .priority(50)
                .schedulable(true)
                .build())
            .build();

        ClusterNode unavailableNode = createTestNode()
            .toBuilder()
            .status(ClusterNode.NodeStatus.OFFLINE)
            .build();

        double highScore = highScoreNode.calculateScore();
        double lowScore = lowScoreNode.calculateScore();
        double unavailableScore = unavailableNode.calculateScore();

        assertTrue(highScore > lowScore);
        assertTrue(lowScore > unavailableScore);
        assertEquals(0.0, unavailableScore, 0.01);
        assertTrue(highScore > 0.5); // Should be a good score
        assertTrue(lowScore < 0.5); // Should be a poor score
    }

    @Test
    @DisplayName("Should validate node builder pattern")
    void shouldValidateNodeBuilderPattern() {
        ClusterNode original = createTestNode();

        ClusterNode modified = original.toBuilder()
            .status(ClusterNode.NodeStatus.HEALTHY)
            .metadata(Map.of("updated", "true"))
            .build();

        // Original should be unchanged
        assertEquals(ClusterNode.NodeStatus.STARTING, original.getStatus());
        assertTrue(original.getMetadata().isEmpty());

        // Modified should have new values
        assertEquals(ClusterNode.NodeStatus.HEALTHY, modified.getStatus());
        assertEquals("true", modified.getMetadata().get("updated"));
        
        // Common fields should be the same
        assertEquals(original.getNodeId(), modified.getNodeId());
        assertEquals(original.getNodeName(), modified.getNodeName());
        assertEquals(original.getClusterId(), modified.getClusterId());
    }

    // Helper methods for creating test objects

    private ClusterNode createTestNode() {
        return ClusterNode.builder()
            .nodeId("test-node")
            .nodeName("Test Node")
            .clusterId("test-cluster")
            .host("localhost")
            .port(8080)
            .roles(Set.of(ClusterConfig.NodeRole.EXECUTOR))
            .health(createDefaultHealth())
            .capabilities(createDefaultCapabilities())
            .configuration(createDefaultConfiguration())
            .version(createDefaultVersion())
            .build();
    }

    private ClusterNode.NodeHealth createDefaultHealth() {
        return ClusterNode.NodeHealth.builder()
            .overallStatus(ClusterNode.HealthStatus.UNKNOWN)
            .build();
    }

    private ClusterNode.NodeHealth createHealthyHealth() {
        return ClusterNode.NodeHealth.builder()
            .overallStatus(ClusterNode.HealthStatus.HEALTHY)
            .cpuUsage(25.0)
            .memoryUsage(40.0)
            .diskUsage(30.0)
            .build();
    }

    private ClusterNode.NodeHealth createUnhealthyHealth() {
        return ClusterNode.NodeHealth.builder()
            .overallStatus(ClusterNode.HealthStatus.UNHEALTHY)
            .cpuUsage(95.0)
            .memoryUsage(90.0)
            .diskUsage(85.0)
            .build();
    }

    private ClusterNode.NodeCapabilities createDefaultCapabilities() {
        return ClusterNode.NodeCapabilities.builder()
            .cpuCores(4)
            .totalMemoryMB(8192)
            .availableMemoryMB(4096)
            .totalDiskMB(51200)
            .availableDiskMB(25600)
            .maxConcurrentExecutions(10)
            .currentConcurrentExecutions(0)
            .schedulable(true)
            .build();
    }

    private ClusterNode.NodeConfiguration createDefaultConfiguration() {
        return ClusterNode.NodeConfiguration.builder()
            .environment("test")
            .jvm(ClusterNode.JvmConfiguration.builder()
                .version("17")
                .vendor("OpenJDK")
                .build())
            .network(ClusterNode.NetworkConfiguration.builder()
                .bindAddress("0.0.0.0")
                .build())
            .storage(ClusterNode.StorageConfiguration.builder()
                .dataDirectory("/tmp/data")
                .tempDirectory("/tmp")
                .logDirectory("/tmp/logs")
                .build())
            .security(ClusterNode.SecurityConfiguration.builder()
                .build())
            .build();
    }

    private ClusterNode.NodeVersion createDefaultVersion() {
        return ClusterNode.NodeVersion.builder()
            .dataFlareVersion("1.0.0")
            .buildTimestamp("2024-01-01T00:00:00Z")
            .build();
    }
}
