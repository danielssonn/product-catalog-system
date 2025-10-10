package com.bank.product.party.domain;

import lombok.Data;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.RelationshipProperties;
import org.springframework.data.neo4j.core.schema.TargetNode;

import java.time.LocalDate;

/**
 * Service provider relationship.
 */
@RelationshipProperties
@Data
public class ServiceRelationship {

    @Id
    @GeneratedValue
    private Long id;

    @TargetNode
    private Organization client;

    /**
     * Type of service provided
     */
    private String serviceType;

    /**
     * Relationship manager
     */
    private String relationshipManager;

    /**
     * Relationship start date
     */
    private LocalDate since;

    /**
     * Source system
     */
    private String sourceSystem;
}
