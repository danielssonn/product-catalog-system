package com.bank.product.domain.catalog.service.impl;

import com.bank.product.domain.catalog.model.ProductTypeDefinition;
import com.bank.product.domain.catalog.repository.CatalogRepository;
import com.bank.product.domain.catalog.repository.ProductTypeRepository;
import com.bank.product.domain.catalog.service.ProductTypeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Service implementation for product type management
 * Provides CRUD operations with caching and validation
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductTypeServiceImpl implements ProductTypeService {

    private final ProductTypeRepository productTypeRepository;
    private final CatalogRepository catalogRepository;

    @Override
    @Transactional
    @CacheEvict(value = {"productTypes", "productTypesList", "productTypesByCategory"}, allEntries = true)
    public ProductTypeDefinition createProductType(ProductTypeDefinition productType, String userId) {
        log.info("Creating product type: {}", productType.getTypeCode());

        // Validate type code uniqueness
        if (productTypeRepository.existsByTypeCode(productType.getTypeCode())) {
            throw new IllegalArgumentException("Product type with code " + productType.getTypeCode() + " already exists");
        }

        // Validate type code format (uppercase with underscores)
        if (!productType.getTypeCode().matches("^[A-Z][A-Z0-9_]*$")) {
            throw new IllegalArgumentException("Type code must be uppercase letters, numbers, and underscores only (e.g., CHECKING_ACCOUNT)");
        }

        // Set audit fields
        productType.setId(UUID.randomUUID().toString());
        productType.setCreatedAt(LocalDateTime.now());
        productType.setUpdatedAt(LocalDateTime.now());
        productType.setCreatedBy(userId);
        productType.setUpdatedBy(userId);

        // Set default values
        if (productType.isActive() == false && productType.getCreatedBy().equals(userId)) {
            productType.setActive(true); // Default to active on creation
        }

        ProductTypeDefinition saved = productTypeRepository.save(productType);
        log.info("Product type {} created successfully by {}", saved.getTypeCode(), userId);

        return saved;
    }

    @Override
    @Transactional
    @CacheEvict(value = {"productTypes", "productTypesList", "productTypesByCategory"}, allEntries = true)
    public ProductTypeDefinition updateProductType(String typeCode, ProductTypeDefinition productType, String userId) {
        log.info("Updating product type: {}", typeCode);

        ProductTypeDefinition existing = productTypeRepository.findByTypeCode(typeCode)
                .orElseThrow(() -> new RuntimeException("Product type not found: " + typeCode));

        // Update fields (type code cannot be changed)
        existing.setName(productType.getName());
        existing.setDescription(productType.getDescription());
        existing.setCategory(productType.getCategory());
        existing.setSubcategory(productType.getSubcategory());
        existing.setActive(productType.isActive());
        existing.setDisplayOrder(productType.getDisplayOrder());
        existing.setIcon(productType.getIcon());
        existing.setTags(productType.getTags());
        existing.setMetadata(productType.getMetadata());
        existing.setUpdatedAt(LocalDateTime.now());
        existing.setUpdatedBy(userId);

        ProductTypeDefinition updated = productTypeRepository.save(existing);
        log.info("Product type {} updated successfully by {}", typeCode, userId);

        return updated;
    }

    @Override
    @Cacheable(value = "productTypes", key = "#typeCode")
    public ProductTypeDefinition getProductType(String typeCode) {
        log.debug("Fetching product type: {}", typeCode);
        return productTypeRepository.findByTypeCode(typeCode)
                .orElseThrow(() -> new RuntimeException("Product type not found: " + typeCode));
    }

    @Override
    public Page<ProductTypeDefinition> getAllProductTypes(Pageable pageable) {
        log.debug("Fetching all product types with pagination");
        return productTypeRepository.findAll(pageable);
    }

    @Override
    @Cacheable(value = "productTypesList")
    public List<ProductTypeDefinition> getAllActiveProductTypes() {
        log.debug("Fetching all active product types");
        return productTypeRepository.findByActiveTrue();
    }

    @Override
    public Page<ProductTypeDefinition> getProductTypesByCategory(String category, Pageable pageable) {
        log.debug("Fetching product types in category: {}", category);
        return productTypeRepository.findByCategory(category, pageable);
    }

    @Override
    @Cacheable(value = "productTypesByCategory", key = "#category")
    public List<ProductTypeDefinition> getActiveProductTypesByCategory(String category) {
        log.debug("Fetching active product types in category: {}", category);
        return productTypeRepository.findByCategoryAndActiveTrue(category);
    }

    @Override
    @Transactional
    @CacheEvict(value = {"productTypes", "productTypesList", "productTypesByCategory"}, allEntries = true)
    public void deactivateProductType(String typeCode, String userId) {
        log.info("Deactivating product type: {}", typeCode);

        ProductTypeDefinition productType = productTypeRepository.findByTypeCode(typeCode)
                .orElseThrow(() -> new RuntimeException("Product type not found: " + typeCode));

        productType.setActive(false);
        productType.setUpdatedAt(LocalDateTime.now());
        productType.setUpdatedBy(userId);

        productTypeRepository.save(productType);
        log.info("Product type {} deactivated by {}", typeCode, userId);
    }

    @Override
    @Transactional
    @CacheEvict(value = {"productTypes", "productTypesList", "productTypesByCategory"}, allEntries = true)
    public void reactivateProductType(String typeCode, String userId) {
        log.info("Reactivating product type: {}", typeCode);

        ProductTypeDefinition productType = productTypeRepository.findByTypeCode(typeCode)
                .orElseThrow(() -> new RuntimeException("Product type not found: " + typeCode));

        productType.setActive(true);
        productType.setUpdatedAt(LocalDateTime.now());
        productType.setUpdatedBy(userId);

        productTypeRepository.save(productType);
        log.info("Product type {} reactivated by {}", typeCode, userId);
    }

    @Override
    @Transactional
    @CacheEvict(value = {"productTypes", "productTypesList", "productTypesByCategory"}, allEntries = true)
    public void deleteProductType(String typeCode) {
        log.info("Deleting product type: {}", typeCode);

        ProductTypeDefinition productType = productTypeRepository.findByTypeCode(typeCode)
                .orElseThrow(() -> new RuntimeException("Product type not found: " + typeCode));

        // Check if any catalog products reference this type
        long catalogCount = catalogRepository.countByType(typeCode);
        if (catalogCount > 0) {
            throw new IllegalStateException(
                    "Cannot delete product type " + typeCode + ". " +
                    catalogCount + " catalog product(s) still reference this type. " +
                    "Please deactivate instead, or update/remove those products first."
            );
        }

        productTypeRepository.delete(productType);
        log.info("Product type {} deleted permanently", typeCode);
    }

    @Override
    public boolean isTypeCodeAvailable(String typeCode) {
        return !productTypeRepository.existsByTypeCode(typeCode);
    }
}
