package com.bank.product.version.domain.model;

/**
 * API Versioning Strategy
 */
public enum VersioningStrategy {
    /**
     * Version in URL path (e.g., /api/v1/solutions)
     */
    URL_PATH,

    /**
     * Version in request header (e.g., X-API-Version: v1)
     */
    HEADER,

    /**
     * Version in query parameter (e.g., ?version=v1)
     */
    QUERY_PARAM,

    /**
     * Content negotiation (e.g., Accept: application/vnd.bank.v1+json)
     */
    CONTENT_NEGOTIATION,

    /**
     * Custom version detection strategy
     */
    CUSTOM
}
