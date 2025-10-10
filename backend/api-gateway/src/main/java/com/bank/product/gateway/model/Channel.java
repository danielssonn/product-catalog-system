package com.bank.product.gateway.model;

/**
 * Channel types supported by the API Gateway
 */
public enum Channel {
    /**
     * Public REST API - JSON/XML over HTTPS
     * Auth: OAuth 2.0 / JWT
     * Rate Limit: 1000 req/min per tenant
     */
    PUBLIC_API,
    
    /**
     * Host-to-Host file processing - SFTP/S3 upload
     * Auth: Mutual TLS + API Key
     * File Formats: CSV, Fixed-width, ISO20022 XML
     * Processing: Async with callback
     */
    HOST_TO_HOST,
    
    /**
     * ERP Integration (Kyriba, SAP Treasury, etc.)
     * Auth: OAuth 2.0 client credentials
     * Protocol: REST + SOAP fallback
     * Rate Limit: 5000 req/min (batch operations)
     */
    ERP_INTEGRATION,
    
    /**
     * Client Self-Service Portal - Web UI
     * Auth: OAuth 2.0 authorization code flow
     * Session: Redis-backed
     * Rate Limit: 500 req/min per user
     */
    CLIENT_PORTAL,
    
    /**
     * Salesforce Operations Workbench - CRM integration
     * Auth: Salesforce OAuth + Connected App
     * Sync: Real-time + batch
     * Rate Limit: 2000 req/min
     */
    SALESFORCE_OPS,
    
    /**
     * Internal Admin - Internal operations
     * Auth: Internal SSO (LDAP/AD)
     * Rate Limit: Unlimited
     */
    INTERNAL_ADMIN;
    
    /**
     * Get default rate limit (requests per minute)
     */
    public int getDefaultRateLimit() {
        return switch (this) {
            case PUBLIC_API -> 1000;
            case HOST_TO_HOST -> 100; // File uploads are slower
            case ERP_INTEGRATION -> 5000;
            case CLIENT_PORTAL -> 500;
            case SALESFORCE_OPS -> 2000;
            case INTERNAL_ADMIN -> Integer.MAX_VALUE;
        };
    }
    
    /**
     * Check if channel supports file processing
     */
    public boolean supportsFileProcessing() {
        return this == HOST_TO_HOST || this == ERP_INTEGRATION;
    }
    
    /**
     * Get authentication method for channel
     */
    public String getAuthMethod() {
        return switch (this) {
            case PUBLIC_API -> "JWT_BEARER";
            case HOST_TO_HOST -> "MUTUAL_TLS_API_KEY";
            case ERP_INTEGRATION -> "OAUTH2_CLIENT_CREDENTIALS";
            case CLIENT_PORTAL -> "OAUTH2_AUTHORIZATION_CODE";
            case SALESFORCE_OPS -> "SALESFORCE_OAUTH";
            case INTERNAL_ADMIN -> "INTERNAL_SSO";
        };
    }
}
