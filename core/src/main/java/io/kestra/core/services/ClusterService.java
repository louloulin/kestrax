package io.kestra.core.services;

import io.kestra.core.models.ha.ClusterConfig;
import io.kestra.core.models.ha.ClusterNode;
import io.micronaut.context.annotation.Requires;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Service for managing DataFlare cluster operations
 * Handles node registration, health monitoring, and load balancing
 */
@Singleton
@Slf4j
@Requires(property = "dataflare.cluster.enabled", value = "true")
public class ClusterService {

    private final Map<String, ClusterNode> nodes = new ConcurrentHashMap<>();
    private final Map<String, ClusterConfig> clusters = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    
    private ClusterConfig currentClusterConfig;
    private String currentNodeId;

    /**
     * Initialize cluster service
     */
    public void initialize(ClusterConfig clusterConfig, String nodeId) {
        this.currentClusterConfig = clusterConfig;
        this.currentNodeId = nodeId;
        
        clusters.put(clusterConfig.getClusterId(), clusterConfig);
        
        // Start health monitoring
        startHealthMonitoring();
        
        // Start node discovery
        startNodeDiscovery();
        
        log.info("Cluster service initialized for cluster: {}, node: {}", 
                clusterConfig.getClusterId(), nodeId);
    }

    /**
     * Register a node in the cluster
     */
    public void registerNode(ClusterNode node) {
        if (node == null || node.getNodeId() == null) {
            throw new IllegalArgumentException("Node and node ID cannot be null");
        }

        ClusterNode updatedNode = node.toBuilder()
            .lastHeartbeat(Instant.now())
            .updatedAt(Instant.now())
            .build();

        nodes.put(node.getNodeId(), updatedNode);
        
        log.info("Node registered: {} in cluster: {}", 
                node.getNodeId(), node.getClusterId());
    }

    /**
     * Unregister a node from the cluster
     */
    public void unregisterNode(String nodeId) {
        ClusterNode removedNode = nodes.remove(nodeId);
        if (removedNode != null) {
            log.info("Node unregistered: {} from cluster: {}", 
                    nodeId, removedNode.getClusterId());
        }
    }

    /**
     * Update node heartbeat
     */
    public void updateHeartbeat(String nodeId) {
        ClusterNode node = nodes.get(nodeId);
        if (node != null) {
            ClusterNode updatedNode = node.toBuilder()
                .lastHeartbeat(Instant.now())
                .status(ClusterNode.NodeStatus.HEALTHY)
                .build();
            
            nodes.put(nodeId, updatedNode);
        }
    }

    /**
     * Update node health information
     */
    public void updateNodeHealth(String nodeId, ClusterNode.NodeHealth health) {
        ClusterNode node = nodes.get(nodeId);
        if (node != null) {
            ClusterNode updatedNode = node.toBuilder()
                .health(health)
                .lastHeartbeat(Instant.now())
                .updatedAt(Instant.now())
                .build();
            
            nodes.put(nodeId, updatedNode);
        }
    }

    /**
     * Get all nodes in the cluster
     */
    public List<ClusterNode> getAllNodes() {
        return new ArrayList<>(nodes.values());
    }

    /**
     * Get healthy nodes in the cluster
     */
    public List<ClusterNode> getHealthyNodes() {
        return nodes.values().stream()
            .filter(ClusterNode::isHealthy)
            .collect(Collectors.toList());
    }

    /**
     * Get available nodes for scheduling
     */
    public List<ClusterNode> getAvailableNodes() {
        return nodes.values().stream()
            .filter(ClusterNode::isAvailable)
            .collect(Collectors.toList());
    }

    /**
     * Get nodes by role
     */
    public List<ClusterNode> getNodesByRole(ClusterConfig.NodeRole role) {
        return nodes.values().stream()
            .filter(node -> node.getRoles().contains(role))
            .filter(ClusterNode::isHealthy)
            .collect(Collectors.toList());
    }

    /**
     * Select best node for task execution using load balancing
     */
    public Optional<ClusterNode> selectBestNode(ClusterConfig.NodeRole role) {
        List<ClusterNode> candidateNodes = getNodesByRole(role).stream()
            .filter(ClusterNode::isAvailable)
            .filter(ClusterNode::hasCapacity)
            .collect(Collectors.toList());

        if (candidateNodes.isEmpty()) {
            return Optional.empty();
        }

        // Use load balancing algorithm from cluster config
        ClusterConfig.LoadBalancingAlgorithm algorithm = 
            currentClusterConfig.getLoadBalancer() != null ? 
            currentClusterConfig.getLoadBalancer().getAlgorithm() : 
            ClusterConfig.LoadBalancingAlgorithm.ROUND_ROBIN;

        return selectNodeByAlgorithm(candidateNodes, algorithm);
    }

    /**
     * Select node using specified algorithm
     */
    private Optional<ClusterNode> selectNodeByAlgorithm(
            List<ClusterNode> nodes, 
            ClusterConfig.LoadBalancingAlgorithm algorithm) {
        
        switch (algorithm) {
            case ROUND_ROBIN:
                return selectRoundRobin(nodes);
            case LEAST_CONNECTIONS:
                return selectLeastConnections(nodes);
            case WEIGHTED_ROUND_ROBIN:
                return selectWeightedRoundRobin(nodes);
            case RANDOM:
                return selectRandom(nodes);
            default:
                return selectRoundRobin(nodes);
        }
    }

    /**
     * Round robin selection
     */
    private Optional<ClusterNode> selectRoundRobin(List<ClusterNode> nodes) {
        if (nodes.isEmpty()) return Optional.empty();
        
        // Simple round robin based on current time
        int index = (int) (System.currentTimeMillis() % nodes.size());
        return Optional.of(nodes.get(index));
    }

