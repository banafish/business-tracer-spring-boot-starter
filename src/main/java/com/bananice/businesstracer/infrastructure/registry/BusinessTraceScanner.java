package com.bananice.businesstracer.infrastructure.registry;

import com.bananice.businesstracer.api.BusinessTrace;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;

/**
 * Scans all Spring beans for @BusinessTrace annotations and registers them.
 * This runs during application startup.
 */
@Slf4j
@Component
public class BusinessTraceScanner implements BeanPostProcessor {

    @Resource
    private BusinessTraceRegistry registry;

    @Override
    public Object postProcessAfterInitialization(@NonNull Object bean, @NonNull String beanName) throws BeansException {
        // Get the actual class (unwrap proxy if needed)
        Class<?> targetClass = AopProxyUtils.ultimateTargetClass(bean);

        // Scan all methods for @BusinessTrace annotation
        ReflectionUtils.doWithMethods(targetClass, method -> {
            BusinessTrace annotation = AnnotationUtils.findAnnotation(method, BusinessTrace.class);
            if (annotation != null) {
                String code = annotation.code();
                String name = StringUtils.hasText(annotation.name()) ? annotation.name() : code;

                if (!registry.hasNode(code)) {
                    registry.register(code, name);
                    log.debug("Registered BusinessTrace node: code={}, name={}", code, name);
                }
            }
        });

        return bean;
    }
}
