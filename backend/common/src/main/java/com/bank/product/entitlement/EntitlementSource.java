package com.bank.product.entitlement;

/**
 * Source of an entitlement (how it was granted)
 */
public enum EntitlementSource {
    /**
     * Granted based on role (e.g., ROLE_ADMIN grants all permissions)
     */
    ROLE_BASED,

    /**
     * Granted based on relationship in party graph
     * (e.g., AuthorizedSigner relationship grants TRANSACT permission)
     */
    RELATIONSHIP_BASED,

    /**
     * Explicitly granted by an administrator
     */
    EXPLICIT_GRANT,

    /**
     * Inherited from parent entity in hierarchy
     * (e.g., organization admin inherits permissions for subsidiary accounts)
     */
    INHERITED,

    /**
     * Granted based on ownership
     * (e.g., beneficial owner with >50% ownership)
     */
    OWNERSHIP_BASED,

    /**
     * Temporary delegation from another party
     */
    DELEGATED,

    /**
     * Default permissions for resource creator
     */
    OWNER,

    /**
     * System-generated entitlement
     */
    SYSTEM
}
