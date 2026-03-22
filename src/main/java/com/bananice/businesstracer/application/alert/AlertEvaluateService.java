package com.bananice.businesstracer.application.alert;

import com.bananice.businesstracer.domain.model.NodeLog;
import com.bananice.businesstracer.domain.model.alert.AlertEvent;
import com.bananice.businesstracer.domain.model.alert.AlertStatus;
import com.bananice.businesstracer.domain.model.alert.AlertType;
import com.bananice.businesstracer.domain.repository.alert.AlertEventRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Evaluate runtime trace signals into alert events.
 */
@Service
public class AlertEvaluateService {

    private final AlertEventRepository alertEventRepository;
    private final long slowNodeThresholdMs;

    public AlertEvaluateService(AlertEventRepository alertEventRepository,
                                @Value("${business-tracer.alert.slow-node-threshold-ms:2000}") long slowNodeThresholdMs) {
        this.alertEventRepository = alertEventRepository;
        this.slowNodeThresholdMs = slowNodeThresholdMs;
    }

    public List<AlertEvent> evaluateNode(NodeLog nodeLog) {
        List<AlertEvent> events = new ArrayList<>();
        if (nodeLog == null) {
            return events;
        }

        if ("FAILED".equalsIgnoreCase(nodeLog.getStatus())) {
            events.add(buildNodeEvent(nodeLog, AlertType.NODE_FAILED, "node failed"));
        }
        if (nodeLog.getCostTime() != null && nodeLog.getCostTime() > slowNodeThresholdMs) {
            events.add(buildNodeEvent(nodeLog, AlertType.SLOW_NODE,
                    "node slow, cost=" + nodeLog.getCostTime() + "ms"));
        }
        return events;
    }

    public void openOrUpdateFlowStuck(String flowCode, String businessId, String traceId, LocalDateTime occurredAt) {
        if (!StringUtils.hasText(flowCode) || !StringUtils.hasText(businessId)) {
            return;
        }
        LocalDateTime actualOccurredAt = occurredAt == null ? LocalDateTime.now() : occurredAt;
        AlertEvent openEvent = alertEventRepository.findOpenFlowStuck(flowCode, businessId);
        String message = "flow stuck";

        if (openEvent == null) {
            AlertEvent event = AlertEvent.builder()
                    .alertType(AlertType.FLOW_STUCK)
                    .status(AlertStatus.NEW)
                    .flowCode(flowCode)
                    .businessId(businessId)
                    .traceId(traceId)
                    .message(message)
                    .occurredAt(actualOccurredAt)
                    .aggregateKey(flowCode + ":" + businessId + ":" + AlertType.FLOW_STUCK.name())
                    .build();
            alertEventRepository.save(event);
            return;
        }

        alertEventRepository.updateOpenFlowStuck(openEvent.getId(), message, actualOccurredAt);
    }

    public void closeFlowStuck(String flowCode, String businessId, LocalDateTime closedAt) {
        if (!StringUtils.hasText(flowCode) || !StringUtils.hasText(businessId)) {
            return;
        }
        AlertEvent openEvent = alertEventRepository.findOpenFlowStuck(flowCode, businessId);
        if (openEvent == null) {
            return;
        }
        alertEventRepository.closeFlowStuck(openEvent.getId(), closedAt == null ? LocalDateTime.now() : closedAt);
    }

    private AlertEvent buildNodeEvent(NodeLog nodeLog, AlertType alertType, String message) {
        return AlertEvent.builder()
                .alertType(alertType)
                .status(AlertStatus.NEW)
                .businessId(nodeLog.getBusinessId())
                .flowCode(null)
                .nodeCode(nodeLog.getCode())
                .traceId(nodeLog.getTraceId())
                .message(message)
                .occurredAt(nodeLog.getCreateTime() == null ? LocalDateTime.now() : nodeLog.getCreateTime())
                .aggregateKey(nodeLog.getBusinessId() + ":" + nodeLog.getCode() + ":" + alertType.name())
                .build();
    }
}
