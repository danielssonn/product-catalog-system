package com.bank.product.context;

import com.bank.product.entitlement.ResourceOperation;
import com.bank.product.entitlement.ResourcePermission;
import com.bank.product.entitlement.ResourceType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.*;

/**
 * Permission Context for Authorization Decisions
 *
 * Captures party-specific and role-based permissions.
 * Used for authorization decisions without additional lookups.
 *
 * @author System Architecture Team
 * @since 1.0
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PermissionContext implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Can this party open new accounts?
     */
    @Builder.Default
    private boolean canOpenAccounts = true;

    /**
     * Can this party initiate payments?
     */
    @Builder.Default
    private boolean canInitiatePayments = true;

    /**
     * Can this party view account details?
     */
    @Builder.Default
    private boolean canViewAccounts = true;

    /**
     * Can this party manage other parties?
     */
    @Builder.Default
    private boolean canManageParties = false;

    /**
     * Does this party require approval for transactions?
     */
    @Builder.Default
    private boolean requiresApproval = false;

    /**
     * Maximum transaction amount (in base currency)
     */
    @Builder.Default
    private BigDecimal maxTransactionLimit = new BigDecimal("100000");

    /**
     * Daily transaction limit
     */
    @Builder.Default
    private BigDecimal dailyTransactionLimit = new BigDecimal("1000000");

    /**
     * Monthly transaction limit
     */
    @Builder.Default
    private BigDecimal monthlyTransactionLimit = new BigDecimal("10000000");

    /**
     * Approved product types for this party
     * Example: ["CHECKING", "SAVINGS", "LOAN"]
     */
    @Builder.Default
    private Set<String> approvedProductTypes = new HashSet<>(Arrays.asList(
            "CHECKING", "SAVINGS"
    ));

    /**
     * Approved transaction types
     * Example: ["ACH", "WIRE", "CHECK"]
     */
    @Builder.Default
    private Set<String> approvedTransactionTypes = new HashSet<>(Arrays.asList(
            "ACH", "WIRE", "CHECK", "CARD"
    ));

    /**
     * Restricted operations for this party
     * Example: ["INTERNATIONAL_WIRE", "CRYPTO_TRANSFER"]
     */
    @Builder.Default
    private Set<String> restrictedOperations = new HashSet<>();

    /**
     * Allowed countries for transactions
     * Empty = all countries allowed
     */
    @Builder.Default
    private Set<String> allowedCountries = new HashSet<>();

    /**
     * Blocked countries for transactions
     */
    @Builder.Default
    private Set<String> blockedCountries = new HashSet<>();

    /**
     * Special permissions granted
     * Example: ["ADMIN_OVERRIDE", "BULK_PROCESSING"]
     */
    @Builder.Default
    private Set<String> specialPermissions = new HashSet<>();

    /**
     * Permission source
     * Values: ROLE_BASED, PARTY_BASED, INHERITED, COMBINED
     */
    @Builder.Default
    private String permissionSource = "COMBINED";

    /**
     * Additional permission metadata
     */
    @Builder.Default
    private Map<String, String> permissionMetadata = new HashMap<>();

    /**
     * Fine-grained resource-scoped entitlements
     * Key: resourceType:resourceId (e.g., "SOLUTION:sol-123", "ACCOUNT:acc-456")
     * Value: ResourcePermission with allowed operations and constraints
     *
     * This enables checks like:
     * - Can party VIEW solution sol-123?
     * - Can party TRANSACT on account acc-456 with amount $50K?
     * - Can party CONFIGURE CHECKING solutions (resourceId = null)?
     */
    @Builder.Default
    private Map<String, ResourcePermission> resourceEntitlements = new HashMap<>();

    /**
     * Check if party has specific permission
     */
    public boolean hasPermission(String operation) {
        if (operation == null) {
            return false;
        }

        // Check if operation is restricted
        if (restrictedOperations.contains(operation)) {
            return false;
        }

        // Check special permissions
        if (specialPermissions.contains(operation)) {
            return true;
        }

        // Check specific operation permissions
        return switch (operation.toUpperCase()) {
            case "OPEN_ACCOUNT", "CREATE_ACCOUNT" -> canOpenAccounts;
            case "INITIATE_PAYMENT", "MAKE_PAYMENT" -> canInitiatePayments;
            case "VIEW_ACCOUNT", "READ_ACCOUNT" -> canViewAccounts;
            case "MANAGE_PARTY", "MANAGE_PARTIES" -> canManageParties;
            default -> !restrictedOperations.contains(operation);
        };
    }

    /**
     * Check if product type is approved
     */
    public boolean isProductTypeApproved(String productType) {
        if (productType == null) {
            return false;
        }
        // Empty set means all types approved
        return approvedProductTypes.isEmpty() || approvedProductTypes.contains(productType);
    }

    /**
     * Check if transaction type is approved
     */
    public boolean isTransactionTypeApproved(String transactionType) {
        if (transactionType == null) {
            return false;
        }
        // Empty set means all types approved
        return approvedTransactionTypes.isEmpty() || approvedTransactionTypes.contains(transactionType);
    }

    /**
     * Check if amount is within limits
     */
    public boolean isWithinTransactionLimit(BigDecimal amount) {
        if (amount == null) {
            return false;
        }
        return amount.compareTo(maxTransactionLimit) <= 0;
    }

    /**
     * Check if country is allowed for transactions
     */
    public boolean isCountryAllowed(String countryCode) {
        if (countryCode == null) {
            return false;
        }

        // Check blocked list first
        if (blockedCountries.contains(countryCode)) {
            return false;
        }

        // If allowed list is empty, all countries are allowed
        if (allowedCountries.isEmpty()) {
            return true;
        }

        // Check allowed list
        return allowedCountries.contains(countryCode);
    }

    /**
     * Add special permission
     */
    public void addSpecialPermission(String permission) {
        if (specialPermissions == null) {
            specialPermissions = new HashSet<>();
        }
        specialPermissions.add(permission);
    }

    /**
     * Add restricted operation
     */
    public void addRestrictedOperation(String operation) {
        if (restrictedOperations == null) {
            restrictedOperations = new HashSet<>();
        }
        restrictedOperations.add(operation);
    }

    /**
     * Create default permission context
     */
    public static PermissionContext createDefault() {
        return PermissionContext.builder()
                .canOpenAccounts(true)
                .canInitiatePayments(true)
                .canViewAccounts(true)
                .canManageParties(false)
                .requiresApproval(false)
                .maxTransactionLimit(new BigDecimal("100000"))
                .dailyTransactionLimit(new BigDecimal("1000000"))
                .monthlyTransactionLimit(new BigDecimal("10000000"))
                .approvedProductTypes(new HashSet<>(Arrays.asList("CHECKING", "SAVINGS")))
                .approvedTransactionTypes(new HashSet<>(Arrays.asList("ACH", "WIRE", "CHECK", "CARD")))
                .restrictedOperations(new HashSet<>())
                .allowedCountries(new HashSet<>())
                .blockedCountries(new HashSet<>())
                .specialPermissions(new HashSet<>())
                .permissionSource("DEFAULT")
                .permissionMetadata(new HashMap<>())
                .build();
    }

    /**
     * Create admin permission context
     */
    public static PermissionContext createAdmin() {
        PermissionContext admin = createDefault();
        admin.setCanManageParties(true);
        admin.setMaxTransactionLimit(new BigDecimal("100000000"));
        admin.setDailyTransactionLimit(new BigDecimal("1000000000"));
        admin.setApprovedProductTypes(new HashSet<>()); // Empty = all approved
        admin.setApprovedTransactionTypes(new HashSet<>()); // Empty = all approved
        admin.addSpecialPermission("ADMIN_OVERRIDE");
        admin.addSpecialPermission("BULK_PROCESSING");
        admin.setPermissionSource("ADMIN");
        return admin;
    }

    /**
     * Create restricted permission context
     */
    public static PermissionContext createRestricted() {
        return PermissionContext.builder()
                .canOpenAccounts(false)
                .canInitiatePayments(false)
                .canViewAccounts(true)
                .canManageParties(false)
                .requiresApproval(true)
                .maxTransactionLimit(new BigDecimal("1000"))
                .dailyTransactionLimit(new BigDecimal("5000"))
                .monthlyTransactionLimit(new BigDecimal("50000"))
                .approvedProductTypes(Set.of("CHECKING"))
                .approvedTransactionTypes(Set.of("ACH"))
                .restrictedOperations(new HashSet<>(Arrays.asList(
                        "INTERNATIONAL_WIRE", "CRYPTO_TRANSFER", "BULK_PAYMENT"
                )))
                .allowedCountries(Set.of("US"))
                .blockedCountries(new HashSet<>())
                .specialPermissions(new HashSet<>())
                .permissionSource("RESTRICTED")
                .permissionMetadata(new HashMap<>())
                .build();
    }

    // ========================================================================
    // Resource-Scoped Permission Methods (Fine-Grained ABAC)
    // ========================================================================

    /**
     * Check if party has permission to perform operation on specific resource
     *
     * @param operation Operation to perform (e.g., "VIEW", "TRANSACT")
     * @param resourceType Resource type (e.g., SOLUTION, ACCOUNT)
     * @param resourceId Specific resource ID (null checks type-level permissions)
     * @return true if permitted
     */
    public boolean hasPermissionOnResource(String operation, ResourceType resourceType, String resourceId) {
        if (operation == null || resourceType == null) {
            return false;
        }

        try {
            ResourceOperation resourceOp = ResourceOperation.valueOf(operation.toUpperCase());
            return hasPermissionOnResource(resourceOp, resourceType, resourceId);
        } catch (IllegalArgumentException e) {
            // Operation not recognized as ResourceOperation
            return false;
        }
    }

    /**
     * Check if party has permission to perform operation on specific resource
     *
     * @param operation ResourceOperation enum
     * @param resourceType Resource type
     * @param resourceId Specific resource ID (null checks type-level permissions)
     * @return true if permitted
     */
    public boolean hasPermissionOnResource(ResourceOperation operation, ResourceType resourceType, String resourceId) {
        if (operation == null || resourceType == null) {
            return false;
        }

        // Check specific resource permission first
        if (resourceId != null) {
            String key = makeResourceKey(resourceType, resourceId);
            ResourcePermission perm = resourceEntitlements.get(key);
            if (perm != null && perm.hasOperation(operation)) {
                return true;
            }
        }

        // Check type-level permission (resourceId = null)
        String typeKey = makeResourceKey(resourceType, null);
        ResourcePermission typePerm = resourceEntitlements.get(typeKey);
        if (typePerm != null && typePerm.hasOperation(operation)) {
            return true;
        }

        // Fall back to global permissions for backward compatibility
        return hasPermission(operation.name());
    }

    /**
     * Get resource permission for specific resource
     *
     * @param resourceType Resource type
     * @param resourceId Specific resource ID
     * @return ResourcePermission if exists, null otherwise
     */
    public ResourcePermission getResourcePermission(ResourceType resourceType, String resourceId) {
        if (resourceType == null) {
            return null;
        }

        // Try specific resource first
        if (resourceId != null) {
            String key = makeResourceKey(resourceType, resourceId);
            ResourcePermission perm = resourceEntitlements.get(key);
            if (perm != null) {
                return perm;
            }
        }

        // Fall back to type-level permission
        String typeKey = makeResourceKey(resourceType, null);
        return resourceEntitlements.get(typeKey);
    }

    /**
     * Add resource permission
     *
     * @param permission ResourcePermission to add
     */
    public void addResourcePermission(ResourcePermission permission) {
        if (permission == null || permission.getResourceType() == null) {
            return;
        }

        String key = makeResourceKey(permission.getResourceType(), permission.getResourceId());
        resourceEntitlements.put(key, permission);
    }

    /**
     * Check if party has permission on resource with amount constraint
     *
     * @param operation Operation to perform
     * @param resourceType Resource type
     * @param resourceId Specific resource ID
     * @param amount Transaction amount
     * @return true if permitted and amount within limits
     */
    public boolean hasPermissionOnResourceWithAmount(ResourceOperation operation,
                                                      ResourceType resourceType,
                                                      String resourceId,
                                                      BigDecimal amount) {
        if (!hasPermissionOnResource(operation, resourceType, resourceId)) {
            return false;
        }

        ResourcePermission perm = getResourcePermission(resourceType, resourceId);
        if (perm == null) {
            return false;
        }

        return perm.isAmountAllowed(amount);
    }

    /**
     * Check if party has permission on resource with channel constraint
     *
     * @param operation Operation to perform
     * @param resourceType Resource type
     * @param resourceId Specific resource ID
     * @param channel Channel (e.g., "WEB", "MOBILE")
     * @return true if permitted and channel allowed
     */
    public boolean hasPermissionOnResourceWithChannel(ResourceOperation operation,
                                                       ResourceType resourceType,
                                                       String resourceId,
                                                       String channel) {
        if (!hasPermissionOnResource(operation, resourceType, resourceId)) {
            return false;
        }

        ResourcePermission perm = getResourcePermission(resourceType, resourceId);
        if (perm == null) {
            return false;
        }

        return perm.isChannelAllowed(channel);
    }

    /**
     * Get all resource permissions
     *
     * @return Map of all resource permissions
     */
    public Map<String, ResourcePermission> getAllResourcePermissions() {
        return new HashMap<>(resourceEntitlements);
    }

    /**
     * Get all permissions for a specific resource type
     *
     * @param resourceType Resource type
     * @return List of ResourcePermissions for that type
     */
    public List<ResourcePermission> getPermissionsForType(ResourceType resourceType) {
        if (resourceType == null) {
            return Collections.emptyList();
        }

        return resourceEntitlements.values().stream()
                .filter(perm -> perm.getResourceType().equals(resourceType))
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * Make resource key for map lookup
     */
    private String makeResourceKey(ResourceType resourceType, String resourceId) {
        if (resourceId == null) {
            return resourceType.name() + ":*";
        }
        return resourceType.name() + ":" + resourceId;
    }
}
