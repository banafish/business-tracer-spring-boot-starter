package com.bananice.businesstracer.application.alert;

import com.bananice.businesstracer.domain.model.alert.AlertChannel;
import com.bananice.businesstracer.domain.model.alert.AlertDispatchLog;
import com.bananice.businesstracer.domain.model.alert.AlertEvent;
import com.bananice.businesstracer.domain.model.alert.AlertStatus;
import com.bananice.businesstracer.domain.model.alert.AlertType;
import com.bananice.businesstracer.domain.repository.alert.AlertChannelRepository;
import com.bananice.businesstracer.domain.repository.alert.AlertDispatchLogRepository;
import com.bananice.businesstracer.infrastructure.alert.channel.AlertChannelSender;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.concurrent.TimeoutException;

/**
 * Dispatch alert events to enabled channels.
 */
@Service
public class AlertDispatchService {

    private final AlertChannelRepository alertChannelRepository;
    private final AlertDispatchLogRepository alertDispatchLogRepository;
    private final List<AlertChannelSender> channelSenders;
    private final long maxAttemptTimeoutMs;
    private final int maxRetries;

    public AlertDispatchService(AlertChannelRepository alertChannelRepository,
                                AlertDispatchLogRepository alertDispatchLogRepository,
                                List<AlertChannelSender> channelSenders,
                                @Value("${business-tracer.alert.dispatch-attempt-timeout-ms:1000}") long maxAttemptTimeoutMs,
                                @Value("${business-tracer.alert.dispatch-max-retries:1}") int maxRetries) {
        this.alertChannelRepository = alertChannelRepository;
        this.alertDispatchLogRepository = alertDispatchLogRepository;
        this.channelSenders = channelSenders;
        this.maxAttemptTimeoutMs = Math.max(1L, maxAttemptTimeoutMs);
        this.maxRetries = Math.max(0, maxRetries);
    }

    public void dispatchRealtime(AlertEvent event) {
        if (event == null) {
            return;
        }
        dispatchToChannels(event, alertChannelRepository.findEnabled());
    }

    public void dispatchToChannel(AlertEvent event, AlertChannel channel) {
        if (event == null || channel == null) {
            return;
        }
        dispatchToChannels(event, java.util.Collections.singletonList(channel));
    }

    public void dispatchAggregated(AlertAggregationService.AggregationResult aggregationResult) {
        if (aggregationResult == null || aggregationResult.getAggregateKey() == null) {
            return;
        }
        AlertEvent aggregatedEvent = AlertEvent.builder()
                .id(-1L)
                .alertType(aggregationResult.getAlertType())
                .status(AlertStatus.NEW)
                .aggregateKey(aggregationResult.getAggregateKey())
                .message(String.format("Aggregated alert %s count=%d window=[%s,%s)",
                        aggregationResult.getAggregateKey(),
                        aggregationResult.getCount(),
                        aggregationResult.getBucketStart(),
                        aggregationResult.getBucketEnd()))
                .occurredAt(aggregationResult.getBucketEnd())
                .build();
        dispatchRealtime(aggregatedEvent);
    }

    public boolean isWithinSilenceWindow(LocalTime now, LocalTime start, LocalTime end) {
        if (now == null || start == null || end == null) {
            return false;
        }
        if (start.equals(end)) {
            return false;
        }
        if (start.isBefore(end)) {
            return !now.isBefore(start) && now.isBefore(end);
        }
        return !now.isBefore(start) || now.isBefore(end);
    }

    private void dispatchToChannels(AlertEvent event, List<AlertChannel> channels) {
        if (channels == null || channels.isEmpty()) {
            return;
        }

        for (AlertChannel channel : channels) {
            AlertChannelSender sender = resolveSender(channel);
            if (sender == null) {
                continue;
            }
            int totalAttempts = Math.max(1, maxRetries + 1);
            for (int attempt = 0; attempt < totalAttempts; attempt++) {
                LocalDateTime dispatchTime = LocalDateTime.now();
                try {
                    long started = System.currentTimeMillis();
                    String response = sender.send(channel, event);
                    long elapsed = System.currentTimeMillis() - started;
                    if (elapsed > maxAttemptTimeoutMs) {
                        throw new TimeoutException("attempt timeout " + elapsed + "ms");
                    }
                    writeLog(event, channel, AlertStatus.SENT, response, dispatchTime, attempt);
                    break;
                } catch (Exception ex) {
                    writeLog(event, channel, AlertStatus.FAILED, ex.getMessage(), dispatchTime, attempt);
                }
            }
        }
    }

    private AlertChannelSender resolveSender(AlertChannel channel) {
        if (channel == null || channel.getChannelType() == null || channelSenders == null) {
            return null;
        }
        for (AlertChannelSender sender : channelSenders) {
            if (sender != null && sender.supports(channel.getChannelType())) {
                return sender;
            }
        }
        return null;
    }

    private void writeLog(AlertEvent event, AlertChannel channel, AlertStatus status,
                          String response, LocalDateTime dispatchTime, int retryCount) {
        AlertDispatchLog log = AlertDispatchLog.builder()
                .eventId(event.getId())
                .channelId(channel.getId())
                .status(status)
                .response(response)
                .dispatchTime(dispatchTime)
                .retryCount(retryCount)
                .build();
        alertDispatchLogRepository.save(log);
    }
}
