package com.bananice.businesstracer.application.alert

import com.bananice.businesstracer.domain.model.NodeLog
import com.bananice.businesstracer.domain.model.alert.AlertEvent
import com.bananice.businesstracer.domain.model.alert.AlertStatus
import com.bananice.businesstracer.domain.model.alert.AlertType
import com.bananice.businesstracer.domain.repository.alert.AlertEventRepository
import spock.lang.Specification
import spock.lang.Subject

import java.time.LocalDateTime

class AlertEvaluateServiceSpec extends Specification {

    AlertEventRepository alertEventRepository = Mock()

    @Subject
    AlertEvaluateService alertEvaluateService = new AlertEvaluateService(alertEventRepository, 2000L)

    private NodeLog buildNodeLog(Map overrides = [:]) {
        def defaults = [
                businessId: "biz-1",
                code      : "node-a",
                traceId   : "trace-1",
                status    : "COMPLETED",
                costTime  : 100L,
                createTime: LocalDateTime.of(2026, 3, 22, 10, 0)
        ]
        def merged = defaults + overrides

        NodeLog.builder()
                .businessId(merged.businessId as String)
                .code(merged.code as String)
                .traceId(merged.traceId as String)
                .status(merged.status as String)
                .costTime(merged.costTime as Long)
                .createTime(merged.createTime as LocalDateTime)
                .build()
    }

    private AlertEvent buildOpenFlowStuck(Map overrides = [:]) {
        def defaults = [
                id        : 101L,
                alertType : AlertType.FLOW_STUCK,
                status    : AlertStatus.NEW,
                flowCode  : "flow-a",
                businessId: "biz-1",
                message   : "flow stuck",
                occurredAt: LocalDateTime.of(2026, 3, 22, 10, 0)
        ]
        def merged = defaults + overrides

        AlertEvent.builder()
                .id(merged.id as Long)
                .alertType(merged.alertType as AlertType)
                .status(merged.status as AlertStatus)
                .flowCode(merged.flowCode as String)
                .businessId(merged.businessId as String)
                .message(merged.message as String)
                .occurredAt(merged.occurredAt as LocalDateTime)
                .build()
    }

    def "evaluate emits NODE_FAILED and SLOW_NODE by status and threshold"() {
        given:
        def failedAndSlow = buildNodeLog(status: "FAILED", costTime: 3001L)

        when:
        def events = alertEvaluateService.evaluateNode(failedAndSlow)

        then:
        events*.alertType.containsAll([AlertType.NODE_FAILED, AlertType.SLOW_NODE])
    }

    def "evaluate uses configurable threshold and default boundary"() {
        given:
        def evaluateService = new AlertEvaluateService(alertEventRepository, 3000L)

        when:
        def eventsAtBoundary = evaluateService.evaluateNode(buildNodeLog(costTime: 3000L))
        def eventsAboveBoundary = evaluateService.evaluateNode(buildNodeLog(costTime: 3001L))

        then:
        !eventsAtBoundary*.alertType.contains(AlertType.SLOW_NODE)
        eventsAboveBoundary*.alertType.contains(AlertType.SLOW_NODE)
    }

    def "flow stuck lifecycle opens once updates existing and closes existing"() {
        given:
        def now = LocalDateTime.of(2026, 3, 22, 10, 30)
        def openEvent = buildOpenFlowStuck(id: 201L, flowCode: "flow-a", businessId: "biz-1")

        when: "opening first stuck incident"
        alertEvaluateService.openOrUpdateFlowStuck("flow-a", "biz-1", "trace-1", now)

        then:
        1 * alertEventRepository.findOpenFlowStuck("flow-a", "biz-1") >> null
        1 * alertEventRepository.save({ AlertEvent event ->
            event.alertType == AlertType.FLOW_STUCK &&
                    event.status == AlertStatus.NEW &&
                    event.flowCode == "flow-a" &&
                    event.businessId == "biz-1"
        })

        when: "same stuck incident appears again"
        alertEvaluateService.openOrUpdateFlowStuck("flow-a", "biz-1", "trace-1", now.plusMinutes(2))

        then:
        1 * alertEventRepository.findOpenFlowStuck("flow-a", "biz-1") >> openEvent
        1 * alertEventRepository.updateOpenFlowStuck(201L, _ as String, now.plusMinutes(2))
        0 * alertEventRepository.save(_)

        when: "stuck incident is recovered and closed"
        alertEvaluateService.closeFlowStuck("flow-a", "biz-1", now.plusMinutes(5))

        then:
        1 * alertEventRepository.findOpenFlowStuck("flow-a", "biz-1") >> openEvent
        1 * alertEventRepository.closeFlowStuck(201L, now.plusMinutes(5))
    }
}
