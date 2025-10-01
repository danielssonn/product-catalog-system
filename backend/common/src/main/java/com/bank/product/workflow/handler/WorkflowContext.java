package com.bank.product.workflow.handler;

import com.bank.product.workflow.model.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Context passed to workflow callback handlers
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowContext {

    /**
     * The workflow subject
     */
    private WorkflowSubject subject;

    /**
     * Entity type
     */
    private String entityType;

    /**
     * Entity ID
     */
    private String entityId;

    /**
     * Entity data snapshot
     */
    private Map<String, Object> entityData;

    /**
     * Entity metadata
     */
    private Map<String, Object> entityMetadata;

    /**
     * All approval decisions made
     */
    private List<ApprovalDecision> decisions;

    /**
     * All rejection decisions (if any)
     */
    private List<RejectionDecision> rejections;

    /**
     * Workflow result (if completed)
     */
    private WorkflowResult result;

    /**
     * Previous result (for retry scenarios)
     */
    private WorkflowResult previousResult;

    /**
     * Workflow template used
     */
    private WorkflowTemplate template;

    /**
     * Computed approval plan
     */
    private ComputedApprovalPlan approvalPlan;

    /**
     * Tenant context
     */
    private String tenantId;

    /**
     * User who initiated the workflow
     */
    private String initiatedBy;

    /**
     * Current user (approver/rejecter)
     */
    private String currentUser;

    /**
     * Additional context data
     */
    private Map<String, Object> additionalContext;
}
