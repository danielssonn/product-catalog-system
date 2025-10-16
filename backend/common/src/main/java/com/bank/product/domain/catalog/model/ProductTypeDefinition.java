package com.bank.product.domain.catalog.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * Product Type Definition - Data-driven product type management
 * Replaces the hard-coded ProductType enum to enable business self-service
 * for adding new product types without code deployments.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "product_types")
public class ProductTypeDefinition {

    @Id
    private String id;

    /**
     * Unique code for the product type (e.g., "CHECKING_ACCOUNT", "ACH_TRANSFER")
     * This is stored in the product catalog documents as the 'type' field
     */
    @Indexed(unique = true)
    private String typeCode;

    /**
     * Display name for the product type
     */
    private String name;

    /**
     * Description of the product type
     */
    private String description;

    /**
     * Category this type belongs to (e.g., "ACCOUNT", "PAYMENT", "LENDING")
     */
    private String category;

    /**
     * Subcategory for finer classification
     */
    private String subcategory;

    /**
     * Whether this product type is currently active and can be used
     */
    @Builder.Default
    private boolean active = true;

    /**
     * Display order for UI listing
     */
    private Integer displayOrder;

    /**
     * Icon or symbol for UI representation
     */
    private String icon;

    /**
     * Tags for filtering and searching
     */
    private java.util.List<String> tags;

    /**
     * Audit fields
     */
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;

    /**
     * Metadata for extensibility
     */
    private java.util.Map<String, Object> metadata;
}
