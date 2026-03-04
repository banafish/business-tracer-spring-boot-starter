package com.bananice.businesstracer.domain.model;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * Flow Log Domain Entity - records the association between DSL flow and
 * business ID
 */
@Data
@Builder
public class FlowLog {
    /**
     * Primary Key
     */
    private Long id;

    /**
     * Flow Code - unique identifier of the DSL configuration
     */
    private String flowCode;

    /**
     * Flow Name - display name of the DSL configuration
     */
    private String name;

    /**
     * Business ID - the business key this flow is associated with
     */
    private String businessId;

    /**
     * Flow Status
     */
    private String status;

    /**
     * Creation Time
     */
    private LocalDateTime createTime;
}
