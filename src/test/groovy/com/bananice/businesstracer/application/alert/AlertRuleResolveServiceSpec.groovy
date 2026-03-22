package com.bananice.businesstracer.application.alert

import com.bananice.businesstracer.domain.model.alert.AlertRule
import com.bananice.businesstracer.domain.model.alert.AlertScopeType
import com.bananice.businesstracer.domain.model.alert.AlertType
import com.bananice.businesstracer.domain.repository.alert.AlertRuleRepository
import spock.lang.Specification
import spock.lang.Subject

class AlertRuleResolveServiceSpec extends Specification {

    AlertRuleRepository alertRuleRepository = Mock()

    @Subject
    AlertRuleResolveService alertRuleResolveService = new AlertRuleResolveService(alertRuleRepository)

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

    def "resolve returns NODE scope first when node rule exists"() {
        given:
        def nodeRule = buildRule(scopeType: AlertScopeType.NODE, flowCode: "flow-a", scopeRef: "node-x", name: "node-rule")
        def flowRule = buildRule(scopeType: AlertScopeType.FLOW, flowCode: "flow-a", scopeRef: "flow-a", name: "flow-rule")
        def globalRule = buildRule(scopeType: AlertScopeType.GLOBAL, scopeRef: "GLOBAL", name: "global-rule")

        and:
        alertRuleRepository.findByScope(AlertScopeType.NODE, "flow-a", "node-x") >> nodeRule
        alertRuleRepository.findByScope(AlertScopeType.FLOW, "flow-a", "flow-a") >> flowRule
        alertRuleRepository.findByScope(AlertScopeType.GLOBAL, null, "GLOBAL") >> globalRule

        when:
        def resolved = alertRuleResolveService.resolve("flow-a", "node-x")

        then:
        resolved == nodeRule
    }

    def "resolve falls back to FLOW when NODE rule missing"() {
        given:
        def flowRule = buildRule(scopeType: AlertScopeType.FLOW, flowCode: "flow-a", scopeRef: "flow-a", name: "flow-rule")
        def globalRule = buildRule(scopeType: AlertScopeType.GLOBAL, scopeRef: "GLOBAL", name: "global-rule")

        and:
        alertRuleRepository.findByScope(AlertScopeType.NODE, "flow-a", "node-x") >> null
        alertRuleRepository.findByScope(AlertScopeType.FLOW, "flow-a", "flow-a") >> flowRule
        alertRuleRepository.findByScope(AlertScopeType.GLOBAL, null, "GLOBAL") >> globalRule

        when:
        def resolved = alertRuleResolveService.resolve("flow-a", "node-x")

        then:
        resolved == flowRule
    }

    def "resolve falls back to GLOBAL when NODE and FLOW rules missing"() {
        given:
        def globalRule = buildRule(scopeType: AlertScopeType.GLOBAL, scopeRef: "GLOBAL", name: "global-rule")

        and:
        alertRuleRepository.findByScope(AlertScopeType.NODE, "flow-a", "node-x") >> null
        alertRuleRepository.findByScope(AlertScopeType.FLOW, "flow-a", "flow-a") >> null
        alertRuleRepository.findByScope(AlertScopeType.GLOBAL, null, "GLOBAL") >> globalRule

        when:
        def resolved = alertRuleResolveService.resolve("flow-a", "node-x")

        then:
        resolved == globalRule
    }
}
