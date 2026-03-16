package com.bananice.businesstracer.application;

import com.bananice.businesstracer.domain.model.DetailLog;
import com.bananice.businesstracer.domain.model.NodeLog;
import com.bananice.businesstracer.domain.repository.DetailLogRepository;
import com.bananice.businesstracer.domain.repository.NodeLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

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

    @Async("businessTracerTaskExecutor")
    public void saveNodeLogAndFlowLogsAsync(NodeLog logRecord, String code, String businessId, boolean hasFailed) {
        try {
            nodeLogRepository.save(logRecord);
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
}
