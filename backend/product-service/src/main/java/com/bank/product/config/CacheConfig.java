package com.bank.product.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Cache configuration for idempotency protection
 */
@Configuration
public class CacheConfig {

    @Bean
    public Cache<String, Boolean> idempotencyCache() {
        return Caffeine.newBuilder()
                .maximumSize(10_000)
                .expireAfterWrite(24, TimeUnit.HOURS)
                .recordStats()
                .build();
    }
}
