package com.bank.productcatalog.common.domain.product.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Tenant's Product Instance - An active product offering for a specific tenant
 * This is derived from TenantProductConfiguration after activation
 */
@Data
@Document(collection = "products")
@CompoundIndex(name = "tenant_product_idx", def = "{'tenantId': 1, 'productId': 1}")
@CompoundIndex(name = "tenant_status_idx", def = "{'tenantId': 1, 'status': 1}")
@CompoundIndex(name = "tenant_catalog_idx", def = "{'tenantId': 1, 'catalogProductId': 1}")
public class Product {

    @Id
    private String id;

    @Indexed
    private String tenantId;

    @Indexed
    private String productId;

    // Reference to master catalog product
    @Indexed
    private String catalogProductId;

    // Reference to tenant configuration
    @Indexed
    private String configurationId;

    private String name;

    private String description;

    private String category;

    private ProductStatus status;

    private PricingDetails pricing;

    private List<String> availableChannels;

    private Map<String, Object> features;

    private List<String> eligibilityCriteria;

    private ProductTerms terms;

    // Version tracking
    private String version;

    private Integer versionNumber;

    private LocalDateTime effectiveDate;

    private LocalDateTime expirationDate;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private String createdBy;

    private String updatedBy;

    private Map<String, Object> metadata;
}