package com.bank.product.core.config;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Configuration for Resilience4j circuit breakers and retry logic.
 */
@Configuration
public class ResilienceConfig {

    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .failureRateThreshold(50)                    // 50% failure rate opens circuit
                .slowCallRateThreshold(50)                   // 50% slow calls opens circuit
                .slowCallDurationThreshold(Duration.ofSeconds(30))  // Call > 30s is slow
                .waitDurationInOpenState(Duration.ofSeconds(60))    // Wait 60s before half-open
                .permittedNumberOfCallsInHalfOpenState(3)    // 3 calls in half-open
                .minimumNumberOfCalls(5)                     // Need 5 calls to calculate rate
                .slidingWindowSize(10)                       // Track last 10 calls
                .build();

        return CircuitBreakerRegistry.of(config);
    }
}
