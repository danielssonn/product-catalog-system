package com.bank.product.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Party-aware routing filter
 * Enriches requests with party context based on user identity
 */
@Slf4j
@Component
public class PartyAwareFilter implements GatewayFilter {

    public static final String PARTY_ID_HEADER = "X-Party-ID";
    public static final String PARTY_ATTRIBUTE = "gateway.partyId";
    public static final String PARTY_ROLES_HEADER = "X-Party-Roles";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // Extract party ID from header if provided
        String partyId = exchange.getRequest().getHeaders().getFirst(PARTY_ID_HEADER);
        
        if (partyId != null && !partyId.isEmpty()) {
            // Store in exchange attributes
            exchange.getAttributes().put(PARTY_ATTRIBUTE, partyId);
            
            // TODO: Lookup party details from party service
            // TODO: Validate user has permission to act on behalf of this party
            // TODO: Load party roles and relationships
            
            log.info("Party context: partyId={}", partyId);
            
            // Propagate to downstream services
            return chain.filter(exchange.mutate()
                .request(exchange.getRequest().mutate()
                    .header(PARTY_ID_HEADER, partyId)
                    .build())
                .build());
        }
        
        // If no party ID provided, try to resolve from user
        String userId = exchange.getRequest().getHeaders().getFirst(MultiTenancyFilter.USER_ID_HEADER);
        if (userId != null) {
            // TODO: Lookup default party for user
            log.debug("No party ID provided, using default for user: {}", userId);
        }
        
        return chain.filter(exchange);
    }
}
