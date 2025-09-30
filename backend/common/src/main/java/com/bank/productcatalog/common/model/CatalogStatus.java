package com.bank.productcatalog.common.model;

public enum CatalogStatus {
    AVAILABLE,      // Available for tenant selection
    PREVIEW,        // Preview mode, not yet available for production
    DEPRECATED,     // No longer recommended, but existing configs remain
    RETIRED         // Removed from catalog, no new configurations allowed
}