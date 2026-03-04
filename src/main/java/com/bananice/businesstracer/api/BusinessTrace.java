package com.bananice.businesstracer.api;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface BusinessTrace {

    /**
     * Node Code - unique identifier for DSL orchestration (REQUIRED)
     * e.g., "CREATE_ORDER", "PAYMENT_CALLBACK"
     */
    String code();

    /**
     * Node Display Name (optional, defaults to code or method name)
     * e.g., "创建订单", "支付回调"
     */
    String name() default "";

    /**
     * SpEL expression to extract Business ID
     * e.g., "#order.id"
     */
    String key();

    /**
     * Operation description
     */
    String operation() default "";

    /**
     * SpEL expression to extract Input parameters
     */
    String inputParams() default "";

    /**
     * SpEL expression to extract Output parameters
     */
    String outputParams() default "";
}
