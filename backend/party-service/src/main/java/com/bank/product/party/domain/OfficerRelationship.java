package com.bank.product.party.domain;

import lombok.Data;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.RelationshipProperties;
import org.springframework.data.neo4j.core.schema.TargetNode;

import java.time.LocalDate;

/**
 * Corporate officer relationship (CEO, CFO, etc.).
 */
@RelationshipProperties
@Data
public class OfficerRelationship {

    @Id
    @GeneratedValue
    private Long id;

    @TargetNode
    private LegalEntity entity;

    /**
     * Officer title (CEO, CFO, COO, etc.)
     */
    private String title;

    /**
     * Start date
     */
    private LocalDate startDate;

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
        boolean afterStart = startDate == null || !now.isBefore(startDate);
        boolean beforeEnd = endDate == null || !now.isAfter(endDate);
        return afterStart && beforeEnd;
    }

    public boolean isExecutive() {
        return title != null && (
                title.toUpperCase().contains("CEO") ||
                        title.toUpperCase().contains("CFO") ||
                        title.toUpperCase().contains("COO") ||
                        title.toUpperCase().contains("PRESIDENT")
        );
    }

    public Party getTarget() {
        return entity;
    }
}
