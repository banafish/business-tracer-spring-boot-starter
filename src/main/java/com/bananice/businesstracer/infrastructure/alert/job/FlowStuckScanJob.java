package com.bananice.businesstracer.infrastructure.alert.job;

import com.bananice.businesstracer.application.FlowLogService;
import com.bananice.businesstracer.application.alert.AlertEvaluateService;
import com.bananice.businesstracer.config.BusinessTracerProperties;
import com.bananice.businesstracer.domain.model.FlowLog;
import com.bananice.businesstracer.domain.model.TraceStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

/**
 * Periodic scan job for opening/updating FLOW_STUCK events.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FlowStuckScanJob {

    private final FlowLogService flowLogService;
    private final AlertEvaluateService alertEvaluateService;
    private final BusinessTracerProperties properties;

    @Scheduled(fixedDelayString = "${business-tracer.alert.flow-stuck-scan-fixed-delay-ms:60000}")
    public void scanAndPublishFlowStuck() {
        Duration threshold = Duration.ofMillis(resolveThresholdMs());
        int batchSize = resolveBatchSize();

        List<FlowLog> stuckInProgressFlows = flowLogService.findStuckInProgressFlows(threshold, batchSize);
        for (FlowLog flowLog : stuckInProgressFlows) {
            try {
                alertEvaluateService.openOrUpdateFlowStuck(flowLog.getFlowCode(), flowLog.getBusinessId(), null, null);
            } catch (Exception e) {
                log.error("Failed to open/update flow-stuck alert for flowCode={}, businessId={}",
                        flowLog.getFlowCode(), flowLog.getBusinessId(), e);
            }
        }

        List<FlowLog> staleFlows = flowLogService.findStaleFlows(threshold, batchSize);
        for (FlowLog flowLog : staleFlows) {
            if (!TraceStatus.IN_PROGRESS.getValue().equalsIgnoreCase(flowLog.getStatus())) {
                try {
                    alertEvaluateService.closeFlowStuckByStatus(flowLog.getFlowCode(), flowLog.getBusinessId(), flowLog.getStatus(), null);
                } catch (Exception e) {
                    log.error("Failed to close flow-stuck alert for flowCode={}, businessId={}, status={}",
                            flowLog.getFlowCode(), flowLog.getBusinessId(), flowLog.getStatus(), e);
                }
            }
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
