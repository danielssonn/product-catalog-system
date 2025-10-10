package com.bank.product.core.model;

/**
 * Types of provisioning lifecycle events.
 */
public enum ProvisioningEventType {
    /**
     * Readiness check initiated
     */
    READINESS_CHECK_STARTED,

    /**
     * Product ready for provisioning
     */
    READY_TO_PROVISION,

    /**
     * Product not ready for provisioning
     */
    NOT_READY_TO_PROVISION,

    /**
     * Provisioning operation started
     */
    PROVISIONING_STARTED,

    /**
     * Provisioning succeeded
     */
    PROVISIONING_SUCCEEDED,

    /**
     * Provisioning failed
     */
    PROVISIONING_FAILED,

    /**
     * Update operation started
     */
    UPDATE_STARTED,

    /**
     * Update succeeded
     */
    UPDATE_SUCCEEDED,

    /**
     * Update failed
     */
    UPDATE_FAILED,

    /**
     * Deactivation started
     */
    DEACTIVATION_STARTED,

    /**
     * Deactivation succeeded
     */
    DEACTIVATION_SUCCEEDED,

    /**
     * Deactivation failed
     */
    DEACTIVATION_FAILED,

    /**
     * Sunset operation started
     */
    SUNSET_STARTED,

    /**
     * Sunset succeeded
     */
    SUNSET_SUCCEEDED,

    /**
     * Sunset failed
     */
    SUNSET_FAILED,

    /**
     * Drift detected between catalog and core
     */
    DRIFT_DETECTED,

    /**
     * Reconciliation started
     */
    RECONCILIATION_STARTED,

    /**
     * Reconciliation succeeded
     */
    RECONCILIATION_SUCCEEDED,

    /**
     * Reconciliation failed
     */
    RECONCILIATION_FAILED,

    /**
     * Core system sunset event received
     */
    CORE_SYSTEM_SUNSET_RECEIVED
}
