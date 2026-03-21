package com.bananice.businesstracer.domain.model.alert;

import lombok.Builder;
import lombok.Data;

/**
 * Alert rule domain entity.
 */
@Data
@Builder
public class AlertRule {
    private Long id;
    private String name;
    private AlertType alertType;
    private AlertScopeType scopeType;
    private String scopeRef;
    private String flowCode;
    private Boolean enabled;
}
