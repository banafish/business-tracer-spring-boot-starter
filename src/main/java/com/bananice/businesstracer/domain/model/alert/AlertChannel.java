package com.bananice.businesstracer.domain.model.alert;

import lombok.Builder;
import lombok.Data;

/**
 * Alert channel domain entity.
 */
@Data
@Builder
public class AlertChannel {
    private Long id;
    private String name;
    private AlertChannelType channelType;
    private String target;
    private Boolean enabled;
}
