package com.bank.product.filter;

import com.bank.product.context.ContextExtractor;
import com.bank.product.context.InvalidContextException;
import com.bank.product.context.MissingContextException;
import com.bank.product.context.ProcessingContext;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Context Extraction Filter for Product Service
 *
 * Extracts and validates ProcessingContext from X-Processing-Context header
 * injected by the API Gateway. This filter runs early in the filter chain
 * to ensure context is available for all subsequent processing.
 *
 * Filter Responsibilities:
 * 1. Extract ProcessingContext from HTTP header
 * 2. Validate context (not expired, party active, etc.)
 * 3. Store context in request attribute for controller/service access
 * 4. Enforce mandatory context for protected endpoints
 *
 * Context Storage:
 * - Stored in request attribute: "processingContext"
 * - Controllers can access via @RequestAttribute or ContextExtractor
 *
 * Error Handling:
 * - Missing context on protected endpoint → HTTP 400 Bad Request
 * - Invalid/expired context → HTTP 401 Unauthorized
 * - Context validation errors → HTTP 401 Unauthorized
 *
 * @author System Architecture Team
 * @since 1.0
 */
@Slf4j
@Component
@Order(1) // Run early in filter chain
@RequiredArgsConstructor
public class ContextExtractionFilter implements Filter {

    public static final String PROCESSING_CONTEXT_ATTRIBUTE = "processingContext";

    private final ContextExtractor contextExtractor;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String path = httpRequest.getRequestURI();

        // Skip context extraction for public/health endpoints
        if (isPublicEndpoint(path)) {
            log.debug("Skipping context extraction for public endpoint: {}", path);
            chain.doFilter(request, response);
            return;
        }

        try {
            // Extract and validate context
            ProcessingContext context = contextExtractor.extractContext(httpRequest);

            if (context != null) {
                // Validate context
                if (!context.isValid()) {
                    log.warn("Invalid or expired context for request: {}", path);
                    throw new InvalidContextException("Context has expired or is invalid");
                }

                // Store context in request attribute
                httpRequest.setAttribute(PROCESSING_CONTEXT_ATTRIBUTE, context);

                log.debug("Context extracted for tenant: {}, party: {}, path: {}",
                        context.getTenantId(), context.getPartyId(), path);

                // Continue with context
                chain.doFilter(request, response);
            } else {
                // No context provided - decide if this is acceptable
                if (requiresContext(path)) {
                    log.warn("Missing required context for protected endpoint: {}", path);
                    throw new MissingContextException("X-Processing-Context header is required");
                } else {
                    // Allow request without context (for backwards compatibility)
                    log.debug("Proceeding without context for path: {}", path);
                    chain.doFilter(request, response);
                }
            }

        } catch (MissingContextException e) {
            log.error("Missing context for request {}: {}", path, e.getMessage());
            httpResponse.sendError(HttpServletResponse.SC_BAD_REQUEST,
                    "Missing X-Processing-Context header");

        } catch (InvalidContextException e) {
            log.error("Invalid context for request {}: {}", path, e.getMessage());
            httpResponse.sendError(HttpServletResponse.SC_UNAUTHORIZED,
                    "Invalid or expired processing context");

        } catch (Exception e) {
            log.error("Error extracting context for request {}", path, e);
            httpResponse.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Error processing request context");
        }
    }

    /**
     * Check if endpoint is public (no context required)
     */
    private boolean isPublicEndpoint(String path) {
        return path.startsWith("/actuator/health") ||
               path.startsWith("/actuator/info") ||
               path.startsWith("/api/") && path.contains("/public/");
    }

    /**
     * Check if endpoint requires context
     *
     * For now, all non-public endpoints require context.
     * This can be refined based on specific endpoint patterns.
     */
    private boolean requiresContext(String path) {
        // Require context for all solution and catalog operations
        return path.startsWith("/api/v1/solutions") ||
               path.startsWith("/api/v1/catalog") ||
               path.startsWith("/api/v2/");
    }
}
