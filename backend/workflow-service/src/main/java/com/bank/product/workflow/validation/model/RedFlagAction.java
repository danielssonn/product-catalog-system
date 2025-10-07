package com.bank.product.workflow.validation.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Action to take when a red flag is detected by an agent
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RedFlagAction {

    /**
     * Action type
     */
    private ActionType action;

    /**
     * Whether to auto-reject the workflow
     */
    private boolean autoReject;

    /**
     * Reason for the action
     */
    private String reason;

    /**
     * Roles to notify
     */
    private List<String> notifyRoles;

    /**
     * Role to escalate to
     */
    private String escalateTo;

    /**
     * Whether to add additional approvers
     */
    private boolean addApprovers;

    /**
     * Additional approver roles to add
     */
    private List<String> additionalApproverRoles;

    public enum ActionType {
        /**
         * Continue workflow but log the red flag
         */
        CONTINUE,

        /**
         * Add additional review/approval layer
         */
        ENHANCE_REVIEW,

        /**
         * Terminate workflow and auto-reject
         */
        TERMINATE_REJECT,

        /**
         * Escalate to senior approver
         */
        ESCALATE
    }
}
