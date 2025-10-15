package com.bank.product.gateway.security;

import com.bank.product.security.jwt.JwtClaims;
import com.bank.product.security.jwt.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;

/**
 * JWT Authentication Filter for API Gateway
 *
 * Intercepts requests with Authorization: Bearer <token> header
 * and validates JWT tokens. Populates SecurityContext with user details.
 *
 * Filter Chain Order:
 * 1. JWT Filter (this) - attempts JWT authentication
 * 2. Basic Auth Filter - fallback to Basic authentication if JWT fails
 * 3. SecurityWebFilterChain - enforces authorization rules
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter implements WebFilter {

    private final JwtService jwtService;
    private final JwtTokenBlacklistService blacklistService;

    private static final String BEARER_PREFIX = "Bearer ";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        // Extract JWT token from Authorization header
        String token = extractToken(request);

        // If no JWT token, continue to next filter (Basic Auth)
        if (token == null) {
            return chain.filter(exchange);
        }

        // Validate and authenticate
        return authenticateWithJwt(token)
                .flatMap(authentication -> {
                    // Set authentication in SecurityContext and continue
                    return chain.filter(exchange)
                            .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication));
                })
                .onErrorResume(ex -> {
                    // JWT validation failed - log and continue to next filter
                    log.warn("JWT authentication failed: {}", ex.getMessage());
                    return chain.filter(exchange);
                });
    }

    /**
     * Extract JWT token from Authorization header
     */
    private String extractToken(ServerHttpRequest request) {
        List<String> authHeaders = request.getHeaders().get(HttpHeaders.AUTHORIZATION);

        if (authHeaders == null || authHeaders.isEmpty()) {
            return null;
        }

        String authHeader = authHeaders.get(0);
        if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
            return authHeader.substring(BEARER_PREFIX.length());
        }

        return null;
    }

    /**
     * Authenticate request using JWT token
     */
    private Mono<Authentication> authenticateWithJwt(String token) {
        return Mono.fromCallable(() -> {
                    // Check if token is blacklisted
                    if (blacklistService.isBlacklisted(token)) {
                        throw new JwtAuthenticationException("Token has been revoked");
                    }

                    // Validate and parse JWT
                    JwtClaims claims = jwtService.validateAndParse(token);

                    // Extract authorities from roles
                    List<SimpleGrantedAuthority> authorities = claims.getRoles() != null
                            ? claims.getRoles().stream()
                            .map(SimpleGrantedAuthority::new)
                            .collect(Collectors.toList())
                            : List.of();

                    // Create Authentication object
                    JwtAuthenticationToken authentication = new JwtAuthenticationToken(
                            claims.getSubject(),
                            token,
                            authorities,
                            claims
                    );

                    log.debug("JWT authentication successful for user: {}, tenant: {}, roles: {}",
                            claims.getUsername(), claims.getTenantId(), claims.getRoles());

                    return (Authentication) authentication;
                })
                .onErrorMap(ex -> {
                    log.error("JWT authentication error: {}", ex.getMessage());
                    return new JwtAuthenticationException("Invalid JWT token", ex);
                });
    }

    /**
     * Custom Authentication token that carries JWT claims
     */
    public static class JwtAuthenticationToken extends UsernamePasswordAuthenticationToken {
        private final JwtClaims jwtClaims;

        public JwtAuthenticationToken(String principal, String credentials,
                                       List<SimpleGrantedAuthority> authorities,
                                       JwtClaims jwtClaims) {
            super(principal, credentials, authorities);
            this.jwtClaims = jwtClaims;
            setAuthenticated(true);
        }

        public JwtClaims getJwtClaims() {
            return jwtClaims;
        }

        public String getTenantId() {
            return jwtClaims != null ? jwtClaims.getTenantId() : null;
        }

        public String getChannel() {
            return jwtClaims != null ? jwtClaims.getChannel() : null;
        }
    }

    /**
     * Custom exception for JWT authentication failures
     */
    public static class JwtAuthenticationException extends RuntimeException {
        public JwtAuthenticationException(String message) {
            super(message);
        }

        public JwtAuthenticationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
