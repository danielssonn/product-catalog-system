package com.bank.product.workflow.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Generic workflow subject - represents any entity being approved
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
     * Workflow ID (business identifier)
     */
    @Indexed(unique = true)
    private String workflowId;

    /**
     * Temporal workflow instance ID
     */
    @Indexed
    private String workflowInstanceId;

    /**
     * Entity type being approved
     */
    @Indexed
    private String entityType;

    /**
     * Reference to the entity being approved
     */
    @Indexed
    private String entityId;

    /**
     * Snapshot of entity data at submission time
     */
    private Map<String, Object> entityData;

    /**
     * Metadata extracted for rule evaluation
     */
    private Map<String, Object> entityMetadata;

    /**
     * Template reference
     */
    @Indexed
    private String templateId;
    private String templateVersion;

    /**
     * Workflow state
     */
    @Indexed
    private WorkflowState state;

    /**
     * Tenant context
     */
    @Indexed
    private String tenantId;

    /**
     * User who initiated the workflow
     */
    @Indexed
    private String initiatedBy;

    /**
     * Timestamps
     */
    private LocalDateTime initiatedAt;
    private LocalDateTime completedAt;

    /**
     * Computed approval plan from rule evaluation
     */
    private ComputedApprovalPlan approvalPlan;

    /**
     * Workflow result
     */
    private WorkflowResult result;

    /**
     * Error message if workflow failed
     */
    private String errorMessage;

    /**
     * Business justification
     */
    private String businessJustification;

    /**
     * Priority: LOW, MEDIUM, HIGH, CRITICAL
     */
    private String priority;
}
