package com.bananice.businesstracer.infrastructure.context;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TraceContext {
    private String businessId;
    private String code;
    private String name;
    private String traceId;
    /**
     * The ID of the current Node (Method execution)
     */
    private String nodeId;
    /**
     * Flag indicating an error was recorded programmatically via
     * BusinessTracer.recordError().
     * The aspect checks this flag to mark the node status as FAILED.
     */
    @Builder.Default
    private boolean errorRecorded = false;
}
