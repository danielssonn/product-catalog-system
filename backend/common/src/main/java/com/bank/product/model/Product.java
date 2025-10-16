package com.bank.product.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Document(collection = "products")
@CompoundIndex(name = "tenant_product_idx", def = "{'tenantId': 1, 'productId': 1}")
@CompoundIndex(name = "tenant_status_idx", def = "{'tenantId': 1, 'status': 1}")
public class Product {

    @Id
    private String id;

    @Indexed
    private String tenantId;

    @Indexed
    private String productId;

    // Reference to master catalog product (if product is based on catalog)
    @Indexed
    private String catalogProductId;

    // Reference to tenant configuration (if product is from tenant config)
    @Indexed
    private String tenantConfigurationId;

    private String name;

    private String description;

    private String category;

    /**
     * Product type code - references ProductTypeDefinition.typeCode
     * Changed from enum to String for data-driven product type management
     */
    private String type;

    private ProductStatus status;

    private PricingDetails pricing;

    private List<String> availableChannels;

    private Map<String, Object> features;

    private List<String> eligibilityCriteria;

    private Terms terms;

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