package com.bananice.businesstracer.domain.repository.alert;

import com.bananice.businesstracer.domain.model.alert.AlertDispatchLog;

import java.util.List;

/**
 * Repository interface for alert dispatch logs.
 */
public interface AlertDispatchLogRepository {

    /**
     * Save an alert dispatch log.
     */
    void save(AlertDispatchLog alertDispatchLog);

    /**
     * Find dispatch logs by event id.
     */
    List<AlertDispatchLog> findByEventId(Long eventId);
}
