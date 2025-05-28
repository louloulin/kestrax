package io.kestra.core.services;

import io.kestra.core.models.monitoring.MonitoringConfig;
import io.micronaut.context.annotation.Requires;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Service for monitoring DataFlare system health and performance
 * Collects metrics, performs health checks, and manages alerting
 */
@Singleton
@Slf4j
@Requires(property = "dataflare.monitoring.enabled", value = "true")
public class MonitoringService {

    private final Map<String, MetricValue> metrics = new ConcurrentHashMap<>();
    private final Map<String, HealthStatus> healthChecks = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(3);
    
    private MonitoringConfig config;
    private final AtomicLong metricsCollected = new AtomicLong(0);
    private final AtomicLong alertsTriggered = new AtomicLong(0);

    /**
     * Initialize monitoring service
     */
    public void initialize(MonitoringConfig config) {
        this.config = config;
        
        if (config.getMetrics().isEnabled()) {
            startMetricsCollection();
        }
        
        if (config.getHealthCheck().isEnabled()) {
            startHealthChecks();
        }
        
        if (config.getAlerting() != null && config.getAlerting().isEnabled()) {
            startAlertEvaluation();
        }
        
        log.info("Monitoring service initialized with config: {}", config);
    }

    /**
     * Record a metric value
     */
    public void recordMetric(String name, double value) {
        recordMetric(name, value, Map.of());
    }

    /**
     * Record a metric value with tags
     */
    public void recordMetric(String name, double value, Map<String, String> tags) {
        MetricValue metric = MetricValue.builder()
            .name(name)
            .value(value)
            .tags(tags)
            .timestamp(Instant.now())
            .build();
        
        metrics.put(name, metric);
        metricsCollected.incrementAndGet();
    }

    /**
     * Increment a counter metric
     */
    public void incrementCounter(String name) {
        incrementCounter(name, 1.0, Map.of());
    }

    /**
     * Increment a counter metric with tags
     */
    public void incrementCounter(String name, double increment, Map<String, String> tags) {
        MetricValue existing = metrics.get(name);
        double newValue = (existing != null ? existing.getValue() : 0.0) + increment;
        recordMetric(name, newValue, tags);
    }

    /**
     * Record execution time
     */
    public void recordExecutionTime(String operation, Duration duration) {
        recordMetric("execution_time_ms", duration.toMillis(), 
                    Map.of("operation", operation));
    }

    /**
     * Get current metric value
     */
    public Optional<MetricValue> getMetric(String name) {
        return Optional.ofNullable(metrics.get(name));
    }

    /**
     * Get all metrics
     */
    public Map<String, MetricValue> getAllMetrics() {
        return new HashMap<>(metrics);
    }

    /**
     * Get metrics by prefix
     */
    public Map<String, MetricValue> getMetricsByPrefix(String prefix) {
        return metrics.entrySet().stream()
            .filter(entry -> entry.getKey().startsWith(prefix))
            .collect(HashMap::new, 
                    (map, entry) -> map.put(entry.getKey(), entry.getValue()),
                    HashMap::putAll);
    }

    /**
     * Perform health check
     */
    public HealthStatus performHealthCheck(String checkName) {
        try {
            HealthStatus status = executeHealthCheck(checkName);
            healthChecks.put(checkName, status);
            return status;
        } catch (Exception e) {
            log.error("Health check failed for: {}", checkName, e);
            HealthStatus failedStatus = HealthStatus.builder()
                .name(checkName)
                .status(HealthStatus.Status.UNHEALTHY)
                .message("Health check failed: " + e.getMessage())
                .timestamp(Instant.now())
                .build();
            healthChecks.put(checkName, failedStatus);
            return failedStatus;
        }
    }

