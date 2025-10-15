package com.bank.product.util;

import com.bank.product.context.ProcessingContext;
import com.bank.product.filter.ContextExtractionFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Utility class for accessing ProcessingContext from anywhere in the request lifecycle
 *
 * This provides a convenient way for services and components to access
 * the current request's processing context without passing it through
 * every method parameter.
 *
 * Usage:
 * <pre>
 * ProcessingContext context = ContextHolder.getContext();
 * String tenantId = ContextHolder.getTenantId();
 * String partyId = ContextHolder.getPartyId();
 * </pre>
 *
 * @author System Architecture Team
 * @since 1.0
 */
public class ContextHolder {

    /**
     * Get the current processing context from the request
     *
     * @return ProcessingContext or null if not available
     */
    public static ProcessingContext getContext() {
        HttpServletRequest request = getCurrentRequest();
        if (request == null) {
            return null;
        }

        return (ProcessingContext) request.getAttribute(
                ContextExtractionFilter.PROCESSING_CONTEXT_ATTRIBUTE);
    }

    /**
     * Get the current processing context, throwing exception if not available
     *
     * @return ProcessingContext (never null)
     * @throws IllegalStateException if context is not available
     */
    public static ProcessingContext getRequiredContext() {
        ProcessingContext context = getContext();
        if (context == null) {
            throw new IllegalStateException(
                    "ProcessingContext is required but not available in current request");
        }
        return context;
    }

    /**
     * Get tenant ID from current context
     *
     * @return Tenant ID or null if context not available
     */
    public static String getTenantId() {
        ProcessingContext context = getContext();
        return context != null ? context.getTenantId() : null;
    }

    /**
     * Get tenant ID, throwing exception if not available
     *
     * @return Tenant ID (never null)
     * @throws IllegalStateException if context is not available
     */
    public static String getRequiredTenantId() {
        return getRequiredContext().getTenantId();
    }

    /**
     * Get party ID from current context
     *
     * @return Party ID or null if context not available
     */
    public static String getPartyId() {
        ProcessingContext context = getContext();
        return context != null ? context.getPartyId() : null;
    }

    /**
     * Get party ID, throwing exception if not available
     *
     * @return Party ID (never null)
     * @throws IllegalStateException if context is not available
     */
    public static String getRequiredPartyId() {
        return getRequiredContext().getPartyId();
    }

    /**
     * Get principal ID from current context
     *
     * @return Principal ID or null if context not available
     */
    public static String getPrincipalId() {
        ProcessingContext context = getContext();
        return context != null ? context.getPrincipalId() : null;
    }

    /**
     * Check if a permission is granted in current context
     *
     * @param operation Operation name to check
     * @return true if permission is granted, false otherwise
     */
    public static boolean hasPermission(String operation) {
        ProcessingContext context = getContext();
        if (context == null || context.getPermissions() == null) {
            return false;
        }
        return context.getPermissions().hasPermission(operation);
    }

    /**
     * Check if context is available in current request
     *
     * @return true if context is available
     */
    public static boolean hasContext() {
        return getContext() != null;
    }

    /**
     * Get the current HTTP servlet request
     *
     * @return HttpServletRequest or null if not in request context
     */
    private static HttpServletRequest getCurrentRequest() {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

        if (attributes == null) {
            return null;
        }

        return attributes.getRequest();
    }
}
