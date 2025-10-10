package com.bank.product.core.model;

/**
 * Represents the provisioning status of a product in core banking system(s).
 */
public enum CoreProvisioningStatus {
    /**
     * Product does not require core banking provisioning
     */
    NOT_REQUIRED,

    /**
     * Waiting for configuration to be complete (pending readiness rules)
     */
    PENDING_READINESS,

    /**
     * Configuration is complete and ready to provision
     */
    READY_TO_PROVISION,

    /**
     * Provisioning operation in progress
     */
    PROVISIONING,

    /**
     * Successfully provisioned in core banking system
     */
    PROVISIONED,

    /**
     * Provisioning failed but can be retried
     */
    PROVISION_FAILED,

    /**
     * Provisioning failed permanently (requires manual intervention)
     */
    PROVISION_FAILED_PERMANENT,

    /**
     * Deactivation in progress
     */
    DEACTIVATING,

    /**
     * Deactivated in core system (no new accounts allowed)
     */
    DEACTIVATED,

    /**
     * Permanently removed from core system
     */
    SUNSET
}
