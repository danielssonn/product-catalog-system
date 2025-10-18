package com.bank.product.entitlement;

/**
 * Types of resources that can have entitlements
 */
public enum ResourceType {
    /**
     * Product catalog template
     */
    CATALOG_PRODUCT,

    /**
     * Tenant-specific solution instance
     */
    SOLUTION,

    /**
     * Bank account
     */
    ACCOUNT,

    /**
     * Transaction
     */
    TRANSACTION,

    /**
     * Party/customer entity
     */
    PARTY,

    /**
     * Workflow instance
     */
    WORKFLOW,

    /**
     * Document
     */
    DOCUMENT,

    /**
     * Report
     */
    REPORT,

    /**
     * Any other resource type
     */
    OTHER
}
