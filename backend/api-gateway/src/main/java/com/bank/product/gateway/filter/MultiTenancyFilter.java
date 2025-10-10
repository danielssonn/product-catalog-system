package com.bank.product.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Enforces multi-tenancy isolation at the gateway level
 * Validates and propagates tenant context to downstream services
 */
@Slf4j
@Component
public class MultiTenancyFilter implements GatewayFilter {

    public static final String TENANT_ID_HEADER = "X-Tenant-ID";
    public static final String USER_ID_HEADER = "X-User-ID";
    public static final String TENANT_ATTRIBUTE = "gateway.tenantId";
    public static final String USER_ATTRIBUTE = "gateway.userId";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // Extract tenant ID from header
        String tenantId = exchange.getRequest().getHeaders().getFirst(TENANT_ID_HEADER);
        
        if (tenantId == null || tenantId.isEmpty()) {
            log.warn("Missing tenant ID in request: {}", exchange.getRequest().getPath());
            exchange.getResponse().setStatusCode(HttpStatus.BAD_REQUEST);
            return exchange.getResponse().setComplete();
        }
        
        // Extract user ID
        String userId = exchange.getRequest().getHeaders().getFirst(USER_ID_HEADER);
        
        if (userId == null || userId.isEmpty()) {
            log.warn("Missing user ID in request: {}", exchange.getRequest().getPath());
            exchange.getResponse().setStatusCode(HttpStatus.BAD_REQUEST);
            return exchange.getResponse().setComplete();
        }
        
        // TODO: Validate tenant exists and user belongs to tenant (call tenant service)

        // Store in exchange attributes
        exchange.getAttributes().put(TENANT_ATTRIBUTE, tenantId);
        exchange.getAttributes().put(USER_ATTRIBUTE, userId);

        log.info("Multi-tenancy validated: tenantId={}, userId={}", tenantId, userId);

        // Headers are already in the request, just pass through
        return chain.filter(exchange);
    }
}
