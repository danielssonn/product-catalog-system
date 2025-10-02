package com.bank.product.version.domain.model;

/**
 * API Version Lifecycle Status
 */
public enum VersionStatus {
    /**
     * Version is in development/beta
     */
    BETA,

    /**
     * Version is stable and recommended for production
     */
    STABLE,

    /**
     * Version is deprecated but still supported
     */
    DEPRECATED,

    /**
     * Version is in sunset period (read-only, no new features)
     */
    SUNSET,

    /**
     * Version has reached end-of-life (no longer available)
     */
    EOL
}
