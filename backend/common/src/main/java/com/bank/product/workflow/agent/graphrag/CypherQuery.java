package com.bank.product.workflow.agent.graphrag;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Cypher query configuration for GraphRAG
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CypherQuery {

    /**
     * Query name
     */
    private String name;

    /**
     * Cypher query string
     */
    private String cypher;

    /**
     * Query parameters (field name -> JSONPath expression)
     * e.g., "customerId": "$.entityData.customerId"
     */
    private Map<String, String> parameters;

    /**
     * How to map results to enrichment fields
     */
    private String resultMapping;

    /**
     * Description of what this query retrieves
     */
    private String description;

    /**
     * Whether this query is optional
     */
    private boolean optional;

    /**
     * Max results to return
     */
    private int limit;
}
