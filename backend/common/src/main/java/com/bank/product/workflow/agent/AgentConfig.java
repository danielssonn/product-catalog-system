package com.bank.product.workflow.agent;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Configuration for agent execution in workflow
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentConfig {

    /**
     * Whether agents are enabled
     */
    private boolean enableAgents;

    /**
     * Execution mode
     */
    private ExecutionMode mode;

    /**
     * List of agent tasks
     */
    private List<AgentTask> agents;

    /**
     * Overall timeout for all agents (milliseconds)
     */
    private int timeout;

    /**
     * Whether to continue on timeout
     */
    private boolean continueOnTimeout;

    /**
     * Orchestration strategy
     */
    private AgentOrchestrationStrategy orchestrationStrategy;

    /**
     * Whether to fail workflow on agent error
     */
    private boolean failOnAgentError;

    /**
     * Additional configuration
     */
    private Map<String, Object> additionalConfig;
}
