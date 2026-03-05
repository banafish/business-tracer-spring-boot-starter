package com.bananice.businesstracer.application;

import com.bananice.businesstracer.application.dto.DslRenderNode;
import com.bananice.businesstracer.application.dto.DslRenderResult;
import com.bananice.businesstracer.domain.model.DslConfig;
import com.bananice.businesstracer.domain.model.DslNode;
import com.bananice.businesstracer.domain.model.NodeLog;
import com.bananice.businesstracer.domain.repository.DslConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for DSL operations
 */
@Service
@RequiredArgsConstructor
public class DslService {

    private final DslConfigRepository dslConfigRepository;

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
                .filter(dsl -> DslNode.listContainsCode(dsl.getNodes(), nodeCode))
                .collect(Collectors.toList());
    }

    /**
     * Organize logs according to DSL structure
     * Returns a structured DTO based on the DSL configuration
     */
    public DslRenderResult renderByDsl(List<NodeLog> logs, DslConfig dsl) {
        DslRenderResult result = new DslRenderResult();
        result.setFlowCode(dsl != null ? dsl.getFlowCode() : null);
        result.setDslName(dsl != null ? dsl.getName() : null);
        result.setLayout(dsl != null ? dsl.getLayout() : "timeline");

        // Include raw drawflow data for flow layout rendering
        if (dsl != null && dsl.getRawNodesJson() != null) {
            result.setDrawflowData(dsl.getRawNodesJson());
        }

        if (dsl == null || dsl.getNodes() == null || dsl.getNodes().isEmpty()) {
            // No DSL, return logs as flat list
            List<DslRenderNode> flatNodes = logs.stream()
                    .map(log -> DslRenderNode.builder()
                            .code(log.getCode())
                            .logs(Collections.singletonList(log))
                            .build())
                    .collect(Collectors.toList());
            result.setNodes(flatNodes);
            return result;
        }

        // Group logs by code
        Map<String, List<NodeLog>> logsByCode = logs.stream()
                .filter(log -> log.getCode() != null)
                .collect(Collectors.groupingBy(NodeLog::getCode));

        // Build tree structure according to DSL
        List<DslRenderNode> treeNodes = new ArrayList<>();
        for (DslNode dslNode : dsl.getNodes()) {
            DslRenderNode node = buildNode(dslNode, logsByCode);
            if (node != null) {
                treeNodes.add(node);
            }
        }

        // Add orphan logs (not in DSL) at the end
        Set<String> dslCodes = DslNode.collectAllCodes(dsl.getNodes());
        List<NodeLog> orphans = logs.stream()
                .filter(log -> log.getCode() == null || !dslCodes.contains(log.getCode()))
                .collect(Collectors.toList());

        result.setNodes(treeNodes);
        result.setOrphans(orphans);

        return result;
    }

    private DslRenderNode buildNode(DslNode dslNode, Map<String, List<NodeLog>> logsByCode) {
        List<NodeLog> matchingLogs = logsByCode.getOrDefault(dslNode.getCode(), Collections.emptyList());

        DslRenderNode node = DslRenderNode.builder()
                .code(dslNode.getCode())
                .logs(matchingLogs)
                .build();

        if (dslNode.getChildren() != null && !dslNode.getChildren().isEmpty()) {
            List<DslRenderNode> childNodes = new ArrayList<>();
            for (DslNode child : dslNode.getChildren()) {
                DslRenderNode childNode = buildNode(child, logsByCode);
                if (childNode != null) {
                    childNodes.add(childNode);
                }
            }
            node.setChildren(childNodes);
        }

        return node;
    }
}
