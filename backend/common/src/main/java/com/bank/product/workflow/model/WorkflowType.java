package com.bank.product.workflow.model;

/**
 * Types of workflows supported in the system
 */
public enum WorkflowType {
    /**
     * Solution configuration from catalog product
     */
    SOLUTION_CONFIG,

    /**
     * Pricing changes for existing solution
     */
    PRICING_CHANGE,

    /**
     * Create or update catalog product
     */
    CATALOG_PRODUCT,

    /**
     * Solution retirement
     */
    SOLUTION_RETIREMENT,

    /**
     * Bulk solution updates
     */
    BULK_UPDATE,

    /**
     * Fee structure changes
     */
    FEE_CHANGE,

    /**
     * Terms and conditions update
     */
    TERMS_UPDATE
}
