package com.bank.product.party.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Cache configuration for context resolution
 *
 * Uses Caffeine for high-performance in-memory caching.
 * Context is cached for 5 minutes to reduce Neo4j load.
 *
 * @author System Architecture Team
 * @since 1.0
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * Configure cache manager with Caffeine
     */
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager("context");
        cacheManager.setCaffeine(caffeineCacheBuilder());
        return cacheManager;
    }

    /**
     * Caffeine cache builder with 5-minute TTL
     */
    private Caffeine<Object, Object> caffeineCacheBuilder() {
        return Caffeine.newBuilder()
                .maximumSize(10000) // Max 10K cached contexts
                .expireAfterWrite(5, TimeUnit.MINUTES) // 5-minute TTL
                .recordStats(); // Enable cache statistics
    }
}
