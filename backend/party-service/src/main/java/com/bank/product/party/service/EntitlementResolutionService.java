package com.bank.product.party.service;

import com.bank.product.entitlement.*;
import com.bank.product.party.repository.EntitlementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for resolving fine-grained entitlements for parties
 *
 * This service:
 * 1. Loads entitlements from MongoDB
 * 2. Merges multiple entitlements into ResourcePermissions
 * 3. Handles relationship-based, role-based, and explicit entitlements
 * 4. Provides efficient lookup for authorization decisions
 *
 * @author Product Catalog Team
 * @since 2.0 - Fine-Grained Entitlements
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EntitlementResolutionService {

    private final EntitlementRepository entitlementRepository;

    /**
     * Resolve all resource permissions for a party in a tenant
     *
     * This is the main entry point for context resolution.
     * Returns a map of ResourcePermissions keyed by "resourceType:resourceId".
     *
     * @param tenantId Tenant ID
     * @param partyId Party ID
     * @return Map of resource permissions
     */
    public Map<String, ResourcePermission> resolveAllPermissions(String tenantId, String partyId) {
        long startTime = System.currentTimeMillis();

        log.debug("Resolving entitlements for tenant: {}, party: {}", tenantId, partyId);

        // Load all active entitlements for the party
        List<Entitlement> entitlements = entitlementRepository.findByTenantIdAndPartyIdAndActiveTrue(
                tenantId, partyId);

        log.debug("Found {} active entitlements for party: {}", entitlements.size(), partyId);

        // Group entitlements by resource (type + id)
        Map<String, List<Entitlement>> groupedByResource = entitlements.stream()
                .collect(Collectors.groupingBy(e ->
                        makeResourceKey(e.getResourceType(), e.getResourceId())));

        // Merge entitlements into ResourcePermissions
        Map<String, ResourcePermission> permissions = new HashMap<>();
        for (Map.Entry<String, List<Entitlement>> entry : groupedByResource.entrySet()) {
            String resourceKey = entry.getKey();
            List<Entitlement> resourceEntitlements = entry.getValue();

            // Parse resource type and ID from key
            String[] parts = resourceKey.split(":", 2);
            ResourceType resourceType = ResourceType.valueOf(parts[0]);
            String resourceId = parts.length > 1 && !parts[1].equals("*") ? parts[1] : null;

            // Merge entitlements into single permission
            ResourcePermission permission = ResourcePermission.merge(
                    resourceEntitlements, resourceType, resourceId);

            permissions.put(resourceKey, permission);
        }

        long resolutionTime = System.currentTimeMillis() - startTime;
        log.info("Resolved {} resource permissions in {}ms for party: {}",
                permissions.size(), resolutionTime, partyId);

        return permissions;
    }

    /**
     * Resolve permissions for a specific resource type
     *
     * @param tenantId Tenant ID
     * @param partyId Party ID
     * @param resourceType Resource type (SOLUTION, ACCOUNT, etc.)
     * @return Map of resource permissions for that type
     */
    public Map<String, ResourcePermission> resolvePermissionsForType(
            String tenantId, String partyId, ResourceType resourceType) {

        log.debug("Resolving {} entitlements for party: {}", resourceType, partyId);

        List<Entitlement> entitlements = entitlementRepository
                .findByTenantIdAndPartyIdAndResourceTypeAndActiveTrue(tenantId, partyId, resourceType);

        // Group by resource ID
        Map<String, List<Entitlement>> groupedByResourceId = entitlements.stream()
                .collect(Collectors.groupingBy(e ->
                        e.getResourceId() != null ? e.getResourceId() : "*"));

        Map<String, ResourcePermission> permissions = new HashMap<>();
        for (Map.Entry<String, List<Entitlement>> entry : groupedByResourceId.entrySet()) {
            String resourceId = entry.getKey().equals("*") ? null : entry.getKey();
            List<Entitlement> resourceEntitlements = entry.getValue();

            ResourcePermission permission = ResourcePermission.merge(
                    resourceEntitlements, resourceType, resourceId);

            String key = makeResourceKey(resourceType, resourceId);
            permissions.put(key, permission);
        }

        log.debug("Resolved {} {} permissions for party: {}",
                permissions.size(), resourceType, partyId);

        return permissions;
    }

    /**
     * Resolve permissions for a specific resource
     *
     * @param tenantId Tenant ID
     * @param partyId Party ID
     * @param resourceType Resource type
     * @param resourceId Specific resource ID
     * @return ResourcePermission if exists, null otherwise
     */
    public ResourcePermission resolvePermissionForResource(
            String tenantId, String partyId, ResourceType resourceType, String resourceId) {

        log.debug("Resolving permission for {}:{} for party: {}",
                resourceType, resourceId, partyId);

        // Load entitlements for specific resource
        List<Entitlement> entitlements = entitlementRepository
                .findByTenantIdAndPartyIdAndResourceTypeAndResourceIdAndActiveTrue(
                        tenantId, partyId, resourceType, resourceId);

        // Also load type-level entitlements (resourceId = null)
        List<Entitlement> typeLevelEntitlements = entitlementRepository
                .findTypeLevelEntitlements(tenantId, partyId, resourceType);

        // Combine both
        List<Entitlement> allEntitlements = new ArrayList<>();
        allEntitlements.addAll(entitlements);
        allEntitlements.addAll(typeLevelEntitlements);

        if (allEntitlements.isEmpty()) {
            log.debug("No entitlements found for {}:{} for party: {}",
                    resourceType, resourceId, partyId);
            return null;
        }

        // Merge into single permission
        return ResourcePermission.merge(allEntitlements, resourceType, resourceId);
    }

    /**
     * Check if party has specific operation on resource
     *
     * @param tenantId Tenant ID
     * @param partyId Party ID
     * @param operation Operation to check
     * @param resourceType Resource type
     * @param resourceId Resource ID
     * @return true if permitted
     */
    public boolean hasPermission(String tenantId, String partyId,
                                  ResourceOperation operation,
                                  ResourceType resourceType,
                                  String resourceId) {

        ResourcePermission permission = resolvePermissionForResource(
                tenantId, partyId, resourceType, resourceId);

        if (permission == null) {
            return false;
        }

        return permission.hasOperation(operation);
    }

    /**
     * Grant entitlement to a party
     *
     * @param tenantId Tenant ID
     * @param partyId Party ID who receives the entitlement
     * @param resourceType Resource type
     * @param resourceId Specific resource ID (null for type-level)
     * @param operations Operations to grant
     * @param constraints Constraints on the entitlement
     * @param grantedBy Who granted this entitlement
     * @param source How the entitlement was granted
     * @return Created entitlement
     */
    public Entitlement grantEntitlement(String tenantId, String partyId,
                                        ResourceType resourceType, String resourceId,
                                        Set<ResourceOperation> operations,
                                        EntitlementConstraints constraints,
                                        String grantedBy,
                                        EntitlementSource source) {

        log.info("Granting entitlement to party: {} for {}:{}, operations: {}",
                partyId, resourceType, resourceId, operations);

        Entitlement entitlement = Entitlement.builder()
                .tenantId(tenantId)
                .partyId(partyId)
                .resourceType(resourceType)
                .resourceId(resourceId)
                .operations(operations)
                .constraints(constraints)
                .grantedBy(grantedBy)
                .source(source)
                .active(true)
                .build();

        Entitlement saved = entitlementRepository.save(entitlement);

        log.info("Entitlement granted: {} for party: {}", saved.getId(), partyId);

        return saved;
    }

    /**
     * Revoke entitlement
     *
     * @param entitlementId Entitlement ID
     * @param revokedBy Who revoked it
     * @param reason Reason for revocation
     */
    public void revokeEntitlement(String entitlementId, String revokedBy, String reason) {
        log.info("Revoking entitlement: {} by: {}, reason: {}",
                entitlementId, revokedBy, reason);

        Entitlement entitlement = entitlementRepository.findById(entitlementId)
                .orElseThrow(() -> new IllegalArgumentException("Entitlement not found: " + entitlementId));

        entitlement.revoke(revokedBy, reason);
        entitlementRepository.save(entitlement);

        log.info("Entitlement revoked: {}", entitlementId);
    }

    /**
     * Revoke all entitlements for a resource
     * Used when resource is deleted or party access should be removed
     *
     * @param tenantId Tenant ID
     * @param resourceType Resource type
     * @param resourceId Resource ID
     * @param revokedBy Who revoked them
     * @param reason Reason for revocation
     */
    public void revokeAllEntitlementsForResource(String tenantId,
                                                  ResourceType resourceType,
                                                  String resourceId,
                                                  String revokedBy,
                                                  String reason) {

        log.info("Revoking all entitlements for {}:{} by: {}",
                resourceType, resourceId, revokedBy);

        List<Entitlement> entitlements = entitlementRepository
                .findByTenantIdAndResourceTypeAndResourceId(tenantId, resourceType, resourceId);

        for (Entitlement entitlement : entitlements) {
            entitlement.revoke(revokedBy, reason);
        }

        entitlementRepository.saveAll(entitlements);

        log.info("Revoked {} entitlements for {}:{}",
                entitlements.size(), resourceType, resourceId);
    }

    /**
     * Get all entitlements for a party (for audit/debugging)
     *
     * @param tenantId Tenant ID
     * @param partyId Party ID
     * @return List of all entitlements (including inactive)
     */
    public List<Entitlement> getAllEntitlements(String tenantId, String partyId) {
        return entitlementRepository.findByTenantIdAndPartyId(tenantId, partyId);
    }

    /**
     * Get all parties with access to a resource (for audit)
     *
     * @param tenantId Tenant ID
     * @param resourceType Resource type
     * @param resourceId Resource ID
     * @return List of entitlements (showing who has access)
     */
    public List<Entitlement> getPartiesWithAccessToResource(String tenantId,
                                                             ResourceType resourceType,
                                                             String resourceId) {
        return entitlementRepository.findByTenantIdAndResourceTypeAndResourceId(
                tenantId, resourceType, resourceId);
    }

    /**
     * Cleanup expired entitlements
     * This should be called by a scheduled job
     */
    public int cleanupExpiredEntitlements() {
        log.info("Cleaning up expired entitlements");

        List<Entitlement> expired = entitlementRepository.findExpiredEntitlements(
                java.time.Instant.now());

        for (Entitlement entitlement : expired) {
            entitlement.revoke("SYSTEM", "Automatic expiration");
        }

        entitlementRepository.saveAll(expired);

        log.info("Deactivated {} expired entitlements", expired.size());

        return expired.size();
    }

    /**
     * Make resource key for map lookup
     * Format: "RESOURCE_TYPE:resourceId" or "RESOURCE_TYPE:*" for type-level
     */
    private String makeResourceKey(ResourceType resourceType, String resourceId) {
        if (resourceId == null) {
            return resourceType.name() + ":*";
        }
        return resourceType.name() + ":" + resourceId;
    }
}
