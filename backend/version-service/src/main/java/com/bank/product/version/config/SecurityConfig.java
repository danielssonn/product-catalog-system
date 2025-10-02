package com.bank.product.version.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security configuration for version service
 * Implements role-based access control (RBAC)
 * - ROLE_ADMIN: Can manage API versions and transformations
 * - ROLE_USER: Can query versions and perform transformations
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                // Public endpoints
                .requestMatchers("/actuator/health").permitAll()

                // Version management - ADMIN only
                .requestMatchers(HttpMethod.POST, "/api/v1/versions").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/v1/versions/*/deprecate").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PATCH, "/api/v1/versions/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/v1/versions/**").hasRole("ADMIN")

                // Transformation validation - ADMIN only
                .requestMatchers(HttpMethod.POST, "/api/v1/transformations/validate").hasRole("ADMIN")

                // Query endpoints - USER or ADMIN
                .requestMatchers(HttpMethod.GET, "/api/v1/versions/**").hasAnyRole("USER", "ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/v1/transformations/**").hasAnyRole("USER", "ADMIN")

                // Transformation endpoints - USER or ADMIN
                .requestMatchers(HttpMethod.POST, "/api/v1/transformations/**").hasAnyRole("USER", "ADMIN")

                // Actuator endpoints - ADMIN only (except health)
                .requestMatchers("/actuator/**").hasRole("ADMIN")

                // All other API endpoints require authentication
                .requestMatchers("/api/**").authenticated()
                .anyRequest().authenticated()
            )
            .httpBasic(basic -> {})
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
