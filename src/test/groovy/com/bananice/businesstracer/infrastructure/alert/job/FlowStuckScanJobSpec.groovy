package com.bananice.businesstracer.infrastructure.alert.job

import com.bananice.businesstracer.application.FlowLogService
import com.bananice.businesstracer.application.alert.AlertEvaluateService
import com.bananice.businesstracer.config.BusinessTracerProperties
import com.bananice.businesstracer.domain.model.FlowLog
import spock.lang.Specification
import spock.lang.Subject

import java.time.Duration
import java.time.LocalDateTime

class FlowStuckScanJobSpec extends Specification {

    FlowLogService flowLogService = Mock()
    AlertEvaluateService alertEvaluateService = Mock()
    BusinessTracerProperties properties = new BusinessTracerProperties()

    @Subject
    FlowStuckScanJob flowStuckScanJob = new FlowStuckScanJob(flowLogService, alertEvaluateService, properties)

    private FlowLog buildFlowLog(Map overrides = [:]) {
        def defaults = [
                flowCode  : "flow-a",
                businessId: "biz-1",
                status    : "IN_PROGRESS",
                createTime: LocalDateTime.now().minusMinutes(10)
        ]
        def merged = defaults + overrides
        FlowLog.builder()
                .flowCode(merged.flowCode as String)
                .name("Flow ${merged.flowCode}")
                .businessId(merged.businessId as String)
                .status(merged.status as String)
                .createTime(merged.createTime as LocalDateTime)
                .build()
    }

    def "scan job opens in-progress stale flow and closes terminal stale flows"() {
        given:
        properties.alert.flowStuckThresholdMs = 120000L
        properties.alert.flowStuckScanBatchSize = 50

        when:
        flowStuckScanJob.scanAndPublishFlowStuck()

        then:
        1 * flowLogService.findStaleFlows(_ as Duration, 50) >> [
                buildFlowLog(flowCode: "flow-a", businessId: "biz-1", status: "IN_PROGRESS"),
                buildFlowLog(flowCode: "flow-b", businessId: "biz-2", status: "COMPLETED"),
                buildFlowLog(flowCode: "flow-c", businessId: "biz-3", status: "FAILED")
        ]
        1 * alertEvaluateService.openOrUpdateFlowStuck("flow-a", "biz-1", null, null)
        1 * alertEvaluateService.closeFlowStuckByStatus("flow-b", "biz-2", "COMPLETED", null)
        1 * alertEvaluateService.closeFlowStuckByStatus("flow-c", "biz-3", "FAILED", null)
        0 * _
    }
}
