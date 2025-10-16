package com.bank.product.gateway.filter;

import com.bank.product.context.ProcessingContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Context Injection Filter for API Gateway
 *
 * This filter runs AFTER ContextResolutionFilter and injects the resolved
 * processing context into downstream HTTP requests as headers.
 *
 * Filter Chain Order:
 * 1. JwtAuthenticationFilter - validates JWT
 * 2. ContextResolutionFilter - resolves context from Party Service
 * 3. ContextInjectionFilter (this) - injects context headers
 * 4. Downstream service receives request with context
 *
 * Headers Injected:
 * - X-Processing-Context: Full context as JSON (Base64 encoded for header safety)
 * - X-Tenant-ID: Tenant ID (for quick filtering)
 * - X-Party-ID: Party ID (for quick filtering)
 * - X-Request-ID: Request correlation ID
 * - X-Channel-ID: Channel identifier
 * - X-Principal-ID: Principal/user ID
 *
 * These headers enable downstream services to:
 * 1. Access complete processing context without additional lookups
 * 2. Enforce tenant isolation at repository level
 * 3. Apply party-specific business rules
 * 4. Route to correct core banking system
 * 5. Enforce jurisdiction-specific compliance
 *
 * @author System Architecture Team
 * @since 1.0
 */
@Slf4j
@Component
public class ContextInjectionFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // Retrieve context from exchange attributes (set by ContextResolutionFilter)
        ProcessingContext context = exchange.getAttribute(ContextResolutionFilter.PROCESSING_CONTEXT_ATTRIBUTE);
        String requestId = exchange.getAttribute(ContextResolutionFilter.REQUEST_ID_ATTRIBUTE);

        // If no context available, continue without injecting headers
        if (context == null) {
            log.debug("No processing context available for request: {}", requestId);

            // Still inject request ID if available
            if (requestId != null) {
                return chain.filter(injectRequestId(exchange, requestId));
            }

            return chain.filter(exchange);
        }

        // Inject context headers into downstream request
        ServerHttpRequest modifiedRequest = exchange.getRequest().mutate()
                .header("X-Processing-Context", context.toBase64Json())
                .header("X-Tenant-ID", context.getTenantId())
                .header("X-Party-ID", context.getPartyId())
                .header("X-Request-ID", requestId != null ? requestId : context.getRequestId())
                .header("X-Channel-ID", context.getChannelId())
                .header("X-Principal-ID", context.getPrincipalId())
                .build();

        log.debug("Context headers injected for tenant: {}, party: {}, requestId: {}",
                context.getTenantId(), context.getPartyId(), requestId);

        // Continue with modified request
        return chain.filter(exchange.mutate().request(modifiedRequest).build());
    }

    /**
     * Inject only request ID when context is not available
     */
    private ServerWebExchange injectRequestId(ServerWebExchange exchange, String requestId) {
        ServerHttpRequest modifiedRequest = exchange.getRequest().mutate()
                .header("X-Request-ID", requestId)
                .build();

        return exchange.mutate().request(modifiedRequest).build();
    }

    @Override
    public int getOrder() {
        // Run after ContextResolutionFilter but before downstream routing
        return Ordered.HIGHEST_PRECEDENCE + 30;
    }
}
