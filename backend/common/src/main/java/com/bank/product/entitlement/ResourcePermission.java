package com.bank.product.entitlement;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Computed permission for a resource
 *
 * This is the result of resolving all entitlements for a party on a specific resource.
 * Multiple entitlements may be merged into a single ResourcePermission.
 *
 * Included in ProcessingContext for fast authorization checks without database lookups.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ResourcePermission implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Resource type
     */
    private ResourceType resourceType;

    /**
     * Specific resource ID (null = applies to all resources of this type)
     */
    private String resourceId;

    /**
     * Operations allowed on this resource
     */
    @Builder.Default
    private Set<ResourceOperation> allowedOperations = new HashSet<>();

    /**
     * Merged constraints from all applicable entitlements
     * Uses most restrictive constraint when multiple entitlements apply
     */
    private EntitlementConstraints effectiveConstraints;

    /**
     * Highest priority among all merged entitlements
     */
    @Builder.Default
    private int priority = 0;

    /**
     * Source entitlement IDs that contributed to this permission
     * For audit trail
     */
    @Builder.Default
    private Set<String> sourceEntitlementIds = new HashSet<>();

    /**
     * Check if operation is allowed
     */
    public boolean hasOperation(ResourceOperation operation) {
        return allowedOperations.contains(operation);
    }

    /**
     * Check if amount is within constraints
     */
    public boolean isAmountAllowed(BigDecimal amount) {
        if (effectiveConstraints == null) {
            return true; // No constraints
        }
        return effectiveConstraints.isAmountAllowed(amount);
    }

    /**
     * Check if channel is allowed
     */
    public boolean isChannelAllowed(String channel) {
        if (effectiveConstraints == null) {
            return true; // No constraints
        }
        return effectiveConstraints.isChannelAllowed(channel);
    }

    /**
     * Check if country is allowed
     */
    public boolean isCountryAllowed(String countryCode) {
        if (effectiveConstraints == null) {
            return true; // No constraints
        }
        return effectiveConstraints.isCountryAllowed(countryCode);
    }

    /**
     * Check if product type is allowed
     */
    public boolean isProductTypeAllowed(String productType) {
        if (effectiveConstraints == null) {
            return true; // No constraints
        }
        return effectiveConstraints.isProductTypeAllowed(productType);
    }

    /**
     * Get maximum transaction amount from constraints
     */
    public BigDecimal getMaxAmount() {
        if (effectiveConstraints == null) {
            return null; // No limit
        }
        return effectiveConstraints.getMaxAmount();
    }

    /**
     * Requires approval?
     */
    public boolean requiresApproval() {
        if (effectiveConstraints == null) {
            return false;
        }
        return effectiveConstraints.isRequiresApproval();
    }

    /**
     * Requires MFA?
     */
    public boolean requiresMfa() {
        if (effectiveConstraints == null) {
            return false;
        }
        return effectiveConstraints.isRequiresMfa();
    }

    /**
     * Merge multiple entitlements into a single ResourcePermission
     *
     * Merge strategy:
     * - Operations: UNION (grant access if ANY entitlement allows)
     * - Constraints: INTERSECTION (most restrictive wins)
     * - Priority: MAX (highest priority wins for conflict resolution)
     */
    public static ResourcePermission merge(List<Entitlement> entitlements,
                                           ResourceType resourceType,
                                           String resourceId) {
        if (entitlements == null || entitlements.isEmpty()) {
            return ResourcePermission.builder()
                    .resourceType(resourceType)
                    .resourceId(resourceId)
                    .allowedOperations(new HashSet<>())
                    .build();
        }

        // Filter valid entitlements
        List<Entitlement> validEntitlements = entitlements.stream()
                .filter(Entitlement::isValid)
                .collect(Collectors.toList());

        if (validEntitlements.isEmpty()) {
            return ResourcePermission.builder()
                    .resourceType(resourceType)
                    .resourceId(resourceId)
                    .allowedOperations(new HashSet<>())
                    .build();
        }

        // Collect all operations (UNION)
        Set<ResourceOperation> allOperations = new HashSet<>();
        Set<String> entitlementIds = new HashSet<>();
        int maxPriority = 0;

        for (Entitlement ent : validEntitlements) {
            allOperations.addAll(ent.getOperations());
            entitlementIds.add(ent.getId());
            maxPriority = Math.max(maxPriority, ent.getPriority());
        }

        // Merge constraints (most restrictive wins)
        EntitlementConstraints mergedConstraints = mergeConstraints(validEntitlements);

        return ResourcePermission.builder()
                .resourceType(resourceType)
                .resourceId(resourceId)
                .allowedOperations(allOperations)
                .effectiveConstraints(mergedConstraints)
                .priority(maxPriority)
                .sourceEntitlementIds(entitlementIds)
                .build();
    }

    /**
     * Merge constraints from multiple entitlements
     * Strategy: Most restrictive wins
     */
    private static EntitlementConstraints mergeConstraints(List<Entitlement> entitlements) {
        EntitlementConstraints.EntitlementConstraintsBuilder builder =
                EntitlementConstraints.builder();

        BigDecimal minMaxAmount = null;
        BigDecimal maxMinAmount = null;
        Set<String> intersectedChannels = null;
        Set<String> unionBlockedChannels = new HashSet<>();
        boolean anyRequiresApproval = false;
        boolean anyRequiresMfa = false;

        for (Entitlement ent : entitlements) {
            EntitlementConstraints c = ent.getConstraints();
            if (c == null) continue;

            // Max amount: minimum of all max amounts (most restrictive)
            if (c.getMaxAmount() != null) {
                if (minMaxAmount == null || c.getMaxAmount().compareTo(minMaxAmount) < 0) {
                    minMaxAmount = c.getMaxAmount();
                }
            }

            // Min amount: maximum of all min amounts (most restrictive)
            if (c.getMinAmount() != null) {
                if (maxMinAmount == null || c.getMinAmount().compareTo(maxMinAmount) > 0) {
                    maxMinAmount = c.getMinAmount();
                }
            }

            // Channels: intersection (only if ALL allow)
            if (!c.getAllowedChannels().isEmpty()) {
                if (intersectedChannels == null) {
                    intersectedChannels = new HashSet<>(c.getAllowedChannels());
                } else {
                    intersectedChannels.retainAll(c.getAllowedChannels());
                }
            }

            // Blocked channels: union (if ANY blocks)
            unionBlockedChannels.addAll(c.getBlockedChannels());

            // Approval/MFA: if ANY requires, all require
            if (c.isRequiresApproval()) {
                anyRequiresApproval = true;
            }
            if (c.isRequiresMfa()) {
                anyRequiresMfa = true;
            }
        }

        builder.maxAmount(minMaxAmount);
        builder.minAmount(maxMinAmount);
        if (intersectedChannels != null) {
            builder.allowedChannels(intersectedChannels);
        }
        builder.blockedChannels(unionBlockedChannels);
        builder.requiresApproval(anyRequiresApproval);
        builder.requiresMfa(anyRequiresMfa);

        return builder.build();
    }

    /**
     * Create empty permission (no access)
     */
    public static ResourcePermission none(ResourceType resourceType, String resourceId) {
        return ResourcePermission.builder()
                .resourceType(resourceType)
                .resourceId(resourceId)
                .allowedOperations(new HashSet<>())
                .build();
    }

    /**
     * Create full permission (all operations, no constraints)
     */
    public static ResourcePermission full(ResourceType resourceType, String resourceId) {
        Set<ResourceOperation> allOps = new HashSet<>();
        for (ResourceOperation op : ResourceOperation.values()) {
            allOps.add(op);
        }

        return ResourcePermission.builder()
                .resourceType(resourceType)
                .resourceId(resourceId)
                .allowedOperations(allOps)
                .effectiveConstraints(EntitlementConstraints.none())
                .build();
    }
}
