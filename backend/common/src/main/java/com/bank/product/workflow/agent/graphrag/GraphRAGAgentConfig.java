package com.bank.product.workflow.agent.graphrag;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Configuration for GraphRAG (Graph Retrieval Augmented Generation) agent
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GraphRAGAgentConfig {

    /**
     * Graph database URL
     */
    private String graphDbUrl;

    /**
     * Username for graph DB
     */
    private String username;

    /**
     * Password for graph DB
     */
    private String password;

    /**
     * Embedding model for semantic search
     */
    private String embeddingModel;

    /**
     * Retrieval depth for graph traversal
     */
    private int retrievalDepth;

    /**
     * Entity types to retrieve
     */
    private List<String> entities;

    /**
     * Cypher queries to execute
     */
    private List<CypherQuery> queries;

    /**
     * Semantic search configuration
     */
    private SemanticSearchConfig semanticSearch;

    /**
     * Timeout for graph queries (milliseconds)
     */
    private int timeout;

    /**
     * Whether to cache results
     */
    private boolean enableCache;

    /**
     * Cache TTL in seconds
     */
    private int cacheTtl;
}
