package com.bananice.businesstracer.application.dto;

import com.bananice.businesstracer.domain.model.NodeLog;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DSL 渲染节点 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DslRenderNode {
    private String code;
    private List<NodeLog> logs;
    private List<DslRenderNode> children;
}
