package com.bank.product.workflow.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Approval or rejection decision
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalDecision {
    /**
     * User who made the decision
     */
    private String approverId;

    /**
     * Decision type: APPROVE, REJECT
     */
    private String decision;

    /**
     * Comments
     */
    private String comments;

    /**
     * Conditions attached to approval
     */
    private List<String> conditions;

    /**
     * Reason for rejection
     */
    private String rejectionReason;

    /**
     * Required changes (if rejected)
     */
    private List<String> requiredChanges;

    /**
     * Timestamp
     */
    private LocalDateTime timestamp;
}
