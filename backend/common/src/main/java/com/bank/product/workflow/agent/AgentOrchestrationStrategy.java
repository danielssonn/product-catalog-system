package com.bank.product.workflow.agent;

/**
 * Strategy for orchestrating multiple agents
 */
public enum AgentOrchestrationStrategy {
    /**
     * All agents execute simultaneously
     */
    PARALLEL,

    /**
     * Execute agents one after another
     */
    SEQUENTIAL,

    /**
     * Execute based on previous results
     */
    CONDITIONAL,

    /**
     * Execute high priority agents first
     */
    PRIORITY_BASED
}
