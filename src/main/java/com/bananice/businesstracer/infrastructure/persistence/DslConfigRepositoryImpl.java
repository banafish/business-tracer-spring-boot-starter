package com.bananice.businesstracer.infrastructure.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bananice.businesstracer.domain.model.DslConfig;
import com.bananice.businesstracer.domain.model.DslNode;
import com.bananice.businesstracer.domain.repository.DslConfigRepository;
import com.bananice.businesstracer.infrastructure.persistence.mapper.FlowDslMapper;
import com.bananice.businesstracer.infrastructure.persistence.po.FlowDslPO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Repository implementation for DSL configuration
 */
@Repository
@RequiredArgsConstructor
public class DslConfigRepositoryImpl implements DslConfigRepository {

    private final FlowDslMapper flowDslMapper;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public List<DslConfig> findAll() {
        List<FlowDslPO> poList = flowDslMapper.selectList(null);
        return poList.stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public DslConfig findByFlowCode(String flowCode) {
        LambdaQueryWrapper<FlowDslPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FlowDslPO::getFlowCode, flowCode);
        FlowDslPO po = flowDslMapper.selectOne(wrapper);
        return po != null ? toDomain(po) : null;
    }

    @Override
    public DslConfig save(DslConfig dslConfig) {
        FlowDslPO po = toPO(dslConfig);
        flowDslMapper.insert(po);
        dslConfig.setId(po.getId());
        dslConfig.setCreateTime(po.getCreateTime());
        dslConfig.setUpdateTime(po.getUpdateTime());
        return dslConfig;
    }

    @Override
    public DslConfig update(DslConfig dslConfig) {
        LambdaQueryWrapper<FlowDslPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FlowDslPO::getFlowCode, dslConfig.getFlowCode());
        FlowDslPO existingPO = flowDslMapper.selectOne(wrapper);

        if (existingPO == null) {
            return null;
        }

        existingPO.setName(dslConfig.getName());
        existingPO.setLayout(dslConfig.getLayout());
        existingPO.setNodesJson(serializeNodesJson(dslConfig));

