package com.bank.product.events;

import com.bank.product.workflow.model.ApprovalDecision;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Event published when a workflow completes (approved or rejected)
 */
@Data
@EqualsAndHashCode(callSuper = true)
@JsonTypeName("WorkflowCompletedEvent")
public class WorkflowCompletedEvent extends DomainEvent {

    private final String workflowId;
    private final String workflowInstanceId;
    private final String entityType;
    private final String entityId;  // solutionId
    private final String tenantId;
    private final String outcome;  // "APPROVED" or "REJECTED"
    private final List<ApprovalDecision> approvals;
    private final LocalDateTime completedAt;
    private final String rejectionReason;

    @Builder
    public WorkflowCompletedEvent(String workflowId, String workflowInstanceId,
                                   String entityType, String entityId, String tenantId,
                                   String outcome, List<ApprovalDecision> approvals,
                                   LocalDateTime completedAt, String rejectionReason) {
        super();
        this.workflowId = workflowId;
        this.workflowInstanceId = workflowInstanceId;
        this.entityType = entityType;
        this.entityId = entityId;
        this.tenantId = tenantId;
        this.outcome = outcome;
        this.approvals = approvals;
        this.completedAt = completedAt;
        this.rejectionReason = rejectionReason;
    }
}
