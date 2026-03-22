package com.bananice.businesstracer.application.alert;

import com.bananice.businesstracer.domain.model.alert.AlertRule;
import com.bananice.businesstracer.domain.model.alert.AlertScopeType;
import com.bananice.businesstracer.domain.repository.alert.AlertConfigVersionRepository;
import com.bananice.businesstracer.domain.repository.alert.AlertRuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * In-memory cache service for alert rules.
 */
@Service
@RequiredArgsConstructor
public class AlertConfigCacheService {

    private final AlertRuleRepository alertRuleRepository;
    private final AlertConfigVersionRepository alertConfigVersionRepository;

    private final AtomicReference<List<AlertRule>> localCacheRef =
            new AtomicReference<>(Collections.emptyList());

    private volatile Long localPublishedVersion;

    public void saveRule(AlertRule alertRule) {
        alertRuleRepository.save(alertRule);
        refreshCache();
    }

    public AlertRule findByScope(AlertScopeType scopeType, String flowCode, String scopeRef) {
        if (scopeType == null || !StringUtils.hasText(scopeRef)) {
            return null;
        }

        List<AlertRule> rules = localCacheRef.get();
        for (AlertRule rule : rules) {
            if (rule == null || rule.getScopeType() != scopeType) {
                continue;
            }
            if (!scopeRef.equals(rule.getScopeRef())) {
                continue;
            }
            if (StringUtils.hasText(flowCode)) {
                if (!flowCode.equals(rule.getFlowCode())) {
                    continue;
                }
            } else if (StringUtils.hasText(rule.getFlowCode())) {
                continue;
            }
            return rule;
        }
        return null;
    }

    public void syncIfVersionChanged() {
        Long publishedVersion = alertConfigVersionRepository.getPublishedVersion();
        if (publishedVersion == null || publishedVersion.equals(localPublishedVersion)) {
            return;
        }
        refreshCache();
        localPublishedVersion = publishedVersion;
    }

    public void refreshCache() {
        List<AlertRule> latestRules = alertRuleRepository.findAll();
        if (latestRules == null || latestRules.isEmpty()) {
            localCacheRef.set(Collections.emptyList());
            return;
        }
        localCacheRef.set(new ArrayList<>(latestRules));
    }
}
