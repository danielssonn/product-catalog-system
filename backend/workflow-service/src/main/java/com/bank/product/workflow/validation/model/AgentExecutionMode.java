package com.bank.product.workflow.validation.model;

/**
 * Execution mode for agents in workflows
 */
public enum AgentExecutionMode {
    /**
     * Agent runs asynchronously and can trigger early workflow termination
     * on red flag detection
     */
    ASYNC_RED_FLAG,

    /**
     * Agent runs synchronously to enrich entity metadata before rule evaluation
     */
    SYNC_ENRICHMENT,

    /**
     * Hybrid mode - can do both enrichment and red flag detection
     */
    HYBRID
}
