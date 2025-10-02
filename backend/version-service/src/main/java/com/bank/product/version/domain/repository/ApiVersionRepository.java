package com.bank.product.version.domain.repository;

import com.bank.product.version.domain.model.ApiVersion;
import com.bank.product.version.domain.model.VersionStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApiVersionRepository extends MongoRepository<ApiVersion, String> {

    /**
     * Find version by service and version identifier
     */
    Optional<ApiVersion> findByServiceIdAndVersion(String serviceId, String version);

    /**
     * Find all versions for a service
     */
    List<ApiVersion> findByServiceIdOrderByReleasedAtDesc(String serviceId);

    /**
     * Find all versions by status
     */
    List<ApiVersion> findByServiceIdAndStatus(String serviceId, VersionStatus status);

    /**
     * Find first (stable) version for a service by status
     */
    Optional<ApiVersion> findFirstByServiceIdAndStatusOrderByReleasedAtDesc(String serviceId, VersionStatus status);

    /**
     * Check if version exists
     */
    boolean existsByServiceIdAndVersion(String serviceId, String version);
}
