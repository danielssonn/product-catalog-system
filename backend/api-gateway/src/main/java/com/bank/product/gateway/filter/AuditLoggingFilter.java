package com.bank.product.gateway.filter;

import com.bank.product.gateway.model.ApiAuditLog;
import com.bank.product.gateway.model.Channel;
import com.bank.product.gateway.service.AuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

/**
 * Comprehensive audit logging for all API requests
 * Logs to MongoDB for compliance and analytics
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuditLoggingFilter implements GatewayFilter {

    private final AuditService auditService;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String requestId = UUID.randomUUID().toString();
        Instant startTime = Instant.now();
        
        // Extract context from exchange
        Channel channel = (Channel) exchange.getAttributes().get(ChannelIdentificationFilter.CHANNEL_ATTRIBUTE);
        String tenantId = (String) exchange.getAttributes().get(MultiTenancyFilter.TENANT_ATTRIBUTE);
        String userId = (String) exchange.getAttributes().get(MultiTenancyFilter.USER_ATTRIBUTE);
        String partyId = (String) exchange.getAttributes().get(PartyAwareFilter.PARTY_ATTRIBUTE);
        
        // Get request details
        String path = exchange.getRequest().getPath().toString();
        String method = exchange.getRequest().getMethod().name();
        String sourceIp = getClientIp(exchange);
        String userAgent = exchange.getRequest().getHeaders().getFirst("User-Agent");
        String apiVersion = exchange.getRequest().getHeaders().getFirst("X-API-Version");
        String idempotencyKey = exchange.getRequest().getHeaders().getFirst("X-Idempotency-Key");
        
        // Add request ID to exchange
        exchange.getAttributes().put("gateway.requestId", requestId);
        
        log.info("Request: requestId={}, channel={}, tenantId={}, userId={}, path={}, method={}", 
            requestId, channel, tenantId, userId, path, method);
        
        // Continue filter chain and measure duration
        return chain.filter(exchange)
            .doOnSuccess(aVoid -> {
                long durationMs = Instant.now().toEpochMilli() - startTime.toEpochMilli();
                int statusCode = exchange.getResponse().getStatusCode() != null ? 
                    exchange.getResponse().getStatusCode().value() : 200;
                
                // Create audit log
                ApiAuditLog auditLog = ApiAuditLog.builder()
                    .requestId(requestId)
                    .tenantId(tenantId)
                    .userId(userId)
                    .partyId(partyId)
                    .channel(channel)
                    .path(path)
                    .method(method)
                    .apiVersion(apiVersion)
                    .timestamp(startTime)
                    .statusCode(statusCode)
                    .durationMs(durationMs)
                    .sourceIp(sourceIp)
                    .userAgent(userAgent)
                    .idempotencyKey(idempotencyKey)
                    .build();
                
                // Save asynchronously
                auditService.logRequest(auditLog).subscribe();
                
                log.info("Response: requestId={}, statusCode={}, durationMs={}", 
                    requestId, statusCode, durationMs);
            })
            .doOnError(error -> {
                long durationMs = Instant.now().toEpochMilli() - startTime.toEpochMilli();
                
                // Create error audit log
                ApiAuditLog auditLog = ApiAuditLog.builder()
                    .requestId(requestId)
                    .tenantId(tenantId)
                    .userId(userId)
                    .partyId(partyId)
                    .channel(channel)
                    .path(path)
                    .method(method)
                    .apiVersion(apiVersion)
                    .timestamp(startTime)
                    .statusCode(500)
                    .durationMs(durationMs)
                    .sourceIp(sourceIp)
                    .userAgent(userAgent)
                    .errorMessage(error.getMessage())
                    .build();
                
                // Save asynchronously
                auditService.logRequest(auditLog).subscribe();
                
                log.error("Error: requestId={}, error={}, durationMs={}", 
                    requestId, error.getMessage(), durationMs);
            });
    }
    
    private String getClientIp(ServerWebExchange exchange) {
        String xForwardedFor = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        if (exchange.getRequest().getRemoteAddress() != null) {
            return exchange.getRequest().getRemoteAddress().getAddress().getHostAddress();
        }
        
        return "unknown";
    }
}
