package com.bank.product.core.model;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.Map;

/**
 * Result of a core banking provisioning operation.
 * Returned by adapters after provision/update/deactivate/sunset operations.
 */
@Data
@Builder
public class CoreProvisioningResult {
    /**
     * Whether the operation succeeded
     */
    private boolean success;

    /**
     * Product ID assigned by the core banking system
     */
    private String coreProductId;

    /**
     * Error message if operation failed
     */
    private String errorMessage;

    /**
     * Error code from core system (vendor-specific)
     */
    private String errorCode;

    /**
     * Whether the operation can be retried
     */
    private boolean retryable;

    /**
     * Additional metadata from core system
     */
    private Map<String, Object> metadata;

    /**
     * Timestamp of operation
     */
    private Instant timestamp;

    /**
     * HTTP status code (if applicable)
     */
    private Integer httpStatusCode;

    /**
     * Duration of operation in milliseconds
     */
    private Long durationMs;
}
