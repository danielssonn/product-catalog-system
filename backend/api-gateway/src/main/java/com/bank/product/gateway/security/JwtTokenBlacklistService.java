package com.bank.product.gateway.security;

import com.bank.product.security.jwt.JwtClaims;
import com.bank.product.security.jwt.JwtProperties;
import com.bank.product.security.jwt.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;

/**
 * JWT Token Blacklist Service
 *
 * Manages token revocation using Redis for distributed blacklist.
 * Blacklisted tokens are stored with TTL matching token expiration.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JwtTokenBlacklistService {

    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private final JwtProperties jwtProperties;
    private final JwtService jwtService;

    /**
     * Add token to blacklist (revoke token)
     */
    public Mono<Boolean> blacklistToken(String token) {
        try {
            // Parse token to get JTI and expiration
            JwtClaims claims = jwtService.validateAndParse(token);
            String jti = claims.getJti();

            if (jti == null) {
                log.warn("Cannot blacklist token without JTI claim");
                return Mono.just(false);
            }

            // Calculate TTL based on token expiration
            long expiresAt = claims.getExpiresAt();
            long ttlSeconds = expiresAt - Instant.now().getEpochSecond();

            if (ttlSeconds <= 0) {
                log.info("Token already expired, no need to blacklist");
                return Mono.just(true);
            }

            String key = getBlacklistKey(jti);
            Duration ttl = Duration.ofSeconds(ttlSeconds);

            return redisTemplate.opsForValue()
                    .set(key, token, ttl)
                    .doOnSuccess(result -> log.info("Token blacklisted: {}, TTL: {} seconds", jti, ttlSeconds))
                    .doOnError(error -> log.error("Failed to blacklist token: {}", error.getMessage()));

        } catch (Exception e) {
            log.error("Error blacklisting token", e);
            return Mono.just(false);
        }
    }

    /**
     * Check if token is blacklisted
     */
    public boolean isBlacklisted(String token) {
        try {
            // Parse token to get JTI
            JwtClaims claims = jwtService.validateAndParse(token);
            String jti = claims.getJti();

            if (jti == null) {
                return false;
            }

            String key = getBlacklistKey(jti);
            Boolean exists = redisTemplate.hasKey(key).block();

            return Boolean.TRUE.equals(exists);

        } catch (Exception e) {
            log.error("Error checking token blacklist", e);
            // Fail closed - treat as blacklisted if we can't check
            return true;
        }
    }

    /**
     * Remove token from blacklist (for testing/admin purposes)
     */
    public Mono<Boolean> removeFromBlacklist(String jti) {
        String key = getBlacklistKey(jti);
        return redisTemplate.delete(key)
                .map(count -> count > 0)
                .doOnSuccess(result -> log.info("Token removed from blacklist: {}", jti));
    }

    /**
     * Get blacklist key for Redis
     */
    private String getBlacklistKey(String jti) {
        return jwtProperties.getBlacklistPrefix() + jti;
    }
}
