package com.bank.product.party.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a predicted relationship extracted from document analysis.
 *
 * Used in predictive graph construction to automatically build party relationships
 * from document data without manual data entry.
 *
 * Example predictions:
 * - Parent-subsidiary from incorporation certificates
 * - Officer-Organization from incumbency certificates
 * - Beneficial ownership from ownership documentation
 *
 * Based on ENTITY_RESOLUTION_DESIGN.md Section 1C: Predictive Graph Construction.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RelationshipPrediction {

    /**
     * Source party federated ID (already exists in system)
     * If null, sourcePartyName will be used to resolve or create PLACEHOLDER
     */
    private String sourcePartyId;

    /**
     * Source party name (for entity reference resolution)
     */
    private String sourcePartyName;

    /**
     * Target party federated ID (already exists in system)
     * If null, targetPartyName will be used to resolve or create PLACEHOLDER
     */
    private String targetPartyId;

    /**
     * Target party name (for entity reference resolution)
     */
    private String targetPartyName;

    /**
     * Type of relationship being predicted
     * Examples: SUBSIDIARY_OF, PARENT_OF, OFFICER_OF, BENEFICIAL_OWNER_OF, AUTHORIZED_SIGNER
     */
    private String relationshipType;

    /**
     * Confidence score of the prediction (0.0-1.0)
     * - 0.90-1.00: Auto-approve and materialize
     * - 0.75-0.95: Needs human review
     * - Below 0.75: Suggestion only
     */
    private Double confidence;

    /**
     * Document ID that provided evidence for this relationship
     */
    private String evidenceDocumentId;

    /**
     * Source of the prediction
     * Examples: INCORPORATION_CERTIFICATE, INCUMBENCY_CERTIFICATE, W9_FORM
     */
    private String predictionSource;

    /**
     * Extraction context (additional details)
     * Example: "Mentioned as 'subsidiary of XYZ Holdings' in Line 3 of incorporation certificate"
     */
    private String extractionContext;

    /**
     * Relationship-specific properties (JSON)
     * Examples:
     * - For ownership: {"ownershipPercentage": 75.0, "ownershipType": "DIRECT"}
     * - For officer: {"title": "CEO", "appointmentDate": "2020-01-15"}
     * - For authorized signer: {"amountLimit": 100000.0, "authorityScope": "BANKING"}
     */
    private String relationshipProperties;

    /**
     * Whether this prediction requires review before materialization
     */
    public boolean needsReview() {
        return confidence != null && confidence < 0.90 && confidence >= 0.75;
    }

    /**
     * Whether this prediction should be auto-approved
     */
    public boolean shouldAutoApprove() {
        return confidence != null && confidence >= 0.90;
    }

    /**
     * Whether this prediction is low confidence (suggestion only)
     */
    public boolean isSuggestion() {
        return confidence == null || confidence < 0.75;
    }
}
