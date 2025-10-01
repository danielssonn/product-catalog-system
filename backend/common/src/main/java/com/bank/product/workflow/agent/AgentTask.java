package com.bank.product.workflow.agent;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Individual agent task configuration
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentTask {

    /**
     * Agent identifier
     */
    private String agentId;

    /**
     * Agent type
     */
    private AgentType type;

    /**
     * Agent-specific configuration (MCPAgentConfig or GraphRAGAgentConfig)
     */
    private Object config;

    /**
     * Execution mode for this agent
     */
    private ExecutionMode mode;

    /**
     * Red flag detection conditions
     * Map of field -> condition expression
     * e.g., "fraudProbability": "> 0.75"
     */
    private Map<String, String> redFlagConditions;

    /**
     * Action to take if red flag detected
     */
    private RedFlagAction redFlagAction;

    /**
     * Reason template for red flag
     */
    private String redFlagReasonTemplate;

    /**
     * Roles to notify on red flag
     */
    private List<String> redFlagNotifyRoles;

    /**
     * Fields to extract for enrichment
     */
    private List<String> enrichmentOutputs;

    /**
     * Mapping of agent outputs to metadata fields
     */
    private Map<String, String> outputMapping;

    /**
     * Task priority (higher = more important)
     */
    private int priority;

    /**
     * Task timeout (milliseconds)
     */
    private int timeout;

    /**
     * Whether to retry on failure
     */
    private boolean retryOnFailure;

    /**
     * Max retry attempts
     */
    private int maxRetries;
}
