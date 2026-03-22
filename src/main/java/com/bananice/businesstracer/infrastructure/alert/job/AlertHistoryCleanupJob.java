package com.bananice.businesstracer.infrastructure.alert.job;

import com.bananice.businesstracer.config.BusinessTracerProperties;
import com.bananice.businesstracer.domain.repository.alert.AlertEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Periodic cleanup job for alert history data retention.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AlertHistoryCleanupJob {

    private final AlertEventRepository alertEventRepository;
    private final BusinessTracerProperties properties;

    @Scheduled(fixedDelayString = "${business-tracer.alert.history-cleanup-fixed-delay-ms:3600000}")
    public void cleanupHistory() {
        int retentionDays = resolveRetentionDays();
        LocalDateTime cutoff = LocalDateTime.now().minusDays(retentionDays);
        alertEventRepository.deleteOlderThan(cutoff);
        log.debug("Alert history cleanup executed with retentionDays={}, cutoff={}", retentionDays, cutoff);
    }

    private int resolveRetentionDays() {
        Integer configured = properties.getAlert().getRetentionDays();
        return configured == null || configured <= 0 ? 30 : configured;
    }
}
