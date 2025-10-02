package com.bank.product.version.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Endpoint-level versioning
 * Tracks individual endpoint versions and their evolution
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "api_endpoint_versions")
public class ApiEndpointVersion {

    @Id
    private String id;

    /**
     * Service identifier
     */
    private String serviceId;

    /**
     * API version (e.g., "v1", "v2")
     */
    private String apiVersion;

    /**
     * Endpoint path pattern (e.g., "/api/v1/solutions/{id}")
     */
    private String endpointPath;

    /**
     * HTTP method (GET, POST, PUT, DELETE, PATCH)
     */
    private String httpMethod;

    /**
     * Endpoint status
     */
    private VersionStatus status;

    /**
     * Request schema reference
     */
    private String requestSchemaRef;

    /**
     * Response schema reference
     */
    private String responseSchemaRef;

    /**
     * Error response schemas
     */
    private Map<Integer, String> errorSchemaRefs;

    /**
     * Deprecation notice
     */
    private String deprecationNotice;

    /**
     * Replacement endpoint (if deprecated)
     */
    private String replacementEndpoint;

    /**
     * Created at
     */
    private LocalDateTime createdAt;

    /**
     * Deprecated at
     */
    private LocalDateTime deprecatedAt;

    /**
     * Sunset at
     */
    private LocalDateTime sunsetAt;
}
