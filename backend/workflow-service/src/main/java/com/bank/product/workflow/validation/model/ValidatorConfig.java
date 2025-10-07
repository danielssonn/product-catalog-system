package com.bank.product.workflow.validation.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Configuration for a validator in the workflow
 * Supports rules-based, MCP, and GraphRAG validators
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidatorConfig {

    /**
     * Unique identifier for this validator
     */
    private String validatorId;

    /**
     * Type of validator (RULES_BASED, MCP, GRAPH_RAG, CUSTOM)
     */
    private ValidatorType type;

    /**
     * Execution mode
     */
    private AgentExecutionMode mode;

    /**
     * Priority for execution ordering (higher = first)
     */
    private int priority;

    /**
     * Timeout in milliseconds
     */
    private int timeoutMs;

    /**
     * Type-specific configuration
     * For RULES_BASED: rule configuration
     * For MCP: MCPConfig (serverUrl, apiKey, model, temperature, etc.)
     * For GRAPH_RAG: GraphRAGConfig (neo4jUri, queryTemplates, etc.)
     * For CUSTOM: custom configuration map
     */
    private Map<String, Object> config;

    /**
     * Red flag detection conditions
     * Map of field -> condition expression (e.g., "documentCompleteness" -> "< 0.5")
     */
    private Map<String, String> redFlagConditions;

    /**
     * Action to take when red flag is detected
     */
    private RedFlagAction redFlagAction;

    /**
     * Fields to extract for enrichment
     */
    private java.util.List<String> enrichmentOutputs;

    /**
     * Whether this validator is required (fail workflow if validator fails)
     */
    private boolean required;

    // MCP-specific helper methods
    @JsonIgnore
    public String getMcpServerUrl() {
        return config != null ? (String) config.get("serverUrl") : null;
    }

    @JsonIgnore
    public String getMcpApiKey() {
        return config != null ? (String) config.get("apiKey") : null;
    }

    @JsonIgnore
    public String getMcpModel() {
        return config != null ? (String) config.get("model") : null;
    }

    @JsonIgnore
    public Integer getMcpMaxTokens() {
        return config != null ? (Integer) config.get("maxTokens") : null;
    }

    @JsonIgnore
    public Double getMcpTemperature() {
        return config != null ? (Double) config.get("temperature") : null;
    }

    // GraphRAG-specific helper methods
    @JsonIgnore
    public String getNeo4jUri() {
        return config != null ? (String) config.get("neo4jUri") : null;
    }

    @JsonIgnore
    public String getNeo4jUsername() {
        return config != null ? (String) config.get("neo4jUsername") : null;
    }

    @JsonIgnore
    public String getNeo4jPassword() {
        return config != null ? (String) config.get("neo4jPassword") : null;
    }
}
