package com.bank.product.core.model;

import lombok.Data;
import lombok.Builder;

import java.util.Map;

/**
 * Configuration for connecting to a core banking system.
 * Contains credentials and connection details.
 */
@Data
@Builder
public class CoreSystemConfig {
    /**
     * Unique identifier for this core system instance
     */
    private String coreSystemId;

    /**
     * Type of core banking system
     */
    private CoreSystemType type;

    /**
     * API endpoint URL
     */
    private String apiEndpoint;

    /**
     * API key for authentication (encrypted)
     */
    private String apiKey;

    /**
     * Username for basic auth (if applicable)
     */
    private String username;

    /**
     * Password for basic auth (encrypted, if applicable)
     */
    private String password;

    /**
     * OAuth2 token endpoint (if applicable)
     */
    private String tokenEndpoint;

    /**
     * OAuth2 client ID (if applicable)
     */
    private String clientId;

    /**
     * OAuth2 client secret (encrypted, if applicable)
     */
    private String clientSecret;

    /**
     * Connection timeout in milliseconds
     */
    private Integer connectionTimeoutMs;

    /**
     * Read timeout in milliseconds
     */
    private Integer readTimeoutMs;

    /**
     * Additional vendor-specific configuration
     */
    private Map<String, String> additionalConfig;

    /**
     * Whether to use TLS/SSL
     */
    private boolean useSsl;

    /**
     * Environment (SANDBOX, UAT, PRODUCTION)
     */
    private String environment;
}
