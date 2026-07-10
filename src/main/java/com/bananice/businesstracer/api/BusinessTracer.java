package com.bananice.businesstracer.api;

import com.bananice.businesstracer.application.TraceAsyncLogService;
import com.bananice.businesstracer.domain.model.DetailLog;
import com.bananice.businesstracer.domain.model.TraceStatus;
import com.bananice.businesstracer.infrastructure.context.TraceContext;
import com.bananice.businesstracer.infrastructure.context.TraceContextHolder;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.LocalDateTime;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

/**
 * Static API for recording detailed logs within a traced flow.
 */
@Component
public class BusinessTracer implements ApplicationContextAware {

    private static ApplicationContext applicationContext;

    @Override
    @SuppressFBWarnings(
            value = "ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD",
            justification = "标准的 Spring ApplicationContextAware 静态持有者模式：BusinessTracer 对外是静态门面"
                    + "（record/recordError），需要在启动期由 Spring 回调注入一次 context；"
                    + "该写入发生在容器刷新阶段、任何业务流量之前，属有意设计而非并发缺陷")
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
