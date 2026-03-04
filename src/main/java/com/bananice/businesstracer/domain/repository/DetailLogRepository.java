package com.bananice.businesstracer.domain.repository;

import com.bananice.businesstracer.domain.model.DetailLog;

import java.util.List;

/**
 * Repository Interface for Detail Log
 */
public interface DetailLogRepository {

    /**
     * Save a detail log record
     */
    void save(DetailLog detailLog);

    /**
     * Find detail logs by Parent Node ID
     */
    List<DetailLog> findByParentNodeId(String parentNodeId);
}
