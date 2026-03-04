package com.bananice.businesstracer.application;

import com.bananice.businesstracer.domain.model.DslConfig;
import com.bananice.businesstracer.domain.model.DslNode;
import com.bananice.businesstracer.domain.model.NodeLog;
import com.bananice.businesstracer.domain.repository.DslConfigRepository;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for DSL operations
 */
@Service
public class DslService {

    @Resource
    private DslConfigRepository dslConfigRepository;

    /**
     * Get all DSL configurations
     */
    public List<DslConfig> getAllDsl() {
        return dslConfigRepository.findAll();
    }

    /**
     * Get DSL configuration by flowCode
     */
    public DslConfig getDslByFlowCode(String flowCode) {
        if (flowCode == null) {
            return null;
        }
        return dslConfigRepository.findByFlowCode(flowCode);
    }

    /**
     * Create a new DSL configuration
     */
    public DslConfig createDsl(DslConfig dslConfig) {
        if (dslConfigRepository.existsByFlowCode(dslConfig.getFlowCode())) {
            throw new IllegalArgumentException("DSL with flowCode '" + dslConfig.getFlowCode() + "' already exists");
        }
        return dslConfigRepository.save(dslConfig);
    }

    /**
     * Update an existing DSL configuration
     */
    public DslConfig updateDsl(String flowCode, DslConfig dslConfig) {
        if (!dslConfigRepository.existsByFlowCode(flowCode)) {
            throw new IllegalArgumentException("DSL with flowCode '" + flowCode + "' not found");
        }
        dslConfig.setFlowCode(flowCode);
        return dslConfigRepository.update(dslConfig);
    }

    /**
     * Delete a DSL configuration
     */
    public boolean deleteDsl(String flowCode) {
        return dslConfigRepository.deleteByFlowCode(flowCode);
    }

    /**
     * Find all DSL configurations that contain a specific node code
     * 
     * @param nodeCode the code of the node to search for
     * @return list of DSL configurations containing this node code
     */
    public List<DslConfig> findDslsByNodeCode(String nodeCode) {
        if (nodeCode == null) {
            return Collections.emptyList();
        }
        return dslConfigRepository.findAll().stream()
                .filter(dsl -> containsNodeCode(dsl.getNodes(), nodeCode))
                .collect(Collectors.toList());
    }

    /**
     * Recursively check if a node list contains a specific code
     */
    private boolean containsNodeCode(List<DslNode> nodes, String nodeCode) {
        if (nodes == null) {
            return false;
        }
        for (DslNode node : nodes) {
            if (nodeCode.equals(node.getCode())) {
                return true;
            }
            if (node.getChildren() != null && containsNodeCode(node.getChildren(), nodeCode)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Organize logs according to DSL structure
     * Returns a tree structure based on the DSL configuration
     */
    public Map<String, Object> renderByDsl(List<NodeLog> logs, DslConfig dsl) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("flowCode", dsl != null ? dsl.getFlowCode() : null);
        result.put("dslName", dsl != null ? dsl.getName() : null);
        result.put("layout", dsl != null ? dsl.getLayout() : "timeline");

        // Include raw drawflow data for flow layout rendering
        if (dsl != null && dsl.getRawNodesJson() != null) {
            result.put("drawflowData", dsl.getRawNodesJson());
        }

        if (dsl == null || dsl.getNodes() == null || dsl.getNodes().isEmpty()) {
            // No DSL, return logs as flat list
            result.put("nodes", logs.stream().map(this::logToMap).collect(Collectors.toList()));
            return result;
        }

        // Group logs by code
        Map<String, List<NodeLog>> logsByCode = logs.stream()
                .filter(log -> log.getCode() != null)
                .collect(Collectors.groupingBy(NodeLog::getCode));

        // Build tree structure according to DSL
        List<Map<String, Object>> treeNodes = new ArrayList<>();
        for (DslNode dslNode : dsl.getNodes()) {
            Map<String, Object> node = buildNode(dslNode, logsByCode);
            if (node != null) {
                treeNodes.add(node);
            }
        }

        // Add orphan logs (not in DSL) at the end
        Set<String> dslCodes = collectAllCodes(dsl.getNodes());
        List<Map<String, Object>> orphans = logs.stream()
                .filter(log -> log.getCode() == null || !dslCodes.contains(log.getCode()))
                .map(this::logToMap)
                .collect(Collectors.toList());

        result.put("nodes", treeNodes);
        result.put("orphans", orphans);

        return result;
    }

    private Map<String, Object> buildNode(DslNode dslNode, Map<String, List<NodeLog>> logsByCode) {
        List<NodeLog> matchingLogs = logsByCode.getOrDefault(dslNode.getCode(), Collections.emptyList());

        Map<String, Object> node = new LinkedHashMap<>();
        node.put("code", dslNode.getCode());
        node.put("logs", matchingLogs.stream().map(this::logToMap).collect(Collectors.toList()));

        if (dslNode.getChildren() != null && !dslNode.getChildren().isEmpty()) {
            List<Map<String, Object>> childNodes = new ArrayList<>();
            for (DslNode child : dslNode.getChildren()) {
                Map<String, Object> childNode = buildNode(child, logsByCode);
                if (childNode != null) {
                    childNodes.add(childNode);
                }
            }
            node.put("children", childNodes);
        }

        return node;
    }

    private Set<String> collectAllCodes(List<DslNode> nodes) {
        Set<String> codes = new HashSet<>();
        if (nodes == null)
            return codes;

        for (DslNode node : nodes) {
            if (node.getCode() != null) {
                codes.add(node.getCode());
            }
            if (node.getChildren() != null) {
                codes.addAll(collectAllCodes(node.getChildren()));
            }
        }
        return codes;
    }

    private Map<String, Object> logToMap(NodeLog log) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", log.getId());
        map.put("businessId", log.getBusinessId());
        map.put("code", log.getCode());
        map.put("name", log.getName());
        map.put("traceId", log.getTraceId());
        map.put("nodeId", log.getNodeId());
        map.put("parentNodeId", log.getParentNodeId());
        map.put("content", log.getContent());
        map.put("appName", log.getAppName());
        map.put("status", log.getStatus());
        map.put("costTime", log.getCostTime());
        map.put("exception", log.getException());
        map.put("inputParams", log.getInputParams());
        map.put("outputParams", log.getOutputParams());
        map.put("createTime", log.getCreateTime() != null ? log.getCreateTime().toString() : null);
        return map;
    }
}
