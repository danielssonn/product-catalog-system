package com.bank.product.version.domain.model;

/**
 * Schema Transformation Type
 */
public enum TransformationType {
    /**
     * Simple field mappings only
     */
    SIMPLE,

    /**
     * Complex transformations with functions
     */
    COMPLEX,

    /**
     * Custom script-based transformation
     */
    SCRIPTED,

    /**
     * Bidirectional transformation (can go both ways)
     */
    BIDIRECTIONAL
}
