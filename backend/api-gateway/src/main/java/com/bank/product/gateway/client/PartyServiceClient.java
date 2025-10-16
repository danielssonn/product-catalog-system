package com.bank.product.gateway.client;

import com.bank.product.context.ProcessingContext;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * Client for Party Service Context Resolution API
 *
 * This client calls the Party Service to resolve processing context
 * from authenticated principal information.
 *
 * Resilience Patterns Applied:
 * - Circuit Breaker: Opens after 50% failures in 10 call window, prevents cascading failures
 * - Retry: 3 attempts with exponential backoff (500ms, 1s, 2s)
 * - Bulkhead: Limits concurrent calls to Party Service (max 10)
 * - Timeout: 5 seconds (configurable via application.yml)
 * - Fallback: Returns empty context on failure, allows request to continue
 *
 * Exception Handling Strategy:
 * - Transient errors (5xx, timeouts, IOExceptions): Retry with backoff
 * - Client errors (4xx): No retry, fail fast
 * - Circuit open: Fallback immediately without calling service
 *
 * @author System Architecture Team
 * @since 1.0
 */
@Slf4j
@Component
public class PartyServiceClient {

    private final WebClient webClient;
    private final Duration timeout;

    public PartyServiceClient(
            @Value("${services.party-service.url}") String partyServiceUrl,
            @Value("${services.party-service.timeout:5000}") int timeoutMs) {
        this.webClient = WebClient.builder()
                .baseUrl(partyServiceUrl)
                .build();
        this.timeout = Duration.ofMillis(timeoutMs);
    }

    /**
     * Resolve processing context from principal information
     *
     * Resilience patterns applied:
     * - @CircuitBreaker: Opens after 50% failures, prevents cascading failures
     * - @Retry: 3 attempts with exponential backoff (500ms → 1s → 2s)
     * - @Bulkhead: Max 10 concurrent calls to Party Service
     *
     * @param principalId Principal ID from authentication
     * @param username Username (optional)
     * @param roles User roles
     * @param channelId Channel identifier
     * @param requestId Request ID for correlation
     * @return ProcessingContext (wrapped in Mono), or empty Mono on failure
     */
    @CircuitBreaker(name = "party-service-cb", fallbackMethod = "fallbackResolveContext")
    @Retry(name = "party-service-retry")
    @Bulkhead(name = "party-service-bulkhead", type = Bulkhead.Type.SEMAPHORE)
    public Mono<ProcessingContext> resolveContext(
            String principalId,
            String username,
            String[] roles,
            String channelId,
            String requestId) {

        log.debug("Resolving context for principal: {}, channel: {}, requestId: {}",
                principalId, channelId, requestId);

        ContextResolutionRequest request = ContextResolutionRequest.builder()
                .principalId(principalId)
                .username(username)
                .roles(roles)
                .channelId(channelId)
                .requestId(requestId)
                .build();

        return webClient.post()
                .uri("/api/v1/context/resolve")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(ContextResolutionResponse.class)
                .timeout(timeout)
                .map(ContextResolutionResponse::getContext)
                .doOnSuccess(context -> {
                    if (context != null) {
                        log.info("Context resolved successfully for principal: {}, tenant: {}, party: {}",
                                principalId, context.getTenantId(), context.getPartyId());
                    }
                })
                .doOnError(WebClientResponseException.class, error -> {
                    log.error("Party Service returned error {} for principal {}: {}",
                            error.getStatusCode(), principalId, error.getMessage());
                })
                .doOnError(Exception.class, error -> {
                    log.error("Failed to resolve context for principal {}: {}",
                            principalId, error.getMessage());
                });
    }

    /**
     * Fallback method for circuit breaker
     *
     * Called when:
     * - Circuit breaker is OPEN (too many failures)
     * - All retry attempts exhausted
     * - Bulkhead is full (too many concurrent calls)
     *
     * @param principalId Principal ID
     * @param username Username
     * @param roles User roles
     * @param channelId Channel ID
     * @param requestId Request ID
     * @param throwable Exception that triggered fallback
     * @return Empty Mono - request will continue without context
     */
    private Mono<ProcessingContext> fallbackResolveContext(
            String principalId,
            String username,
            String[] roles,
            String channelId,
            String requestId,
            Throwable throwable) {

        log.warn("Party Service unavailable - using fallback for principal: {}. Reason: {} - {}",
                principalId, throwable.getClass().getSimpleName(), throwable.getMessage());

        // Metrics: increment fallback counter
        log.info("METRIC: context_resolution_fallback_total principal={} channel={} reason={}",
                principalId, channelId, throwable.getClass().getSimpleName());

        // Return empty context - request continues without context
        // Downstream services should handle missing context gracefully
        return Mono.empty();
    }

    /**
     * Invalidate cached context for a party
     *
     * Called when party information changes
     *
     * @param partyId Party ID
     * @return Mono<Void>
     */
    public Mono<Void> invalidateCache(String partyId) {
        log.info("Invalidating context cache for party: {}", partyId);

        return webClient.delete()
                .uri("/api/v1/context/cache/{partyId}", partyId)
                .retrieve()
                .bodyToMono(Void.class)
                .timeout(Duration.ofSeconds(2))
                .doOnSuccess(v -> log.debug("Cache invalidated successfully for party: {}", partyId))
                .doOnError(error -> log.error("Failed to invalidate cache for party {}: {}",
                        partyId, error.getMessage()))
                .onErrorResume(error -> Mono.empty()); // Swallow errors
    }

    /**
     * Health check for Party Service
     *
     * @return Mono<Boolean> - true if healthy
     */
    public Mono<Boolean> isHealthy() {
        return webClient.get()
                .uri("/api/v1/context/health")
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(2))
                .map(response -> response.contains("healthy"))
                .doOnError(error -> log.warn("Party Service health check failed: {}", error.getMessage()))
                .onErrorReturn(false);
    }

    // ===== DTOs =====

    /**
     * Request DTO for context resolution
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ContextResolutionRequest {
        private String principalId;
        private String username;
        private String[] roles;
        private String channelId;
        private String partyId;
        private String requestId;
    }

    /**
     * Response DTO from context resolution endpoint
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ContextResolutionResponse {
        private ProcessingContext context;
        private String contextJson;
        private long resolutionTimeMs;
        private boolean cached;
        private String requestId;
    }
}
