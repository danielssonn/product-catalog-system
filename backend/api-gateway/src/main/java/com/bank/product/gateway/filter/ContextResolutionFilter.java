package com.bank.product.gateway.filter;

import com.bank.product.context.ProcessingContext;
import com.bank.product.gateway.client.PartyServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Context Resolution Filter for API Gateway
 *
 * This filter runs AFTER authentication and resolves the complete processing context
 * by calling the Party Service. The resolved context is stored in the exchange
 * attributes for use by the ContextInjectionFilter.
 *
 * Filter Chain Order:
 * 1. JwtAuthenticationFilter - validates JWT and sets SecurityContext
 * 2. ContextResolutionFilter (this) - resolves processing context
 * 3. ContextInjectionFilter - injects context into downstream requests
 * 4. Business logic filters
 *
 * Context Resolution Flow:
 * 1. Extract principal from SecurityContext
 * 2. Call Party Service POST /api/v1/context/resolve
 * 3. Store ProcessingContext in exchange attribute "processingContext"
 * 4. Continue filter chain
 *
 * Error Handling:
 * - If context resolution fails, log warning and continue WITHOUT context
 * - Downstream services will receive request without X-Processing-Context header
 * - Downstream services should handle missing context appropriately
 *
 * @author System Architecture Team
 * @since 1.0
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20) // After JWT filter, before context injection
@RequiredArgsConstructor
public class ContextResolutionFilter implements WebFilter {

    public static final String PROCESSING_CONTEXT_ATTRIBUTE = "processingContext";
    public static final String REQUEST_ID_ATTRIBUTE = "requestId";

    private final PartyServiceClient partyServiceClient;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        // Generate request ID for tracing
        String requestId = UUID.randomUUID().toString();
        exchange.getAttributes().put(REQUEST_ID_ATTRIBUTE, requestId);

        // Skip context resolution for public endpoints
        String path = exchange.getRequest().getPath().value();
        if (isPublicEndpoint(path)) {
            log.debug("Skipping context resolution for public endpoint: {}", path);
            return chain.filter(exchange);
        }

        // Resolve context from SecurityContext
        return ReactiveSecurityContextHolder.getContext()
                .flatMap(securityContext -> resolveAndStoreContext(exchange, securityContext, requestId))
                .then(chain.filter(exchange))
                .onErrorResume(error -> {
                    log.error("Error in context resolution filter: {}", error.getMessage(), error);
                    return chain.filter(exchange); // Continue even on error
                });
    }

    /**
     * Resolve processing context from SecurityContext and store in exchange
     */
    private Mono<Void> resolveAndStoreContext(
            ServerWebExchange exchange,
            SecurityContext securityContext,
            String requestId) {

        Authentication authentication = securityContext.getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            log.debug("No authenticated user, skipping context resolution");
            return Mono.empty();
        }

        // Extract principal information
        String principalId = authentication.getName(); // Username or principal ID
        String[] roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toArray(String[]::new);

        // Extract channel ID from request path or headers
        String channelId = extractChannelId(exchange);

        log.debug("Resolving context for principal: {}, roles: {}, channel: {}",
                principalId, roles, channelId);

        // Call Party Service to resolve context
        return partyServiceClient.resolveContext(principalId, principalId, roles, channelId, requestId)
                .doOnNext(context -> {
                    // Store context in exchange attributes
                    exchange.getAttributes().put(PROCESSING_CONTEXT_ATTRIBUTE, context);
                    log.info("Context resolved and stored for principal: {}, tenant: {}, party: {}",
                            principalId, context.getTenantId(), context.getPartyId());
                })
                .doOnError(error -> {
                    log.warn("Failed to resolve context for principal: {}, error: {}",
                            principalId, error.getMessage());
                })
                .onErrorResume(error -> {
                    // Don't fail the request if context resolution fails
                    log.warn("Continuing without context for principal: {}", principalId);
                    return Mono.empty();
                })
                .then();
    }

    /**
     * Extract channel ID from request path or headers
     */
    private String extractChannelId(ServerWebExchange exchange) {
        String path = exchange.getRequest().getPath().value();

        // Channel-based routing: /channel/{channelId}/...
        if (path.startsWith("/channel/")) {
            String[] parts = path.split("/");
            if (parts.length > 2) {
                return parts[2].toUpperCase(); // e.g., "HOST_TO_HOST", "PORTAL"
            }
        }

        // Check for X-Channel-ID header
        String channelHeader = exchange.getRequest().getHeaders().getFirst("X-Channel-ID");
        if (channelHeader != null) {
            return channelHeader.toUpperCase();
        }

        // Default channel
        return "UNKNOWN";
    }

    /**
     * Check if endpoint is public (no context resolution needed)
     */
    private boolean isPublicEndpoint(String path) {
        return path.startsWith("/actuator/health") ||
               path.startsWith("/oauth/") ||
               path.startsWith("/test/") ||
               path.startsWith("/api/") && path.contains("/public/");
    }
}
