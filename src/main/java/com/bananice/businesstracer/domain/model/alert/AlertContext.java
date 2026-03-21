package com.bananice.businesstracer.domain.model.alert;

import lombok.Builder;
import lombok.Data;

/**
 * Alert context.
 */
@Data
@Builder
public class AlertContext {
    private String businessId;
    private String flowCode;
    private String nodeCode;
    private String traceId;
}
