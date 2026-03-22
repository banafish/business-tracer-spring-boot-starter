package com.bananice.businesstracer.infrastructure.alert.channel;

import com.bananice.businesstracer.domain.model.alert.AlertChannel;
import com.bananice.businesstracer.domain.model.alert.AlertChannelType;
import com.bananice.businesstracer.domain.model.alert.AlertEvent;
import org.springframework.stereotype.Component;

/**
 * DingTalk sender implementation.
 */
@Component
public class DingtalkAlertChannelSender implements AlertChannelSender {

    @Override
    public boolean supports(AlertChannelType channelType) {
        return AlertChannelType.DINGTALK == channelType;
    }

    @Override
    public String send(AlertChannel channel, AlertEvent event) {
        return "DINGTALK_OK";
    }
}
