package com.bank.product.workflow.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Rule for escalating tasks based on conditions
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EscalationRule {
    /**
     * Condition expression (e.g., "task.age > slaHours * 0.8")
     */
    private String condition;

    /**
     * Action to take: SEND_REMINDER, ESCALATE_TO_ROLE, AUTO_APPROVE, AUTO_REJECT
     */
    private String action;

    /**
     * Role to escalate to (if action is ESCALATE_TO_ROLE)
     */
    private String escalateToRole;

    /**
     * Human-readable description
     */
    private String description;
}
