package com.bank.product.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories;

/**
 * Authentication Service
 *
 * Provides OAuth 2.0 token management:
 * - Token generation (login)
 * - Token refresh
 * - Token revocation (logout)
 * - Token introspection
 *
 * This service is accessed through the API Gateway at /oauth/**
 */
@SpringBootApplication(
    scanBasePackages = {
        "com.bank.product.auth",
        "com.bank.product.security.jwt"  // Scan JWT components from common module
    }
)
@EnableReactiveMongoRepositories(basePackages = "com.bank.product.auth.repository")
@ConfigurationPropertiesScan(basePackages = "com.bank.product.security.jwt")
public class AuthServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthServiceApplication.class, args);
    }
}
