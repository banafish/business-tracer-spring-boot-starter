package com.bananice.businesstracer.domain.model;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * Node Log Domain Entity
 */
@Data
@Builder
public class NodeLog {
    private Long id;
    private String businessId;
    private String code;
    private String name;
    private String traceId;
    private String nodeId;
    private String parentNodeId;
    private String content;
    private String appName;
    private String status;
    private Long costTime;
    private String exception;
    private String inputParams;
    private String outputParams;
    private LocalDateTime createTime;
}
