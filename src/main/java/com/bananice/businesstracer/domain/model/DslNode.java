package com.bananice.businesstracer.domain.model;

import lombok.Data;
import java.util.List;

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
}
