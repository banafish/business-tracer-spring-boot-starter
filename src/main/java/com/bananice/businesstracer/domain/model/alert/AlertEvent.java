package com.bananice.businesstracer.domain.model.alert;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Alert event domain entity.
 */
@Data
@Builder
public class AlertEvent {
    private Long id;
    private Long ruleId;
    private AlertType alertType;
    private AlertStatus status;
    private String aggregateKey;
    private String businessId;
    private String flowCode;
    private String nodeCode;
    private String traceId;
    private String message;
    private LocalDateTime occurredAt;
}
