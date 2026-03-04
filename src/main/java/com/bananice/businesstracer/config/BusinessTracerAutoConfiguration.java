package com.bananice.businesstracer.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

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
}
