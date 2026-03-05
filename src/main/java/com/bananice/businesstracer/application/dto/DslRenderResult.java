package com.bananice.businesstracer.application.dto;

import com.bananice.businesstracer.domain.model.NodeLog;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DSL 渲染结果 DTO
 * JSON key 与原有 Map 格式一致: {flowCode, dslName, layout, drawflowData, nodes,
 * orphans}
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DslRenderResult {
    private String flowCode;
    private String dslName;
    private String layout;
    private Object drawflowData;
    private List<DslRenderNode> nodes;
    private List<NodeLog> orphans;
}
