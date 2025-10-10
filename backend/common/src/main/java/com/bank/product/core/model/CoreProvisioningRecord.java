package com.bank.product.core.model;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a product's provisioning record in a specific core banking system.
 * A solution can have multiple provisioning records if provisioned to multiple cores.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CoreProvisioningRecord {
    /**
     * Core system instance ID (e.g., "temenos-us-east", "fis-eu-west")
     */
    private String coreSystemId;

    /**
     * Core system type
     */
    private CoreSystemType coreSystemType;

    /**
     * Product ID in the core system
     */
    private String coreProductId;

    /**
     * Provisioning status for this specific core
     */
    private CoreProvisioningStatus status;

    /**
     * When the product was successfully provisioned
     */
    private Instant provisionedAt;

    /**
     * Last time configuration was synced to core
     */
    private Instant lastSyncedAt;

    /**
     * Core system-specific metadata
     */
    @Builder.Default
    private Map<String, Object> coreMetadata = new HashMap<>();

    /**
     * Error messages from failed operations
     */
    @Builder.Default
    private List<String> errorMessages = new ArrayList<>();

    /**
     * Number of retry attempts
     */
    @Builder.Default
    private int retryCount = 0;

    /**
     * Last error timestamp
     */
    private Instant lastErrorAt;
}
