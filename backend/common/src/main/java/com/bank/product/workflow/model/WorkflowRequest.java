package com.bank.product.workflow.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Represents a workflow request submitted by a maker
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "workflow_requests")
public class WorkflowRequest {

    @Id
    private String id;

    /**
     * Unique workflow identifier (Temporal workflow ID)
     */
    private String workflowId;

    /**
     * Type of workflow
     */
    private WorkflowType workflowType;

    /**
     * Current state of the workflow
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
     * Request payload specific to workflow type
     */
    private Map<String, Object> requestData;

    /**
     * Business context for the request
     */
    private BusinessContext businessContext;

    /**
     * Approval policy for this workflow
     */
    private ApprovalPolicy approvalPolicy;

    /**
     * Current approval task ID
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
    private Long version;

    /**
     * Additional metadata
     */
    private Map<String, String> metadata;
}
