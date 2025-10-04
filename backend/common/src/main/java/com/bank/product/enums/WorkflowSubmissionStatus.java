package com.bank.product.enums;

/**
 * Status of workflow submission process
 * Tracks the async gap between solution creation and workflow submission
 */
public enum WorkflowSubmissionStatus {
    /**
     * Auto-approved solution, no workflow needed
     */
    NOT_REQUIRED,

    /**
     * Workflow submission in progress (async)
     */
    PENDING_SUBMISSION,

    /**
     * Workflow successfully created
     */
    SUBMITTED,

    /**
     * Workflow submission failed (will retry)
     */
    SUBMISSION_FAILED,

    /**
     * Retry scheduled after failure
     */
    RETRY_SCHEDULED
}
