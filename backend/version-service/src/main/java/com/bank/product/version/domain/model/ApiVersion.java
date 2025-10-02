package com.bank.product.version.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * API Version Registry
 * Tracks all API versions, their lifecycle, and metadata
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "api_versions")
public class ApiVersion {

    @Id
    private String id;

    /**
     * Service identifier (e.g., "product-service", "workflow-service")
     */
    private String serviceId;

    /**
     * Version identifier (e.g., "v1", "v2", "v3")
     */
    private String version;

    /**
     * Semantic version (e.g., "1.0.0", "2.1.0")
     */
    private String semanticVersion;

    /**
     * Version status
     */
    private VersionStatus status;

    /**
     * Release date
     */
    private LocalDateTime releasedAt;

    /**
     * Deprecation date (if deprecated)
     */
    private LocalDateTime deprecatedAt;

    /**
     * Sunset date (when version will be removed)
     */
    private LocalDateTime sunsetAt;

    /**
     * End-of-life date (version completely unavailable)
     */
    private LocalDateTime eolAt;

    /**
     * Breaking changes from previous version
     */
    private List<BreakingChange> breakingChanges;

    /**
     * New features in this version
     */
    private List<String> newFeatures;

    /**
     * Bug fixes in this version
     */
    private List<String> bugFixes;

    /**
     * Migration guide URL
     */
    private String migrationGuideUrl;

    /**
     * Documentation URL
     */
    private String documentationUrl;

    /**
     * OpenAPI spec URL
     */
    private String openApiSpecUrl;

    /**
     * Schema transformations from this version to others
     */
    private Map<String, SchemaTransformation> transformations;

    /**
     * Supported content types
     */
    private List<String> contentTypes;

    /**
     * Default content type
     */
    private String defaultContentType;

    /**
     * Version metadata
     */
    private Map<String, Object> metadata;

    /**
     * Created by
     */
    private String createdBy;

    /**
     * Created at
     */
    private LocalDateTime createdAt;

    /**
     * Updated by
     */
    private String updatedBy;

    /**
     * Updated at
     */
    private LocalDateTime updatedAt;
}
