package com.bananice.businesstracer.domain.model;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * Detail Log Domain Entity
 */
@Data
@Builder
public class DetailLog {
    private Long id;
    private String businessId;
    private String parentNodeId;
    private String content;
    private String status;
    private LocalDateTime createTime;
}
