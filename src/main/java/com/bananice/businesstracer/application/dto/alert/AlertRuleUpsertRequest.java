package com.bananice.businesstracer.application.dto.alert;

import com.bananice.businesstracer.domain.model.alert.AlertType;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class AlertRuleUpsertRequest {

    @NotBlank(message = "name is required")
    private String name;
    @NotNull(message = "alertType is required")
    private AlertType alertType;
    private String flowCode;
    @NotNull(message = "enabled is required")
    private Boolean enabled;
}
