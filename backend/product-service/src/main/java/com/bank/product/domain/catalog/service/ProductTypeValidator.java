package com.bank.product.domain.catalog.service;

import com.bank.product.domain.catalog.model.ProductTypeDefinition;
import com.bank.product.domain.catalog.repository.ProductTypeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Service for validating product types against the database
 * Provides caching for performance
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductTypeValidator {

    private final ProductTypeRepository productTypeRepository;

    /**
     * Validate that a product type code exists and is active
     * @param typeCode The product type code to validate
     * @return true if valid and active, false otherwise
     */
    @Cacheable(value = "productTypes", key = "#typeCode")
    public boolean isValidProductType(String typeCode) {
        if (typeCode == null || typeCode.trim().isEmpty()) {
            return false;
        }

        Optional<ProductTypeDefinition> productType = productTypeRepository.findByTypeCode(typeCode);
        return productType.isPresent() && productType.get().isActive();
    }

    /**
     * Get product type definition by code
     */
    @Cacheable(value = "productTypes", key = "#typeCode")
    public Optional<ProductTypeDefinition> getProductType(String typeCode) {
        return productTypeRepository.findByTypeCode(typeCode);
    }

    /**
     * Get all active product types
     */
    @Cacheable(value = "productTypesList")
    public List<ProductTypeDefinition> getAllActiveProductTypes() {
        return productTypeRepository.findByActiveTrue();
    }

    /**
     * Validate product type and throw exception if invalid
     * @param typeCode The product type code to validate
     * @throws IllegalArgumentException if the product type is invalid
     */
    public void validateProductTypeOrThrow(String typeCode) {
        if (!isValidProductType(typeCode)) {
            throw new IllegalArgumentException(
                "Invalid product type: " + typeCode + ". " +
                "Product type must exist in the system and be active. " +
                "Please check available product types or contact administrator."
            );
        }
    }

    /**
     * Get product types by category
     */
    @Cacheable(value = "productTypesByCategory", key = "#category")
    public List<ProductTypeDefinition> getProductTypesByCategory(String category) {
        return productTypeRepository.findByCategoryAndActiveTrue(category);
    }
}
