package com.bananice.businesstracer.application.alert;

import com.bananice.businesstracer.domain.model.alert.AlertEvent;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Aggregate alert events in fixed minute buckets.
 */
@Service
public class AlertAggregationService {

    private final int bucketMinutes;
    private final Map<BucketKey, AggregateCounter> counters = new LinkedHashMap<>();

    public AlertAggregationService(@Value("${business-tracer.alert.aggregation-bucket-minutes:5}") int bucketMinutes) {
        this.bucketMinutes = Math.max(1, bucketMinutes);
    }

    public synchronized void aggregate(AlertEvent alertEvent) {
        if (alertEvent == null || alertEvent.getOccurredAt() == null || alertEvent.getAggregateKey() == null) {
            return;
        }
        LocalDateTime bucketStart = bucketStart(alertEvent.getOccurredAt());
        BucketKey key = new BucketKey(alertEvent.getAggregateKey(), bucketStart);
        AggregateCounter counter = counters.get(key);
        if (counter == null) {
            counter = new AggregateCounter(alertEvent.getAggregateKey(), bucketStart);
            counters.put(key, counter);
        }
        counter.increment();
    }

    public synchronized List<AggregationResult> flush(LocalDateTime now) {
        LocalDateTime flushTime = now == null ? LocalDateTime.now() : now;
        List<AggregationResult> results = new ArrayList<>();
        List<BucketKey> toRemove = new ArrayList<>();

        for (Map.Entry<BucketKey, AggregateCounter> entry : counters.entrySet()) {
            AggregateCounter counter = entry.getValue();
            LocalDateTime bucketEnd = counter.getBucketStart().plusMinutes(bucketMinutes);
            if (!bucketEnd.isAfter(flushTime)) {
                results.add(new AggregationResult(counter.getAggregateKey(), counter.getCount(), counter.getBucketStart(), bucketEnd));
                toRemove.add(entry.getKey());
            }
        }
        for (BucketKey key : toRemove) {
            counters.remove(key);
        }
        return results;
    }

    private LocalDateTime bucketStart(LocalDateTime time) {
        int alignedMinute = (time.getMinute() / bucketMinutes) * bucketMinutes;
        return time.withMinute(alignedMinute).withSecond(0).withNano(0);
    }

    @Getter
    @AllArgsConstructor
    public static class AggregationResult {
        private final String aggregateKey;
        private final int count;
        private final LocalDateTime bucketStart;
        private final LocalDateTime bucketEnd;
    }

    private static class AggregateCounter {
        private final String aggregateKey;
        private final LocalDateTime bucketStart;
        private int count;

        private AggregateCounter(String aggregateKey, LocalDateTime bucketStart) {
            this.aggregateKey = aggregateKey;
            this.bucketStart = bucketStart;
        }

        private void increment() {
            count++;
        }

        public String getAggregateKey() {
            return aggregateKey;
        }

        public LocalDateTime getBucketStart() {
            return bucketStart;
        }

        public int getCount() {
            return count;
        }
    }

    @Getter
    @EqualsAndHashCode
    @AllArgsConstructor
    private static class BucketKey {
        private final String aggregateKey;
        private final LocalDateTime bucketStart;
    }
}
