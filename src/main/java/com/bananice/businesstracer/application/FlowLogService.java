package com.bananice.businesstracer.application;

import com.bananice.businesstracer.application.dto.PageResult;
import com.bananice.businesstracer.domain.model.DslConfig;
import com.bananice.businesstracer.domain.model.DslNode;
import com.bananice.businesstracer.domain.model.FlowLog;
import com.bananice.businesstracer.domain.model.NodeLog;
import com.bananice.businesstracer.domain.model.TraceStatus;
import com.bananice.businesstracer.domain.repository.FlowLogRepository;
import com.bananice.businesstracer.domain.repository.NodeLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Application service for Flow Log operations
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FlowLogService {

    private final FlowLogRepository flowLogRepository;
    private final NodeLogRepository nodeLogRepository;
    private final DslService dslService;

    /**
     * Record flow logs for all DSLs that contain the given node code.
     * This method is called when a @BusinessTrace annotated method is executed.
     * It finds all DSL configurations containing the node code and creates
     * flow log entries for each, avoiding duplicates.
     *
     * @param nodeCode   the code from @BusinessTrace annotation
     * @param businessId the business ID from @BusinessTrace key
     */
    public void recordFlowLogs(String nodeCode, String businessId) {
        if (nodeCode == null || businessId == null) {
            return;
        }

        // Find all DSLs containing this node code
        List<DslConfig> matchingDsls = dslService.findDslsByNodeCode(nodeCode);

        for (DslConfig dsl : matchingDsls) {
            saveIfNotExists(dsl.getFlowCode(), dsl.getName(), businessId);
        }
    }

    /**
     * Save a flow log if it doesn't already exist (by flowCode + businessId
     * combination)
     */
    public void saveIfNotExists(String flowCode, String name, String businessId) {
        if (flowCode == null || businessId == null) {
            return;
        }

        try {
            // Check if already exists
            if (flowLogRepository.existsByFlowCodeAndBusinessId(flowCode, businessId)) {
                log.debug("Flow log already exists for flowCode={} and businessId={}", flowCode, businessId);
                return;
            }

            // Create and save new flow log
            FlowLog flowLog = FlowLog.builder()
                    .flowCode(flowCode)
                    .name(name)
                    .businessId(businessId)
                    .status(TraceStatus.IN_PROGRESS.getValue())
                    .createTime(LocalDateTime.now())
                    .build();

            flowLogRepository.save(flowLog);
            log.debug("Created flow log for flowCode={} and businessId={}", flowCode, businessId);
        } catch (Exception e) {
            // Log error but don't throw - flow log recording should not break business
            // logic
            log.error("Failed to save flow log for flowCode={} and businessId={}", flowCode, businessId, e);
        }
    }

    /**
     * Query flow logs with pagination
     */
    public PageResult<FlowLog> queryFlowLogs(String flowCode, String businessId, int pageNum, int pageSize) {
        List<FlowLog> logs = flowLogRepository.findAll(flowCode, businessId, pageNum, pageSize);
        long total = flowLogRepository.count(flowCode, businessId);
        return PageResult.of(total, pageNum, pageSize, logs);
    }

    /**
     * Check if all end nodes are executed and update flow status to COMPLETED
     */
    public void checkAndUpdateFlowStatus(String businessId, String flowCode) {
        List<FlowLog> logs = flowLogRepository.findAll(flowCode, businessId, 1, 1);
        if (logs.isEmpty() || TraceStatus.FAILED.getValue().equals(logs.get(0).getStatus())) {
            return;
        }

        if (businessId == null || flowCode == null)
            return;

        DslConfig dsl = dslService.getDslByFlowCode(flowCode);
        if (dsl == null || dsl.getNodes() == null || dsl.getNodes().isEmpty())
            return;

        List<String> endNodeCodes = DslNode.findAllEndNodeCodes(dsl.getNodes());
        if (endNodeCodes.isEmpty())
            return; // No end nodes defined

        List<NodeLog> nodeLogs = nodeLogRepository.findByBusinessId(businessId);
        List<String> executedCodes = nodeLogs.stream()
                .map(NodeLog::getCode)
                .filter(code -> code != null)
                .collect(Collectors.toList());

        boolean allEndNodesExecuted = endNodeCodes.stream().allMatch(executedCodes::contains);

        if (allEndNodesExecuted) {
            // Update flow status to COMPLETED
            flowLogRepository.updateStatus(flowCode, businessId, TraceStatus.COMPLETED.getValue());
        }
    }

    /**
     * Check and update flow status for all flows containing this node code
     */
    public void checkAndUpdateFlowStatusByNodeCode(String businessId, String nodeCode) {
        if (nodeCode == null || businessId == null) {
            return;
        }
        List<DslConfig> matchingDsls = dslService.findDslsByNodeCode(nodeCode);
        for (DslConfig dsl : matchingDsls) {
            checkAndUpdateFlowStatus(businessId, dsl.getFlowCode());
        }
    }

    /**
     * Mark all flows associated with this businessId as FAILED
     */
    public void markFlowsAsFailed(String businessId) {
        if (businessId == null)
            return;
        flowLogRepository.updateStatusByBusinessId(businessId, TraceStatus.FAILED.getValue());
    }

    /**
     * Find IN_PROGRESS flows that have exceeded the given max age.
     */
    public List<FlowLog> findStuckInProgressFlows(Duration maxAge, int limit) {
        if (maxAge == null || maxAge.isNegative() || maxAge.isZero()) {
            return Collections.emptyList();
        }
        LocalDateTime threshold = LocalDateTime.now().minus(maxAge);
        return flowLogRepository.findInProgressBefore(threshold, limit);
    }
}
