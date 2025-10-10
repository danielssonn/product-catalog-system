package com.bank.product.party.domain;

import lombok.Data;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.RelationshipProperties;
import org.springframework.data.neo4j.core.schema.TargetNode;

import java.util.ArrayList;
import java.util.List;

/**
 * Counterparty trading/business relationship.
 */
@RelationshipProperties
@Data
public class CounterpartyRelationship {

    @Id
    @GeneratedValue
    private Long id;

    @TargetNode
    private Organization counterparty;

    /**
     * Exposure amount (in millions USD)
     */
    private Double exposureAmount;

    /**
     * Risk category
     */
    private String riskCategory;

    /**
     * Product types involved in relationship
     */
    private List<String> productTypes = new ArrayList<>();

    /**
     * Source system
     */
    private String sourceSystem;
}
