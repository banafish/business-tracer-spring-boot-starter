package com.bananice.businesstracer.domain.repository.alert;

/**
 * Repository interface for alert configuration versions.
 */
public interface AlertConfigVersionRepository {

    /**
     * Get currently published version.
     */
    Long getPublishedVersion();

    /**
     * Save a new version record.
     */
    void saveVersion(Long versionNo);
}
