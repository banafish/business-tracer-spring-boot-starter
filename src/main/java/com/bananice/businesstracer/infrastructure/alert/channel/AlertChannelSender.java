package com.bananice.businesstracer.infrastructure.alert.channel;

import com.bananice.businesstracer.domain.model.alert.AlertChannel;
import com.bananice.businesstracer.domain.model.alert.AlertChannelType;
import com.bananice.businesstracer.domain.model.alert.AlertEvent;

/**
 * Alert channel sender abstraction.
 */
public interface AlertChannelSender {

    /**
     * Whether this sender supports the specified channel type.
     */
    boolean supports(AlertChannelType channelType);

    /**
     * Send alert event to channel and return provider response.
     */
    String send(AlertChannel channel, AlertEvent event);
}
