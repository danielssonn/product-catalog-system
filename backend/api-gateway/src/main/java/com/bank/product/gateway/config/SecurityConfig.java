package com.bank.product.gateway.config;

import com.bank.product.gateway.security.JwtAuthenticationFilter;
import com.bank.product.gateway.service.MongoReactiveUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UserDetailsRepositoryReactiveAuthenticationManager;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;

/**
 * Security configuration for API Gateway
 *
 * Multi-layered authentication strategy:
 * 1. JWT Bearer token authentication (preferred for external clients)
 * 2. HTTP Basic authentication (fallback for service-to-service and legacy clients)
 *
 * Security Features:
 * - JWT-based authentication with RSA signature verification
 * - Token revocation support via Redis blacklist
 * - Per-channel authorization rules
 * - MongoDB-backed user management
 * - BCrypt password hashing
 */
@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final MongoReactiveUserDetailsService userDetailsService;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            // Add JWT authentication filter BEFORE the authentication filter
            // This allows JWT tokens to be validated and set in SecurityContext
            .addFilterAt(jwtAuthenticationFilter, SecurityWebFiltersOrder.AUTHENTICATION)
            // Enable HTTP Basic Auth as fallback
            // This provides a second authentication mechanism if JWT is not present
            .httpBasic(httpBasic -> httpBasic.authenticationManager(reactiveAuthenticationManager()))
            .authorizeExchange(exchanges -> exchanges
                // OAuth endpoints - completely public (no authentication required)
                .pathMatchers("/oauth/**").permitAll()

                // Public endpoints
                .pathMatchers("/actuator/health", "/test/**").permitAll()

                // Admin endpoints
                .pathMatchers("/actuator/**").hasRole("ADMIN")
                .pathMatchers("/admin/**").hasRole("ADMIN")

                // Public API
                .pathMatchers("/api/v*/public/**").permitAll()

                // Channel-based authorization
                .pathMatchers("/channel/host-to-host/**").hasRole("SYSTEM")
                .pathMatchers("/channel/erp/**").hasAnyRole("SYSTEM", "ERP_USER", "ADMIN")
                .pathMatchers("/channel/portal/**").hasAnyRole("USER", "CUSTOMER", "ADMIN")
                .pathMatchers("/channel/salesforce/**").hasAnyRole("SALESFORCE", "ADMIN")

                // All other requests require authentication
                .anyExchange().authenticated()
            )
            .build();
    }

    @Bean
    public ReactiveAuthenticationManager reactiveAuthenticationManager() {
        UserDetailsRepositoryReactiveAuthenticationManager authenticationManager =
                new UserDetailsRepositoryReactiveAuthenticationManager(userDetailsService);
        authenticationManager.setPasswordEncoder(passwordEncoder());
        return authenticationManager;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
