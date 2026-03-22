package com.bananice.businesstracer.domain.repository.alert;

import com.bananice.businesstracer.domain.model.alert.AlertChannel;

import java.util.List;

/**
 * Repository interface for alert channels.
 */
public interface AlertChannelRepository {

    /**
     * Save an alert channel.
     */
    void save(AlertChannel alertChannel);

    /**
     * Find enabled alert channels.
     */
    List<AlertChannel> findEnabled();

    /**
     * Find all alert channels.
     */
    List<AlertChannel> findAll();

    /**
     * Find channel by id.
     */
    AlertChannel findById(Long id);
}
