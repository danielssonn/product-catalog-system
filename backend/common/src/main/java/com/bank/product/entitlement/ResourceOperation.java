package com.bank.product.entitlement;

/**
 * Operations that can be performed on resources
 * These are fine-grained, resource-specific operations
 */
public enum ResourceOperation {
    // Read operations
    VIEW,
    LIST,
    SEARCH,
    EXPORT,

    // Write operations
    CREATE,
    UPDATE,
    DELETE,

    // Solution/Product operations
    CONFIGURE,
    ACTIVATE,
    DEACTIVATE,
    SUSPEND,

    // Account operations
    OPEN_ACCOUNT,
    CLOSE_ACCOUNT,

    // Transaction operations
    TRANSACT,
    INITIATE_PAYMENT,
    APPROVE_TRANSACTION,
    REJECT_TRANSACTION,

    // Workflow operations
    SUBMIT_WORKFLOW,
    APPROVE_WORKFLOW,
    REJECT_WORKFLOW,

    // Administrative operations
    GRANT_ACCESS,
    REVOKE_ACCESS,
    MANAGE_ENTITLEMENTS,

    // Delegation operations
    DELEGATE,
    ACT_ON_BEHALF_OF,

    // Audit operations
    VIEW_AUDIT_LOG,

    // Custom/extensible
    CUSTOM
}
