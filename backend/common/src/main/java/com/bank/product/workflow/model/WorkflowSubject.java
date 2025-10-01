package com.bank.product.workflow.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Generic workflow subject - can represent ANY entity requiring approval
 * Examples: solution configuration, document verification, customer onboarding, loan application
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "workflow_subjects")
public class WorkflowSubject {

    @Id
    private String id;

    /**
     * Unique workflow identifier (Temporal workflow ID)
     */
    private String workflowId;

    /**
     * Temporal workflow instance ID
     */
    private String workflowInstanceId;

    /**
     * Type of entity being approved
     * Examples: "SOLUTION_CONFIGURATION", "DOCUMENT_VERIFICATION", "CUSTOMER_ONBOARDING"
     */
    private String entityType;

    /**
     * ID of the entity being approved
     */
    private String entityId;

    /**
     * Complete snapshot of entity data at submission time
     */
    private Map<String, Object> entityData;

    /**
     * Metadata extracted for rule evaluation
     * Should contain all attributes referenced in decision tables
     */
    private Map<String, Object> entityMetadata;

    /**
     * Workflow template used
     */
    private String templateId;

    /**
     * Template version used
     */
    private String templateVersion;

    /**
     * Current workflow state
     */
    private WorkflowState state;

    /**
     * Tenant context
     */
    private String tenantId;

    /**
     * User who initiated the workflow (maker)
     */
    private String initiatedBy;

    /**
     * When the workflow was initiated
     */
    private LocalDateTime initiatedAt;

    /**
     * Business justification
     */
    private String businessJustification;

    /**
     * Priority level
     */
    private Priority priority;

    /**
     * Approval plan computed from rules
     */
    private ComputedApprovalPlan approvalPlan;

    /**
     * Current approval tasks
     */
    private String currentTaskId;

    /**
     * Workflow result after completion
     */
    private WorkflowResult result;

    /**
     * Error details if failed
     */
    private String errorMessage;

    /**
     * When the workflow was completed/failed
     */
    private LocalDateTime completedAt;

    /**
     * Version for optimistic locking
     */
    @Version
    private Long version;

    /**
     * Additional metadata
     */
    private Map<String, String> metadata;
}
