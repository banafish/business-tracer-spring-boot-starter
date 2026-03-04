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

    @Data
    public static class VisualizationConfig {
        /**
         * DSL configurations for different business flows
         */
        private List<DslConfig> dsl = new ArrayList<>();
    }
}
