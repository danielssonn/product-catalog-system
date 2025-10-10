package com.bank.product.core.model;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.Map;

/**
 * Event published to Kafka during provisioning lifecycle.
 * Used for event-driven orchestration and audit trail.
 */
@Data
@Builder
public class ProvisioningEvent {

    /**
     * Unique event ID
     */
    private String eventId;

    /**
     * Type of provisioning event
     */
    private ProvisioningEventType eventType;

    /**
     * Tenant ID
     */
    private String tenantId;

    /**
     * Solution ID in catalog system
     */
    private String solutionId;

    /**
     * Core system ID
     */
    private String coreSystemId;

    /**
     * Core system type
     */
    private CoreSystemType coreSystemType;

    /**
     * Product ID in core system (if applicable)
     */
    private String coreProductId;

    /**
     * Provisioning status
     */
    private CoreProvisioningStatus status;

    /**
     * Error message (if failed)
     */
    private String errorMessage;

    /**
     * Whether the operation can be retried
     */
    private Boolean retryable;

    /**
     * Additional event metadata
     */
    private Map<String, Object> metadata;

    /**
     * When the event occurred
     */
    private Instant timestamp;

    /**
     * User who triggered the operation (if applicable)
     */
    private String triggeredBy;

    /**
     * Correlation ID for tracking related events
     */
    private String correlationId;
}
