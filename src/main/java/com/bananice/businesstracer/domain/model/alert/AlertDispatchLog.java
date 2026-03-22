package com.bananice.businesstracer.domain.model.alert;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Alert dispatch log domain entity.
 */
@Data
@Builder
public class AlertDispatchLog {
    private Long id;
    private Long eventId;
    private Long channelId;
    private AlertStatus status;
    private String response;
    private LocalDateTime dispatchTime;
    private Integer retryCount;
}
