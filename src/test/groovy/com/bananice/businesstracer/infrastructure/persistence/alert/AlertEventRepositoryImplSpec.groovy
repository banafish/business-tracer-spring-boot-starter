package com.bananice.businesstracer.infrastructure.persistence.alert

import com.bananice.businesstracer.TestApplication
import com.bananice.businesstracer.config.BusinessTracerProperties
import com.bananice.businesstracer.domain.model.alert.AlertDispatchLog
import com.bananice.businesstracer.domain.model.alert.AlertEvent
import com.bananice.businesstracer.infrastructure.alert.job.AlertHistoryCleanupJob
import com.bananice.businesstracer.domain.model.alert.AlertStatus
import com.bananice.businesstracer.domain.model.alert.AlertType
import com.bananice.businesstracer.domain.repository.alert.AlertDispatchLogRepository
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

    @Resource
    AlertDispatchLogRepository alertDispatchLogRepository

    @Resource
    AlertHistoryCleanupJob alertHistoryCleanupJob

    @Resource
    BusinessTracerProperties businessTracerProperties

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

    private AlertDispatchLog buildDispatchLog(Map overrides = [:]) {
        def defaults = [
                eventId    : null,
                channelId  : 1L,
                status     : AlertStatus.SENT,
                response   : "ok",
                dispatchTime: LocalDateTime.now(),
                retryCount : 0
        ]
        def merged = defaults + overrides

        AlertDispatchLog.builder()
                .eventId(merged.eventId as Long)
                .channelId(merged.channelId as Long)
                .status(merged.status as AlertStatus)
                .response(merged.response as String)
                .dispatchTime(merged.dispatchTime as LocalDateTime)
                .retryCount(merged.retryCount as Integer)
                .build()
    }

    def "cleanup job deletes events and dispatch logs older than retention days"() {
        given: "old and recent events each with dispatch logs"
        def now = LocalDateTime.now()
        def oldOccurredAt = now.minusDays(31)
        def recentOccurredAt = now.minusDays(1)

        def oldEvent = buildEvent(
                businessId: "biz-old",
                flowCode: "flow-old",
                nodeCode: "node-old",
                message: "old-event",
                occurredAt: oldOccurredAt
        )
        def recentEvent = buildEvent(
                businessId: "biz-new",
                flowCode: "flow-new",
                nodeCode: "node-new",
                message: "recent-event",
                occurredAt: recentOccurredAt
        )

        alertEventRepository.save(oldEvent)
        alertEventRepository.save(recentEvent)

        def oldPersisted = alertEventRepository.query(null, null, null, null, "flow-old", "node-old", "biz-old", 1, 1).first()
        def recentPersisted = alertEventRepository.query(null, null, null, null, "flow-new", "node-new", "biz-new", 1, 1).first()

        alertDispatchLogRepository.save(buildDispatchLog(eventId: oldPersisted.id, dispatchTime: oldOccurredAt, response: "old-log"))
        alertDispatchLogRepository.save(buildDispatchLog(eventId: recentPersisted.id, dispatchTime: recentOccurredAt, response: "recent-log"))

        when: "running cleanup job with 30-day retention"
        businessTracerProperties.alert.retentionDays = 30
        alertHistoryCleanupJob.cleanupHistory()

        then: "old event and its dispatch log are removed while recent data remains"
        alertEventRepository.query(null, null, null, null, "flow-old", "node-old", "biz-old", 1, 10).isEmpty()
        alertEventRepository.query(null, null, null, null, "flow-new", "node-new", "biz-new", 1, 10).size() == 1

        and:
        alertDispatchLogRepository.findByEventId(oldPersisted.id).isEmpty()
        alertDispatchLogRepository.findByEventId(recentPersisted.id).size() == 1
    }

    def "queryEvents should support filters and pagination"() {
        given: "five events in an isolated time range with mixed attributes"
        def now = LocalDateTime.of(2035, 1, 1, 10, 0, 0)

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
