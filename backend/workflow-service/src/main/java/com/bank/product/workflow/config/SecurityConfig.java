package com.bank.product.workflow.config;

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
 * Security configuration for workflow service
 * Implements role-based access control (RBAC)
 * - ROLE_ADMIN: Can manage workflow templates, approve/reject workflows, view all workflows
 * - ROLE_USER: Can submit workflows, approve assigned tasks, view own workflows
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

                // Workflow template management - ADMIN only
                .requestMatchers(HttpMethod.POST, "/api/v1/workflow-templates").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/v1/workflow-templates/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PATCH, "/api/v1/workflow-templates/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/v1/workflow-templates/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/v1/workflow-templates/*/publish").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/v1/workflow-templates/*/test").hasRole("ADMIN")

                // Workflow template queries - USER or ADMIN
                .requestMatchers(HttpMethod.GET, "/api/v1/workflow-templates/**").hasAnyRole("USER", "ADMIN")

                // Workflow submission - USER or ADMIN
                .requestMatchers(HttpMethod.POST, "/api/v1/workflows/submit").hasAnyRole("USER", "ADMIN")

                // Workflow approval/rejection - USER or ADMIN (controlled by business logic)
                .requestMatchers(HttpMethod.POST, "/api/v1/workflows/*/approve").hasAnyRole("USER", "ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/v1/workflows/*/reject").hasAnyRole("USER", "ADMIN")

                // Workflow queries - USER or ADMIN (filtered by role in business logic)
                .requestMatchers(HttpMethod.GET, "/api/v1/workflows/**").hasAnyRole("USER", "ADMIN")

                // Task queries - USER or ADMIN
                .requestMatchers(HttpMethod.GET, "/api/v1/workflows/my-tasks").hasAnyRole("USER", "ADMIN")

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
