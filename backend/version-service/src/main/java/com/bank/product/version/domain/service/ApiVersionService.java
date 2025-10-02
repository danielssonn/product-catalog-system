package com.bank.product.version.domain.service;

import com.bank.product.version.domain.model.ApiVersion;
import com.bank.product.version.domain.model.VersionStatus;

import java.util.List;
import java.util.Optional;

/**
 * API Version Management Service
 */
public interface ApiVersionService {

    /**
     * Register a new API version
     */
    ApiVersion registerVersion(ApiVersion apiVersion);

    /**
     * Get version by service and version identifier
     */
    Optional<ApiVersion> getVersion(String serviceId, String version);

    /**
     * Get all versions for a service
     */
    List<ApiVersion> getAllVersions(String serviceId);

    /**
     * Get current stable version
     */
    Optional<ApiVersion> getStableVersion(String serviceId);

    /**
     * Get default version (fallback if not specified)
     */
    Optional<ApiVersion> getDefaultVersion(String serviceId);

    /**
     * Update version status
     */
    ApiVersion updateVersionStatus(String serviceId, String version, VersionStatus newStatus);

    /**
     * Deprecate version
     */
    ApiVersion deprecateVersion(String serviceId, String version, String reason);

    /**
     * Check if version is supported
     */
    boolean isVersionSupported(String serviceId, String version);

    /**
     * Get version by semantic version
     */
    Optional<ApiVersion> getVersionBySemanticVersion(String serviceId, String semanticVersion);
}
