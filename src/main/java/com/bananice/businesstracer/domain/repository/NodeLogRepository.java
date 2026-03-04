package com.bananice.businesstracer.domain.repository;

import com.bananice.businesstracer.domain.model.NodeLog;

import java.util.List;

/**
 * Repository Interface for Node Log
 */
public interface NodeLogRepository {

    /**
     * Save a node log record
     */
    void save(NodeLog nodeLog);

    /**
     * Find node logs by Business ID
     */
    List<NodeLog> findByBusinessId(String businessId);
}
