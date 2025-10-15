package com.bank.product.context;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Context Extraction Utility
 *
 * Used by all business services to extract processing context from HTTP headers.
 * This is a mandatory component for all services following the Context Resolution Architecture.
 *
 * @author System Architecture Team
 * @since 1.0
 * @see ProcessingContext
 */
@Component
@Slf4j
public class ContextExtractor {

    // HTTP Header names
    public static final String CONTEXT_HEADER = "X-Processing-Context";
    public static final String TENANT_HEADER = "X-Tenant-ID";
    public static final String PARTY_HEADER = "X-Party-ID";
    public static final String JURISDICTION_HEADER = "X-Jurisdiction";
    public static final String REQUEST_ID_HEADER = "X-Request-ID";

    /**
     * Extract full processing context from request headers
     *
     * @param request HTTP servlet request
     * @return ProcessingContext
     * @throws MissingContextException if context header is missing
     * @throws InvalidContextException if context is invalid or expired
     */
    public ProcessingContext extractContext(HttpServletRequest request) {
        String contextJson = request.getHeader(CONTEXT_HEADER);

        if (contextJson == null || contextJson.isEmpty()) {
            log.error("X-Processing-Context header is missing");
            throw new MissingContextException(
                    "X-Processing-Context header is required for all requests. " +
                            "Ensure request passes through API Gateway for context resolution."
            );
        }

        ProcessingContext context;
        try {
            context = ProcessingContext.fromJson(contextJson);
        } catch (ContextSerializationException e) {
            log.error("Failed to deserialize context from header", e);
            throw new InvalidContextException("Failed to parse processing context", e);
        }

        if (!context.isValid()) {
            log.error("Processing context is invalid or expired: {}", context);
            throw new InvalidContextException(
                    "Processing context is expired or invalid. Please re-authenticate."
            );
        }

        log.debug("Successfully extracted context: tenantId={}, partyId={}, principalId={}, requestId={}",
                context.getTenantId(), context.getPartyId(), context.getPrincipalId(), context.getRequestId());

        return context;
    }

    /**
     * Extract context with Base64 decoding
     *
     * @param request HTTP servlet request
     * @return ProcessingContext
     */
    public ProcessingContext extractBase64Context(HttpServletRequest request) {
        String base64Json = request.getHeader(CONTEXT_HEADER);

        if (base64Json == null || base64Json.isEmpty()) {
            throw new MissingContextException("X-Processing-Context header is required");
        }

        try {
            ProcessingContext context = ProcessingContext.fromBase64Json(base64Json);
            if (!context.isValid()) {
                throw new InvalidContextException("Processing context is expired or invalid");
            }
            return context;
        } catch (Exception e) {
            log.error("Failed to extract Base64 context", e);
            throw new InvalidContextException("Failed to parse processing context", e);
        }
    }

    /**
     * Extract tenant ID quickly (without full context deserialization)
     * Use this for performance-critical paths where only tenant is needed
     *
     * @param request HTTP servlet request
     * @return tenant ID
     */
    public String extractTenantId(HttpServletRequest request) {
        // Try quick header first
        String tenantId = request.getHeader(TENANT_HEADER);
        if (tenantId != null && !tenantId.isEmpty()) {
            return tenantId;
        }

        // Fallback to full context extraction
        ProcessingContext context = extractContext(request);
        return context.getTenantId();
    }

    /**
     * Extract party ID quickly
     *
     * @param request HTTP servlet request
     * @return party ID
     */
    public String extractPartyId(HttpServletRequest request) {
        String partyId = request.getHeader(PARTY_HEADER);
        if (partyId != null && !partyId.isEmpty()) {
            return partyId;
        }

        ProcessingContext context = extractContext(request);
        return context.getPartyId();
    }

    /**
     * Extract request ID for correlation
     *
     * @param request HTTP servlet request
     * @return request ID
     */
    public String extractRequestId(HttpServletRequest request) {
        String requestId = request.getHeader(REQUEST_ID_HEADER);
        if (requestId != null && !requestId.isEmpty()) {
            return requestId;
        }

        ProcessingContext context = extractContext(request);
        return context.getRequestId();
    }

    /**
     * Check if request has valid context
     *
     * @param request HTTP servlet request
     * @return true if context is present and valid
     */
    public boolean hasValidContext(HttpServletRequest request) {
        try {
            ProcessingContext context = extractContext(request);
            return context.isValid();
        } catch (Exception e) {
            log.debug("Request does not have valid context", e);
            return false;
        }
    }

    /**
     * Try to extract context, return null if not present or invalid
     *
     * @param request HTTP servlet request
     * @return ProcessingContext or null
     */
    public ProcessingContext tryExtractContext(HttpServletRequest request) {
        try {
            return extractContext(request);
        } catch (Exception e) {
            log.debug("Could not extract context: {}", e.getMessage());
            return null;
        }
    }
}
