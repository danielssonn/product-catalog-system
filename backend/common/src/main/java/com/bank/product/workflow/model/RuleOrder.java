package com.bank.product.workflow.model;

/**
 * Rule evaluation order
 */
public enum RuleOrder {
    /**
     * Evaluate rules in declaration order
     */
    DECLARATION,

    /**
     * Evaluate rules by priority (highest first)
     */
    PRIORITY,

    /**
     * Evaluate rules by specificity (most specific first)
     */
    SPECIFICITY
}
