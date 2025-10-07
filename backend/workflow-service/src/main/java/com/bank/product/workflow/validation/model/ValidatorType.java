package com.bank.product.workflow.validation.model;

/**
 * Type of validator/agent in the workflow
 */
public enum ValidatorType {
    /**
     * Rules-based validation (deterministic logic)
     */
    RULES_BASED,

    /**
     * MCP (Model Context Protocol) - LLM-powered agent
     */
    MCP,

    /**
     * GraphRAG - Knowledge graph + LLM reasoning
     */
    GRAPH_RAG,

    /**
     * Custom validator implementation
     */
    CUSTOM
}
