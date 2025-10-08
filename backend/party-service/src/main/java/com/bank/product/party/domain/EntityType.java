package com.bank.product.party.domain;

/**
 * Types of legal entities
 */
public enum EntityType {
    /**
     * Corporation (Inc., Corp.)
     */
    CORPORATION,

    /**
     * Limited Liability Company
     */
    LLC,

    /**
     * Limited Liability Partnership
     */
    LLP,

    /**
     * General Partnership
     */
    PARTNERSHIP,

    /**
     * Limited Partnership
     */
    LIMITED_PARTNERSHIP,

    /**
     * Sole Proprietorship
     */
    SOLE_PROPRIETORSHIP,

    /**
     * Trust
     */
    TRUST,

    /**
     * Public Limited Company (PLC)
     */
    PLC,

    /**
     * Private Limited Company
     */
    PRIVATE_LIMITED,

    /**
     * Special Purpose Vehicle
     */
    SPV,

    /**
     * Non-Profit Organization
     */
    NON_PROFIT,

    /**
     * Government Entity
     */
    GOVERNMENT
}
