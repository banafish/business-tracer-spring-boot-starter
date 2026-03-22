package com.bananice.businesstracer.config;

import com.bananice.businesstracer.domain.model.DslConfig;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration properties for Business Tracer
 */
@Data
@ConfigurationProperties(prefix = "business-tracer")
public class BusinessTracerProperties {

    /**
     * Visualization configuration
     */
    private VisualizationConfig visualization = new VisualizationConfig();

    /**
     * Alert center runtime configuration
     */
    private AlertConfig alert = new AlertConfig();

    @Data
    public static class VisualizationConfig {
        /**
         * DSL configurations for different business flows
         */
        private List<DslConfig> dsl = new ArrayList<>();
    }

    @Data
    public static class AlertConfig {

        /**
         * Enable scheduling tasks used by alert center runtime.
         */
        private Boolean schedulingEnabled = true;

        /**
         * Fixed delay(ms) for polling business_alert_config_version.
         */
        private Long configSyncFixedDelayMs = 5000L;

        /**
         * Slow node threshold in milliseconds.
         */
        private Long slowNodeThresholdMs = 2000L;

        /**
         * Aggregation bucket size in minutes.
         */
        private Integer aggregationBucketMinutes = 5;

        /**
         * Fixed delay(ms) for aggregation flush scheduling.
         */
        private Long aggregationFlushFixedDelayMs = 60000L;

        /**
         * Per dispatch attempt timeout in milliseconds.
         */
        private Long dispatchAttemptTimeoutMs = 1000L;

        /**
         * Max retry times for each channel dispatch.
         */
        private Integer dispatchMaxRetries = 1;
    }
}
