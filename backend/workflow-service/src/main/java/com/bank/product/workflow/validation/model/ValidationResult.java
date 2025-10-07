package com.bank.product.workflow.validation.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Result of a validation execution (rules-based, MCP, or GraphRAG)
 * This replaces the misleading "AgentDecision" terminology
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidationResult {

    /**
     * ID of the validator that produced this result
     */
    private String validatorId;

    /**
     * Type of validator (RULES_BASED, MCP, GRAPH_RAG)
     */
    private ValidatorType validatorType;

    /**
     * When the validation executed
     */
    private LocalDateTime executedAt;

    /**
     * How long the validation took to execute
     */
    private Duration executionTime;

    /**
     * Whether a red flag was detected
     */
    private boolean redFlagDetected;

    /**
     * Reason for the red flag
     */
    private String redFlagReason;

    /**
     * Severity of the red flag
     */
    private RedFlagSeverity severity;

    /**
     * Recommended action
     */
    private RedFlagAction recommendedAction;

    /**
     * Data to enrich the entity with
     */
    private Map<String, Object> enrichmentData;

    /**
     * Validation/reasoning trace (for explainability)
     * - Rules-based: execution trace of rules
     * - MCP: LLM reasoning steps
     * - GraphRAG: retrieval + reasoning trace
     */
    private List<ValidationStep> validationSteps;

    /**
     * Confidence score (0.0 - 1.0)
     * - Rules-based: deterministic (0.0 or 1.0)
     * - MCP/GraphRAG: model confidence
     */
    private double confidenceScore;

    /**
     * Model used for AI-powered validation (e.g., "claude-3-5-sonnet-20241022")
     * Null for rules-based validation
     */
    private String model;

    /**
     * Additional metadata
     */
    private Map<String, Object> metadata;

    /**
     * Whether the validation execution succeeded
     */
    private boolean success;

    /**
     * Error message if validation failed
     */
    private String errorMessage;
}