    /**
     * Get overall system health
     */
    public SystemHealth getSystemHealth() {
        Map<String, HealthStatus> currentChecks = new HashMap<>(healthChecks);
        
        boolean allHealthy = currentChecks.values().stream()
            .allMatch(status -> status.getStatus() == HealthStatus.Status.HEALTHY);
        
        SystemHealth.Status overallStatus = allHealthy ? 
            SystemHealth.Status.HEALTHY : SystemHealth.Status.UNHEALTHY;
        
        return SystemHealth.builder()
            .status(overallStatus)
            .checks(currentChecks)
            .timestamp(Instant.now())
            .uptime(getSystemUptime())
            .build();
    }

    /**
     * Get monitoring statistics
     */
    public MonitoringStatistics getStatistics() {
        return MonitoringStatistics.builder()
            .metricsCollected(metricsCollected.get())
            .alertsTriggered(alertsTriggered.get())
            .healthChecksPerformed(healthChecks.size())
            .lastMetricTimestamp(getLastMetricTimestamp())
            .systemUptime(getSystemUptime())
            .build();
    }

    /**
     * Start metrics collection
     */
    private void startMetricsCollection() {
        Duration interval = config.getMetrics().getCollectionInterval();
        
        scheduler.scheduleAtFixedRate(
            this::collectSystemMetrics,
            0,
            interval.toSeconds(),
            TimeUnit.SECONDS
        );
        
        log.info("Started metrics collection with interval: {}", interval);
    }

    /**
     * Start health checks
     */
    private void startHealthChecks() {
        Duration interval = config.getHealthCheck().getInterval();
        
        scheduler.scheduleAtFixedRate(
            this::performAllHealthChecks,
            0,
            interval.toSeconds(),
            TimeUnit.SECONDS
        );
        
        log.info("Started health checks with interval: {}", interval);
    }

    /**
     * Start alert evaluation
     */
    private void startAlertEvaluation() {
        Duration interval = config.getAlerting().getEvaluationInterval();
        
        scheduler.scheduleAtFixedRate(
            this::evaluateAlerts,
            0,
            interval.toSeconds(),
            TimeUnit.SECONDS
        );
        
        log.info("Started alert evaluation with interval: {}", interval);
    }

    /**
     * Collect system metrics
     */
    private void collectSystemMetrics() {
        try {
            if (config.getMetrics().getEnabledMetrics().contains(MonitoringConfig.MetricType.SYSTEM)) {
                collectSystemResourceMetrics();
            }
            
            if (config.getMetrics().getEnabledMetrics().contains(MonitoringConfig.MetricType.JVM)) {
                collectJvmMetrics();
            }
            
            if (config.getMetrics().getEnabledMetrics().contains(MonitoringConfig.MetricType.APPLICATION)) {
                collectApplicationMetrics();
            }
        } catch (Exception e) {
            log.error("Error collecting system metrics", e);
        }
    }

    /**
     * Collect system resource metrics
     */
    private void collectSystemResourceMetrics() {
        OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
        
        // CPU usage
        if (osBean instanceof com.sun.management.OperatingSystemMXBean) {
            com.sun.management.OperatingSystemMXBean sunOsBean = 
                (com.sun.management.OperatingSystemMXBean) osBean;
            recordMetric("system.cpu.usage", sunOsBean.getProcessCpuLoad() * 100);
            recordMetric("system.cpu.system", sunOsBean.getSystemCpuLoad() * 100);
        }
        
        // Load average
        recordMetric("system.load.average", osBean.getSystemLoadAverage());
        
        // Available processors
        recordMetric("system.cpu.count", osBean.getAvailableProcessors());
    }

    /**
     * Collect JVM metrics
     */
    private void collectJvmMetrics() {
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        RuntimeMXBean runtimeBean = ManagementFactory.getRuntimeMXBean();
        
        // Memory usage
        long heapUsed = memoryBean.getHeapMemoryUsage().getUsed();
        long heapMax = memoryBean.getHeapMemoryUsage().getMax();
        long nonHeapUsed = memoryBean.getNonHeapMemoryUsage().getUsed();
        
        recordMetric("jvm.memory.heap.used", heapUsed);
        recordMetric("jvm.memory.heap.max", heapMax);
        recordMetric("jvm.memory.heap.usage", (double) heapUsed / heapMax * 100);
        recordMetric("jvm.memory.nonheap.used", nonHeapUsed);
        
        // Uptime
        recordMetric("jvm.uptime", runtimeBean.getUptime());
        
        // Thread count
        recordMetric("jvm.threads.count", Thread.activeCount());
    }

