package com.bank.product.party.domain;

/**
 * Type of control in ownership relationships
 */
public enum ControlType {
    /**
     * Control through voting rights
     */
    VOTING,

    /**
     * Economic control (profit/loss sharing)
     */
    ECONOMIC,

    /**
     * Operational control (management authority)
     */
    OPERATIONAL,

    /**
     * Control through board representation
     */
    BOARD,

    /**
     * Combined control types
     */
    COMBINED
}
