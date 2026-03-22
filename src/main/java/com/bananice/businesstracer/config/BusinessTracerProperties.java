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
         * Fixed delay(ms) for polling business_alert_config_version.
         */
        private Long configSyncFixedDelayMs = 5000L;
    }
}
