package com.bank.product.context;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

/**
 * Processing Context - Complete context for transaction processing
 *
 * This object is resolved ONCE by the Party Service and propagated
 * to all downstream services. It contains ALL information needed
 * for proper multi-tenant, party-aware, jurisdiction-compliant processing.
 *
 * @immutable - Context should not be modified after creation
 * @serializable - Must be JSON serializable for HTTP header transmission
 *
 * @author System Architecture Team
 * @since 1.0
 * @see <a href="../../../../../../CONTEXT_RESOLUTION_ARCHITECTURE.md">Context Resolution Architecture</a>
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@Slf4j
public class ProcessingContext implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    // ═══════════════════════════════════════════════════════════════
    // PRINCIPAL INFORMATION (from Authentication)
    // ═══════════════════════════════════════════════════════════════

    /**
     * User ID of the authenticated principal
     * Source: Authentication Service
     */
    private String principalId;

    /**
     * Username/email of the principal
     */
    private String principalUsername;

    /**
     * Roles assigned to the principal
     */
    @Builder.Default
    private Set<String> principalRoles = new HashSet<>();

    /**
     * Channel through which request originated
     * Values: PORTAL, MOBILE, API, BRANCH, HOST_TO_HOST
     */
    private String channelId;

    // ═══════════════════════════════════════════════════════════════
    // PARTY CONTEXT (from Party Service)
    // ═══════════════════════════════════════════════════════════════

    /**
     * Primary party ID for this request
     * This is the party on whose behalf the transaction is executed
     */
    private String partyId;

    /**
     * Party name (for logging/audit)
     */
    private String partyName;

    /**
     * Party type
     * Values: INDIVIDUAL, CORPORATE, GOVERNMENT, FINANCIAL_INSTITUTION
     */
    private String partyType;

    /**
     * Legal Entity Identifier (LEI) if available
     * Used for entity resolution and regulatory reporting
     */
    private String legalEntityId;

    /**
     * Party status
     * Values: ACTIVE, INACTIVE, SUSPENDED, PENDING_APPROVAL
     */
    private String partyStatus;

    // ═══════════════════════════════════════════════════════════════
    // TENANT CONTEXT (from Party Service)
    // ═══════════════════════════════════════════════════════════════

    /**
     * Tenant ID - CRITICAL for multi-tenancy
     * All data filtering MUST use this field
     */
    private String tenantId;

    /**
     * Tenant name (for logging/audit)
     */
    private String tenantName;

    /**
     * Tenant type
     * Values: COMMERCIAL_BANK, INVESTMENT_BANK, CREDIT_UNION, FINTECH
     */
    private String tenantType;

    // ═══════════════════════════════════════════════════════════════
    // JURISDICTION & REGULATORY CONTEXT
    // ═══════════════════════════════════════════════════════════════

    /**
     * Primary jurisdiction code (ISO 3166-1 alpha-2)
     * Examples: US, GB, DE, JP
     */
    private String jurisdictionCountry;

    /**
     * Sub-jurisdiction (state/province)
     * Examples: US-NY, US-CA, CA-ON
     */
    private String jurisdictionRegion;

    /**
     * Processing region for infrastructure routing
     * Values: AMERICAS, EMEA, APAC
     */
    private String processingRegion;

    /**
     * Regulatory framework applicable
     * Examples: BASEL_III, DODD_FRANK, MiFID_II, SOX
     */
    @Builder.Default
    private Set<String> regulatoryFrameworks = new HashSet<>();

    /**
     * Compliance tags for this context
     * Examples: KYC_VERIFIED, AML_CHECKED, SANCTIONS_CLEARED
     */
    @Builder.Default
    private Set<String> complianceTags = new HashSet<>();

    // ═══════════════════════════════════════════════════════════════
    // RELATIONSHIP CONTEXT (from Neo4j Party Graph)
    // ═══════════════════════════════════════════════════════════════

    /**
     * Relationship context for "Manages On Behalf Of" scenarios
     */
    private RelationshipContext relationshipContext;

    // ═══════════════════════════════════════════════════════════════
    // PERMISSIONS & LIMITS (resolved from Party + Roles)
    // ═══════════════════════════════════════════════════════════════

    /**
     * Permission context for authorization decisions
     */
    private PermissionContext permissions;

    // ═══════════════════════════════════════════════════════════════
    // CORE BANKING CONTEXT (for routing)
    // ═══════════════════════════════════════════════════════════════

    /**
     * Core banking system type for this tenant
     * Values: TEMENOS_T24, FINACLE, FIS_PROFILE, CUSTOM
     */
    private String coreSystemType;

    /**
     * Core banking system URL/endpoint
     */
    private String coreSystemEndpoint;

    /**
     * Core system customer ID (if different from partyId)
     */
    private String coreSystemCustomerId;

    // ═══════════════════════════════════════════════════════════════
    // AUDIT & METADATA
    // ═══════════════════════════════════════════════════════════════

    /**
     * Unique request ID for tracing
     */
    private String requestId;

    /**
     * Timestamp when context was resolved
     */
    private Instant contextResolvedAt;

    /**
     * Context resolution source
     * Values: PARTY_SERVICE, CACHE, FALLBACK
     */
    private String resolutionSource;

    /**
     * Context version (for compatibility)
     */
    @Builder.Default
    private String contextVersion = "1.0";

    /**
     * Additional metadata as key-value pairs
     * For extensibility without schema changes
     */
    @Builder.Default
    private Map<String, String> metadata = new HashMap<>();

    // ═══════════════════════════════════════════════════════════════
    // HELPER METHODS
    // ═══════════════════════════════════════════════════════════════

    /**
     * Serialize context to JSON for HTTP header transmission
     */
    public String toJson() {
        try {
            return OBJECT_MAPPER.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize ProcessingContext to JSON", e);
            throw new ContextSerializationException("Failed to serialize context", e);
        }
    }

    /**
     * Serialize context to JSON with Base64 encoding for header safety
     */
    public String toBase64Json() {
        String json = toJson();
        return Base64.getEncoder().encodeToString(json.getBytes());
    }

    /**
     * Deserialize context from JSON
     */
    public static ProcessingContext fromJson(String json) {
        try {
            return OBJECT_MAPPER.readValue(json, ProcessingContext.class);
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize ProcessingContext from JSON: {}", json, e);
            throw new ContextSerializationException("Failed to deserialize context", e);
        }
    }

    /**
     * Deserialize context from Base64 JSON
     */
    public static ProcessingContext fromBase64Json(String base64Json) {
        byte[] decodedBytes = Base64.getDecoder().decode(base64Json);
        String json = new String(decodedBytes);
        return fromJson(json);
    }

    /**
     * Check if context is valid
     */
    public boolean isValid() {
        // Check required fields
        if (principalId == null || partyId == null || tenantId == null) {
            log.warn("Context missing required fields: principalId={}, partyId={}, tenantId={}",
                    principalId, partyId, tenantId);
            return false;
        }

        // Check timestamp (5 minute expiry)
        if (contextResolvedAt == null) {
            log.warn("Context missing resolution timestamp");
            return false;
        }

        Instant expiryTime = Instant.now().minusSeconds(300); // 5 minutes ago
        if (contextResolvedAt.isBefore(expiryTime)) {
            log.warn("Context expired: resolved at {}, expiry time {}", contextResolvedAt, expiryTime);
            return false;
        }

        // Check party status
        if (!"ACTIVE".equals(partyStatus)) {
            log.warn("Party {} is not active: status={}", partyId, partyStatus);
            return false;
        }

        return true;
    }

    /**
     * Check if party has specific permission
     */
    public boolean hasPermission(String operation) {
        if (permissions == null) {
            log.warn("Permissions not set in context for party {}", partyId);
            return false;
        }
        return permissions.hasPermission(operation);
    }

    /**
     * Get full jurisdiction code
     */
    public String getFullJurisdiction() {
        if (jurisdictionRegion != null && !jurisdictionRegion.isEmpty()) {
            return jurisdictionCountry + "-" + jurisdictionRegion;
        }
        return jurisdictionCountry;
    }

    /**
     * Check if managing on behalf of another party
     */
    public boolean isManagingOnBehalfOf() {
        return relationshipContext != null && relationshipContext.isManagingOnBehalfOf();
    }

    /**
     * Get managed party IDs
     */
    public Set<String> getManagedPartyIds() {
        if (relationshipContext == null) {
            return Collections.emptySet();
        }
        return relationshipContext.getManagedPartyIds();
    }

    /**
     * Check if principal can manage specific party
     */
    public boolean canManageParty(String targetPartyId) {
        if (partyId.equals(targetPartyId)) {
            return true; // Can always manage own party
        }
        return getManagedPartyIds().contains(targetPartyId);
    }

    /**
     * Create a minimal context for testing
     */
    public static ProcessingContext createTestContext(String tenantId, String partyId, String principalId) {
        return ProcessingContext.builder()
                .tenantId(tenantId)
                .partyId(partyId)
                .principalId(principalId)
                .principalUsername("test-user")
                .principalRoles(Set.of("ROLE_USER"))
                .partyStatus("ACTIVE")
                .channelId("API")
                .requestId(UUID.randomUUID().toString())
                .contextResolvedAt(Instant.now())
                .resolutionSource("TEST")
                .contextVersion("1.0")
                .permissions(PermissionContext.createDefault())
                .build();
    }

    @Override
    public String toString() {
        // Safe toString that doesn't expose sensitive data
        return "ProcessingContext{" +
                "principalId='" + principalId + '\'' +
                ", partyId='" + partyId + '\'' +
                ", tenantId='" + tenantId + '\'' +
                ", channelId='" + channelId + '\'' +
                ", jurisdiction='" + getFullJurisdiction() + '\'' +
                ", requestId='" + requestId + '\'' +
                ", contextResolvedAt=" + contextResolvedAt +
                '}';
    }
}
