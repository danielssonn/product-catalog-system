package com.bank.product.domain.catalog.service;

import com.bank.product.domain.catalog.model.ProductTypeDefinition;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Service interface for managing product type definitions
 * Enables business users to self-serve product type management
 */
public interface ProductTypeService {

    /**
     * Create a new product type definition
     * @param productType The product type to create
     * @param userId The user creating the type
     * @return The created product type
     */
    ProductTypeDefinition createProductType(ProductTypeDefinition productType, String userId);

    /**
     * Update an existing product type definition
     * @param typeCode The unique type code
     * @param productType The updated product type data
     * @param userId The user updating the type
     * @return The updated product type
     */
    ProductTypeDefinition updateProductType(String typeCode, ProductTypeDefinition productType, String userId);

    /**
     * Get a product type by its code
     * @param typeCode The unique type code
     * @return The product type definition
     */
    ProductTypeDefinition getProductType(String typeCode);

    /**
     * Get all product types with pagination
     * @param pageable Pagination parameters
     * @return Page of product types
     */
    Page<ProductTypeDefinition> getAllProductTypes(Pageable pageable);

    /**
     * Get all active product types
     * @return List of active product types
     */
    List<ProductTypeDefinition> getAllActiveProductTypes();

    /**
     * Get product types by category
     * @param category The category to filter by
     * @param pageable Pagination parameters
     * @return Page of product types in the category
     */
    Page<ProductTypeDefinition> getProductTypesByCategory(String category, Pageable pageable);

    /**
     * Get product types by category (active only)
     * @param category The category to filter by
     * @return List of active product types in the category
     */
    List<ProductTypeDefinition> getActiveProductTypesByCategory(String category);

    /**
     * Deactivate a product type (soft delete)
     * @param typeCode The unique type code
     * @param userId The user deactivating the type
     */
    void deactivateProductType(String typeCode, String userId);

    /**
     * Reactivate a deactivated product type
     * @param typeCode The unique type code
     * @param userId The user reactivating the type
     */
    void reactivateProductType(String typeCode, String userId);

    /**
     * Delete a product type permanently (hard delete)
     * Only allowed if no catalog products reference this type
     * @param typeCode The unique type code
     */
    void deleteProductType(String typeCode);

    /**
     * Check if a product type code is available (not already used)
     * @param typeCode The type code to check
     * @return true if available, false if already exists
     */
    boolean isTypeCodeAvailable(String typeCode);
}
