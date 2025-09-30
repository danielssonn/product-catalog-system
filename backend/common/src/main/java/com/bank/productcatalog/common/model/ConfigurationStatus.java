package com.bank.productcatalog.common.model;

public enum ConfigurationStatus {
    DRAFT,              // Being configured by tenant
    PENDING_APPROVAL,   // Submitted for approval
    APPROVED,           // Approved but not yet active
    ACTIVE,             // Currently active for tenant
    INACTIVE,           // Temporarily disabled
    REJECTED,           // Approval rejected
    ARCHIVED            // Archived/historical
}