package com.bananice.businesstracer.config;

import com.bananice.businesstracer.infrastructure.context.TraceContextTaskDecorator;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@EnableAsync
@Configuration
@EnableConfigurationProperties(BusinessTracerProperties.class)
@ComponentScan(basePackages = {
        "com.bananice.businesstracer.infrastructure",
        "com.bananice.businesstracer.api",
        "com.bananice.businesstracer.application",
        "com.bananice.businesstracer.presentation"
})
@MapperScan("com.bananice.businesstracer.infrastructure.persistence.mapper")
public class BusinessTracerAutoConfiguration {

    @Bean(name = "businessTracerTaskExecutor")
    public Executor businessTracerTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("businessTracer-task-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setTaskDecorator(new TraceContextTaskDecorator());
        executor.initialize();
        return executor;
    }

    @Configuration
    @EnableScheduling
    @ConditionalOnProperty(prefix = "business-tracer.alert", name = "scheduling-enabled", havingValue = "true", matchIfMissing = true)
    static class AlertSchedulingConfiguration {
    }
}
