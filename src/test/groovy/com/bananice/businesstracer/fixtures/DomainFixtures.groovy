package com.bananice.businesstracer.fixtures

import com.bananice.businesstracer.domain.model.FlowLog
import com.bananice.businesstracer.domain.model.NodeLog
import com.bananice.businesstracer.domain.model.alert.AlertChannel
import com.bananice.businesstracer.domain.model.alert.AlertChannelType
import com.bananice.businesstracer.domain.model.alert.AlertEvent
import com.bananice.businesstracer.domain.model.alert.AlertRule
import com.bananice.businesstracer.domain.model.alert.AlertScopeType
import com.bananice.businesstracer.domain.model.alert.AlertStatus
import com.bananice.businesstracer.domain.model.alert.AlertType

import java.time.LocalDateTime

/**
 * 领域对象测试工厂：合理默认值 + Map 覆写。
 * 用法：DomainFixtures.anAlertEvent(alertType: AlertType.FLOW_STUCK)
 * 新测试一律用本类构造领域对象；既有 Spec 顺手改到时迁移。
 */
class DomainFixtures {

    static AlertRule anAlertRule(Map overrides = [:]) {
        def m = [
                id       : 1L,
                name     : "Node failed in payment",
                alertType: AlertType.NODE_FAILED,
                scopeType: AlertScopeType.NODE,
                scopeRef : "PAYMENT",
                enabled  : true,
        ] + overrides
        AlertRule.builder()
                .id(m.id as Long)
                .name(m.name as String)
                .alertType(m.alertType as AlertType)
                .scopeType(m.scopeType as AlertScopeType)
                .scopeRef(m.scopeRef as String)
                .enabled(m.enabled as boolean)
                .build()
    }

    static AlertChannel anAlertChannel(Map overrides = [:]) {
        def m = [
                id         : 2L,
                name       : "Ops WeCom",
                channelType: AlertChannelType.WECOM,
                target     : "wecom://group/ops",
                enabled    : true,
        ] + overrides
        AlertChannel.builder()
                .id(m.id as Long)
                .name(m.name as String)
                .channelType(m.channelType as AlertChannelType)
                .target(m.target as String)
                .enabled(m.enabled as boolean)
                .build()
    }

    static AlertEvent anAlertEvent(Map overrides = [:]) {
        def m = [
                id        : 3L,
                ruleId    : 1L,
                alertType : AlertType.NODE_FAILED,
                status    : AlertStatus.NEW,
                businessId: "biz-1",
                flowCode  : "order-flow",
                nodeCode  : "PAYMENT",
                traceId   : "trace-1",
                message   : "node failed",
                occurredAt: LocalDateTime.of(2026, 3, 22, 10, 0),
        ] + overrides
        AlertEvent.builder()
                .id(m.id as Long)
                .ruleId(m.ruleId as Long)
                .alertType(m.alertType as AlertType)
                .status(m.status as AlertStatus)
                .businessId(m.businessId as String)
                .flowCode(m.flowCode as String)
                .nodeCode(m.nodeCode as String)
                .traceId(m.traceId as String)
                .message(m.message as String)
                .occurredAt(m.occurredAt as LocalDateTime)
                .build()
    }

    static NodeLog aNodeLog(Map overrides = [:]) {
        def m = [
                businessId: "biz-1",
                code      : "node-a",
                traceId   : "trace-1",
                status    : "COMPLETED",
                costTime  : 100L,
                createTime: LocalDateTime.of(2026, 3, 22, 10, 0),
        ] + overrides
        NodeLog.builder()
                .businessId(m.businessId as String)
                .code(m.code as String)
                .traceId(m.traceId as String)
                .status(m.status as String)
                .costTime(m.costTime as Long)
                .createTime(m.createTime as LocalDateTime)
                .build()
    }

    static FlowLog aFlowLog(Map overrides = [:]) {
        def m = [
                flowCode  : "order-flow",
                name      : "Flow order-flow",
                businessId: "biz-1",
                status    : "IN_PROGRESS",
                createTime: LocalDateTime.of(2026, 3, 22, 10, 0),
        ] + overrides
        FlowLog.builder()
                .flowCode(m.flowCode as String)
                .name(m.name as String)
                .businessId(m.businessId as String)
                .status(m.status as String)
                .createTime(m.createTime as LocalDateTime)
                .build()
    }
}
