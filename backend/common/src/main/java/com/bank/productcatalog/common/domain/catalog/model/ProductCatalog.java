package com.bank.productcatalog.common.domain.catalog.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Master Product Catalog - Template products available for tenant selection
 * This is the bank's master catalog of product offerings
 */
@Data
@Document(collection = "product_catalog")
public class ProductCatalog {

    @Id
    private String id;

    @Indexed(unique = true)
    private String catalogProductId;

    private String name;

    private String description;

    private String category;

    private ProductType type;

    private CatalogStatus status;

    // Default/template pricing that tenants can customize
    private PricingTemplate pricingTemplate;

    // Features available in this product
    private Map<String, Object> availableFeatures;

    // Default terms that can be customized
    private CatalogTerms defaultTerms;

    // Configuration options that tenants can customize
    private ProductConfigOptions configOptions;

    // Supported channels for this product
    private List<String> supportedChannels;

    // Eligibility criteria template
    private List<String> defaultEligibilityCriteria;

    // Regulatory compliance tags
    private List<String> complianceTags;

    // Product tier (Basic, Premium, Enterprise, etc.)
    private String productTier;

    private boolean requiresApproval;

    private String documentationUrl;

    private List<String> relatedProducts;

    private Map<String, Object> metadata;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private String createdBy;

    private String updatedBy;
}