package com.bank.product.workflow.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.util.List;

/**
 * Defines the approval policy for a workflow
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalPolicy {

    /**
     * Number of approvals required
     */
    private int requiredApprovals;

    /**
     * Roles that can approve
     */
    private List<String> approverRoles;

    /**
     * Whether approvals must be sequential (true) or parallel (false)
     */
    private boolean sequential;

    /**
     * Timeout duration for approval
     */
    private Duration timeoutDuration;

    /**
     * Escalation rules
     */
    private List<EscalationRule> escalationRules;

    /**
     * Auto-approval conditions
     */
    private AutoApprovalCondition autoApprovalCondition;

    /**
     * Specific users who can approve (overrides roles)
     */
    private List<String> specificApprovers;

    /**
     * Whether maker can approve their own request
     */
    private boolean allowSelfApproval;

    /**
     * Minimum approval level required
     */
    private ApprovalLevel minimumLevel;
}
