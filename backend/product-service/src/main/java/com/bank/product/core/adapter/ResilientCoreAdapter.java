package com.bank.product.core.adapter;

import com.bank.product.core.config.CoreProvisioningConfig;
import com.bank.product.core.model.*;
import com.bank.product.domain.solution.model.Solution;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.Instant;
import java.util.function.Supplier;

/**
 * Resilient wrapper for core banking adapters.
 * Adds circuit breaker and retry logic.
 */
@Slf4j
public class ResilientCoreAdapter implements CoreBankingAdapter {

    private final CoreBankingAdapter delegate;
    private final CircuitBreaker circuitBreaker;
    private final Retry retry;

    public ResilientCoreAdapter(
            CoreBankingAdapter delegate,
            CircuitBreakerRegistry circuitBreakerRegistry,
            CoreProvisioningConfig config) {

        this.delegate = delegate;

        // Create circuit breaker for this adapter
        String circuitBreakerName = "core-adapter-" + delegate.getType().name().toLowerCase();
        this.circuitBreaker = circuitBreakerRegistry.circuitBreaker(circuitBreakerName);

        // Create retry with config
        RetryConfig retryConfig = RetryConfig.custom()
                .maxAttempts(config.getRetry().getMaxAttempts())
                .waitDuration(Duration.ofMillis(config.getRetry().getBackoffMs()))
                .retryExceptions(Exception.class)
                .retryOnResult(result -> {
                    // Retry if result is not successful and retryable
                    if (result instanceof CoreProvisioningResult) {
                        CoreProvisioningResult provResult = (CoreProvisioningResult) result;
                        return !provResult.isSuccess() && provResult.isRetryable();
                    }
                    return false;
                })
                .build();

        RetryRegistry retryRegistry = RetryRegistry.of(retryConfig);
        String retryName = "core-adapter-retry-" + delegate.getType().name().toLowerCase();
        this.retry = retryRegistry.retry(retryName);

        // Add event listeners
        circuitBreaker.getEventPublisher()
                .onStateTransition(event -> {
                    log.warn("Circuit breaker state transition for {}: {} -> {}",
                            circuitBreakerName,
                            event.getStateTransition().getFromState(),
                            event.getStateTransition().getToState());
                })
                .onError(event -> {
                    log.error("Circuit breaker error for {}: {}",
                            circuitBreakerName, event.getThrowable().getMessage());
                });

        retry.getEventPublisher()
                .onRetry(event -> {
                    log.warn("Retry attempt {} for {} due to: {}",
                            event.getNumberOfRetryAttempts(),
                            retryName,
                            event.getLastThrowable() != null
                                    ? event.getLastThrowable().getMessage()
                                    : "non-successful result");
                });
    }

    @Override
    public CoreSystemType getType() {
        return delegate.getType();
    }

    @Override
    public CoreProvisioningResult provisionProduct(Solution solution, CoreSystemConfig config) {
        return executeWithResilience(
                () -> delegate.provisionProduct(solution, config),
                "provisionProduct"
        );
    }

    @Override
    public CoreProvisioningResult updateProduct(Solution solution, String coreProductId, CoreSystemConfig config) {
        return executeWithResilience(
                () -> delegate.updateProduct(solution, coreProductId, config),
                "updateProduct"
        );
    }

    @Override
    public CoreProvisioningResult deactivateProduct(String coreProductId, CoreSystemConfig config) {
        return executeWithResilience(
                () -> delegate.deactivateProduct(coreProductId, config),
                "deactivateProduct"
        );
    }

    @Override
    public CoreProvisioningResult sunsetProduct(String coreProductId, CoreSystemConfig config) {
        return executeWithResilience(
                () -> delegate.sunsetProduct(coreProductId, config),
                "sunsetProduct"
        );
    }

    @Override
    public boolean verifyProductExists(String coreProductId, CoreSystemConfig config) {
        try {
            return CircuitBreaker.decorateSupplier(circuitBreaker,
                    () -> delegate.verifyProductExists(coreProductId, config))
                    .get();
        } catch (Exception e) {
            log.error("Failed to verify product exists: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public CoreProductDetails getProductDetails(String coreProductId, CoreSystemConfig config) {
        return Retry.decorateSupplier(retry,
                CircuitBreaker.decorateSupplier(circuitBreaker,
                        () -> delegate.getProductDetails(coreProductId, config)))
                .get();
    }

    @Override
    public boolean healthCheck(CoreSystemConfig config) {
        try {
            return delegate.healthCheck(config);
        } catch (Exception e) {
            log.error("Health check failed for {}: {}", getType(), e.getMessage());
            return false;
        }
    }

    @Override
    public String getAdapterVersion() {
        return delegate.getAdapterVersion() + "-resilient";
    }

    /**
     * Execute operation with circuit breaker and retry.
     */
    private CoreProvisioningResult executeWithResilience(
            Supplier<CoreProvisioningResult> operation,
            String operationName) {

        try {
            // Wrap with retry, then circuit breaker
            Supplier<CoreProvisioningResult> resilientOperation = Retry.decorateSupplier(
                    retry,
                    CircuitBreaker.decorateSupplier(circuitBreaker, operation)
            );

            return resilientOperation.get();

        } catch (Exception e) {
            log.error("Resilient operation {} failed after retries: {}",
                    operationName, e.getMessage());

            // Return failure result
            return CoreProvisioningResult.builder()
                    .success(false)
                    .errorMessage(e.getMessage())
                    .errorCode("RESILIENCE_FAILURE")
                    .retryable(false)
                    .timestamp(Instant.now())
                    .build();
        }
    }
}
