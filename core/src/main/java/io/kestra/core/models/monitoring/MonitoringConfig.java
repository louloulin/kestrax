package io.kestra.core.models.monitoring;

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
 * Monitoring and observability configuration for DataFlare
 * Defines metrics collection, alerting, and monitoring endpoints
 */
@Value
@Builder(toBuilder = true)
@Jacksonized
@Introspected
@With
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MonitoringConfig {

    /**
     * Whether monitoring is enabled
     */
    @Builder.Default
    boolean enabled = true;

    /**
     * Metrics configuration
     */
    @NotNull
    MetricsConfig metrics;

    /**
     * Health check configuration
     */
    @NotNull
    HealthCheckConfig healthCheck;

    /**
     * Alerting configuration
     */
    AlertingConfig alerting;

    /**
     * Logging configuration
     */
    @NotNull
    LoggingConfig logging;

    /**
     * Tracing configuration
     */
    TracingConfig tracing;

    /**
     * Performance monitoring configuration
     */
    @NotNull
    PerformanceConfig performance;

    /**
     * Metrics collection and export configuration
     */
    @Value
    @Builder(toBuilder = true)
    @Jacksonized
    @Introspected
    @With
    public static class MetricsConfig {

        /**
         * Whether metrics collection is enabled
         */
        @Builder.Default
        boolean enabled = true;

        /**
         * Metrics collection interval
         */
        @NotNull
        @Builder.Default
        Duration collectionInterval = Duration.ofSeconds(30);

        /**
         * Metrics retention period
         */
        @NotNull
        @Builder.Default
        Duration retentionPeriod = Duration.ofDays(7);

        /**
         * Prometheus configuration
         */
        PrometheusConfig prometheus;

        /**
         * InfluxDB configuration
         */
        InfluxDBConfig influxdb;

        /**
         * Custom metrics endpoints
         */
        @Builder.Default
        List<MetricsEndpoint> customEndpoints = List.of();

        /**
         * Metrics to collect
         */
        @NotNull
        @Builder.Default
        Set<MetricType> enabledMetrics = Set.of(
            MetricType.SYSTEM,
            MetricType.JVM,
            MetricType.APPLICATION,
            MetricType.EXECUTION
        );
    }

    /**
     * Types of metrics to collect
     */
    public enum MetricType {
        SYSTEM,      // CPU, Memory, Disk, Network
        JVM,         // Heap, GC, Threads
        APPLICATION, // Custom application metrics
        EXECUTION,   // Workflow and task execution metrics
        DATABASE,    // Database connection and query metrics
        CACHE,       // Cache hit/miss rates
        SECURITY     // Authentication and authorization metrics
    }

    /**
     * Prometheus metrics configuration
     */
    @Value
    @Builder(toBuilder = true)
    @Jacksonized
    @Introspected
    @With
    public static class PrometheusConfig {

        /**
         * Whether Prometheus export is enabled
         */
        @Builder.Default
        boolean enabled = false;

        /**
         * Prometheus endpoint path
         */
        @NotNull
        @Builder.Default
        String endpoint = "/metrics";

        /**
         * Prometheus port
         */
        @Positive
        @Builder.Default
        int port = 9090;

        /**
         * Metrics prefix
         */
        @NotNull
        @Builder.Default
        String prefix = "dataflare";

        /**
         * Additional labels
         */
        @Builder.Default
        Map<String, String> labels = Map.of();
    }

    /**
     * InfluxDB metrics configuration
     */
    @Value
    @Builder(toBuilder = true)
    @Jacksonized
    @Introspected
    @With
    public static class InfluxDBConfig {

        /**
         * Whether InfluxDB export is enabled
         */
        @Builder.Default
        boolean enabled = false;

        /**
         * InfluxDB URL
         */
        @NotNull
        String url;

        /**
         * Database name
         */
        @NotNull
        String database;

        /**
         * Username
         */
        String username;

        /**
         * Password
         */
        String password;

        /**
         * Batch size for metrics
         */
        @Positive
        @Builder.Default
        int batchSize = 1000;

        /**
         * Flush interval
         */
        @NotNull
        @Builder.Default
        Duration flushInterval = Duration.ofSeconds(10);
    }

    /**
     * Custom metrics endpoint configuration
     */
    @Value
    @Builder(toBuilder = true)
    @Jacksonized
    @Introspected
    @With
    public static class MetricsEndpoint {

        /**
         * Endpoint name
         */
        @NotNull
        String name;

        /**
         * Endpoint URL
         */
        @NotNull
        String url;

        /**
         * HTTP method
         */
        @NotNull
        @Builder.Default
        String method = "POST";

        /**
         * Headers
         */
        @Builder.Default
        Map<String, String> headers = Map.of();

        /**
         * Authentication configuration
         */
        AuthConfig auth;
    }

    /**
     * Authentication configuration for endpoints
     */
    @Value
    @Builder(toBuilder = true)
    @Jacksonized
    @Introspected
    @With
    public static class AuthConfig {

        /**
         * Authentication type
         */
        @NotNull
        AuthType type;

        /**
         * Username (for BASIC auth)
         */
        String username;

        /**
         * Password (for BASIC auth)
         */
        String password;

        /**
         * Token (for BEARER auth)
         */
        String token;

        /**
         * API key (for API_KEY auth)
         */
        String apiKey;

        /**
         * API key header name
         */
        @Builder.Default
        String apiKeyHeader = "X-API-Key";
    }

    /**
     * Authentication types
     */
    public enum AuthType {
        NONE,
        BASIC,
        BEARER,
        API_KEY
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
         * Whether health checks are enabled
         */
        @Builder.Default
        boolean enabled = true;

        /**
         * Health check endpoint path
         */
        @NotNull
        @Builder.Default
        String endpoint = "/health";

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
         * Health checks to perform
         */
        @NotNull
        @Builder.Default
        Set<HealthCheckType> enabledChecks = Set.of(
            HealthCheckType.DATABASE,
            HealthCheckType.CACHE,
            HealthCheckType.DISK_SPACE,
            HealthCheckType.MEMORY
        );

        /**
         * Custom health check endpoints
         */
        @Builder.Default
        List<CustomHealthCheck> customChecks = List.of();
    }

    /**
     * Types of health checks
     */
    public enum HealthCheckType {
        DATABASE,
        CACHE,
        DISK_SPACE,
        MEMORY,
        CPU,
        NETWORK,
        EXTERNAL_SERVICE
    }

    /**
     * Custom health check configuration
     */
    @Value
    @Builder(toBuilder = true)
    @Jacksonized
    @Introspected
    @With
    public static class CustomHealthCheck {

        /**
         * Health check name
         */
        @NotNull
        String name;

        /**
         * Health check URL
         */
        @NotNull
        String url;

        /**
         * Expected status codes
         */
        @NotNull
        @Builder.Default
        Set<Integer> expectedStatusCodes = Set.of(200);

        /**
         * Timeout for health check
         */
        @NotNull
        @Builder.Default
        Duration timeout = Duration.ofSeconds(5);

        /**
         * Critical health check (affects overall health)
         */
        @Builder.Default
        boolean critical = true;
    }

    /**
     * Alerting configuration
     */
    @Value
    @Builder(toBuilder = true)
    @Jacksonized
    @Introspected
    @With
    public static class AlertingConfig {

        /**
         * Whether alerting is enabled
         */
        @Builder.Default
        boolean enabled = false;

        /**
         * Alert rules
         */
        @Builder.Default
        List<AlertRule> rules = List.of();

        /**
         * Notification channels
         */
        @Builder.Default
        List<NotificationChannel> channels = List.of();

        /**
         * Alert evaluation interval
         */
        @NotNull
        @Builder.Default
        Duration evaluationInterval = Duration.ofMinutes(1);
    }

    /**
     * Alert rule configuration
     */
    @Value
    @Builder(toBuilder = true)
    @Jacksonized
    @Introspected
    @With
    public static class AlertRule {

        /**
         * Rule name
         */
        @NotNull
        String name;

        /**
         * Rule description
         */
        String description;

        /**
         * Metric to monitor
         */
        @NotNull
        String metric;

        /**
         * Alert condition
         */
        @NotNull
        AlertCondition condition;

        /**
         * Threshold value
         */
        double threshold;

        /**
         * Duration threshold must be exceeded
         */
        @NotNull
        @Builder.Default
        Duration duration = Duration.ofMinutes(5);

        /**
         * Alert severity
         */
        @NotNull
        @Builder.Default
        AlertSeverity severity = AlertSeverity.WARNING;

        /**
         * Notification channels for this rule
         */
        @Builder.Default
        List<String> channels = List.of();
    }

    /**
     * Alert conditions
     */
    public enum AlertCondition {
        GREATER_THAN,
        LESS_THAN,
        EQUALS,
        NOT_EQUALS
    }

    /**
     * Alert severities
     */
    public enum AlertSeverity {
        INFO,
        WARNING,
        ERROR,
        CRITICAL
    }

    /**
     * Notification channel configuration
     */
    @Value
    @Builder(toBuilder = true)
    @Jacksonized
    @Introspected
    @With
    public static class NotificationChannel {

        /**
         * Channel name
         */
        @NotNull
        String name;

        /**
         * Channel type
         */
        @NotNull
        ChannelType type;

        /**
         * Channel configuration
         */
        @NotNull
        Map<String, Object> config;
    }

    /**
     * Notification channel types
     */
    public enum ChannelType {
        EMAIL,
        SLACK,
        WEBHOOK,
        SMS,
        PAGERDUTY
    }

    /**
     * Logging configuration
     */
    @Value
    @Builder(toBuilder = true)
    @Jacksonized
    @Introspected
    @With
    public static class LoggingConfig {

        /**
         * Log level
         */
        @NotNull
        @Builder.Default
        LogLevel level = LogLevel.INFO;

        /**
         * Log format
         */
        @NotNull
        @Builder.Default
        LogFormat format = LogFormat.JSON;

        /**
         * Log retention period
         */
        @NotNull
        @Builder.Default
        Duration retentionPeriod = Duration.ofDays(30);

        /**
         * Whether to enable structured logging
         */
        @Builder.Default
        boolean structuredLogging = true;

        /**
         * External log aggregation
         */
        LogAggregationConfig aggregation;
    }

    /**
     * Log levels
     */
    public enum LogLevel {
        TRACE,
        DEBUG,
        INFO,
        WARN,
        ERROR
    }

    /**
     * Log formats
     */
    public enum LogFormat {
        JSON,
        PLAIN,
        LOGSTASH
    }

    /**
     * Log aggregation configuration
     */
    @Value
    @Builder(toBuilder = true)
    @Jacksonized
    @Introspected
    @With
    public static class LogAggregationConfig {

        /**
         * Whether log aggregation is enabled
         */
        @Builder.Default
        boolean enabled = false;

        /**
         * Aggregation type
         */
        @NotNull
        AggregationType type;

        /**
         * Aggregation endpoint
         */
        @NotNull
        String endpoint;

        /**
         * Index/topic name
         */
        String index;

        /**
         * Authentication configuration
         */
        AuthConfig auth;
    }

    /**
     * Log aggregation types
     */
    public enum AggregationType {
        ELASTICSEARCH,
        KAFKA,
        FLUENTD,
        LOGSTASH
    }

    /**
     * Distributed tracing configuration
     */
    @Value
    @Builder(toBuilder = true)
    @Jacksonized
    @Introspected
    @With
    public static class TracingConfig {

        /**
         * Whether tracing is enabled
         */
        @Builder.Default
        boolean enabled = false;

        /**
         * Tracing system
         */
        @NotNull
        @Builder.Default
        TracingSystem system = TracingSystem.JAEGER;

        /**
         * Sampling rate (0.0 to 1.0)
         */
        @Builder.Default
        double samplingRate = 0.1;

        /**
         * Tracing endpoint
         */
        String endpoint;

        /**
         * Service name for tracing
         */
        @NotNull
        @Builder.Default
        String serviceName = "dataflare";
    }

    /**
     * Tracing systems
     */
    public enum TracingSystem {
        JAEGER,
        ZIPKIN,
        OPENTELEMETRY
    }

    /**
     * Performance monitoring configuration
     */
    @Value
    @Builder(toBuilder = true)
    @Jacksonized
    @Introspected
    @With
    public static class PerformanceConfig {

        /**
         * Whether performance monitoring is enabled
         */
        @Builder.Default
        boolean enabled = true;

        /**
         * Performance metrics collection interval
         */
        @NotNull
        @Builder.Default
        Duration collectionInterval = Duration.ofSeconds(10);

        /**
         * JVM profiling configuration
         */
        @NotNull
        JvmProfilingConfig jvmProfiling;

        /**
         * Application profiling configuration
         */
        @NotNull
        ApplicationProfilingConfig applicationProfiling;
    }

    /**
     * JVM profiling configuration
     */
    @Value
    @Builder(toBuilder = true)
    @Jacksonized
    @Introspected
    @With
    public static class JvmProfilingConfig {

        /**
         * Whether JVM profiling is enabled
         */
        @Builder.Default
        boolean enabled = true;

        /**
         * Whether to monitor GC
         */
        @Builder.Default
        boolean gcMonitoring = true;

        /**
         * Whether to monitor memory pools
         */
        @Builder.Default
        boolean memoryPoolMonitoring = true;

        /**
         * Whether to monitor thread pools
         */
        @Builder.Default
        boolean threadPoolMonitoring = true;
    }

    /**
     * Application profiling configuration
     */
    @Value
    @Builder(toBuilder = true)
    @Jacksonized
    @Introspected
    @With
    public static class ApplicationProfilingConfig {

        /**
         * Whether application profiling is enabled
         */
        @Builder.Default
        boolean enabled = true;

        /**
         * Whether to monitor execution times
         */
        @Builder.Default
        boolean executionTimeMonitoring = true;

        /**
         * Whether to monitor queue sizes
         */
        @Builder.Default
        boolean queueSizeMonitoring = true;

        /**
         * Whether to monitor error rates
         */
        @Builder.Default
        boolean errorRateMonitoring = true;
    }
}
