package com.bank.product.workflow.validation.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Represents a single step in a validation process
 * Provides explainability and audit trail
 *
 * For rules-based: Execution trace of deterministic rules
 * For MCP/GraphRAG: LLM reasoning steps
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidationStep {

    /**
     * Step number in the sequence
     */
    private int stepNumber;

    /**
     * Name of this step
     */
    private String stepName;

    /**
     * Tool/function used in this step
     * Rules-based: "document_presence_checker", "consistency_checker"
     * MCP: "claude_analyze", "document_extract"
     * GraphRAG: "neo4j_query", "regulatory_retrieve"
     */
    private String tool;

    /**
     * Input to the tool
     */
    private Map<String, Object> input;

    /**
     * Output from the tool
     */
    private Map<String, Object> output;

    /**
     * Reasoning/explanation for this step
     * Rules-based: Simple explanation
     * MCP/GraphRAG: Rich LLM reasoning
     */
    private String reasoning;

    /**
     * When this step started
     */
    private LocalDateTime timestamp;

    /**
     * How long this step took
     */
    private Duration duration;

    /**
     * Whether this step succeeded
     */
    private boolean success;

    /**
     * Error message if step failed
     */
    private String errorMessage;
}
