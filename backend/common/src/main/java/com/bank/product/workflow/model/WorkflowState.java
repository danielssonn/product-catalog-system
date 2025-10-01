package com.bank.product.workflow.model;

/**
 * States a workflow can be in during its lifecycle
 */
public enum WorkflowState {
    /**
     * Workflow has been initiated
     */
    INITIATED,

    /**
     * Request is being validated
     */
    VALIDATING,

    /**
     * Validation failed
     */
    VALIDATION_FAILED,

    /**
     * Awaiting approval
     */
    PENDING_APPROVAL,

    /**
     * Approved and executing
     */
    APPROVED,

    /**
     * Rejected by checker
     */
    REJECTED,

    /**
     * Successfully completed
     */
    COMPLETED,

    /**
     * Failed during execution
     */
    FAILED,

    /**
     * Cancelled by user
     */
    CANCELLED,

    /**
     * Timed out waiting for approval
     */
    TIMED_OUT,

    /**
     * Escalated to higher authority
     */
    ESCALATED
}
