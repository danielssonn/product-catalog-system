package com.bank.product.party.domain;

import lombok.Data;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.RelationshipProperties;
import org.springframework.data.neo4j.core.schema.TargetNode;

import java.time.Instant;

/**
 * Represents a merge operation where one party was merged into another.
 * Tracks entity resolution history.
 */
@RelationshipProperties
@Data
public class PartyMerge {

    @Id
    @GeneratedValue
    private Long id;

    @TargetNode
    private Party sourceparty;

    /**
     * When the merge occurred
     */
    private Instant mergeDate;

    /**
     * Reason for merge
     */
    private String mergeReason;

    /**
     * Confidence score in the merge decision (0.0 - 1.0)
     */
    private Double confidenceScore;

    /**
     * Whether this was an automatic merge or required manual review
     */
    private Boolean automatic;

    /**
     * User who approved the merge (if manual)
     */
    private String approvedBy;

    /**
     * Additional notes
     */
    private String notes;

    public PartyMerge() {
        this.mergeDate = Instant.now();
    }
}
