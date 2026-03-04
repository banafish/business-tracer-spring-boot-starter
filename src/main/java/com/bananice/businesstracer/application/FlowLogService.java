package com.bananice.businesstracer.application;

import com.bananice.businesstracer.domain.model.DslConfig;
import com.bananice.businesstracer.domain.model.DslNode;
import com.bananice.businesstracer.domain.model.FlowLog;
import com.bananice.businesstracer.domain.model.NodeLog;
import com.bananice.businesstracer.domain.repository.FlowLogRepository;
import com.bananice.businesstracer.domain.repository.NodeLogRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Application service for Flow Log operations
 */
@Slf4j
@Service
public class FlowLogService {

    @Resource
    private FlowLogRepository flowLogRepository;

    @Resource
    private NodeLogRepository nodeLogRepository;

    @Resource
    private DslService dslService;

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
                    .status("IN_PROGRESS")
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
    public Map<String, Object> queryFlowLogs(String flowCode, String businessId, int pageNum, int pageSize) {
        List<FlowLog> logs = flowLogRepository.findAll(flowCode, businessId, pageNum, pageSize);
        long total = flowLogRepository.count(flowCode, businessId);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", total);
        result.put("pageNum", pageNum);
        result.put("pageSize", pageSize);
        result.put("list", logs.stream().map(this::toMap).collect(Collectors.toList()));

        return result;
    }

    private Map<String, Object> toMap(FlowLog log) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", log.getId());
        map.put("flowCode", log.getFlowCode());
        map.put("name", log.getName());
        map.put("businessId", log.getBusinessId());
        map.put("status", log.getStatus());
        map.put("createTime", log.getCreateTime() != null ? log.getCreateTime().toString() : null);
        return map;
    }

    /**
     * Check if all end nodes are executed and update flow status to COMPLETED
     */
    public void checkAndUpdateFlowStatus(String businessId, String flowCode) {
        List<FlowLog> logs = flowLogRepository.findAll(flowCode, businessId, 1, 1);
        if (logs.isEmpty() || "FAILED".equals(logs.get(0).getStatus())) {
            return;
        }

        if (businessId == null || flowCode == null)
            return;

        DslConfig dsl = dslService.getDslByFlowCode(flowCode);
        if (dsl == null || dsl.getNodes() == null || dsl.getNodes().isEmpty())
            return;

        List<String> endNodeCodes = findEndNodeCodes(dsl.getNodes());
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
            flowLogRepository.updateStatus(flowCode, businessId, "COMPLETED");
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

    private List<String> findEndNodeCodes(List<DslNode> nodes) {
        List<String> endNodes = new ArrayList<>();
        if (nodes == null)
            return endNodes;

        for (DslNode node : nodes) {
            if (Boolean.TRUE.equals(node.getIsEndNode())) {
                endNodes.add(node.getCode());
            }
            if (node.getChildren() != null) {
                endNodes.addAll(findEndNodeCodes(node.getChildren()));
            }
        }
        return endNodes;
    }

    /**
     * Mark all flows associated with this businessId as FAILED
     */
    public void markFlowsAsFailed(String businessId) {
        if (businessId == null)
            return;
        flowLogRepository.updateStatusByBusinessId(businessId, "FAILED");
    }
}
