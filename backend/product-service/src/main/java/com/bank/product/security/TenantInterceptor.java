package com.bank.product.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Intercepts all requests to extract and validate X-Tenant-ID header.
 * Populates TenantContext for use throughout request processing.
 *
 * Automatically applied to all /api/v1/** endpoints (except health checks).
 */
@Slf4j
@Component
public class TenantInterceptor implements HandlerInterceptor {

    private static final String TENANT_HEADER = "X-Tenant-ID";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String requestUri = request.getRequestURI();

        // Skip tenant check for health endpoints and public APIs
        if (isPublicEndpoint(requestUri)) {
            log.debug("Skipping tenant check for public endpoint: {}", requestUri);
            return true;
        }

        // Skip tenant check for callback endpoints (authenticated via service credentials)
        if (isCallbackEndpoint(requestUri)) {
            log.debug("Skipping tenant check for callback endpoint: {}", requestUri);
            return true;
        }

        // Extract tenant ID from header
        String tenantId = request.getHeader(TENANT_HEADER);

        if (tenantId == null || tenantId.trim().isEmpty()) {
            log.warn("Missing {} header for request: {} {}", TENANT_HEADER, request.getMethod(), requestUri);
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.setContentType("application/json");
            try {
                response.getWriter().write("{\"error\":\"Missing X-Tenant-ID header\",\"status\":400}");
            } catch (Exception e) {
                log.error("Error writing response", e);
            }
            return false;
        }

        // Set tenant context for this request
        TenantContext.setCurrentTenant(tenantId);
        log.debug("Tenant context set: {} for request: {} {}", tenantId, request.getMethod(), requestUri);

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                 Object handler, Exception ex) {
        // Always clear tenant context to prevent leaks
        TenantContext.clear();
    }

    /**
     * Public endpoints that don't require tenant context
     */
    private boolean isPublicEndpoint(String uri) {
        return uri.startsWith("/actuator/")
            || uri.startsWith("/swagger-ui")
            || uri.startsWith("/v3/api-docs")
            || uri.startsWith("/api/v1/admin/");  // Admin endpoints are global (product types, catalog management)
    }

    /**
     * Callback endpoints called by workflow service (no X-Tenant-ID header)
     * These endpoints will retrieve tenant from the entity itself
     */
    private boolean isCallbackEndpoint(String uri) {
        return uri.matches(".*/solutions/[^/]+/activate")
            || uri.matches(".*/solutions/[^/]+/reject");
    }
}
