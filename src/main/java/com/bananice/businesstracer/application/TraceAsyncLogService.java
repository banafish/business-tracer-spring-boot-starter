package com.bananice.businesstracer.application;

import com.bananice.businesstracer.application.alert.AlertAggregationService;
import com.bananice.businesstracer.application.alert.AlertEvaluateService;
import com.bananice.businesstracer.domain.model.DetailLog;
import com.bananice.businesstracer.domain.model.NodeLog;
import com.bananice.businesstracer.domain.model.alert.AlertEvent;
import com.bananice.businesstracer.domain.repository.DetailLogRepository;
import com.bananice.businesstracer.domain.repository.NodeLogRepository;
import com.bananice.businesstracer.domain.repository.alert.AlertEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * Service dedicated to saving logs asynchronously to prevent blocking the main business execution thread.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TraceAsyncLogService {

    private final NodeLogRepository nodeLogRepository;
    private final DetailLogRepository detailLogRepository;
    private final FlowLogService flowLogService;
    private final AlertEvaluateService alertEvaluateService;
    private final AlertEventRepository alertEventRepository;
    private final AlertAggregationService alertAggregationService;

    @Async("businessTracerTaskExecutor")
    public void saveNodeLogAndFlowLogsAsync(NodeLog logRecord, String code, String businessId, boolean hasFailed) {
        try {
            nodeLogRepository.save(logRecord);
            try {
                produceNodeAlerts(logRecord);
            } catch (Exception e) {
                log.error("Failed to produce node alerts during async log save", e);
            }

            flowLogService.recordFlowLogs(code, businessId);

            if (businessId != null && !"UNKNOWN".equals(businessId)) {
                if (hasFailed) {
                    flowLogService.markFlowsAsFailed(businessId);
                } else {
                    flowLogService.checkAndUpdateFlowStatusByNodeCode(businessId, code);
                }
            }
        } catch (Exception e) {
            log.error("Failed to asynchronously save BusinessTrace log", e);
        }
    }

    @Async("businessTracerTaskExecutor")
    public void saveDetailLogAsync(DetailLog logRecord) {
        try {
            detailLogRepository.save(logRecord);
        } catch (Exception e) {
            log.error("Failed to asynchronously save BusinessTracer detail log", e);
        }
    }

    private void produceNodeAlerts(NodeLog nodeLog) {
        List<AlertEvent> events = alertEvaluateService.evaluateNode(nodeLog);
        if (events == null) {
            events = Collections.emptyList();
        }
        for (AlertEvent event : events) {
            String dedupKey = buildNodeAlertDedupKey(nodeLog.getNodeId(), event.getAlertType().name());
            if (alertEventRepository.existsByAggregateKey(dedupKey)) {
                continue;
            }
            event.setAggregateKey(dedupKey);
            alertEventRepository.save(event);
            alertAggregationService.aggregate(event);
        }
    }

    private String buildNodeAlertDedupKey(String nodeId, String alertTypeName) {
        if (nodeId == null || alertTypeName == null) {
            return null;
        }
        return nodeId + ":" + alertTypeName;
    }
}
