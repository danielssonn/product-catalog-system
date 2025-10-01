package com.bank.product.workflow.agent.graphrag;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Semantic search configuration for GraphRAG
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SemanticSearchConfig {

    /**
     * Whether semantic search is enabled
     */
    private boolean enabled;

    /**
     * Field to use as question/query
     */
    private String questionField;

    /**
     * Number of results to retrieve
     */
    private int topK;

    /**
     * Minimum similarity threshold (0-1)
     */
    private double similarityThreshold;

    /**
     * Node types to search
     */
    private String[] nodeTypes;

    /**
     * Embedding property name in graph
     */
    private String embeddingProperty;

    /**
     * Whether to use hybrid search (keyword + semantic)
     */
    private boolean hybridSearch;
}
