package com.bananice.businesstracer.infrastructure.registry;

import lombok.Data;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry for storing all @BusinessTrace annotated node information.
 * Nodes are registered during application startup by BusinessTraceScanner.
 */
@Component
public class BusinessTraceRegistry {

    /**
     * Stores node info by code
     * Key: node code
     * Value: NodeInfo (code, name)
     */
    private final Map<String, NodeInfo> nodes = new ConcurrentHashMap<>();

    /**
     * Register a node
     * 
     * @param code node code
     * @param name node display name
     */
    public void register(String code, String name) {
        nodes.put(code, new NodeInfo(code, name));
    }

    /**
     * Get all registered nodes
     * 
     * @return collection of all node info
     */
    public Collection<NodeInfo> getAllNodes() {
        return nodes.values();
    }

    /**
     * Get node info by code
     * 
     * @param code node code
     * @return NodeInfo or null if not found
     */
    public NodeInfo getNode(String code) {
        return nodes.get(code);
    }

    /**
     * Check if a node is registered
     * 
     * @param code node code
     * @return true if registered
     */
    public boolean hasNode(String code) {
        return nodes.containsKey(code);
    }

    /**
     * Node information
     */
    @Data
    @AllArgsConstructor
    public static class NodeInfo {
        private String code;
        private String name;
    }
}
