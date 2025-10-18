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
    PENDING_DELETION,

    /**
     * Placeholder party created from document extraction.
     * This party was referenced in a document (e.g., parent company in incorporation certificate)
     * but has not been verified or fully onboarded yet.
     *
     * Placeholder parties:
     * - Have limited information (name, potentially jurisdiction)
     * - Need human review and verification
     * - Can be merged with existing parties or upgraded to ACTIVE after verification
     * - Support predictive graph construction from document data
     *
     * @see com.bank.product.party.document.RelationshipExtractionService
     * @see ENTITY_RESOLUTION_DESIGN.md Section 1C: Predictive Graph Construction
     */
    PLACEHOLDER
}
