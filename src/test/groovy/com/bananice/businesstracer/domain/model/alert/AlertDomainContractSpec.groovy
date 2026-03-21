package com.bananice.businesstracer.domain.model.alert

import spock.lang.Specification

import java.time.LocalDateTime

class AlertDomainContractSpec extends Specification {

    def "alert enums expose required constants"() {
        expect:
        AlertScopeType.values()*.name().containsAll(["GLOBAL", "FLOW", "NODE"])
        AlertChannelType.values()*.name().containsAll(["WEBHOOK", "WECOM", "DINGTALK", "EMAIL"])
        AlertType.values()*.name().containsAll(["NODE_FAILED", "SLOW_NODE", "FLOW_STUCK"])
        AlertStatus.values()*.name().containsAll(["NEW", "SENT", "FAILED", "SUPPRESSED"])
    }

    def "alert entities satisfy minimal builder and getter contracts"() {
        given:
        def now = LocalDateTime.now()

        and:
        def rule = AlertRule.builder()
                .id(1L)
                .name("Node failed in payment")
                .alertType(AlertType.NODE_FAILED)
                .scopeType(AlertScopeType.NODE)
                .scopeRef("PAYMENT")
                .enabled(true)
                .build()

        and:
        def channel = AlertChannel.builder()
                .id(2L)
                .name("Ops WeCom")
                .channelType(AlertChannelType.WECOM)
                .target("wecom://group/ops")
                .enabled(true)
                .build()

        and:
        def event = AlertEvent.builder()
                .id(3L)
                .ruleId(1L)
                .alertType(AlertType.NODE_FAILED)
                .status(AlertStatus.NEW)
                .businessId("biz-1")
                .flowCode("order-flow")
                .nodeCode("PAYMENT")
                .traceId("trace-1")
                .message("node failed")
                .occurredAt(now)
                .build()

        and:
        def dispatchLog = AlertDispatchLog.builder()
                .id(4L)
                .eventId(3L)
                .channelId(2L)
                .status(AlertStatus.SENT)
                .response("ok")
                .dispatchTime(now)
                .retryCount(0)
                .build()

        and:
        def context = AlertContext.builder()
                .businessId("biz-1")
                .flowCode("order-flow")
                .nodeCode("PAYMENT")
                .traceId("trace-1")
                .build()

        expect:
        rule.id == 1L
        rule.alertType == AlertType.NODE_FAILED
        rule.scopeType == AlertScopeType.NODE
        rule.scopeRef == "PAYMENT"
        rule.enabled

        channel.id == 2L
        channel.channelType == AlertChannelType.WECOM
        channel.target == "wecom://group/ops"
        channel.enabled

        event.id == 3L
        event.ruleId == 1L
        event.alertType == AlertType.NODE_FAILED
        event.status == AlertStatus.NEW
        event.businessId == "biz-1"
        event.flowCode == "order-flow"
        event.nodeCode == "PAYMENT"
        event.traceId == "trace-1"
        event.message == "node failed"
        event.occurredAt == now

        dispatchLog.id == 4L
        dispatchLog.eventId == 3L
        dispatchLog.channelId == 2L
        dispatchLog.status == AlertStatus.SENT
        dispatchLog.response == "ok"
        dispatchLog.dispatchTime == now
        dispatchLog.retryCount == 0

        context.businessId == "biz-1"
        context.flowCode == "order-flow"
        context.nodeCode == "PAYMENT"
        context.traceId == "trace-1"
    }
}
