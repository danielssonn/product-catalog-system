package com.bank.product.workflow.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Response for workflow status query
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowStatusResponse {

    /**
     * Workflow ID
     */
    private String workflowId;

    /**
     * Entity type
     */
    private String entityType;

    /**
     * Entity ID
     */
    private String entityId;

    /**
     * Current state
     */
    private String state;

    /**
     * Initiated by
     */
    private String initiatedBy;

    /**
     * Initiated at
     */
    private LocalDateTime initiatedAt;

    /**
     * Completed at
     */
    private LocalDateTime completedAt;

    /**
     * Approval plan
     */
    private Map<String, Object> approvalPlan;

    /**
     * Is complete
     */
    private boolean complete;

    /**
     * Result
     */
    private Map<String, Object> result;

    /**
     * Error message (if any)
     */
    private String errorMessage;
}
