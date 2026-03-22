package com.bananice.businesstracer.application.alert;

import com.bananice.businesstracer.domain.model.alert.AlertRule;
import com.bananice.businesstracer.domain.model.alert.AlertScopeType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Resolve alert rules by precedence:
 * NODE(flow+node) > FLOW(flow) > GLOBAL.
 */
@Service
@RequiredArgsConstructor
public class AlertRuleResolveService {

    private static final String GLOBAL_SCOPE_REF = "GLOBAL";

    private final AlertConfigCacheService alertConfigCacheService;

    public AlertRule resolve(String flowCode, String nodeCode) {
        if (StringUtils.hasText(flowCode) && StringUtils.hasText(nodeCode)) {
            AlertRule nodeRule = alertConfigCacheService.findByScope(AlertScopeType.NODE, flowCode, nodeCode);
            if (nodeRule != null) {
                return nodeRule;
            }
        }

        if (StringUtils.hasText(flowCode)) {
            AlertRule flowRule = alertConfigCacheService.findByScope(AlertScopeType.FLOW, flowCode, flowCode);
            if (flowRule != null) {
                return flowRule;
            }
        }

        return alertConfigCacheService.findByScope(AlertScopeType.GLOBAL, null, GLOBAL_SCOPE_REF);
    }
}