    /**
     * Least connections selection
     */
    private Optional<ClusterNode> selectLeastConnections(List<ClusterNode> nodes) {
        return nodes.stream()
            .min(Comparator.comparingInt(node -> 
                node.getCapabilities().getCurrentConcurrentExecutions()));
    }

    /**
     * Weighted round robin selection based on node score
     */
    private Optional<ClusterNode> selectWeightedRoundRobin(List<ClusterNode> nodes) {
        return nodes.stream()
            .max(Comparator.comparingDouble(ClusterNode::calculateScore));
    }

    /**
     * Random selection
     */
    private Optional<ClusterNode> selectRandom(List<ClusterNode> nodes) {
        if (nodes.isEmpty()) return Optional.empty();
        
        Random random = new Random();
        return Optional.of(nodes.get(random.nextInt(nodes.size())));
    }

    /**
     * Get cluster statistics
     */
    public ClusterStatistics getClusterStatistics() {
        List<ClusterNode> allNodes = getAllNodes();
        List<ClusterNode> healthyNodes = getHealthyNodes();
        
        return ClusterStatistics.builder()
            .totalNodes(allNodes.size())
            .healthyNodes(healthyNodes.size())
            .unhealthyNodes(allNodes.size() - healthyNodes.size())
            .totalExecutions(allNodes.stream()
                .mapToInt(node -> node.getCapabilities().getCurrentConcurrentExecutions())
                .sum())
            .totalCapacity(allNodes.stream()
                .mapToInt(node -> node.getCapabilities().getMaxConcurrentExecutions())
                .sum())
            .averageLoad(healthyNodes.stream()
                .mapToDouble(ClusterNode::getLoadPercentage)
                .average()
                .orElse(0.0))
            .build();
    }

    /**
     * Start health monitoring
     */
    private void startHealthMonitoring() {
        Duration interval = currentClusterConfig.getHealthCheck().getInterval();
        
        scheduler.scheduleAtFixedRate(
            this::performHealthCheck,
            0,
            interval.toSeconds(),
            TimeUnit.SECONDS
        );
    }

    /**
     * Start node discovery
     */
    private void startNodeDiscovery() {
        if (currentClusterConfig.getNodeConfig().getDiscovery() != null) {
            Duration interval = currentClusterConfig.getNodeConfig()
                .getDiscovery().getDiscoveryInterval();
            
            scheduler.scheduleAtFixedRate(
                this::performNodeDiscovery,
                0,
                interval.toSeconds(),
                TimeUnit.SECONDS
            );
        }
    }

    /**
     * Perform health check on all nodes
     */
    private void performHealthCheck() {
        Duration timeout = currentClusterConfig.getHealthCheck().getTimeout();
        Instant cutoff = Instant.now().minus(timeout.multipliedBy(2));
        
        nodes.entrySet().removeIf(entry -> {
            ClusterNode node = entry.getValue();
            if (node.getLastHeartbeat().isBefore(cutoff)) {
                log.warn("Node {} marked as failed due to missed heartbeats", 
                        node.getNodeId());
                return true;
            }
            return false;
        });
    }

    /**
     * Perform node discovery
     */
    private void performNodeDiscovery() {
        // Implementation depends on discovery method
        // For now, this is a placeholder
        log.debug("Performing node discovery for cluster: {}", 
                currentClusterConfig.getClusterId());
    }

    /**
     * Shutdown cluster service
     */
    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(30, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        log.info("Cluster service shutdown completed");
    }

    /**
     * Cluster statistics data class
     */
    public static class ClusterStatistics {
        private final int totalNodes;
        private final int healthyNodes;
        private final int unhealthyNodes;
        private final int totalExecutions;
        private final int totalCapacity;
        private final double averageLoad;

        private ClusterStatistics(Builder builder) {
            this.totalNodes = builder.totalNodes;
            this.healthyNodes = builder.healthyNodes;
            this.unhealthyNodes = builder.unhealthyNodes;
            this.totalExecutions = builder.totalExecutions;
            this.totalCapacity = builder.totalCapacity;
            this.averageLoad = builder.averageLoad;
        }

        public static Builder builder() {
            return new Builder();
        }

        // Getters
        public int getTotalNodes() { return totalNodes; }
        public int getHealthyNodes() { return healthyNodes; }
        public int getUnhealthyNodes() { return unhealthyNodes; }
        public int getTotalExecutions() { return totalExecutions; }
        public int getTotalCapacity() { return totalCapacity; }
        public double getAverageLoad() { return averageLoad; }

        public static class Builder {
            private int totalNodes;
            private int healthyNodes;
            private int unhealthyNodes;
            private int totalExecutions;
            private int totalCapacity;
            private double averageLoad;

            public Builder totalNodes(int totalNodes) {
                this.totalNodes = totalNodes;
                return this;
            }

            public Builder healthyNodes(int healthyNodes) {
                this.healthyNodes = healthyNodes;
                return this;
            }

            public Builder unhealthyNodes(int unhealthyNodes) {
                this.unhealthyNodes = unhealthyNodes;
                return this;
            }

            public Builder totalExecutions(int totalExecutions) {
                this.totalExecutions = totalExecutions;
                return this;
            }

            public Builder totalCapacity(int totalCapacity) {
                this.totalCapacity = totalCapacity;
                return this;
            }

            public Builder averageLoad(double averageLoad) {
                this.averageLoad = averageLoad;
                return this;
            }

            public ClusterStatistics build() {
                return new ClusterStatistics(this);
            }
        }
    }
}
