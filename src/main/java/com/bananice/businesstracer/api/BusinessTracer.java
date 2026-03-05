package com.bananice.businesstracer.api;

import com.bananice.businesstracer.domain.model.DetailLog;
import com.bananice.businesstracer.domain.model.TraceStatus;
import com.bananice.businesstracer.domain.repository.DetailLogRepository;
import com.bananice.businesstracer.infrastructure.context.TraceContext;
import com.bananice.businesstracer.infrastructure.context.TraceContextHolder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Static API for recording detailed logs within a traced flow.
 */
@Component
public class BusinessTracer {

    private static BusinessTracer instance;

    private final DetailLogRepository repository;

    public BusinessTracer(DetailLogRepository repository) {
        this.repository = repository;
        BusinessTracer.instance = this;
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

        if (instance != null) {
            instance.repository.save(log);
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

        if (instance != null) {
            instance.repository.save(log);
        }

        // Set flag so the aspect knows to mark node and flows as FAILED
        context.setErrorRecorded(true);
    }
}