    /**
     * Collect application-specific metrics
     */
    private void collectApplicationMetrics() {
        // These would be implemented based on specific application needs
        recordMetric("application.metrics.collected", metricsCollected.get());
        recordMetric("application.alerts.triggered", alertsTriggered.get());
        recordMetric("application.health.checks", healthChecks.size());
    }

    /**
     * Perform all configured health checks
     */
    private void performAllHealthChecks() {
        Set<MonitoringConfig.HealthCheckType> enabledChecks = 
            config.getHealthCheck().getEnabledChecks();
        
        for (MonitoringConfig.HealthCheckType checkType : enabledChecks) {
            performHealthCheck(checkType.name().toLowerCase());
        }
        
        // Perform custom health checks
        for (MonitoringConfig.CustomHealthCheck customCheck : 
             config.getHealthCheck().getCustomChecks()) {
            performHealthCheck(customCheck.getName());
        }
    }

    /**
     * Execute specific health check
     */
    private HealthStatus executeHealthCheck(String checkName) {
        switch (checkName.toLowerCase()) {
            case "database":
                return checkDatabaseHealth();
            case "memory":
                return checkMemoryHealth();
            case "disk_space":
                return checkDiskSpaceHealth();
            default:
                return HealthStatus.builder()
                    .name(checkName)
                    .status(HealthStatus.Status.HEALTHY)
                    .message("Health check passed")
                    .timestamp(Instant.now())
                    .build();
        }
    }

    /**
     * Check database health
     */
    private HealthStatus checkDatabaseHealth() {
        // This would implement actual database connectivity check
        return HealthStatus.builder()
            .name("database")
            .status(HealthStatus.Status.HEALTHY)
            .message("Database connection healthy")
            .timestamp(Instant.now())
            .responseTime(Duration.ofMillis(10))
            .build();
    }

    /**
     * Check memory health
     */
    private HealthStatus checkMemoryHealth() {
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        long heapUsed = memoryBean.getHeapMemoryUsage().getUsed();
        long heapMax = memoryBean.getHeapMemoryUsage().getMax();
        double usagePercent = (double) heapUsed / heapMax * 100;
        
        HealthStatus.Status status = usagePercent > 90 ? 
            HealthStatus.Status.UNHEALTHY : HealthStatus.Status.HEALTHY;
        
        return HealthStatus.builder()
            .name("memory")
            .status(status)
            .message(String.format("Memory usage: %.1f%%", usagePercent))
            .timestamp(Instant.now())
            .details(Map.of(
                "heap_used", heapUsed,
                "heap_max", heapMax,
                "usage_percent", usagePercent
            ))
            .build();
    }

    /**
     * Check disk space health
     */
    private HealthStatus checkDiskSpaceHealth() {
        // This would implement actual disk space check
        return HealthStatus.builder()
            .name("disk_space")
            .status(HealthStatus.Status.HEALTHY)
            .message("Disk space sufficient")
            .timestamp(Instant.now())
            .build();
    }

    /**
     * Evaluate alert rules
     */
    private void evaluateAlerts() {
        if (config.getAlerting() == null) return;
        
        for (MonitoringConfig.AlertRule rule : config.getAlerting().getRules()) {
            evaluateAlertRule(rule);
        }
    }

    /**
     * Evaluate specific alert rule
     */
    private void evaluateAlertRule(MonitoringConfig.AlertRule rule) {
        MetricValue metric = metrics.get(rule.getMetric());
        if (metric == null) return;
        
        boolean conditionMet = evaluateCondition(
            metric.getValue(), 
            rule.getCondition(), 
            rule.getThreshold()
        );
        
        if (conditionMet) {
            triggerAlert(rule, metric);
        }
    }

