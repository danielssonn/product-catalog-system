package com.bank.product.core.adapter;

import com.bank.product.core.model.*;
import com.bank.product.domain.solution.model.Solution;

/**
 * Vendor-agnostic interface for core banking system integration.
 * All core banking operations go through this abstraction to avoid vendor lock-in.
 *
 * Implementations must handle vendor-specific API calls, authentication,
 * error handling, and data mapping.
 */
public interface CoreBankingAdapter {

    /**
     * Get the type of core banking system this adapter supports.
     *
     * @return the core system type
     */
    CoreSystemType getType();

    /**
     * Provision a new product in the core banking system.
     *
     * @param solution the solution to provision
     * @param config connection configuration for the core system
     * @return result of the provisioning operation
     */
    CoreProvisioningResult provisionProduct(Solution solution, CoreSystemConfig config);

    /**
     * Update an existing product configuration in the core banking system.
     * Called when solution configuration changes post-provisioning.
     *
     * @param solution the updated solution
     * @param coreProductId the product ID in the core system
     * @param config connection configuration for the core system
     * @return result of the update operation
     */
    CoreProvisioningResult updateProduct(Solution solution, String coreProductId, CoreSystemConfig config);

    /**
     * Deactivate a product in the core banking system.
     * Prevents new accounts from being opened but maintains existing accounts.
     *
     * @param coreProductId the product ID in the core system
     * @param config connection configuration for the core system
     * @return result of the deactivation operation
     */
    CoreProvisioningResult deactivateProduct(String coreProductId, CoreSystemConfig config);

    /**
     * Permanently sunset/remove a product from the core banking system.
     * Should only be called after all accounts have been migrated or closed.
     *
     * @param coreProductId the product ID in the core system
     * @param config connection configuration for the core system
     * @return result of the sunset operation
     */
    CoreProvisioningResult sunsetProduct(String coreProductId, CoreSystemConfig config);

    /**
     * Verify that a product exists in the core banking system.
     * Used for reconciliation and drift detection.
     *
     * @param coreProductId the product ID in the core system
     * @param config connection configuration for the core system
     * @return true if the product exists, false otherwise
     */
    boolean verifyProductExists(String coreProductId, CoreSystemConfig config);

    /**
     * Retrieve current product configuration from the core banking system.
     * Used for drift detection and reconciliation.
     *
     * @param coreProductId the product ID in the core system
     * @param config connection configuration for the core system
     * @return current product details from the core system
     */
    CoreProductDetails getProductDetails(String coreProductId, CoreSystemConfig config);

    /**
     * Check if the core banking system is healthy and reachable.
     * Used by circuit breakers and health monitoring.
     *
     * @param config connection configuration for the core system
     * @return true if the system is healthy, false otherwise
     */
    boolean healthCheck(CoreSystemConfig config);

    /**
     * Get the adapter version/implementation details.
     * Useful for logging and diagnostics.
     *
     * @return adapter version information
     */
    default String getAdapterVersion() {
        return "1.0.0";
    }
}
