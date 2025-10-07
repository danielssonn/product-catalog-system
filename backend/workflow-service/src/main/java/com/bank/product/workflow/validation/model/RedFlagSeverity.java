package com.bank.product.workflow.validation.model;

/**
 * Severity level of a red flag detected by an agent
 */
public enum RedFlagSeverity {
    /**
     * Low severity - log and continue
     */
    LOW,

    /**
     * Medium severity - may add additional review
     */
    MEDIUM,

    /**
     * High severity - likely triggers enhanced approval
     */
    HIGH,

    /**
     * Critical severity - may auto-reject or terminate workflow
     */
    CRITICAL
}
