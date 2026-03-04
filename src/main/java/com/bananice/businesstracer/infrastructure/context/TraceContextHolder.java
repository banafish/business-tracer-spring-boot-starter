package com.bananice.businesstracer.infrastructure.context;

import org.slf4j.MDC;
import java.util.Optional;

public class TraceContextHolder {
    
    private static final ThreadLocal<TraceContext> CONTEXT = new ThreadLocal<>();

    public static final String MDC_BUSINESS_ID = "businessId";
    public static final String MDC_TRACE_ID = "traceId";
    
    public static void setContext(TraceContext context) {
        CONTEXT.set(context);
        if (context != null) {
            if (context.getBusinessId() != null) {
                MDC.put(MDC_BUSINESS_ID, context.getBusinessId());
            }
            if (context.getTraceId() != null) {
                MDC.put(MDC_TRACE_ID, context.getTraceId());
            }
        }
    }

    public static TraceContext getContext() {
        return CONTEXT.get();
    }

    public static void clear() {
        CONTEXT.remove();
        MDC.remove(MDC_BUSINESS_ID);
        MDC.remove(MDC_TRACE_ID);
    }
    
    public static String getBusinessId() {
        return Optional.ofNullable(getContext()).map(TraceContext::getBusinessId).orElse(null);
    }

    public static String getNodeId() {
        return Optional.ofNullable(getContext()).map(TraceContext::getNodeId).orElse(null);
    }
}
