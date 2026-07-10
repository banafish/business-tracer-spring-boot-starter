package com.bananice.businesstracer.infrastructure.context;

import com.bananice.businesstracer.application.TraceContextPort;
import org.springframework.stereotype.Component;

/**
 * Bridges the application-level {@link TraceContextPort} to the thread-local
 * {@link TraceContextHolder}, keeping the holder an infrastructure detail.
 */
@Component
public class TraceContextHolderAdapter implements TraceContextPort {

    @Override
    public boolean hasActiveTrace() {
        return TraceContextHolder.getContext() != null;
    }

    @Override
    public String currentBusinessId() {
        return TraceContextHolder.getBusinessId();
    }

    @Override
    public String currentNodeId() {
        return TraceContextHolder.getNodeId();
    }

    @Override
    public void markErrorRecorded() {
        TraceContext context = TraceContextHolder.getContext();
        if (context != null) {
            context.setErrorRecorded(true);
        }
    }
}
