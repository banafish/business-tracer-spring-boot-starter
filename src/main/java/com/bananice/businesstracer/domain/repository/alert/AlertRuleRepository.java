package com.bananice.businesstracer.domain.repository.alert;

import com.bananice.businesstracer.domain.model.alert.AlertRule;
import com.bananice.businesstracer.domain.model.alert.AlertScopeType;

import java.util.List;

/**
 * Repository interface for alert rules.
 */
public interface AlertRuleRepository {

    /**
     * Save (insert or update) an alert rule by scope uniqueness.
     */
    void save(AlertRule alertRule);

    /**
     * Find one rule by scope.
     */
    AlertRule findByScope(AlertScopeType scopeType, String flowCode, String scopeRef);

    /**
     * Find all rules.
     */
    List<AlertRule> findAll();
}