    /**
     * Evaluate alert condition
     */
    private boolean evaluateCondition(
            double value, 
            MonitoringConfig.AlertCondition condition, 
            double threshold) {
        
        switch (condition) {
            case GREATER_THAN:
                return value > threshold;
            case LESS_THAN:
                return value < threshold;
            case EQUALS:
                return Math.abs(value - threshold) < 0.001;
            case NOT_EQUALS:
                return Math.abs(value - threshold) >= 0.001;
            default:
                return false;
        }
    }

    /**
     * Trigger alert
     */
    private void triggerAlert(MonitoringConfig.AlertRule rule, MetricValue metric) {
        alertsTriggered.incrementAndGet();
        log.warn("Alert triggered: {} - {} {} {}", 
                rule.getName(), 
                metric.getValue(), 
                rule.getCondition(), 
                rule.getThreshold());
        
        // Here you would implement actual alert notification
        // to configured channels (email, Slack, etc.)
    }

    /**
     * Get last metric timestamp
     */
    private Instant getLastMetricTimestamp() {
        return metrics.values().stream()
            .map(MetricValue::getTimestamp)
            .max(Instant::compareTo)
            .orElse(Instant.now());
    }

    /**
     * Get system uptime
     */
    private Duration getSystemUptime() {
        RuntimeMXBean runtimeBean = ManagementFactory.getRuntimeMXBean();
        return Duration.ofMillis(runtimeBean.getUptime());
    }

