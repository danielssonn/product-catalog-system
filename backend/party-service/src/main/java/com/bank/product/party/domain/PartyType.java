package com.bank.product.party.domain;

/**
 * Types of party entities in the federated model
 */
public enum PartyType {
    /**
     * Top-level business organization
     */
    ORGANIZATION,

    /**
     * Legal entity (corporation, LLC, partnership, etc.)
     */
    LEGAL_ENTITY,

    /**
     * Natural person (for beneficial ownership, authorized signers)
     */
    INDIVIDUAL,

    /**
     * Subsidiary or division
     */
    SUBSIDIARY,

    /**
     * Special purpose vehicle
     */
    SPV,

    /**
     * Trust or fiduciary entity
     */
    TRUST,

    /**
     * Government entity
     */
    GOVERNMENT_ENTITY
}
