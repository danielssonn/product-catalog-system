package com.bank.product.security.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

/**
 * JWT Token Service
 *
 * Handles JWT token generation, validation, and parsing.
 * Supports both symmetric (HS256) and asymmetric (RS256) algorithms.
 *
 * Security Features:
 * - Token expiration validation
 * - Signature validation
 * - Issuer validation
 * - JTI (JWT ID) for token revocation support
 * - RSA key pair support for distributed systems
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JwtService {

    private final JwtProperties properties;

    /**
     * Generate access token for user authentication
     */
    public String generateAccessToken(JwtClaims claims) {
        claims.setTokenType(JwtClaims.TokenType.ACCESS);
        claims.setJti(UUID.randomUUID().toString());

        Instant now = Instant.now();
        claims.setIssuedAt(now.getEpochSecond());
        claims.setExpiresAt(now.plus(properties.getAccessTokenExpiration()).getEpochSecond());
        claims.setIssuer(properties.getIssuer());

        return generateToken(claims);
    }

    /**
     * Generate refresh token for token renewal
     */
    public String generateRefreshToken(JwtClaims claims) {
        claims.setTokenType(JwtClaims.TokenType.REFRESH);
        claims.setJti(UUID.randomUUID().toString());

        Instant now = Instant.now();
        claims.setIssuedAt(now.getEpochSecond());
        claims.setExpiresAt(now.plus(properties.getRefreshTokenExpiration()).getEpochSecond());
        claims.setIssuer(properties.getIssuer());

        return generateToken(claims);
    }

    /**
     * Generate service token for service-to-service authentication
     */
    public String generateServiceToken(String serviceId, String tenantId) {
        JwtClaims claims = JwtClaims.builder()
                .subject(serviceId)
                .serviceId(serviceId)
                .tenantId(tenantId)
                .tokenType(JwtClaims.TokenType.SERVICE)
                .jti(UUID.randomUUID().toString())
                .build();

        Instant now = Instant.now();
        claims.setIssuedAt(now.getEpochSecond());
        claims.setExpiresAt(now.plus(properties.getServiceTokenExpiration()).getEpochSecond());
        claims.setIssuer(properties.getIssuer());

        return generateToken(claims);
    }

    /**
     * Generate JWT token with claims
     */
    private String generateToken(JwtClaims claims) {
        try {
            JwtBuilder builder = Jwts.builder()
                    .claims(claims.toClaimsMap())
                    .subject(claims.getSubject())
                    .issuer(claims.getIssuer())
                    .issuedAt(Date.from(Instant.ofEpochSecond(claims.getIssuedAt())))
                    .expiration(Date.from(Instant.ofEpochSecond(claims.getExpiresAt())))
                    .id(claims.getJti());

            // Sign with appropriate algorithm
            if ("RS256".equals(properties.getAlgorithm())) {
                PrivateKey privateKey = loadPrivateKey();
                builder.signWith(privateKey, Jwts.SIG.RS256);
            } else {
                SecretKey secretKey = loadSecretKey();
                builder.signWith(secretKey, Jwts.SIG.HS256);
            }

            return builder.compact();

        } catch (Exception e) {
            log.error("Failed to generate JWT token", e);
            throw new JwtGenerationException("Failed to generate JWT token", e);
        }
    }

    /**
     * Validate and parse JWT token
     */
    public JwtClaims validateAndParse(String token) {
        try {
            JwtParser parser = createParser();
            Claims claims = parser.parseSignedClaims(token).getPayload();

            // Convert to JwtClaims
            return JwtClaims.fromClaimsMap(claims);

        } catch (ExpiredJwtException e) {
            log.warn("JWT token expired: {}", e.getMessage());
            throw new JwtValidationException("Token expired", e);
        } catch (SignatureException e) {
            log.error("JWT signature validation failed: {}", e.getMessage());
            throw new JwtValidationException("Invalid token signature", e);
        } catch (MalformedJwtException e) {
            log.error("Malformed JWT token: {}", e.getMessage());
            throw new JwtValidationException("Malformed token", e);
        } catch (UnsupportedJwtException e) {
            log.error("Unsupported JWT token: {}", e.getMessage());
            throw new JwtValidationException("Unsupported token", e);
        } catch (IllegalArgumentException e) {
            log.error("JWT claims string is empty: {}", e.getMessage());
            throw new JwtValidationException("Invalid token", e);
        }
    }

    /**
     * Extract claims without validation (for debugging/logging)
     */
    public JwtClaims parseWithoutValidation(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                throw new IllegalArgumentException("Invalid JWT token format");
            }

            String payload = new String(Base64.getUrlDecoder().decode(parts[1]));
            // Parse JSON payload to claims
            // This is unsafe for production - use for debugging only
            log.debug("Token payload (unsafe parse): {}", payload);

            return null; // Would need JSON parser here
        } catch (Exception e) {
            log.error("Failed to parse JWT without validation", e);
            return null;
        }
    }

    /**
     * Extract subject from token without full validation
     */
    public String extractSubject(String token) {
        try {
            return validateAndParse(token).getSubject();
        } catch (Exception e) {
            log.error("Failed to extract subject from token", e);
            return null;
        }
    }

    /**
     * Check if token is expired
     */
    public boolean isTokenExpired(String token) {
        try {
            JwtClaims claims = validateAndParse(token);
            return Instant.now().getEpochSecond() > claims.getExpiresAt();
        } catch (JwtValidationException e) {
            return true;
        }
    }

    /**
     * Create JWT parser with appropriate verification key
     */
    private JwtParser createParser() {
        JwtParserBuilder builder = Jwts.parser()
                .requireIssuer(properties.getIssuer());

        if ("RS256".equals(properties.getAlgorithm())) {
            PublicKey publicKey = loadPublicKey();
            builder.verifyWith(publicKey);
        } else {
            SecretKey secretKey = loadSecretKey();
            builder.verifyWith(secretKey);
        }

        return builder.build();
    }

    /**
     * Load RSA private key from configuration
     */
    private PrivateKey loadPrivateKey() {
        try {
            String privateKeyPEM = properties.getPrivateKey()
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s", "");

            byte[] keyBytes = Base64.getDecoder().decode(privateKeyPEM);
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            return kf.generatePrivate(spec);
        } catch (Exception e) {
            throw new JwtConfigurationException("Failed to load private key", e);
        }
    }

    /**
     * Load RSA public key from configuration
     */
    private PublicKey loadPublicKey() {
        try {
            String publicKeyPEM = properties.getPublicKey()
                    .replace("-----BEGIN PUBLIC KEY-----", "")
                    .replace("-----END PUBLIC KEY-----", "")
                    .replaceAll("\\s", "");

            byte[] keyBytes = Base64.getDecoder().decode(publicKeyPEM);
            X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            return kf.generatePublic(spec);
        } catch (Exception e) {
            throw new JwtConfigurationException("Failed to load public key", e);
        }
    }

    /**
     * Load HMAC secret key from configuration
     */
    private SecretKey loadSecretKey() {
        String secret = properties.getSecret();
        if (secret == null || secret.isEmpty()) {
            throw new JwtConfigurationException("JWT secret not configured");
        }
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    // Custom exceptions
    public static class JwtGenerationException extends RuntimeException {
        public JwtGenerationException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class JwtValidationException extends RuntimeException {
        public JwtValidationException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class JwtConfigurationException extends RuntimeException {
        public JwtConfigurationException(String message) {
            super(message);
        }
        public JwtConfigurationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
