package com.bananice.businesstracer.infrastructure.alert.job;

import com.bananice.businesstracer.application.alert.AlertAggregationService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Periodic flush job for alert aggregation buckets.
 */
@Component
@RequiredArgsConstructor
public class AlertAggregationFlushJob {

    private final AlertAggregationService alertAggregationService;

    @Scheduled(fixedDelayString = "${business-tracer.alert.aggregation-flush-fixed-delay-ms:60000}")
    public void flushAggregationBuckets() {
        alertAggregationService.flush(null);
    }
}
