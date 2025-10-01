package com.bank.product.workflow.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Represents an approval decision made by a checker
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalDecision {

    /**
     * ID of the user making the decision
     */
    private String approverId;

    /**
     * Decision type
     */
    private DecisionType decisionType;

    /**
     * When the decision was made
     */
    private LocalDateTime decidedAt;

    /**
     * Comments from approver
     */
    private String comments;

    /**
     * Conditions attached to approval (if any)
     */
    private List<String> conditions;

    /**
     * Digital signature (if required)
     */
    private String signature;

    /**
     * IP address of approver
     */
    private String ipAddress;

    /**
     * User agent
     */
    private String userAgent;
}
