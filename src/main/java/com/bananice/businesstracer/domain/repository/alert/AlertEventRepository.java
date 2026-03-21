package com.bananice.businesstracer.domain.repository.alert;

import com.bananice.businesstracer.domain.model.alert.AlertEvent;
import com.bananice.businesstracer.domain.model.alert.AlertStatus;
import com.bananice.businesstracer.domain.model.alert.AlertType;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository interface for alert events.
 */
public interface AlertEventRepository {

    /**
     * Save an alert event.
     */
    void save(AlertEvent alertEvent);

    /**
     * Query alert events with optional filters and pagination.
     */
    List<AlertEvent> query(LocalDateTime startTime, LocalDateTime endTime,
                           AlertType alertType, AlertStatus status,
                           String flowCode, String nodeCode, String businessId,
                           int pageNum, int pageSize);

    /**
     * Count alert events with optional filters.
     */
    long count(LocalDateTime startTime, LocalDateTime endTime,
               AlertType alertType, AlertStatus status,
               String flowCode, String nodeCode, String businessId);
}
