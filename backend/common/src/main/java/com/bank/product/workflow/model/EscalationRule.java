package com.bank.product.workflow.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Duration;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EscalationRule {

    /**
     * Time before escalation triggers
     */
    private Duration escalationAfter;

    /**
     * Role to escalate to
     */
    private String escalateToRole;

    /**
     * Specific user to escalate to
     */
    private String escalateToUser;

    /**
     * Escalation level
     */
    private ApprovalLevel escalationLevel;

    /**
     * Whether to notify original assignee
     */
    private boolean notifyOriginalAssignee;

    /**
     * Escalation reason template
     */
    private String reasonTemplate;
}
