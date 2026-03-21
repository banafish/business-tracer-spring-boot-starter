package com.bananice.businesstracer.infrastructure.persistence.alert

import com.bananice.businesstracer.TestApplication
import com.bananice.businesstracer.domain.model.alert.AlertEvent
import com.bananice.businesstracer.domain.model.alert.AlertStatus
import com.bananice.businesstracer.domain.model.alert.AlertType
import com.bananice.businesstracer.domain.repository.alert.AlertEventRepository
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.annotation.Transactional
import spock.lang.Specification
import spock.lang.Subject

import javax.annotation.Resource
import java.time.LocalDateTime

@SpringBootTest(classes = TestApplication)
@Transactional
class AlertEventRepositoryImplSpec extends Specification {

    @Resource
    @Subject
    AlertEventRepository alertEventRepository

    private AlertEvent buildEvent(Map overrides = [:]) {
        def defaults = [
                ruleId    : 1L,
                alertType : AlertType.NODE_FAILED,
                status    : AlertStatus.NEW,
                businessId: "biz-default",
                flowCode  : "flow-default",
                nodeCode  : "node-default",
                traceId   : "trace-default",
                message   : "msg-default",
                occurredAt: LocalDateTime.now()
        ]
        def merged = defaults + overrides

        AlertEvent.builder()
                .ruleId(merged.ruleId as Long)
                .alertType(merged.alertType as AlertType)
                .status(merged.status as AlertStatus)
                .businessId(merged.businessId as String)
                .flowCode(merged.flowCode as String)
                .nodeCode(merged.nodeCode as String)
                .traceId(merged.traceId as String)
                .message(merged.message as String)
                .occurredAt(merged.occurredAt as LocalDateTime)
                .build()
    }

    def "queryEvents should support filters and pagination"() {
        given: "five events in time range with mixed attributes"
        def now = LocalDateTime.now()

        alertEventRepository.save(buildEvent(
                alertType: AlertType.NODE_FAILED,
                status: AlertStatus.NEW,
                flowCode: "flow-a",
                nodeCode: "node-a",
                businessId: "biz-a",
                message: "match-old",
                occurredAt: now.minusMinutes(12)
        ))
        alertEventRepository.save(buildEvent(
                alertType: AlertType.NODE_FAILED,
                status: AlertStatus.NEW,
                flowCode: "flow-a",
                nodeCode: "node-a",
                businessId: "biz-a",
                message: "match-new",
                occurredAt: now.minusMinutes(2)
        ))
        alertEventRepository.save(buildEvent(
                alertType: AlertType.NODE_FAILED,
                status: AlertStatus.SENT,
                flowCode: "flow-a",
                nodeCode: "node-a",
                businessId: "biz-a",
                message: "status-not-match",
                occurredAt: now.minusMinutes(3)
        ))
        alertEventRepository.save(buildEvent(
                alertType: AlertType.SLOW_NODE,
                status: AlertStatus.NEW,
                flowCode: "flow-a",
                nodeCode: "node-a",
                businessId: "biz-a",
                message: "type-not-match",
                occurredAt: now.minusMinutes(4)
        ))
        alertEventRepository.save(buildEvent(
                alertType: AlertType.NODE_FAILED,
                status: AlertStatus.NEW,
                flowCode: "flow-b",
                nodeCode: "node-b",
                businessId: "biz-b",
                message: "scope-not-match",
                occurredAt: now.minusMinutes(5)
        ))

        when: "querying page 1 with all filters"
        def start = now.minusMinutes(20)
        def end = now.plusMinutes(1)
        def page1 = alertEventRepository.query(start, end, AlertType.NODE_FAILED, AlertStatus.NEW, "flow-a", "node-a", "biz-a", 1, 1)
        def total = alertEventRepository.count(start, end, AlertType.NODE_FAILED, AlertStatus.NEW, "flow-a", "node-a", "biz-a")

        then: "only matching records are counted and first page returns latest match"
        total == 2L
        page1.size() == 1
        page1[0].message == "match-new"

        when: "querying page 2 with same filters"
        def page2 = alertEventRepository.query(start, end, AlertType.NODE_FAILED, AlertStatus.NEW, "flow-a", "node-a", "biz-a", 2, 1)

        then: "second page returns the second match"
        page2.size() == 1
        page2[0].message == "match-old"

        when: "querying with optional filters omitted"
        def allInTime = alertEventRepository.query(start, end, null, null, null, null, null, 1, 10)
        def allInTimeCount = alertEventRepository.count(start, end, null, null, null, null, null)

        then: "null optional filters do not constrain results"
        allInTime.size() == 5
        allInTimeCount == 5L
    }
}
