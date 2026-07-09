package com.bananice.businesstracer.application.dto.alert;

import com.bananice.businesstracer.domain.model.alert.AlertChannelType;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AlertChannelUpsertRequest {

    @NotBlank(message = "name is required")
    private String name;

    @NotNull(message = "channelType is required")
    private AlertChannelType channelType;

    @NotBlank(message = "target is required")
    private String target;

    @NotNull(message = "enabled is required")
    private Boolean enabled;
}
