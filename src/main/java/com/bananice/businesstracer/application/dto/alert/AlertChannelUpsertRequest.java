package com.bananice.businesstracer.application.dto.alert;

import com.bananice.businesstracer.domain.model.alert.AlertChannelType;
import lombok.Data;

@Data
public class AlertChannelUpsertRequest {

    private String name;
    private AlertChannelType channelType;
    private String target;
    private Boolean enabled;
}
