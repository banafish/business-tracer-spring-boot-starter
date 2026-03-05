package com.bananice.businesstracer.domain.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Collections;

/**
 * DSL Node - represents a node in the visualization layout
 */
@Data
public class DslNode {
    /**
     * Node code - references the code attribute in @BusinessTrace
     */
    private String code;

    /**
     * Is this an end node of the flow
     */
    private Boolean isEndNode;

    /**
     * Child nodes
     */
    private List<DslNode> children;

    // ==================== Domain Behavior ====================

    /**
     * Recursively check if this node or its children contain a specific code
     */
    public boolean containsCode(String targetCode) {
        if (targetCode == null) {
            return false;
        }
        if (targetCode.equals(this.code)) {
            return true;
        }
        if (children != null) {
            for (DslNode child : children) {
                if (child.containsCode(targetCode)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Recursively collect all codes from this node and its children
     */
    public Set<String> collectAllCodes() {
        Set<String> codes = new HashSet<>();
        if (this.code != null) {
            codes.add(this.code);
        }
        if (children != null) {
            for (DslNode child : children) {
                codes.addAll(child.collectAllCodes());
            }
        }
        return codes;
    }

    /**
     * Recursively find all end node codes from this node and its children
     */
    public List<String> findEndNodeCodes() {
        List<String> endNodes = new ArrayList<>();
        if (Boolean.TRUE.equals(this.isEndNode)) {
            endNodes.add(this.code);
        }
        if (children != null) {
            for (DslNode child : children) {
                endNodes.addAll(child.findEndNodeCodes());
            }
        }
        return endNodes;
    }

    // ==================== Static Utility for List<DslNode> ====================

    /**
     * Check if a node list contains a specific code (recursive)
     */
    public static boolean listContainsCode(List<DslNode> nodes, String code) {
        if (nodes == null) {
            return false;
        }
        for (DslNode node : nodes) {
            if (node.containsCode(code)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Collect all codes from a node list (recursive)
     */
    public static Set<String> collectAllCodes(List<DslNode> nodes) {
        if (nodes == null) {
            return Collections.emptySet();
        }
        Set<String> codes = new HashSet<>();
        for (DslNode node : nodes) {
            codes.addAll(node.collectAllCodes());
        }
        return codes;
    }

    /**
     * Find all end node codes from a node list (recursive)
     */
    public static List<String> findAllEndNodeCodes(List<DslNode> nodes) {
        if (nodes == null) {
            return Collections.emptyList();
        }
        List<String> endNodes = new ArrayList<>();
        for (DslNode node : nodes) {
            endNodes.addAll(node.findEndNodeCodes());
        }
        return endNodes;
    }
}
