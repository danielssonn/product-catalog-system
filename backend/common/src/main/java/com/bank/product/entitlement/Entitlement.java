package com.bank.product.entitlement;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

/**
 * Fine-grained entitlement model
 *
 * Represents permission for a specific party to perform specific operations
 * on a specific resource, subject to constraints.
 *
 * This enables fine-grained RBAC:
 * - "Alice can VIEW and TRANSACT on account A123, up to $50K, weekdays only"
 * - "Bob can CONFIGURE CHECKING solutions but not LOAN solutions"
 * - "Carol can APPROVE workflows on behalf of Dave, up to $100K"
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "entitlements")
@CompoundIndexes({
    @CompoundIndex(name = "tenant_party_resource_idx",
                   def = "{'tenantId': 1, 'partyId': 1, 'resourceType': 1}"),
    @CompoundIndex(name = "tenant_resource_idx",
                   def = "{'tenantId': 1, 'resourceType': 1, 'resourceId': 1}"),
    @CompoundIndex(name = "tenant_source_idx",
                   def = "{'tenantId': 1, 'source': 1}"),
    @CompoundIndex(name = "expiry_idx",
                   def = "{'expiresAt': 1}")
})
public class Entitlement {

    @Id
    private String id;

    /**
     * Tenant ID - critical for multi-tenancy isolation
     */
    @Indexed
    private String tenantId;

    /**
     * Party ID who has this entitlement
     * References Party.federatedId in Neo4j
     */
    @Indexed
    private String partyId;

    /**
     * Type of resource this entitlement applies to
     */
    @Indexed
    private ResourceType resourceType;

    /**
     * Specific resource ID (optional - if null, applies to all resources of this type)
     * Examples:
     * - For SOLUTION: solutionId
     * - For ACCOUNT: accountId
     * - For CATALOG_PRODUCT: catalogProductId
     * - null = applies to all resources of resourceType
     */
    @Indexed
    private String resourceId;

    /**
     * Operations allowed on this resource
     */
    @Builder.Default
    private Set<ResourceOperation> operations = new HashSet<>();

    /**
     * Constraints applied to this entitlement
     */
    private EntitlementConstraints constraints;

    /**
     * Source of this entitlement (how it was granted)
     */
    @Indexed
    private EntitlementSource source;

    /**
     * Reference to the source entity
     * - For RELATIONSHIP_BASED: Neo4j relationship ID
     * - For ROLE_BASED: role name
     * - For EXPLICIT_GRANT: granting party ID
     * - For DELEGATED: delegating party ID
     */
    private String sourceReference;

    /**
     * Party who granted this entitlement
     */
    private String grantedBy;

    /**
     * When this entitlement was granted
     */
    private Instant grantedAt;

    /**
     * When this entitlement expires (null = never expires)
     */
    private Instant expiresAt;

    /**
     * Is this entitlement currently active?
     */
    @Builder.Default
    private boolean active = true;

    /**
     * Reason for granting (audit trail)
     */
    private String grantReason;

    /**
     * Reason for revocation (if revoked)
     */
    private String revokeReason;

    /**
     * When this entitlement was revoked
     */
    private Instant revokedAt;

    /**
     * Who revoked this entitlement
     */
    private String revokedBy;

    /**
     * Priority (higher priority entitlements override lower)
     * Used for conflict resolution
     */
    @Builder.Default
    private int priority = 0;

    /**
     * Metadata for extensibility
     */
    @Builder.Default
    private java.util.Map<String, String> metadata = new java.util.HashMap<>();

    /**
     * Audit trail: created timestamp
     */
    private Instant createdAt;

    /**
     * Audit trail: updated timestamp
     */
    private Instant updatedAt;

    /**
     * Check if this entitlement is currently valid
     */
    public boolean isValid() {
        if (!active) {
            return false;
        }

        Instant now = Instant.now();

        // Check expiration
        if (expiresAt != null && now.isAfter(expiresAt)) {
            return false;
        }

        // Check constraints (time-based)
        if (constraints != null && !constraints.isCurrentlyValid()) {
            return false;
        }

        return true;
    }

    /**
     * Check if this entitlement grants a specific operation
     */
    public boolean hasOperation(ResourceOperation operation) {
        return operations.contains(operation);
    }

    /**
     * Check if this entitlement matches a resource
     */
    public boolean matchesResource(ResourceType type, String resourceId) {
        if (!this.resourceType.equals(type)) {
            return false;
        }

        // Null resourceId means applies to all resources of this type
        if (this.resourceId == null) {
            return true;
        }

        return this.resourceId.equals(resourceId);
    }

    /**
     * Add operation to this entitlement
     */
    public void addOperation(ResourceOperation operation) {
        if (this.operations == null) {
            this.operations = new HashSet<>();
        }
        this.operations.add(operation);
        this.updatedAt = Instant.now();
    }

    /**
     * Remove operation from this entitlement
     */
    public void removeOperation(ResourceOperation operation) {
        if (this.operations != null) {
            this.operations.remove(operation);
            this.updatedAt = Instant.now();
        }
    }

    /**
     * Revoke this entitlement
     */
    public void revoke(String revokedBy, String reason) {
        this.active = false;
        this.revokedBy = revokedBy;
        this.revokedAt = Instant.now();
        this.revokeReason = reason;
        this.updatedAt = Instant.now();
    }

    /**
     * Reactivate this entitlement
     */
    public void reactivate() {
        this.active = true;
        this.revokedBy = null;
        this.revokedAt = null;
        this.revokeReason = null;
        this.updatedAt = Instant.now();
    }

    /**
     * Builder helper: Set default timestamps
     */
    public static class EntitlementBuilder {
        public Entitlement build() {
            if (createdAt == null) {
                createdAt = Instant.now();
            }
            if (updatedAt == null) {
                updatedAt = Instant.now();
            }
            if (grantedAt == null) {
                grantedAt = Instant.now();
            }
            if (constraints == null) {
                constraints = EntitlementConstraints.none();
            }
            return new Entitlement(id, tenantId, partyId, resourceType, resourceId,
                    operations, constraints, source, sourceReference, grantedBy,
                    grantedAt, expiresAt, active, grantReason, revokeReason,
                    revokedAt, revokedBy, priority, metadata, createdAt, updatedAt);
        }
    }
}
