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
 * Represents "operates on behalf of" relationships.
 * This is a cross-system synthesized relationship where one organization
 * acts as an agent or representative for another organization.
 */
@RelationshipProperties
@Data
public class OperationalRelationship {

    @Id
    @GeneratedValue
    private Long id;

    @TargetNode
    private Organization principal;

    /**
     * Authority level of the agent
     */
    private String authorityLevel;

    /**
     * Scope of authority (e.g., "TRADING", "SETTLEMENT", "ALL")
     */
    private String scope;

    /**
     * Valid from date
     */
    private LocalDate validFrom;

    /**
     * Valid to date
     */
    private LocalDate validTo;

    /**
     * Source systems that contributed to this relationship
     * This is often synthesized from multiple sources
     */
    private List<String> sourceSystems = new ArrayList<>();

    /**
     * Products/services covered by this arrangement
     */
    private List<String> productTypes = new ArrayList<>();

    /**
     * Geographic regions covered
     */
    private List<String> regions = new ArrayList<>();

    /**
     * Notes or additional context
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
        boolean afterStart = validFrom == null || !now.isBefore(validFrom);
        boolean beforeEnd = validTo == null || !now.isAfter(validTo);
        return afterStart && beforeEnd;
    }

    public boolean isCrossDomain() {
        return sourceSystems != null && sourceSystems.size() > 1;
    }
}
