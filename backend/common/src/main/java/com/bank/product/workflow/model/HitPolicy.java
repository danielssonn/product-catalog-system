package com.bank.product.workflow.model;

/**
 * Decision table hit policy - determines how to handle multiple matching rules
 */
public enum HitPolicy {
    /**
     * First matching rule wins
     */
    FIRST,

    /**
     * All matching rules apply (outputs merged)
     */
    ALL,

    /**
     * Highest priority rule wins
     */
    PRIORITY,

    /**
     * Collect all matching outputs
     */
    COLLECT,

    /**
     * No rules can overlap (error if multiple match)
     */
    UNIQUE,

    /**
     * Any single matching rule
     */
    ANY
}
