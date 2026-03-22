package com.bananice.businesstracer.application.alert

import com.bananice.businesstracer.domain.model.alert.AlertRule
import com.bananice.businesstracer.domain.model.alert.AlertScopeType
import com.bananice.businesstracer.domain.model.alert.AlertType
import com.bananice.businesstracer.domain.repository.alert.AlertConfigVersionRepository
import com.bananice.businesstracer.domain.repository.alert.AlertRuleRepository
import spock.lang.Specification
import spock.lang.Subject

class AlertConfigCacheServiceSpec extends Specification {

    AlertRuleRepository alertRuleRepository = Mock()
    AlertConfigVersionRepository alertConfigVersionRepository = Mock()

    @Subject
    AlertConfigCacheService alertConfigCacheService = new AlertConfigCacheService(alertRuleRepository, alertConfigVersionRepository)

    private AlertRule buildRule(Map overrides = [:]) {
        def defaults = [
                name     : "rule-default",
                alertType: AlertType.NODE_FAILED,
                scopeType: AlertScopeType.GLOBAL,
                scopeRef : "GLOBAL",
                flowCode : null,
                enabled  : true
        ]
        def merged = defaults + overrides

        AlertRule.builder()
                .name(merged.name as String)
                .alertType(merged.alertType as AlertType)
                .scopeType(merged.scopeType as AlertScopeType)
                .scopeRef(merged.scopeRef as String)
                .flowCode(merged.flowCode as String)
                .enabled(merged.enabled as Boolean)
                .build()
    }

    def "save refreshes local cache before returning"() {
        given:
        def savedRule = buildRule(scopeType: AlertScopeType.NODE, flowCode: "flow-a", scopeRef: "node-x")
        def latestRules = [savedRule]

        when:
        alertConfigCacheService.saveRule(savedRule)

        then:
        1 * alertRuleRepository.save(savedRule)
        1 * alertRuleRepository.findAll() >> latestRules

        and:
        alertConfigCacheService.findByScope(AlertScopeType.NODE, "flow-a", "node-x") == savedRule
    }

    def "sync refreshes cache when published version changes"() {
        given:
        def initialRule = buildRule(scopeType: AlertScopeType.FLOW, flowCode: "flow-a", scopeRef: "flow-a", name: "flow-rule-v1")
        def updatedRule = buildRule(scopeType: AlertScopeType.FLOW, flowCode: "flow-a", scopeRef: "flow-a", name: "flow-rule-v2")

        and:
        alertConfigVersionRepository.getPublishedVersion() >>> [1L, 2L]
        alertRuleRepository.findAll() >>> [[initialRule], [updatedRule]]

        when: "initial load"
        alertConfigCacheService.syncIfVersionChanged()

        and: "next sync sees a higher published version"
        alertConfigCacheService.syncIfVersionChanged()

        then:
        alertConfigCacheService.findByScope(AlertScopeType.FLOW, "flow-a", "flow-a").name == "flow-rule-v2"
    }
}
