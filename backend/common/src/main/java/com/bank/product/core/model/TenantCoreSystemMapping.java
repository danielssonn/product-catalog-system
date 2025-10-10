package com.bank.product.core.model;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Maps a tenant to one or more core banking systems.
 * Supports multi-core and geo-distributed deployments.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "tenant_core_mappings")
public class TenantCoreSystemMapping {

    @Id
    private String id;

    /**
     * Tenant ID
     */
    @Indexed(unique = true)
    private String tenantId;

    /**
     * List of core systems configured for this tenant
     */
    @Builder.Default
    private List<CoreSystemMapping> coreSystems = new ArrayList<>();

    /**
     * Default core system ID (used when no routing rule applies)
     */
    private String defaultCoreSystemId;

    /**
     * When this mapping was created
     */
    private Instant createdAt;

    /**
     * When this mapping was last updated
     */
    private Instant updatedAt;

    /**
     * User who last updated this mapping
     */
    private String updatedBy;

    /**
     * Individual core system mapping with configuration
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CoreSystemMapping {
        /**
         * Unique identifier for this core system instance
         */
        private String coreSystemId;

        /**
         * Type of core banking system
         */
        private CoreSystemType coreSystemType;

        /**
         * Whether this core system is currently active
         */
        @Builder.Default
        private boolean active = true;

        /**
         * Priority for routing (higher = preferred)
         */
        @Builder.Default
        private int priority = 0;

        /**
         * Geographic region this core serves (e.g., "US-EAST", "EU-WEST")
         */
        private String region;

        /**
         * Product types this core supports (empty = all types)
         */
        @Builder.Default
        private List<String> supportedProductTypes = new ArrayList<>();

        /**
         * Connection configuration (encrypted)
         */
        private CoreSystemConfig config;

        /**
         * When this core was added to the tenant
         */
        private Instant addedAt;
    }
}
