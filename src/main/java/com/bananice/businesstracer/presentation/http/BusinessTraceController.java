package com.bananice.businesstracer.presentation.http;

import com.bananice.businesstracer.application.DslService;
import com.bananice.businesstracer.application.FlowLogService;
import com.bananice.businesstracer.domain.model.DslConfig;
import com.bananice.businesstracer.domain.model.DetailLog;
import com.bananice.businesstracer.domain.model.NodeLog;
import com.bananice.businesstracer.domain.repository.DetailLogRepository;
import com.bananice.businesstracer.domain.repository.NodeLogRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/business-tracer")
public class BusinessTraceController {

    @Resource
    private NodeLogRepository nodeLogRepository;

    @Resource
    private DetailLogRepository detailLogRepository;

    @Resource
    private DslService dslService;

    @Resource
    private FlowLogService flowLogService;

    /**
     * Get flow logs list
     */
    @GetMapping("/api/flow-logs")
    public Map<String, Object> getFlowLogs(
            @RequestParam(value = "flowCode", required = false) String flowCode,
            @RequestParam(value = "businessId", required = false) String businessId,
            @RequestParam(value = "pageNum", defaultValue = "1") int pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") int pageSize) {
        return flowLogService.queryFlowLogs(flowCode, businessId, pageNum, pageSize);
    }

    /**
     * Get trace with DSL structure
     */
    @GetMapping("/trace")
    public Map<String, Object> getTraceWithDsl(
            @RequestParam("businessId") String businessId,
            @RequestParam(value = "flowCode", required = false) String flowCode) {

        List<NodeLog> logs = nodeLogRepository.findByBusinessId(businessId);

        // If flowCode is not provided, try to find a matching DSL by the first log's
        // code
        if (flowCode == null && !logs.isEmpty()) {
            String firstCode = logs.stream()
                    .filter(log -> log.getCode() != null)
                    .map(NodeLog::getCode)
                    .findFirst()
                    .orElse(null);
            if (firstCode != null) {
                List<DslConfig> matchingDsls = dslService.findDslsByNodeCode(firstCode);
                if (!matchingDsls.isEmpty()) {
                    flowCode = matchingDsls.get(0).getFlowCode();
                }
            }
        }

        DslConfig dsl = dslService.getDslByFlowCode(flowCode);
        return dslService.renderByDsl(logs, dsl);
    }

    /**
     * Get detail logs by parentNodeId
     */
    @GetMapping("/trace/details")
    public List<DetailLog> getTraceDetails(
            @RequestParam("parentNodeId") String parentNodeId) {
        return detailLogRepository.findByParentNodeId(parentNodeId);
    }
}
