package com.bananice.businesstracer.infrastructure.aspect;

import com.bananice.businesstracer.api.BusinessTrace;
import com.bananice.businesstracer.application.FlowLogService;
import com.bananice.businesstracer.domain.model.NodeLog;
import com.bananice.businesstracer.domain.repository.NodeLogRepository;
import com.bananice.businesstracer.infrastructure.context.TraceContext;
import com.bananice.businesstracer.infrastructure.context.TraceContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.UUID;

@Aspect
@Component
@Slf4j
public class BusinessTraceAspect {

    @Resource
    private SpelParser spelParser;

    @Resource
    private NodeLogRepository nodeLogRepository;

    @Resource
    private FlowLogService flowLogService;

    @Value("${spring.application.name:unknown-service}")
    private String appName;

    @Around("@annotation(businessTrace)")
    public Object around(ProceedingJoinPoint point, BusinessTrace businessTrace) throws Throwable {
        long startTime = System.currentTimeMillis();
        MethodSignature signature = (MethodSignature) point.getSignature();
        Method method = signature.getMethod();
        Object[] args = point.getArgs();

        // 1. Resolve Business ID
        String businessId = null;
        try {
            businessId = spelParser.parse(businessTrace.key(), method, args);
        } catch (Exception e) {
            log.error("Failed to parse BusinessTrace key SpEL: {}", businessTrace.key(), e);
        }

        if (!StringUtils.hasText(businessId)) {
            // Fallback: If we can't get businessId, effectively we cannot trace
            // "correctly".
            // We might just proceed or generate a temp one.
            // For now, proceed but maybe skip recording if strict.
            // Let's log warning and proceed without recording or record with 'UNKNOWN'.
            businessId = "UNKNOWN";
        }

        String inputParamsRaw = null;
        try {
            if (StringUtils.hasText(businessTrace.inputParams())) {
                inputParamsRaw = spelParser.parse(businessTrace.inputParams(), method, args);
            }
        } catch (Exception e) {
            log.warn("Failed to parse inputParams SpEL: {}", businessTrace.inputParams(), e);
        }

        // 2. Prepare Context
        TraceContext parentContext = TraceContextHolder.getContext();
        String traceId = (parentContext != null && parentContext.getTraceId() != null)
                ? parentContext.getTraceId()
                : UUID.randomUUID().toString();
        String nodeId = UUID.randomUUID().toString();
        String parentNodeId = (parentContext != null) ? parentContext.getNodeId() : null;

        // 3. Resolve code and name
        String code = businessTrace.code();
        String name = StringUtils.hasText(businessTrace.name()) ? businessTrace.name() : code;

        // 4. Set New Context
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

            try {
                if (businessId != null && !"UNKNOWN".equals(businessId) && code != null) {
                    flowLogService.checkAndUpdateFlowStatusByNodeCode(businessId, code);
                }
            } catch (Exception e) {
                log.error("Failed to check and update flow status", e);
            }

            return result;
        } catch (Throwable t) {
            exceptionToThrow = t;
            throw t;
        } finally {
            long costTime = System.currentTimeMillis() - startTime;
            boolean hasFailed = exceptionToThrow != null || newContext.isErrorRecorded();
            String status = hasFailed ? "FAILED" : "COMPLETED";
            String exceptionMsg = exceptionToThrow != null ? exceptionToThrow.toString() : null;

            String outputParamsRaw = null;
            if (exceptionToThrow == null) {
                try {
                    if (StringUtils.hasText(businessTrace.outputParams())) {
                        outputParamsRaw = spelParser.parse(businessTrace.outputParams(), method, args, result);
                    }
                } catch (Exception e) {
                    log.warn("Failed to parse outputParams SpEL: {}", businessTrace.outputParams(), e);
                }
            }

            // 5. Save Node Log
            NodeLog logRecord = NodeLog.builder()
                    .businessId(businessId)
                    .code(code)
                    .name(name)
                    .traceId(traceId)
                    .nodeId(nodeId)
                    .parentNodeId(parentNodeId)
                    .content(StringUtils.hasText(businessTrace.operation()) ? businessTrace.operation()
                            : method.getName())
                    .appName(appName)
                    .status(status)
                    .costTime(costTime)
                    .exception(exceptionMsg)
                    .inputParams(inputParamsRaw)
                    .outputParams(outputParamsRaw)
                    .createTime(LocalDateTime.now())
                    .build();

            try {
                nodeLogRepository.save(logRecord);
                flowLogService.recordFlowLogs(code, businessId);
                if (hasFailed && businessId != null && !"UNKNOWN".equals(businessId)) {
                    flowLogService.markFlowsAsFailed(businessId);
                }
            } catch (Exception e) {
                log.error("Failed to save BusinessTrace log", e);
            }

            // 6. Cleanup / Restore
            if (parentContext != null) {
                TraceContextHolder.setContext(parentContext);
            } else {
                TraceContextHolder.clear();
            }
        }
    }
}
