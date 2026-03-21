package com.bananice.businesstracer.domain.repository.alert;

import com.bananice.businesstracer.domain.model.alert.AlertRule;

import java.util.List;

/**
 * Repository interface for alert rules.
 */
public interface AlertRuleRepository {

    /**
     * Save an alert rule.
     */
    void save(AlertRule alertRule);

    /**
     * Find enabled alert rules.
     */
    List<AlertRule> findEnabled();
}
