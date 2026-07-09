package com.bananice.businesstracer.domain.model;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

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
