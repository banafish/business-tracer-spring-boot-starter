package com.bananice.businesstracer.infrastructure.alert.channel;

import com.bananice.businesstracer.domain.model.alert.AlertChannel;
import com.bananice.businesstracer.domain.model.alert.AlertChannelType;
import com.bananice.businesstracer.domain.model.alert.AlertEvent;
import org.springframework.stereotype.Component;

/**
 * WeCom sender implementation.
 */
@Component
public class WecomAlertChannelSender implements AlertChannelSender {

    @Override
    public boolean supports(AlertChannelType channelType) {
        return AlertChannelType.WECOM == channelType;
    }

    @Override
    public String send(AlertChannel channel, AlertEvent event) {
        return "WECOM_OK";
    }
}
