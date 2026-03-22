package com.bananice.businesstracer.application.dto.alert;

import com.bananice.businesstracer.domain.model.alert.AlertType;
import lombok.Data;

@Data
public class AlertRuleUpsertRequest {

    private String name;
    private AlertType alertType;
    private String flowCode;
    private Boolean enabled;
}
