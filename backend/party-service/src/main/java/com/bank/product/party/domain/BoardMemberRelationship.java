package com.bank.product.party.domain;

import lombok.Data;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.RelationshipProperties;
import org.springframework.data.neo4j.core.schema.TargetNode;

import java.time.LocalDate;

/**
 * Board member relationship.
 */
@RelationshipProperties
@Data
public class BoardMemberRelationship {

    @Id
    @GeneratedValue
    private Long id;

    @TargetNode
    private LegalEntity entity;

    /**
     * Role on board (Chair, Member, Independent Director, etc.)
     */
    private String role;

    /**
     * Appointment date
     */
    private LocalDate appointedDate;

    /**
     * End date
     */
    private LocalDate endDate;

    /**
     * Committee memberships (Audit, Compensation, etc.)
     */
    private String committees;

    /**
     * Source system
     */
    private String sourceSystem;

    public boolean isActive() {
        LocalDate now = LocalDate.now();
        boolean afterStart = appointedDate == null || !now.isBefore(appointedDate);
        boolean beforeEnd = endDate == null || !now.isAfter(endDate);
        return afterStart && beforeEnd;
    }

    public boolean isChair() {
        return role != null && (
                role.toUpperCase().contains("CHAIR") ||
                        role.toUpperCase().contains("CHAIRMAN")
        );
    }

    public Party getTarget() {
        return entity;
    }
}
