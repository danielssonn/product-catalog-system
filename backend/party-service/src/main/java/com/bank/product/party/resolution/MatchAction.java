package com.bank.product.party.resolution;

/**
 * Recommended action for entity resolution
 */
public enum MatchAction {
    /**
     * Automatically merge (high confidence match)
     */
    AUTO_MERGE,

    /**
     * Require manual review
     */
    MANUAL_REVIEW,

    /**
     * Create as new entity (no match)
     */
    CREATE_NEW
}
