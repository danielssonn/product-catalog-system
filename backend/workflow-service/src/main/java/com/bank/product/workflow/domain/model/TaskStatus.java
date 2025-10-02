package com.bank.product.workflow.domain.model;

/**
 * Approval task status
 */
public enum TaskStatus {
    /**
     * Task assigned and pending
     */
    PENDING,

    /**
     * Task in progress
     */
    IN_PROGRESS,

    /**
     * Task completed
     */
    COMPLETED,

    /**
     * Task cancelled
     */
    CANCELLED,

    /**
     * Task timed out
     */
    TIMEOUT
}
