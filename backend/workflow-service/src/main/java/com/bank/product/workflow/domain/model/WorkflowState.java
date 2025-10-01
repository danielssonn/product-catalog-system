package com.bank.product.workflow.domain.model;

/**
 * Workflow execution state
 */
public enum WorkflowState {
    /**
     * Workflow has been initiated
     */
    INITIATED,

    /**
     * Request is being validated
     */
    VALIDATION,

    /**
     * Waiting for approval
     */
    PENDING_APPROVAL,

    /**
     * Approved and executing action
     */
    APPROVED,

    /**
     * Workflow completed successfully
     */
    COMPLETED,

    /**
     * Request was rejected
     */
    REJECTED,

    /**
     * Workflow was cancelled
     */
    CANCELLED,

    /**
     * Workflow failed due to error
     */
    FAILED,

    /**
     * Workflow timed out
     */
    TIMEOUT
}
