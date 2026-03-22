package com.bananice.businesstracer.application

import com.bananice.businesstracer.application.alert.AlertAggregationService
import com.bananice.businesstracer.application.alert.AlertEvaluateService
import com.bananice.businesstracer.domain.model.NodeLog
import com.bananice.businesstracer.domain.repository.DetailLogRepository
import com.bananice.businesstracer.domain.repository.NodeLogRepository
import com.bananice.businesstracer.domain.repository.alert.AlertEventRepository
import spock.lang.Specification
import spock.lang.Subject

class TraceAsyncLogServiceSpec extends Specification {

    NodeLogRepository nodeLogRepository = Mock()
    DetailLogRepository detailLogRepository = Mock()
    FlowLogService flowLogService = Mock()
    AlertEvaluateService alertEvaluateService = Mock()
    AlertEventRepository alertEventRepository = Mock()
    AlertAggregationService alertAggregationService = Mock()

    @Subject
    TraceAsyncLogService traceAsyncLogService = new TraceAsyncLogService(
            nodeLogRepository,
            detailLogRepository,
            flowLogService,
            alertEvaluateService,
            alertEventRepository,
            alertAggregationService
    )

    def "saveNodeLogAndFlowLogsAsync continues flow recording and status update when alert evaluation fails"() {
        given:
        def logRecord = NodeLog.builder()
                .nodeId("node-1")
                .businessId("biz-1")
                .code("NODE_A")
                .build()

        when:
        traceAsyncLogService.saveNodeLogAndFlowLogsAsync(logRecord, "NODE_A", "biz-1", false)

        then:
        1 * nodeLogRepository.save(logRecord)
        1 * alertEvaluateService.evaluateNode(logRecord) >> { throw new RuntimeException("alert boom") }

        and:
        1 * flowLogService.recordFlowLogs("NODE_A", "biz-1")
        1 * flowLogService.checkAndUpdateFlowStatusByNodeCode("biz-1", "NODE_A")
        0 * flowLogService.markFlowsAsFailed(_)

        and:
        0 * alertEventRepository._
        0 * alertAggregationService._
        0 * _
    }
}
