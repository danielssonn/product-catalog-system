package com.bank.product.workflow.agent;

/**
 * Agent execution mode
 */
public enum ExecutionMode {
    /**
     * Async execution, terminate on red flag
     */
    ASYNC_RED_FLAG,

    /**
     * Synchronous execution to enrich metadata
     */
    SYNC_ENRICHMENT,

    /**
     * Hybrid approach
     */
    HYBRID
}
