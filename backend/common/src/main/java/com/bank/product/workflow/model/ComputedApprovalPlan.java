package com.bank.product.workflow.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Approval plan computed from decision table evaluation
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComputedApprovalPlan {

    /**
     * Whether approval is required
     */
    private boolean approvalRequired;

    /**
     * Number of approvals required
     */
    private int requiredApprovals;

    /**
     * Roles that can approve
     */
    private List<String> approverRoles;

    /**
     * Specific users who can approve (overrides roles)
     */
    private List<String> specificApprovers;

    /**
     * Whether approvals must be sequential
     */
    private boolean sequential;

    /**
     * SLA duration
     */
    private Duration sla;

    /**
     * Escalation rules for this plan
     */
    private List<EscalationRule> escalationRules;

    /**
     * Auto-approval conditions
     */
    private AutoApprovalCondition autoApprovalCondition;

    /**
     * Additional configuration from decision table
     */
    private Map<String, Object> additionalConfig;

    /**
     * Matched rule IDs (for audit)
     */
    private List<String> matchedRules;

    /**
     * Approval level required
     */
    private ApprovalLevel minimumApprovalLevel;
}
