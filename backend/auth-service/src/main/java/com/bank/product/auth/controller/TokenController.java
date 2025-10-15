package com.bank.product.auth.controller;

import com.bank.product.model.User;
import com.bank.product.auth.repository.UserRepository;
import com.bank.product.security.jwt.JwtClaims;
import com.bank.product.security.jwt.JwtService;
import com.bank.product.auth.service.TokenBlacklistService;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * OAuth 2.0 Token Endpoint Controller
 *
 * Provides endpoints for:
 * - Token generation (login)
 * - Token refresh
 * - Token revocation (logout)
 *
 * Supports Resource Owner Password Credentials flow (for now).
 * TODO: Add Authorization Code flow for web clients
 * TODO: Add Client Credentials flow for service accounts
 */
@Slf4j
@RestController
@RequestMapping("/oauth")
@RequiredArgsConstructor
public class TokenController {

    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenBlacklistService blacklistService;

    /**
     * Token endpoint - Issue access and refresh tokens
     *
     * POST /oauth/token
     * Content-Type: application/json
     *
     * Request body:
     * {
     *   "grantType": "password",
     *   "username": "user@example.com",
     *   "password": "password123",
     *   "tenantId": "tenant-001",
     *   "channel": "PORTAL"
     * }
     */
    @PostMapping("/token")
    public Mono<ResponseEntity<TokenResponse>> issueToken(@RequestBody TokenRequest request) {
        log.info("Token request for user: {}, tenant: {}, grant type: {}",
                request.getUsername(), request.getTenantId(), request.getGrantType());

        if ("password".equals(request.getGrantType())) {
            return authenticateUser(request);
        } else if ("refresh_token".equals(request.getGrantType())) {
            return refreshToken(request.getRefreshToken());
        }

        return Mono.just(ResponseEntity
                .badRequest()
                .body(TokenResponse.error("unsupported_grant_type", "Grant type not supported")));
    }

    /**
     * Authenticate user with password and issue tokens
     */
    private Mono<ResponseEntity<TokenResponse>> authenticateUser(TokenRequest request) {
        return userRepository.findByUsername(request.getUsername())
                .switchIfEmpty(Mono.error(new AuthenticationException("Invalid credentials")))
                .flatMap(user -> {
                    // Verify password
                    if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
                        return Mono.error(new AuthenticationException("Invalid credentials"));
                    }

                    // Check if user is enabled
                    if (!user.isEnabled()) {
                        return Mono.error(new AuthenticationException("User account disabled"));
                    }

                    // Generate tokens
                    JwtClaims claims = buildClaims(user, request);
                    String accessToken = jwtService.generateAccessToken(claims);
                    String refreshToken = jwtService.generateRefreshToken(claims);

                    TokenResponse response = TokenResponse.builder()
                            .accessToken(accessToken)
                            .refreshToken(refreshToken)
                            .tokenType("Bearer")
                            .expiresIn(900) // 15 minutes
                            .build();

                    log.info("Tokens issued for user: {}, tenant: {}", user.getUsername(), request.getTenantId());

                    return Mono.just(ResponseEntity.ok(response));
                })
                .onErrorResume(AuthenticationException.class, ex -> {
                    log.warn("Authentication failed for user: {}", request.getUsername());
                    return Mono.just(ResponseEntity
                            .status(HttpStatus.UNAUTHORIZED)
                            .body(TokenResponse.error("invalid_grant", ex.getMessage())));
                })
                .onErrorResume(ex -> {
                    log.error("Error issuing token", ex);
                    return Mono.just(ResponseEntity
                            .status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(TokenResponse.error("server_error", "Internal server error")));
                });
    }

    /**
     * Refresh access token using refresh token
     */
    private Mono<ResponseEntity<TokenResponse>> refreshToken(String refreshToken) {
        try {
            // Validate refresh token
            JwtClaims claims = jwtService.validateAndParse(refreshToken);

            // Verify token type
            if (claims.getTokenType() != JwtClaims.TokenType.REFRESH) {
                return Mono.just(ResponseEntity
                        .status(HttpStatus.UNAUTHORIZED)
                        .body(TokenResponse.error("invalid_token", "Not a refresh token")));
            }

            // Check if token is blacklisted
            return blacklistService.isBlacklisted(refreshToken)
                    .flatMap(isBlacklisted -> {
                        if (isBlacklisted) {
                            return Mono.just(ResponseEntity
                                    .status(HttpStatus.UNAUTHORIZED)
                                    .body(TokenResponse.error("invalid_token", "Token has been revoked")));
                        }

                        // Generate new access token (preserve claims)
                        String newAccessToken = jwtService.generateAccessToken(claims);

                        TokenResponse response = TokenResponse.builder()
                                .accessToken(newAccessToken)
                                .refreshToken(refreshToken) // Keep same refresh token
                                .tokenType("Bearer")
                                .expiresIn(900) // 15 minutes
                                .build();

                        log.info("Access token refreshed for user: {}", claims.getUsername());

                        return Mono.just(ResponseEntity.ok(response));
                    });

        } catch (Exception e) {
            log.error("Error refreshing token", e);
            return Mono.just(ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(TokenResponse.error("invalid_token", "Invalid refresh token")));
        }
    }

    /**
     * Revoke token endpoint (logout)
     *
     * POST /oauth/revoke
     * Authorization: Bearer <token>
     */
    @PostMapping("/revoke")
    public Mono<ResponseEntity<Void>> revokeToken(@RequestHeader("Authorization") String authHeader) {
        String token = extractBearerToken(authHeader);

        if (token == null) {
            return Mono.just(ResponseEntity.badRequest().build());
        }

        return blacklistService.blacklistToken(token)
                .map(success -> {
                    if (success) {
                        log.info("Token revoked successfully");
                        return ResponseEntity.ok().<Void>build();
                    } else {
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).<Void>build();
                    }
                });
    }

    /**
     * Build JWT claims from user and request
     */
    private JwtClaims buildClaims(User user, TokenRequest request) {
        return JwtClaims.builder()
                .subject(user.getId())
                .username(user.getUsername())
                .email(user.getUsername())
                .roles(user.getRoles() != null ? List.copyOf(user.getRoles()) : List.of())
                .tenantId(request.getTenantId())
                .channel(request.getChannel())
                .build();
    }

    /**
     * Extract Bearer token from Authorization header
     */
    private String extractBearerToken(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }

    // DTOs

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TokenRequest {
        private String grantType;    // "password" or "refresh_token"
        private String username;
        private String password;
        private String tenantId;
        private String channel;
        private String refreshToken; // For refresh_token grant
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TokenResponse {
        private String accessToken;
        private String refreshToken;
        private String tokenType;
        private Integer expiresIn;
        private String error;
        private String errorDescription;

        public static TokenResponse error(String error, String description) {
            return TokenResponse.builder()
                    .error(error)
                    .errorDescription(description)
                    .build();
        }
    }

    public static class AuthenticationException extends RuntimeException {
        public AuthenticationException(String message) {
            super(message);
        }
    }
}
