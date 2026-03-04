package com.bananice.businesstracer.infrastructure.context;

import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Interceptor to extract Business Trace context from HTTP Headers.
 */
public class BusinessTraceInterceptor implements HandlerInterceptor {

    public static final String HEADER_BUSINESS_ID = "X-Business-Id";
    public static final String HEADER_TRACE_ID = "X-Trace-Id";
    public static final String HEADER_NODE_ID = "X-Parent-Node-Id"; // The caller's Node ID becomes Parent Node ID

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String businessId = request.getHeader(HEADER_BUSINESS_ID);
        
        if (StringUtils.hasText(businessId)) {
            String traceId = request.getHeader(HEADER_TRACE_ID);
            String parentNodeId = request.getHeader(HEADER_NODE_ID);
            
            TraceContext context = TraceContext.builder()
                    .businessId(businessId)
                    .traceId(traceId)
                    .nodeId(parentNodeId) // Incoming 'Parent' context. The next @BusinessTrace will take this as parent.
                    .build();
            
            TraceContextHolder.setContext(context);
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        TraceContextHolder.clear();
    }
}
