package com.bank.product.party.domain;

/**
 * Status of a party entity in the system
 */
public enum PartyStatus {
    /**
     * Active party, currently in use
     */
    ACTIVE,

    /**
     * Inactive party, not currently used but retained for historical purposes
     */
    INACTIVE,

    /**
     * Party has been merged into another party
     */
    MERGED,

    /**
     * Candidate duplicate awaiting resolution
     */
    DUPLICATE,

    /**
     * Under review for data quality or compliance issues
     */
    UNDER_REVIEW,

    /**
     * Pending deletion (soft delete)
     */
    PENDING_DELETION
}
