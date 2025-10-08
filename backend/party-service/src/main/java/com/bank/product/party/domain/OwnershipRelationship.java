package com.bank.product.party.domain;

import lombok.Data;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.RelationshipProperties;
import org.springframework.data.neo4j.core.schema.TargetNode;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents an ownership relationship between parties.
 * Used for PARENT_OF, SUBSIDIARY_OF, and OWNS relationships.
 */
@RelationshipProperties
@Data
public class OwnershipRelationship {

    @Id
    @GeneratedValue
    private Long id;

    @TargetNode
    private Party target;

    /**
     * Ownership percentage (0.0 - 100.0)
     */
    private Double ownershipPercentage;

    /**
     * Effective date of ownership
     */
    private LocalDate effectiveDate;

    /**
     * End date (if ownership has ended)
     */
    private LocalDate endDate;

    /**
     * Whether this is direct ownership (vs. indirect through intermediaries)
     */
    private Boolean direct;

    /**
     * Voting rights percentage (may differ from ownership)
     */
    private Double votingRights;

    /**
     * Source systems that reported this relationship
     */
    private List<String> sourceSystems = new ArrayList<>();

    /**
     * Type of control
     */
    private ControlType controlType;

    /**
     * Additional notes
     */
    private String notes;

    public void addSourceSystem(String sourceSystem) {
        if (this.sourceSystems == null) {
            this.sourceSystems = new ArrayList<>();
        }
        if (!this.sourceSystems.contains(sourceSystem)) {
            this.sourceSystems.add(sourceSystem);
        }
    }

    public boolean isActive() {
        LocalDate now = LocalDate.now();
        boolean afterStart = effectiveDate == null || !now.isBefore(effectiveDate);
        boolean beforeEnd = endDate == null || !now.isAfter(endDate);
        return afterStart && beforeEnd;
    }

    public boolean isMajorityOwnership() {
        return ownershipPercentage != null && ownershipPercentage > 50.0;
    }
}
