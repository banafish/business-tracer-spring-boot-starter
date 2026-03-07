package com.bananice.businesstracer.infrastructure.context;

import org.slf4j.MDC;
import org.springframework.core.task.TaskDecorator;
import org.springframework.lang.NonNull;

import java.util.Map;

/**
 * TaskDecorator to propagate TraceContext and MDC to async threads.
 */
public class TraceContextTaskDecorator implements TaskDecorator {

    @NonNull
    @Override
    public Runnable decorate(@NonNull Runnable runnable) {
        // Capture context from the submitting thread
        TraceContext context = TraceContextHolder.getContext();
        Map<String, String> mdcContextMap = MDC.getCopyOfContextMap();

        return () -> {
            try {
                // Restore context in the executing thread
                TraceContextHolder.setContext(context);
                if (mdcContextMap != null) {
                    MDC.setContextMap(mdcContextMap);
                }
                runnable.run();
            } finally {
                // Clear context to prevent thread reuse leak
                TraceContextHolder.clear();
                MDC.clear();
            }
        };
    }
}
