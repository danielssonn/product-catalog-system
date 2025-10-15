package com.bank.product.security.jwt;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * JWT Configuration Properties
 *
 * Supports asymmetric key pairs (RS256) for production security.
 * Keys should be stored in Vault in production.
 */
@Data
@Component
@ConfigurationProperties(prefix = "security.jwt")
public class JwtProperties {

    /**
     * Issuer claim for JWT tokens
     */
    private String issuer = "product-catalog-system";

    /**
     * Access token expiration duration
     * Default: 15 minutes for security
     */
    private Duration accessTokenExpiration = Duration.ofMinutes(15);

    /**
     * Refresh token expiration duration
     * Default: 7 days
     */
    private Duration refreshTokenExpiration = Duration.ofDays(7);

    /**
     * Service token expiration duration (for service-to-service calls)
     * Default: 5 minutes
     */
    private Duration serviceTokenExpiration = Duration.ofMinutes(5);

    /**
     * RSA private key for signing tokens (Base64 encoded)
     * In production: Fetch from Vault
     */
    private String privateKey;

    /**
     * RSA public key for validating tokens (Base64 encoded)
     * In production: Fetch from Vault or distribute to services
     */
    private String publicKey;

    /**
     * Secret for HMAC signing (HS256) - fallback for development only
     * In production: Use RSA keys
     */
    private String secret;

    /**
     * Algorithm to use: RS256 (asymmetric) or HS256 (symmetric)
     */
    private String algorithm = "RS256";

    /**
     * Enable token blacklist for revocation
     */
    private boolean enableBlacklist = true;

    /**
     * Redis key prefix for token blacklist
     */
    private String blacklistPrefix = "jwt:blacklist:";
}
