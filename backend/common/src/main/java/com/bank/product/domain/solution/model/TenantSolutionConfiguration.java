package com.bank.product.domain.solution.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Tenant-specific configuration of a solution from the master catalog
 * This represents the configuration phase before a solution becomes active
 */
@Data
@Document(collection = "tenant_solution_configurations")
@CompoundIndex(name = "tenant_config_idx", def = "{'tenantId': 1, 'configurationId': 1}")
@CompoundIndex(name = "tenant_catalog_idx", def = "{'tenantId': 1, 'catalogProductId': 1}")
public class TenantSolutionConfiguration {

    @Id
    private String id;

    @Indexed
    private String tenantId;

    @Indexed
    private String configurationId;

    // Reference to master catalog product
    @Indexed
    private String catalogProductId;

    private String catalogProductName;

    // Tenant's custom name for this product (if allowed)
    private String customName;

    private String customDescription;

    private ConfigurationStatus status;

    // Tenant's customized pricing (within allowed ranges)
    private PricingDetails customPricing;

    // Selected/configured features
    private Map<String, Object> selectedFeatures;

    // Customized terms
    private SolutionTerms customTerms;

    // Tenant-selected channels
    private List<String> enabledChannels;

    // Custom eligibility criteria
    private List<String> customEligibilityCriteria;

    // Approval tracking
    private ApprovalInfo approvalInfo;

    // Version tracking
    private String configVersion;
    private Integer configVersionNumber;

    // When this configuration becomes effective
    private LocalDateTime effectiveDate;

    private LocalDateTime expirationDate;

    // Metadata
    private Map<String, Object> customMetadata;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private String createdBy;

    private String updatedBy;

    private LocalDateTime lastSyncedWithCatalog;
}