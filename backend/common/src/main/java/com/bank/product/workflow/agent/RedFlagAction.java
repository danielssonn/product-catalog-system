package com.bank.product.workflow.agent;

/**
 * Action to take when red flag is detected
 */
public enum RedFlagAction {
    /**
     * Log the red flag but continue workflow
     */
    CONTINUE,

    /**
     * Add additional approver/review step
     */
    ENHANCE_REVIEW,

    /**
     * Terminate workflow and auto-reject
     */
    TERMINATE_REJECT,

    /**
     * Escalate to senior authority
     */
    ESCALATE
}
