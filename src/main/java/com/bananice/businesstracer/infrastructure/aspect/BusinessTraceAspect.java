package com.bananice.businesstracer.infrastructure.aspect;

import com.bananice.businesstracer.api.BusinessTrace;
import com.bananice.businesstracer.application.TraceAsyncLogService;
import com.bananice.businesstracer.domain.model.NodeLog;
import com.bananice.businesstracer.domain.model.TraceStatus;
import com.bananice.businesstracer.infrastructure.context.TraceContext;
import com.bananice.businesstracer.infrastructure.context.TraceContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.UUID;

@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class BusinessTraceAspect {

    private final SpelParser spelParser;
    private final TraceAsyncLogService traceAsyncLogService;

    @Value("${spring.application.name:unknown-service}")
    private String appName;

    @Around("@annotation(businessTrace)")
    public Object around(ProceedingJoinPoint point, BusinessTrace businessTrace) throws Throwable {
        long startTime = System.currentTimeMillis();
        MethodSignature signature = (MethodSignature) point.getSignature();
        Method method = signature.getMethod();
        Object[] args = point.getArgs();

        String businessId = resolveBusinessId(businessTrace, method, args);
        String inputParamsRaw = resolveInputParams(businessTrace, method, args);

        // Prepare Context
        TraceContext parentContext = TraceContextHolder.getContext();
        String traceId = resolveTraceId(parentContext);
        String nodeId = UUID.randomUUID().toString();
        String parentNodeId = (parentContext != null) ? parentContext.getNodeId() : null;
        String code = businessTrace.code();
        String name = StringUtils.hasText(businessTrace.name()) ? businessTrace.name() : code;

        TraceContext newContext = TraceContext.builder()
                .businessId(businessId)
                .code(code)
                .name(name)
                .traceId(traceId)
                .nodeId(nodeId)
                .build();

        TraceContextHolder.setContext(newContext);

        Object result = null;
        Throwable exceptionToThrow = null;
        try {
            result = point.proceed();
            return result;
        } catch (Throwable t) {
            exceptionToThrow = t;
            throw t;
        } finally {
            long costTime = System.currentTimeMillis() - startTime;
            String outputParamsRaw = resolveOutputParams(businessTrace, method, args, result, exceptionToThrow);
            NodeLog logRecord = buildNodeLog(businessId, code, name, traceId, nodeId, parentNodeId,
                    businessTrace, method, costTime, exceptionToThrow, newContext, inputParamsRaw, outputParamsRaw);
            saveAndRecordFlowLogs(logRecord, code, businessId, exceptionToThrow, newContext);
            restoreContext(parentContext);
        }
    }

    private String resolveBusinessId(BusinessTrace businessTrace, Method method, Object[] args) {
        try {
            String businessId = spelParser.parse(businessTrace.key(), method, args);
            if (StringUtils.hasText(businessId)) {
                return businessId;
            }
        } catch (Exception e) {
            log.error("Failed to parse BusinessTrace key SpEL: {}", businessTrace.key(), e);
        }
        return "UNKNOWN";
    }

    private String resolveTraceId(TraceContext parentContext) {
        return (parentContext != null && parentContext.getTraceId() != null)
                ? parentContext.getTraceId()
                : UUID.randomUUID().toString();
    }

    private String resolveInputParams(BusinessTrace businessTrace, Method method, Object[] args) {
        try {
            if (StringUtils.hasText(businessTrace.inputParams())) {
                return spelParser.parse(businessTrace.inputParams(), method, args);
            }
        } catch (Exception e) {
            log.warn("Failed to parse inputParams SpEL: {}", businessTrace.inputParams(), e);
        }
        return null;
    }

    private String resolveOutputParams(BusinessTrace businessTrace, Method method, Object[] args,
            Object result, Throwable exception) {
        if (exception != null) {
            return null;
        }
        try {
            if (StringUtils.hasText(businessTrace.outputParams())) {
                return spelParser.parse(businessTrace.outputParams(), method, args, result);
            }
        } catch (Exception e) {
            log.warn("Failed to parse outputParams SpEL: {}", businessTrace.outputParams(), e);
        }
        return null;
    }

    private NodeLog buildNodeLog(String businessId, String code, String name,
            String traceId, String nodeId, String parentNodeId,
            BusinessTrace businessTrace, Method method,
            long costTime, Throwable exception,
            TraceContext context, String inputParams, String outputParams) {
        boolean hasFailed = exception != null || context.isErrorRecorded();
        String status = hasFailed ? TraceStatus.FAILED.getValue() : TraceStatus.COMPLETED.getValue();
        String exceptionMsg = exception != null ? exception.toString() : null;

        return NodeLog.builder()
                .businessId(businessId)
                .code(code)
                .name(name)
                .traceId(traceId)
                .nodeId(nodeId)
                .parentNodeId(parentNodeId)
                .content(StringUtils.hasText(businessTrace.operation()) ? businessTrace.operation() : method.getName())
                .appName(appName)
                .status(status)
                .costTime(costTime)
                .exception(exceptionMsg)
                .inputParams(inputParams)
                .outputParams(outputParams)
                .createTime(LocalDateTime.now())
                .build();
    }

    private void saveAndRecordFlowLogs(NodeLog logRecord, String code, String businessId,
            Throwable exception, TraceContext context) {
        boolean hasFailed = exception != null || context.isErrorRecorded();
        traceAsyncLogService.saveNodeLogAndFlowLogsAsync(logRecord, code, businessId, hasFailed);
    }

    private void restoreContext(TraceContext parentContext) {
        if (parentContext != null) {
            TraceContextHolder.setContext(parentContext);
        } else {
            TraceContextHolder.clear();
        }
    }
}
