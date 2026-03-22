package com.bananice.businesstracer.infrastructure.alert.channel;

import com.bananice.businesstracer.domain.model.alert.AlertChannel;
import com.bananice.businesstracer.domain.model.alert.AlertChannelType;
import com.bananice.businesstracer.domain.model.alert.AlertEvent;
import org.springframework.stereotype.Component;

/**
 * Webhook sender implementation.
 */
@Component
public class WebhookAlertChannelSender implements AlertChannelSender {

    @Override
    public boolean supports(AlertChannelType channelType) {
        return AlertChannelType.WEBHOOK == channelType;
    }

    @Override
    public String send(AlertChannel channel, AlertEvent event) {
        return "WEBHOOK_OK";
    }
}
