package com.bank.product.workflow.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Computed approval plan after evaluating decision rules
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
     * List of approver roles
     */
    private List<String> approverRoles;

    /**
     * List of specific approver user IDs
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
     * Escalation rules
     */
    private List<EscalationRule> escalationRules;

    /**
     * Additional configuration from rules
     */
    private Map<String, Object> additionalConfig;
}
