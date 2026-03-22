package com.bananice.businesstracer.infrastructure.alert.job;

import com.bananice.businesstracer.application.alert.AlertConfigCacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Job for syncing in-memory alert config cache from published version.
 */
@Component
@RequiredArgsConstructor
public class AlertConfigSyncJob {

    private final AlertConfigCacheService alertConfigCacheService;

    @Scheduled(fixedDelayString = "${business-tracer.alert.config-sync-fixed-delay-ms:5000}")
    public void syncAlertConfig() {
        alertConfigCacheService.syncIfVersionChanged();
    }
}
