package com.bananice.businesstracer.infrastructure.persistence.alert

import com.bananice.businesstracer.TestApplication
import com.bananice.businesstracer.domain.model.alert.AlertRule
import com.bananice.businesstracer.domain.model.alert.AlertScopeType
import com.bananice.businesstracer.domain.model.alert.AlertType
import com.bananice.businesstracer.domain.repository.alert.AlertRuleRepository
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.annotation.Transactional
import spock.lang.Specification
import spock.lang.Subject

import javax.annotation.Resource

@SpringBootTest(classes = TestApplication)
@Transactional
class AlertRuleRepositoryImplSpec extends Specification {

    @Resource
    @Subject
    AlertRuleRepository alertRuleRepository

    private AlertRule buildRule(Map overrides = [:]) {
        def defaults = [
                name     : "rule-default",
                alertType: AlertType.NODE_FAILED,
                scopeType: AlertScopeType.FLOW,
                scopeRef : "scope-default",
                flowCode : "flow-default",
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

    def "saveRule should upsert by FLOW scope uniqueness semantics"() {
        given: "an existing flow-scoped rule"
        def original = buildRule(
                name: "rule-original",
                scopeType: AlertScopeType.FLOW,
                flowCode: "flow-upsert",
                scopeRef: "scope-upsert",
                alertType: AlertType.NODE_FAILED,
                enabled: true
        )
        alertRuleRepository.save(original)

        and: "a new rule payload with the same flow scope"
        def updated = buildRule(
                name: "rule-updated",
                scopeType: AlertScopeType.FLOW,
                flowCode: "flow-upsert",
                scopeRef: "scope-upsert",
                alertType: AlertType.FLOW_STUCK,
                enabled: false
        )

        when: "saving again with the same FLOW uniqueness key"
        alertRuleRepository.save(updated)
        def rule = alertRuleRepository.findByScope(AlertScopeType.FLOW, "flow-upsert", "scope-upsert")
        def allRules = alertRuleRepository.findAll()

        then: "repository keeps a single row and updates values"
        allRules.size() == 1
        rule != null
        verifyAll(rule) {
            name == "rule-updated"
            alertType == AlertType.FLOW_STUCK
            scopeType == AlertScopeType.FLOW
            flowCode == "flow-upsert"
            scopeRef == "scope-upsert"
            enabled == false
        }
    }

    def "saveRule should upsert by NODE scope uniqueness semantics"() {
        given: "an existing NODE-scoped rule"
        def original = buildRule(
                name: "node-rule-original",
                scopeType: AlertScopeType.NODE,
                flowCode: "flow-a",
                scopeRef: "node-x",
                alertType: AlertType.NODE_FAILED,
                enabled: true
        )
        alertRuleRepository.save(original)

        and: "a new payload with same node scope"
        def updated = buildRule(
                name: "node-rule-updated",
                scopeType: AlertScopeType.NODE,
                flowCode: "flow-a",
                scopeRef: "node-x",
                alertType: AlertType.SLOW_NODE,
                enabled: false
        )

        when: "saving again with same NODE uniqueness key"
        alertRuleRepository.save(updated)
        def nodeRule = alertRuleRepository.findByScope(AlertScopeType.NODE, "flow-a", "node-x")
        def allRules = alertRuleRepository.findAll()

        then: "repository upserts existing NODE-scoped row"
        allRules.size() == 1
        nodeRule != null
        verifyAll(nodeRule) {
            name == "node-rule-updated"
            alertType == AlertType.SLOW_NODE
            scopeType == AlertScopeType.NODE
            flowCode == "flow-a"
            scopeRef == "node-x"
            enabled == false
        }
    }
}
