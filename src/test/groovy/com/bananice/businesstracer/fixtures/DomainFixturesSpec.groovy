package com.bananice.businesstracer.fixtures

import com.bananice.businesstracer.domain.model.alert.AlertStatus
import com.bananice.businesstracer.domain.model.alert.AlertType
import spock.lang.Specification

import java.time.LocalDateTime

class DomainFixturesSpec extends Specification {

    def "fixtures provide valid defaults"() {
        expect:
        with(DomainFixtures.anAlertRule()) {
            id != null && name && alertType != null && scopeType != null && enabled
        }
        with(DomainFixtures.anAlertChannel()) {
            id != null && name && channelType != null && target && enabled
        }
        with(DomainFixtures.anAlertEvent()) {
            id != null && ruleId != null && alertType != null && status == AlertStatus.NEW && occurredAt != null
        }
        with(DomainFixtures.aNodeLog()) {
            businessId && code && traceId && status && costTime != null && createTime != null
        }
        with(DomainFixtures.aFlowLog()) {
            flowCode && name && businessId && status && createTime != null
        }
    }

    def "fixtures merge overrides over defaults"() {
        when:
        def event = DomainFixtures.anAlertEvent(alertType: AlertType.FLOW_STUCK, businessId: "biz-x")
        def node = DomainFixtures.aNodeLog(createTime: LocalDateTime.of(2026, 1, 1, 0, 0))

        then:
        event.alertType == AlertType.FLOW_STUCK
        event.businessId == "biz-x"
        event.ruleId == 1L
        node.createTime == LocalDateTime.of(2026, 1, 1, 0, 0)
        node.code == "node-a"
    }
}
