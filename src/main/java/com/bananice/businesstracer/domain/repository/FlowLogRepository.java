package com.bananice.businesstracer.domain.repository;

import com.bananice.businesstracer.domain.model.FlowLog;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository interface for Flow Log
 */
public interface FlowLogRepository {

    /**
     * Save a flow log
     */
    void save(FlowLog flowLog);

    /**
     * Check if a flow log exists by flowCode and businessId
     */
    boolean existsByFlowCodeAndBusinessId(String flowCode, String businessId);

    /**
     * Find all flow logs with pagination
     * 
     * @param flowCode   optional filter by flowCode
     * @param businessId optional filter by businessId
     * @param pageNum    page number (1-based)
     * @param pageSize   page size
     * @return list of flow logs
     */
    List<FlowLog> findAll(String flowCode, String businessId, int pageNum, int pageSize);

    /**
     * Count total flow logs matching the filters
     */
    long count(String flowCode, String businessId);

    /**
     * Update status for a specific flow log
     */
    void updateStatus(String flowCode, String businessId, String status);

    /**
     * Update status for all flow logs of a business ID
     */
    void updateStatusByBusinessId(String businessId, String status);

    /**
     * Find IN_PROGRESS flow logs whose create_time is before the given threshold.
     */
    List<FlowLog> findInProgressBefore(LocalDateTime threshold, int limit);

    /**
     * Find flow logs whose create_time is before the given threshold regardless of status.
     */
    List<FlowLog> findByCreateTimeBefore(LocalDateTime threshold, int limit);
}
