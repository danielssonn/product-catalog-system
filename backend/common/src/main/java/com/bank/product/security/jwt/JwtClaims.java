package com.bank.product.security.jwt;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Standard JWT Claims for Product Catalog System
 *
 * Includes custom claims for multi-tenancy, RBAC, and service identity.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JwtClaims {

    // Standard JWT claims
    private String subject;          // User ID or Service ID
    private String issuer;           // Token issuer
    private Long issuedAt;           // Issue timestamp (epoch seconds)
    private Long expiresAt;          // Expiration timestamp (epoch seconds)
    private String jti;              // JWT ID (for revocation)

    // Custom claims for authentication
    private String username;         // Username
    private String email;            // User email
    private List<String> roles;      // User roles (ROLE_ADMIN, ROLE_USER, etc.)
    private List<String> permissions; // Fine-grained permissions

    // Multi-tenancy claims
    private String tenantId;         // Tenant identifier
    private String tenantName;       // Tenant name

    // Channel & context claims
    private String channel;          // Channel (PORTAL, MOBILE, HOST_TO_HOST, etc.)
    private String clientId;         // OAuth client ID
    private String sessionId;        // Session identifier

    // Service-to-service claims
    private String serviceId;        // Service identifier (for service tokens)
    private List<String> scopes;     // OAuth scopes or service permissions

    // Token type
    private TokenType tokenType;     // ACCESS, REFRESH, SERVICE

    // Additional metadata
    private Map<String, Object> metadata; // Extensible metadata

    public enum TokenType {
        ACCESS,    // Short-lived access token
        REFRESH,   // Long-lived refresh token
        SERVICE    // Service-to-service token
    }

    /**
     * Convert to JWT claims map
     */
    public Map<String, Object> toClaimsMap() {
        Map<String, Object> claims = new java.util.HashMap<>();

        // Standard claims
        if (subject != null) claims.put("sub", subject);
        if (issuer != null) claims.put("iss", issuer);
        if (issuedAt != null) claims.put("iat", issuedAt);
        if (expiresAt != null) claims.put("exp", expiresAt);
        if (jti != null) claims.put("jti", jti);

        // Custom claims
        if (username != null) claims.put("username", username);
        if (email != null) claims.put("email", email);
        if (roles != null) claims.put("roles", roles);
        if (permissions != null) claims.put("permissions", permissions);
        if (tenantId != null) claims.put("tenantId", tenantId);
        if (tenantName != null) claims.put("tenantName", tenantName);
        if (channel != null) claims.put("channel", channel);
        if (clientId != null) claims.put("clientId", clientId);
        if (sessionId != null) claims.put("sessionId", sessionId);
        if (serviceId != null) claims.put("serviceId", serviceId);
        if (scopes != null) claims.put("scopes", scopes);
        if (tokenType != null) claims.put("tokenType", tokenType.name());
        if (metadata != null) claims.putAll(metadata);

        return claims;
    }

    /**
     * Build from JWT claims map
     */
    public static JwtClaims fromClaimsMap(Map<String, Object> claims) {
        return JwtClaims.builder()
                .subject((String) claims.get("sub"))
                .issuer((String) claims.get("iss"))
                .issuedAt(toLong(claims.get("iat")))
                .expiresAt(toLong(claims.get("exp")))
                .jti((String) claims.get("jti"))
                .username((String) claims.get("username"))
                .email((String) claims.get("email"))
                .roles((List<String>) claims.get("roles"))
                .permissions((List<String>) claims.get("permissions"))
                .tenantId((String) claims.get("tenantId"))
                .tenantName((String) claims.get("tenantName"))
                .channel((String) claims.get("channel"))
                .clientId((String) claims.get("clientId"))
                .sessionId((String) claims.get("sessionId"))
                .serviceId((String) claims.get("serviceId"))
                .scopes((List<String>) claims.get("scopes"))
                .tokenType(toTokenType((String) claims.get("tokenType")))
                .build();
    }

    private static Long toLong(Object value) {
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return null;
    }

    private static TokenType toTokenType(String value) {
        if (value == null) return null;
        try {
            return TokenType.valueOf(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
