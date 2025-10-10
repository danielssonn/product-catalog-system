package com.bank.product.core.model;

/**
 * Enumeration of supported core banking system types.
 * This abstraction allows the product catalog to work with multiple
 * core banking vendors without vendor lock-in.
 */
public enum CoreSystemType {
    /**
     * Temenos T24 - Widely used core banking platform
     */
    TEMENOS_T24,

    /**
     * FIS Profile - Modern core banking solution
     */
    FIS_PROFILE,

    /**
     * Finacle by Infosys - Popular in APAC region
     */
    FINACLE,

    /**
     * Jack Henry Symitar - Common in credit unions and community banks
     */
    JACK_HENRY_SYMITAR,

    /**
     * Oracle FLEXCUBE - Enterprise banking platform
     */
    ORACLE_FLEXCUBE,

    /**
     * Finastra Fusion - Cloud-native banking platform
     */
    FINASTRA_FUSION,

    /**
     * Custom adapter for proprietary or unsupported systems
     */
    CUSTOM
}
