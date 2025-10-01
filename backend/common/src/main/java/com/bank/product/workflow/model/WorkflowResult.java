package com.bank.product.workflow.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Result of a completed workflow
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowResult {

    /**
     * Whether the workflow succeeded
     */
    private boolean success;

    /**
     * Result code
     */
    private String resultCode;

    /**
     * Human-readable message
     */
    private String message;

    /**
     * ID of created/updated entity
     */
    private String entityId;

    /**
     * Type of entity
     */
    private String entityType;

    /**
     * Effective date of change
     */
    private LocalDateTime effectiveDate;

    /**
     * Additional result data
     */
    private Map<String, Object> resultData;

    /**
     * Events published as result
     */
    private String[] publishedEvents;

    /**
     * Execution duration in milliseconds
     */
    private long executionDurationMs;
}
