package com.bank.product.party.domain;

import lombok.Data;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.RelationshipProperties;
import org.springframework.data.neo4j.core.schema.TargetNode;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a potential duplicate party that needs review.
 */
@RelationshipProperties
@Data
public class DuplicateCandidate {

    @Id
    @GeneratedValue
    private Long id;

    @TargetNode
    private Party candidateParty;

    /**
     * Similarity score (0.0 - 1.0)
     */
    private Double similarityScore;

    /**
     * Fields that matched
     */
    private List<String> matchingFields = new ArrayList<>();

    /**
     * Resolution status (PENDING, MERGED, NOT_DUPLICATE, NEEDS_REVIEW)
     */
    private String resolutionStatus;

    /**
     * When this candidate was identified
     */
    private Instant identifiedAt;

    /**
     * When this was resolved
     */
    private Instant resolvedAt;

    /**
     * Who resolved it
     */
    private String resolvedBy;

    /**
     * Resolution notes
     */
    private String notes;

    public DuplicateCandidate() {
        this.identifiedAt = Instant.now();
        this.resolutionStatus = "PENDING";
    }

    public void addMatchingField(String field) {
        if (this.matchingFields == null) {
            this.matchingFields = new ArrayList<>();
        }
        if (!this.matchingFields.contains(field)) {
            this.matchingFields.add(field);
        }
    }

    public boolean isPending() {
        return "PENDING".equals(resolutionStatus) || "NEEDS_REVIEW".equals(resolutionStatus);
    }
}
