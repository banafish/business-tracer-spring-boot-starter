package com.bananice.businesstracer.domain.model;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DSL Configuration - defines how nodes should be displayed
 */
@Data
public class DslConfig {

    /**
     * Database ID
     */
    private Long id;

    /**
     * Unique identifier for this DSL configuration
     */
    private String flowCode;

    /**
     * DSL configuration name
     */
    private String name;

    /**
     * Layout type: tree, timeline, flow
     */
    private String layout = "timeline";

    /**
     * Node arrangement list (parsed from nodesJson for tree structure)
     */
    private List<DslNode> nodes;

    /**
     * Raw nodes JSON string (Drawflow format for flow layout)
     * This is used for direct rendering in Drawflow
     */
    private Object rawNodesJson;

    /**
     * Creation time
     */
    private LocalDateTime createTime;

    /**
     * Update time
     */
    private LocalDateTime updateTime;
}
