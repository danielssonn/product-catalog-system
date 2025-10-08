package com.bank.product.party.domain;

import lombok.Data;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.RelationshipProperties;
import org.springframework.data.neo4j.core.schema.TargetNode;

import java.time.LocalDate;

/**
 * Authorized signer relationship.
 */
@RelationshipProperties
@Data
public class AuthorizedSignerRelationship {

    @Id
    @GeneratedValue
    private Long id;

    @TargetNode
    private LegalEntity entity;

    /**
     * Authority limits (e.g., transaction amount limits)
     */
    private String authorityLimits;

    /**
     * Effective date
     */
    private LocalDate effectiveDate;

    /**
     * End date
     */
    private LocalDate endDate;

    /**
     * Source system
     */
    private String sourceSystem;

    public boolean isActive() {
        LocalDate now = LocalDate.now();
        boolean afterStart = effectiveDate == null || !now.isBefore(effectiveDate);
        boolean beforeEnd = endDate == null || !now.isAfter(endDate);
        return afterStart && beforeEnd;
    }
}