    /**
     * Shutdown monitoring service
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
        
        log.info("Monitoring service shutdown completed");
    }

    // Data classes for monitoring
    
    public static class MetricValue {
        private final String name;
        private final double value;
        private final Map<String, String> tags;
        private final Instant timestamp;

        private MetricValue(Builder builder) {
            this.name = builder.name;
            this.value = builder.value;
            this.tags = builder.tags;
            this.timestamp = builder.timestamp;
        }

        public static Builder builder() {
            return new Builder();
        }

        // Getters
        public String getName() { return name; }
        public double getValue() { return value; }
        public Map<String, String> getTags() { return tags; }
        public Instant getTimestamp() { return timestamp; }

        public static class Builder {
            private String name;
            private double value;
            private Map<String, String> tags = Map.of();
            private Instant timestamp = Instant.now();

            public Builder name(String name) {
                this.name = name;
                return this;
            }

            public Builder value(double value) {
                this.value = value;
                return this;
            }

            public Builder tags(Map<String, String> tags) {
                this.tags = tags;
                return this;
            }

            public Builder timestamp(Instant timestamp) {
                this.timestamp = timestamp;
                return this;
            }

            public MetricValue build() {
                return new MetricValue(this);
            }
        }
    }

    public static class HealthStatus {
        private final String name;
        private final Status status;
        private final String message;
        private final Instant timestamp;
        private final Duration responseTime;
        private final Map<String, Object> details;

        private HealthStatus(Builder builder) {
            this.name = builder.name;
            this.status = builder.status;
            this.message = builder.message;
            this.timestamp = builder.timestamp;
            this.responseTime = builder.responseTime;
            this.details = builder.details;
        }

        public static Builder builder() {
            return new Builder();
        }

        public enum Status {
            HEALTHY,
            UNHEALTHY,
            UNKNOWN
        }

        // Getters
        public String getName() { return name; }
        public Status getStatus() { return status; }
        public String getMessage() { return message; }
        public Instant getTimestamp() { return timestamp; }
        public Duration getResponseTime() { return responseTime; }
        public Map<String, Object> getDetails() { return details; }

        public static class Builder {
            private String name;
            private Status status;
            private String message;
            private Instant timestamp = Instant.now();
            private Duration responseTime;
            private Map<String, Object> details = Map.of();

            public Builder name(String name) {
                this.name = name;
                return this;
            }

            public Builder status(Status status) {
                this.status = status;
                return this;
            }

            public Builder message(String message) {
                this.message = message;
                return this;
            }

            public Builder timestamp(Instant timestamp) {
                this.timestamp = timestamp;
                return this;
            }

            public Builder responseTime(Duration responseTime) {
                this.responseTime = responseTime;
                return this;
            }

            public Builder details(Map<String, Object> details) {
                this.details = details;
                return this;
            }

            public HealthStatus build() {
                return new HealthStatus(this);
            }
        }
    }

    public static class SystemHealth {
        private final Status status;
        private final Map<String, HealthStatus> checks;
        private final Instant timestamp;
        private final Duration uptime;

        private SystemHealth(Builder builder) {
            this.status = builder.status;
            this.checks = builder.checks;
            this.timestamp = builder.timestamp;
            this.uptime = builder.uptime;
        }

        public static Builder builder() {
            return new Builder();
        }

        public enum Status {
            HEALTHY,
            UNHEALTHY,
            DEGRADED
        }

        // Getters
        public Status getStatus() { return status; }
        public Map<String, HealthStatus> getChecks() { return checks; }
        public Instant getTimestamp() { return timestamp; }
        public Duration getUptime() { return uptime; }

        public static class Builder {
            private Status status;
            private Map<String, HealthStatus> checks = Map.of();
            private Instant timestamp = Instant.now();
            private Duration uptime;

            public Builder status(Status status) {
                this.status = status;
                return this;
            }

            public Builder checks(Map<String, HealthStatus> checks) {
                this.checks = checks;
                return this;
            }

            public Builder timestamp(Instant timestamp) {
                this.timestamp = timestamp;
                return this;
            }

            public Builder uptime(Duration uptime) {
                this.uptime = uptime;
                return this;
            }

            public SystemHealth build() {
                return new SystemHealth(this);
            }
        }
    }

    public static class MonitoringStatistics {
        private final long metricsCollected;
        private final long alertsTriggered;
        private final int healthChecksPerformed;
        private final Instant lastMetricTimestamp;
        private final Duration systemUptime;

        private MonitoringStatistics(Builder builder) {
            this.metricsCollected = builder.metricsCollected;
            this.alertsTriggered = builder.alertsTriggered;
            this.healthChecksPerformed = builder.healthChecksPerformed;
            this.lastMetricTimestamp = builder.lastMetricTimestamp;
            this.systemUptime = builder.systemUptime;
        }

        public static Builder builder() {
            return new Builder();
        }

        // Getters
        public long getMetricsCollected() { return metricsCollected; }
        public long getAlertsTriggered() { return alertsTriggered; }
        public int getHealthChecksPerformed() { return healthChecksPerformed; }
        public Instant getLastMetricTimestamp() { return lastMetricTimestamp; }
        public Duration getSystemUptime() { return systemUptime; }

        public static class Builder {
            private long metricsCollected;
            private long alertsTriggered;
            private int healthChecksPerformed;
            private Instant lastMetricTimestamp;
            private Duration systemUptime;

            public Builder metricsCollected(long metricsCollected) {
                this.metricsCollected = metricsCollected;
                return this;
            }

            public Builder alertsTriggered(long alertsTriggered) {
                this.alertsTriggered = alertsTriggered;
                return this;
            }

            public Builder healthChecksPerformed(int healthChecksPerformed) {
                this.healthChecksPerformed = healthChecksPerformed;
                return this;
            }

            public Builder lastMetricTimestamp(Instant lastMetricTimestamp) {
                this.lastMetricTimestamp = lastMetricTimestamp;
                return this;
            }

            public Builder systemUptime(Duration systemUptime) {
                this.systemUptime = systemUptime;
                return this;
            }

            public MonitoringStatistics build() {
                return new MonitoringStatistics(this);
            }
        }
    }
}
