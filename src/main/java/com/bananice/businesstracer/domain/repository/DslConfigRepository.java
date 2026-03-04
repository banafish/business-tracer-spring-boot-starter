package com.bananice.businesstracer.domain.repository;

import com.bananice.businesstracer.domain.model.DslConfig;

import java.util.List;

/**
 * Repository interface for DSL configuration
 */
public interface DslConfigRepository {

    /**
     * Find all DSL configurations
     */
    List<DslConfig> findAll();

    /**
     * Find DSL by flowCode
     */
    DslConfig findByFlowCode(String flowCode);

    /**
     * Save a new DSL configuration
     */
    DslConfig save(DslConfig dslConfig);

    /**
     * Update an existing DSL configuration
     */
    DslConfig update(DslConfig dslConfig);

    /**
     * Delete DSL by flowCode
     * 
     * @return true if deleted, false if not found
     */
    boolean deleteByFlowCode(String flowCode);

    /**
     * Check if DSL with flowCode exists
     */
    boolean existsByFlowCode(String flowCode);
}
