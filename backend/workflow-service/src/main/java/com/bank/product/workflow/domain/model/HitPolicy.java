package com.bank.product.workflow.domain.model;

/**
 * Decision table hit policy
 */
public enum HitPolicy {
    /**
     * First matching rule wins
     */
    FIRST,

    /**
     * All matching rules apply
     */
    ALL,

    /**
     * Highest priority rule wins
     */
    PRIORITY,

    /**
     * Collect all outputs from matching rules
     */
    COLLECT
}