        flowDslMapper.updateById(existingPO);
        return toDomain(existingPO);
    }

    @Override
    public boolean deleteByFlowCode(String flowCode) {
        LambdaQueryWrapper<FlowDslPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FlowDslPO::getFlowCode, flowCode);
        int deleted = flowDslMapper.delete(wrapper);
        return deleted > 0;
    }

    @Override
    public boolean existsByFlowCode(String flowCode) {
        LambdaQueryWrapper<FlowDslPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FlowDslPO::getFlowCode, flowCode);
        return flowDslMapper.selectCount(wrapper) > 0;
    }

    /**
     * Convert PO to Domain Model
     */
    private DslConfig toDomain(FlowDslPO po) {
        DslConfig config = new DslConfig();
        config.setId(po.getId());
        config.setFlowCode(po.getFlowCode());
        config.setName(po.getName());
        config.setLayout(po.getLayout());
        try {
            if (po.getNodesJson() != null) {
                config.setRawNodesJson(objectMapper.readValue(po.getNodesJson(), Object.class));
            }
        } catch (JsonProcessingException e) {
            config.setRawNodesJson(po.getNodesJson());
        }
        config.setNodes(parseNodesFromJson(po.getNodesJson()));
        config.setCreateTime(po.getCreateTime());
        config.setUpdateTime(po.getUpdateTime());
        return config;
    }

    /**
     * Parse nodes from JSON - handles both Drawflow format and legacy array format
     */
    private List<DslNode> parseNodesFromJson(String json) {
        if (json == null || json.isEmpty()) {
            return Collections.emptyList();
        }
        try {
            // Check if it's Drawflow format (contains "drawflow" key)
            if (json.trim().startsWith("{") && json.contains("\"drawflow\"")) {
                return extractNodesFromDrawflow(json);
            } else {
                return objectMapper.readValue(json, new TypeReference<List<DslNode>>() {
                });
            }
        } catch (JsonProcessingException e) {
            return Collections.emptyList();
        }
    }

    /**
     * Extract nodes from Drawflow JSON format for tree structure
     */
    @SuppressWarnings("unchecked")
    private List<DslNode> extractNodesFromDrawflow(String json) {
        try {
            Map<String, Object> drawflowData = objectMapper.readValue(json,
                    new TypeReference<Map<String, Object>>() {
                    });

            Map<String, Object> drawflow = (Map<String, Object>) drawflowData.get("drawflow");
            if (drawflow == null)
                return Collections.emptyList();

            Map<String, Object> home = (Map<String, Object>) drawflow.get("Home");
            if (home == null)
                return Collections.emptyList();

            Map<String, Object> data = (Map<String, Object>) home.get("data");
            if (data == null)
                return Collections.emptyList();

            // Build nodes from Drawflow data
            List<DslNode> nodes = new ArrayList<>();
            Map<String, String> nodeParentMap = new HashMap<>(); // nodeId -> parentNodeId

            // First pass: collect all nodes and their connections
            for (Map.Entry<String, Object> entry : data.entrySet()) {
                Map<String, Object> nodeData = (Map<String, Object>) entry.getValue();
                Map<String, Object> inputs = (Map<String, Object>) nodeData.get("inputs");

                if (inputs != null) {
                    for (Object inputObj : inputs.values()) {
                        Map<String, Object> input = (Map<String, Object>) inputObj;
                        List<Map<String, Object>> connections = (List<Map<String, Object>>) input
                                .get("connections");
                        if (connections != null && !connections.isEmpty()) {
                            String parentId = String.valueOf(connections.get(0).get("node"));
                            nodeParentMap.put(entry.getKey(), parentId);
                        }
                    }
                }
            }

            // Find root nodes (nodes without parents)
            for (Map.Entry<String, Object> entry : data.entrySet()) {
                if (!nodeParentMap.containsKey(entry.getKey())) {
                    DslNode node = buildDslNodeFromDrawflow(entry.getKey(), data, nodeParentMap);
                    if (node != null) {
                        nodes.add(node);
                    }
                }
            }

            return nodes;
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    @SuppressWarnings("unchecked")
    private DslNode buildDslNodeFromDrawflow(String nodeId, Map<String, Object> data,
            Map<String, String> nodeParentMap) {
        Map<String, Object> nodeData = (Map<String, Object>) data.get(nodeId);
        if (nodeData == null)
            return null;

        Map<String, Object> nodeInfo = (Map<String, Object>) nodeData.get("data");
        String code = nodeInfo != null ? (String) nodeInfo.get("code") : (String) nodeData.get("name");

        DslNode node = new DslNode();
        node.setCode(code);

        if (nodeInfo != null && nodeInfo.containsKey("isEndNode")) {
            Object isEndNode = nodeInfo.get("isEndNode");
            node.setIsEndNode(Boolean.valueOf(String.valueOf(isEndNode)));
        }

        // Find children
        List<DslNode> children = new ArrayList<>();
        for (Map.Entry<String, String> entry : nodeParentMap.entrySet()) {
            if (nodeId.equals(entry.getValue())) {
                DslNode child = buildDslNodeFromDrawflow(entry.getKey(), data, nodeParentMap);
                if (child != null) {
                    children.add(child);
                }
            }
        }

        if (!children.isEmpty()) {
            node.setChildren(children);
        }

        return node;
    }

    /**
     * Serialize rawNodesJson or nodes to JSON string
     */
    private String serializeNodesJson(DslConfig config) {
        if (config.getRawNodesJson() != null) {
            try {
                if (config.getRawNodesJson() instanceof String) {
                    return (String) config.getRawNodesJson();
                } else {
                    return objectMapper.writeValueAsString(config.getRawNodesJson());
                }
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Failed to serialize rawNodesJson", e);
            }
        }
        return nodesToJson(config.getNodes());
    }

    /**
     * Convert Domain Model to PO
     */
    private FlowDslPO toPO(DslConfig config) {
        FlowDslPO po = new FlowDslPO();
        po.setId(config.getId());
        po.setFlowCode(config.getFlowCode());
        po.setName(config.getName());
        po.setLayout(config.getLayout());
        po.setNodesJson(serializeNodesJson(config));
        return po;
    }

    /**
     * Convert nodes list to JSON string
     */
    private String nodesToJson(List<DslNode> nodes) {
        if (nodes == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(nodes);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize nodes to JSON", e);
        }
    }
}
