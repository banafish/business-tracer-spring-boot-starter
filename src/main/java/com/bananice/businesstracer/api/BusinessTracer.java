package com.bananice.businesstracer.api;

import com.bananice.businesstracer.domain.model.DetailLog;
import com.bananice.businesstracer.domain.model.TraceStatus;
import com.bananice.businesstracer.application.TraceAsyncLogService;
import com.bananice.businesstracer.infrastructure.context.TraceContext;
import com.bananice.businesstracer.infrastructure.context.TraceContextHolder;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Static API for recording detailed logs within a traced flow.
 */
@Component
public class BusinessTracer implements ApplicationContextAware {

    private static ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(@NonNull ApplicationContext context) throws BeansException {
        applicationContext = context;
    }

    private static TraceAsyncLogService getTraceAsyncLogService() {
        if (applicationContext == null) {
            return null;
        }
        return applicationContext.getBean(TraceAsyncLogService.class);
    }

    /**
     * Record a detail log for the current business flow.
     *
     * @param content Detail content
     */
    public static void record(String content) {
        TraceContext context = TraceContextHolder.getContext();
        if (context == null) {
            return;
        }

        DetailLog log = DetailLog.builder()
                .businessId(context.getBusinessId())
                .parentNodeId(context.getNodeId())
                .content(content)
                .status(TraceStatus.NORMAL.getValue())
                .createTime(LocalDateTime.now())
                .build();

        TraceAsyncLogService service = getTraceAsyncLogService();
        if (service != null) {
            service.saveDetailLogAsync(log);
        }
    }

    /**
     * Record an error detail log for the current business flow.
     * This will:
     * 1. Save a detail log with status=FAILED
     * 2. Mark the current node (Flow Node) as FAILED via TraceContext flag
     * 3. The aspect will subsequently mark associated flow logs as FAILED
     *
     * @param content Error detail content
     */
    public static void recordError(String content) {
        TraceContext context = TraceContextHolder.getContext();
        if (context == null) {
            return;
        }

        DetailLog log = DetailLog.builder()
                .businessId(context.getBusinessId())
                .parentNodeId(context.getNodeId())
                .content(content)
                .status(TraceStatus.FAILED.getValue())
                .createTime(LocalDateTime.now())
                .build();

        TraceAsyncLogService service = getTraceAsyncLogService();
        if (service != null) {
            service.saveDetailLogAsync(log);
        }

        // Set flag so the aspect knows to mark node and flows as FAILED
        context.setErrorRecorded(true);
    }
}
