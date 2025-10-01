package com.bank.product.workflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Request to submit a new workflow
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowSubmitRequest {

    /**
     * Type of entity being submitted for approval
     */
    @NotBlank(message = "Entity type is required")
    private String entityType;

    /**
     * ID of the entity being approved
     */
    @NotBlank(message = "Entity ID is required")
    private String entityId;

    /**
     * Full entity data snapshot
     */
    @NotNull(message = "Entity data is required")
    private Map<String, Object> entityData;

    /**
     * Metadata for rule evaluation
     */
    @NotNull(message = "Entity metadata is required")
    private Map<String, Object> entityMetadata;

    /**
     * Tenant ID (for multi-tenancy)
     */
    private String tenantId;

    /**
     * User who initiated the workflow
     */
    @NotBlank(message = "Initiated by is required")
    private String initiatedBy;

    /**
     * Business justification for the request
     */
    private String businessJustification;

    /**
     * Priority: LOW, MEDIUM, HIGH, CRITICAL
     */
    private String priority;

    /**
     * Optional template override (defaults to active template for entity type)
     */
    private String templateId;
}
