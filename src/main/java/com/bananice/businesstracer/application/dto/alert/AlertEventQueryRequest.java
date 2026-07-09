package com.bananice.businesstracer.application.dto.alert;

import com.bananice.businesstracer.domain.model.alert.AlertStatus;
import com.bananice.businesstracer.domain.model.alert.AlertType;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class AlertEventQueryRequest {

    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private AlertType alertType;
    private AlertStatus status;
    private String flowCode;
    private String nodeCode;
    private String businessId;
    private int pageNum;
    private int pageSize;
}
