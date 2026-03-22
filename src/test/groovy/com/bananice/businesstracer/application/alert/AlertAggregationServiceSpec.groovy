package com.bananice.businesstracer.application.alert

import com.bananice.businesstracer.domain.model.alert.AlertEvent
import com.bananice.businesstracer.domain.model.alert.AlertStatus
import com.bananice.businesstracer.domain.model.alert.AlertType
import com.bananice.businesstracer.infrastructure.alert.job.AlertAggregationFlushJob
import spock.lang.Specification
import spock.lang.Subject

import java.time.LocalDateTime

class AlertAggregationServiceSpec extends Specification {

    AlertDispatchService alertDispatchService = Mock()

    @Subject
    AlertAggregationService alertAggregationService = new AlertAggregationService(5)

    private AlertEvent buildEvent(Map overrides = [:]) {
        def defaults = [
                alertType    : AlertType.NODE_FAILED,
                status       : AlertStatus.NEW,
                flowCode     : "flow-a",
                nodeCode     : "node-a",
                businessId   : "biz-1",
                aggregateKey : "flow-a:node-a",
                occurredAt   : LocalDateTime.of(2026, 3, 22, 10, 1)
        ]
        def merged = defaults + overrides

        AlertEvent.builder()
                .alertType(merged.alertType as AlertType)
                .status(merged.status as AlertStatus)
                .flowCode(merged.flowCode as String)
                .nodeCode(merged.nodeCode as String)
                .businessId(merged.businessId as String)
                .aggregateKey(merged.aggregateKey as String)
                .occurredAt(merged.occurredAt as LocalDateTime)
                .build()
    }

    def "aggregate groups by dedup key and flushes completed 5 minute buckets"() {
        given:
        alertAggregationService.aggregate(buildEvent(aggregateKey: "flow-a:node-a", occurredAt: LocalDateTime.of(2026, 3, 22, 10, 1)))
        alertAggregationService.aggregate(buildEvent(aggregateKey: "flow-a:node-a", occurredAt: LocalDateTime.of(2026, 3, 22, 10, 2)))
        alertAggregationService.aggregate(buildEvent(aggregateKey: "flow-a:node-b", occurredAt: LocalDateTime.of(2026, 3, 22, 10, 3)))

        when:
        def flushed = alertAggregationService.flush(LocalDateTime.of(2026, 3, 22, 10, 6))

        then:
        flushed.size() == 2
        flushed.find { it.aggregateKey == "flow-a:node-a" }.count == 2
        flushed.find { it.aggregateKey == "flow-a:node-b" }.count == 1
    }

    def "aggregate supports configurable bucket size"() {
        given:
        def minuteBucketService = new AlertAggregationService(1)
        minuteBucketService.aggregate(buildEvent(aggregateKey: "k1", occurredAt: LocalDateTime.of(2026, 3, 22, 10, 1, 10)))

        when:
        def notFlushedYet = minuteBucketService.flush(LocalDateTime.of(2026, 3, 22, 10, 1, 59))
        def flushed = minuteBucketService.flush(LocalDateTime.of(2026, 3, 22, 10, 2, 0))

        then:
        notFlushedYet.isEmpty()
        flushed.size() == 1
        flushed[0].aggregateKey == "k1"
        flushed[0].count == 1
        flushed[0].alertType == AlertType.NODE_FAILED
    }

    def "aggregate keeps different alert types separated for same aggregate key"() {
        given:
        def occurredAt = LocalDateTime.of(2026, 3, 22, 10, 1)
        alertAggregationService.aggregate(buildEvent(aggregateKey: "same-key", alertType: AlertType.NODE_FAILED, occurredAt: occurredAt))
        alertAggregationService.aggregate(buildEvent(aggregateKey: "same-key", alertType: AlertType.SLOW_NODE, occurredAt: occurredAt.plusSeconds(5)))

        when:
        def flushed = alertAggregationService.flush(LocalDateTime.of(2026, 3, 22, 10, 6))

        then:
        flushed.size() == 2
        flushed.find { it.aggregateKey == "same-key" && it.alertType == AlertType.NODE_FAILED }.count == 1
        flushed.find { it.aggregateKey == "same-key" && it.alertType == AlertType.SLOW_NODE }.count == 1
    }

    def "flush job dispatches each matured aggregation result"() {
        given:
        def aggregationService = new AlertAggregationService(5)
        def flushJob = new AlertAggregationFlushJob(aggregationService, alertDispatchService)
        aggregationService.aggregate(buildEvent(aggregateKey: "flow-a:node-a", occurredAt: LocalDateTime.of(2000, 1, 1, 10, 1)))
        aggregationService.aggregate(buildEvent(aggregateKey: "flow-a:node-b", occurredAt: LocalDateTime.of(2000, 1, 1, 10, 2)))

        when:
        flushJob.flushAggregationBuckets()

        then:
        2 * alertDispatchService.dispatchAggregated(_ as AlertAggregationService.AggregationResult)
    }
}
