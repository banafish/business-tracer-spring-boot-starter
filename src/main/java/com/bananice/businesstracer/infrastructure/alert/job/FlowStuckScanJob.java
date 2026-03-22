package com.bananice.businesstracer.infrastructure.alert.job;

import com.bananice.businesstracer.application.FlowLogService;
import com.bananice.businesstracer.application.alert.AlertEvaluateService;
import com.bananice.businesstracer.config.BusinessTracerProperties;
import com.bananice.businesstracer.domain.model.FlowLog;
import com.bananice.businesstracer.domain.model.TraceStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

/**
 * Periodic scan job for opening/updating FLOW_STUCK events.
 */
@Component
@RequiredArgsConstructor
public class FlowStuckScanJob {

    private final FlowLogService flowLogService;
    private final AlertEvaluateService alertEvaluateService;
    private final BusinessTracerProperties properties;

    @Scheduled(fixedDelayString = "${business-tracer.alert.flow-stuck-scan-fixed-delay-ms:60000}")
    public void scanAndPublishFlowStuck() {
        Duration threshold = Duration.ofMillis(resolveThresholdMs());
        List<FlowLog> stuckFlows = flowLogService.findStuckInProgressFlows(threshold, resolveBatchSize());
        for (FlowLog flowLog : stuckFlows) {
            if (!TraceStatus.IN_PROGRESS.getValue().equalsIgnoreCase(flowLog.getStatus())) {
                alertEvaluateService.closeFlowStuckByStatus(flowLog.getFlowCode(), flowLog.getBusinessId(), flowLog.getStatus(), null);
                continue;
            }
            alertEvaluateService.openOrUpdateFlowStuck(flowLog.getFlowCode(), flowLog.getBusinessId(), null, null);
        }
    }

    private long resolveThresholdMs() {
        Long configured = properties.getAlert().getFlowStuckThresholdMs();
        return configured == null || configured <= 0L ? 300000L : configured;
    }

    private int resolveBatchSize() {
        Integer configured = properties.getAlert().getFlowStuckScanBatchSize();
        return configured == null || configured <= 0 ? 200 : configured;
    }
}
