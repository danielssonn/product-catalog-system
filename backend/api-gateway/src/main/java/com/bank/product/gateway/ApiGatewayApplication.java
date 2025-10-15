package com.bank.product.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

/**
 * Multi-Channel API Gateway for Product Catalog System
 *
 * Supports multiple channels:
 * - Public API (REST JSON)
 * - Host-to-Host File Processing (CSV/Fixed-width)
 * - ERP Integration (Kyriba, Treasury Workstations)
 * - Client Self-Service Portal (Web UI)
 * - Salesforce Operations Workbench (CRM Integration)
 *
 * Enforces:
 * - Multi-tenancy isolation
 * - Party-aware routing
 * - RBAC authorization
 * - API versioning
 * - Rate limiting per channel
 * - Circuit breakers for resiliency
 * - Request/response transformation
 * - Comprehensive audit logging
 */
@SpringBootApplication
@ComponentScan(basePackages = {
    "com.bank.product.gateway",
    "com.bank.product.security.jwt"  // Scan JWT components from common module
})
@ConfigurationPropertiesScan(basePackages = "com.bank.product.security.jwt")
public class ApiGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}
