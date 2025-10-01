package com.bank.product.workflow.agent;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Decision made by an AI agent during workflow execution
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentDecision {

    /**
     * Agent identifier
     */
    private String agentId;

    /**
     * Type of agent
     */
    private AgentType agentType;

    /**
     * When the decision was made
     */
    private LocalDateTime executedAt;

    /**
     * Execution duration
     */
    private Duration executionTime;

    /**
     * Whether a red flag was detected
     */
    private boolean redFlagDetected;

    /**
     * Reason for red flag
     */
    private String redFlagReason;

    /**
     * Severity of red flag
     */
    private RedFlagSeverity severity;

    /**
     * Recommended action
     */
    private RedFlagAction recommendedAction;

    /**
     * Enrichment data to add to entity metadata
     */
    private Map<String, Object> enrichmentData;

    /**
     * Agent reasoning trace for explainability
     */
    private List<AgentReasoningStep> reasoningSteps;

    /**
     * Confidence score (0-1)
     */
    private double confidenceScore;

    /**
     * Model used (for MCP agents)
     */
    private String model;

    /**
     * Additional agent metadata
     */
    private Map<String, Object> agentMetadata;

    /**
     * Whether execution was successful
     */
    private boolean success;

    /**
     * Error message if failed
     */
    private String errorMessage;
}
