package com.bank.product.security;

import lombok.extern.slf4j.Slf4j;

/**
 * Thread-local storage for current tenant context.
 * Automatically populated from X-Tenant-ID header by TenantInterceptor.
 *
 * Usage:
 * <pre>
 * String tenantId = TenantContext.getCurrentTenant();
 * TenantContext.setCurrentTenant("tenant-123");
 * TenantContext.clear();
 * </pre>
 */
@Slf4j
public class TenantContext {

    private static final ThreadLocal<String> CURRENT_TENANT = new ThreadLocal<>();

    private TenantContext() {
        // Utility class
    }

    /**
     * Set the current tenant ID for this thread
     */
    public static void setCurrentTenant(String tenantId) {
        if (tenantId == null || tenantId.trim().isEmpty()) {
            log.warn("Attempted to set null or empty tenant ID");
            throw new IllegalArgumentException("Tenant ID cannot be null or empty");
        }
        log.debug("Setting tenant context: {}", tenantId);
        CURRENT_TENANT.set(tenantId);
    }

    /**
     * Get the current tenant ID for this thread
     * @throws TenantContextException if no tenant is set
     */
    public static String getCurrentTenant() {
        String tenantId = CURRENT_TENANT.get();
        if (tenantId == null) {
            throw new TenantContextException("No tenant context found. Ensure X-Tenant-ID header is present.");
        }
        return tenantId;
    }

    /**
     * Get the current tenant ID, or null if not set
     */
    public static String getCurrentTenantOrNull() {
        return CURRENT_TENANT.get();
    }

    /**
     * Check if tenant context is set
     */
    public static boolean isSet() {
        return CURRENT_TENANT.get() != null;
    }

    /**
     * Clear the tenant context for this thread
     * IMPORTANT: Must be called in finally block to prevent leaks
     */
    public static void clear() {
        String tenantId = CURRENT_TENANT.get();
        if (tenantId != null) {
            log.debug("Clearing tenant context: {}", tenantId);
        }
        CURRENT_TENANT.remove();
    }

    /**
     * Exception thrown when tenant context is required but not set
     */
    public static class TenantContextException extends RuntimeException {
        public TenantContextException(String message) {
            super(message);
        }
    }
}
