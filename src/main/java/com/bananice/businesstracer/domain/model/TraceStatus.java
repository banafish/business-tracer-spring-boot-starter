package com.bananice.businesstracer.domain.model;

/**
 * 链路追踪状态枚举 - 统一管理所有状态常量
 */
public enum TraceStatus {

    NORMAL("NORMAL"),
    FAILED("FAILED"),
    COMPLETED("COMPLETED"),
    IN_PROGRESS("IN_PROGRESS");

    private final String value;

    TraceStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
