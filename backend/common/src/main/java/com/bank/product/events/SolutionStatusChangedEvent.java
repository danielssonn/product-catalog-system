package com.bank.product.events;

import com.bank.product.domain.solution.model.SolutionStatus;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Event published when a solution status changes
 */
@Data
@EqualsAndHashCode(callSuper = true)
@JsonTypeName("SolutionStatusChangedEvent")
public class SolutionStatusChangedEvent extends DomainEvent {

    private final String solutionId;
    private final String tenantId;
    private final SolutionStatus fromStatus;
    private final SolutionStatus toStatus;
    private final String changedBy;
    private final String reason;
    private final String workflowId;

    @Builder
    public SolutionStatusChangedEvent(String solutionId, String tenantId,
                                       SolutionStatus fromStatus, SolutionStatus toStatus,
                                       String changedBy, String reason, String workflowId) {
        super();
        this.solutionId = solutionId;
        this.tenantId = tenantId;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.changedBy = changedBy;
        this.reason = reason;
        this.workflowId = workflowId;
    }
}
