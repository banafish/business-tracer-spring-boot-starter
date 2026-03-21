package com.bananice.businesstracer.domain.repository.alert;

import com.bananice.businesstracer.domain.model.alert.AlertEvent;
import com.bananice.businesstracer.domain.model.alert.AlertStatus;

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
     * Find alert events by status with pagination.
     */
    List<AlertEvent> findByStatus(AlertStatus status, int pageNum, int pageSize);
}
