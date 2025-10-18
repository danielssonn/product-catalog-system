package com.bank.product.party.repository;

import com.bank.product.entitlement.Entitlement;
import com.bank.product.entitlement.ResourceType;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

/**
 * Repository for Entitlement entities
 *
 * Provides fine-grained access control queries for resource-scoped permissions.
 */
@Repository
public interface EntitlementRepository extends MongoRepository<Entitlement, String> {

    /**
     * Find all active entitlements for a party in a tenant
     *
     * @param tenantId Tenant ID
     * @param partyId Party ID
     * @return List of active entitlements
     */
    List<Entitlement> findByTenantIdAndPartyIdAndActiveTrue(String tenantId, String partyId);

    /**
     * Find all entitlements for a party in a tenant (including inactive)
     *
     * @param tenantId Tenant ID
     * @param partyId Party ID
     * @return List of all entitlements
     */
    List<Entitlement> findByTenantIdAndPartyId(String tenantId, String partyId);

    /**
     * Find active entitlements for a party on a specific resource type
     *
     * @param tenantId Tenant ID
     * @param partyId Party ID
     * @param resourceType Resource type (SOLUTION, ACCOUNT, etc.)
     * @return List of entitlements for that resource type
     */
    List<Entitlement> findByTenantIdAndPartyIdAndResourceTypeAndActiveTrue(
            String tenantId, String partyId, ResourceType resourceType);

    /**
     * Find active entitlements for a party on a specific resource
     *
     * @param tenantId Tenant ID
     * @param partyId Party ID
     * @param resourceType Resource type
     * @param resourceId Specific resource ID
     * @return List of entitlements for that specific resource
     */
    List<Entitlement> findByTenantIdAndPartyIdAndResourceTypeAndResourceIdAndActiveTrue(
            String tenantId, String partyId, ResourceType resourceType, String resourceId);

    /**
     * Find all entitlements for a specific resource (across all parties)
     * Useful for audit: "Who has access to solution-123?"
     *
     * @param tenantId Tenant ID
     * @param resourceType Resource type
     * @param resourceId Resource ID
     * @return List of entitlements for that resource
     */
    List<Entitlement> findByTenantIdAndResourceTypeAndResourceId(
            String tenantId, ResourceType resourceType, String resourceId);

    /**
     * Find all relationship-based entitlements for a party
     * These are derived from Neo4j relationships
     *
     * @param tenantId Tenant ID
     * @param partyId Party ID
     * @return List of relationship-based entitlements
     */
    @Query("{'tenantId': ?0, 'partyId': ?1, 'source': 'RELATIONSHIP_BASED', 'active': true}")
    List<Entitlement> findRelationshipBasedEntitlements(String tenantId, String partyId);

    /**
     * Find all expired entitlements that should be deactivated
     *
     * @param now Current timestamp
     * @return List of expired entitlements
     */
    @Query("{'expiresAt': {$lte: ?0}, 'active': true}")
    List<Entitlement> findExpiredEntitlements(Instant now);

    /**
     * Find all entitlements granted by a specific party
     * Useful for audit: "What access did Alice grant?"
     *
     * @param tenantId Tenant ID
     * @param grantedBy Party ID who granted the entitlements
     * @return List of entitlements
     */
    List<Entitlement> findByTenantIdAndGrantedBy(String tenantId, String grantedBy);

    /**
     * Find all type-level entitlements (not scoped to specific resource)
     * Example: "Can configure ANY CHECKING solution"
     *
     * @param tenantId Tenant ID
     * @param partyId Party ID
     * @param resourceType Resource type
     * @return List of type-level entitlements (resourceId = null)
     */
    @Query("{'tenantId': ?0, 'partyId': ?1, 'resourceType': ?2, 'resourceId': null, 'active': true}")
    List<Entitlement> findTypeLevelEntitlements(String tenantId, String partyId, ResourceType resourceType);

    /**
     * Find all delegated entitlements (temporary permissions)
     *
     * @param tenantId Tenant ID
     * @param partyId Party ID (delegatee)
     * @return List of delegated entitlements
     */
    @Query("{'tenantId': ?0, 'partyId': ?1, 'source': 'DELEGATED', 'active': true}")
    List<Entitlement> findDelegatedEntitlements(String tenantId, String partyId);

    /**
     * Count active entitlements for a party
     *
     * @param tenantId Tenant ID
     * @param partyId Party ID
     * @return Number of active entitlements
     */
    long countByTenantIdAndPartyIdAndActiveTrue(String tenantId, String partyId);

    /**
     * Delete all entitlements for a specific resource
     * Use when resource is deleted (cascade cleanup)
     *
     * @param tenantId Tenant ID
     * @param resourceType Resource type
     * @param resourceId Resource ID
     */
    void deleteByTenantIdAndResourceTypeAndResourceId(String tenantId, ResourceType resourceType, String resourceId);

    /**
     * Delete all entitlements for a party
     * Use when party is removed from tenant
     *
     * @param tenantId Tenant ID
     * @param partyId Party ID
     */
    void deleteByTenantIdAndPartyId(String tenantId, String partyId);
}
