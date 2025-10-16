package com.bank.product.domain.catalog.service.impl;

import com.bank.product.domain.catalog.repository.CatalogRepository;
import com.bank.product.domain.catalog.service.CatalogService;
import com.bank.product.domain.catalog.service.ProductTypeValidator;
import com.bank.product.domain.catalog.model.CatalogStatus;
import com.bank.product.domain.catalog.model.ProductCatalog;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CatalogServiceImpl implements CatalogService {

    private final CatalogRepository catalogRepository;
    private final ProductTypeValidator productTypeValidator;

    @Override
    @Transactional
    public ProductCatalog createCatalogProduct(ProductCatalog catalog, String userId) {
        log.info("Creating catalog product {}", catalog.getCatalogProductId());

        if (catalogRepository.existsByCatalogProductId(catalog.getCatalogProductId())) {
            throw new RuntimeException("Catalog product with ID " + catalog.getCatalogProductId() + " already exists");
        }

        // Validate product type exists and is active
        if (catalog.getType() != null) {
            productTypeValidator.validateProductTypeOrThrow(catalog.getType());
        }

        catalog.setId(UUID.randomUUID().toString());
        catalog.setCreatedAt(LocalDateTime.now());
        catalog.setUpdatedAt(LocalDateTime.now());
        catalog.setCreatedBy(userId);
        catalog.setUpdatedBy(userId);

        ProductCatalog saved = catalogRepository.save(catalog);
        log.info("Catalog product {} created successfully", saved.getCatalogProductId());

        return saved;
    }

    @Override
    @Transactional
    public ProductCatalog updateCatalogProduct(String catalogProductId, ProductCatalog catalog, String userId) {
        log.info("Updating catalog product {}", catalogProductId);

        ProductCatalog existing = catalogRepository.findByCatalogProductId(catalogProductId)
                .orElseThrow(() -> new RuntimeException("Catalog product not found: " + catalogProductId));

        existing.setName(catalog.getName());
        existing.setDescription(catalog.getDescription());
        existing.setCategory(catalog.getCategory());
        existing.setType(catalog.getType());
        existing.setStatus(catalog.getStatus());
        existing.setPricingTemplate(catalog.getPricingTemplate());
        existing.setAvailableFeatures(catalog.getAvailableFeatures());
        existing.setDefaultTerms(catalog.getDefaultTerms());
        existing.setConfigOptions(catalog.getConfigOptions());
        existing.setSupportedChannels(catalog.getSupportedChannels());
        existing.setDefaultEligibilityCriteria(catalog.getDefaultEligibilityCriteria());
        existing.setComplianceTags(catalog.getComplianceTags());
        existing.setProductTier(catalog.getProductTier());
        existing.setRequiresApproval(catalog.isRequiresApproval());
        existing.setDocumentationUrl(catalog.getDocumentationUrl());
        existing.setRelatedProducts(catalog.getRelatedProducts());
        existing.setMetadata(catalog.getMetadata());
        existing.setUpdatedAt(LocalDateTime.now());
        existing.setUpdatedBy(userId);

        ProductCatalog updated = catalogRepository.save(existing);
        log.info("Catalog product {} updated successfully", catalogProductId);

        return updated;
    }

    @Override
    public ProductCatalog getCatalogProduct(String catalogProductId) {
        log.debug("Fetching catalog product {}", catalogProductId);
        return catalogRepository.findByCatalogProductId(catalogProductId)
                .orElseThrow(() -> new RuntimeException("Catalog product not found: " + catalogProductId));
    }

    @Override
    public Page<ProductCatalog> getCatalogProducts(Pageable pageable) {
        log.debug("Fetching all catalog products with pagination");
        return catalogRepository.findAll(pageable);
    }

    @Override
    public Page<ProductCatalog> getCatalogProductsByStatus(CatalogStatus status, Pageable pageable) {
        log.debug("Fetching catalog products with status {}", status);
        return catalogRepository.findByStatus(status, pageable);
    }

    @Override
    public Page<ProductCatalog> getCatalogProductsByCategory(String category, Pageable pageable) {
        log.debug("Fetching catalog products in category {}", category);
        return catalogRepository.findByCategory(category, pageable);
    }

    @Override
    public Page<ProductCatalog> getCatalogProductsByType(String typeCode, Pageable pageable) {
        log.debug("Fetching catalog products of type {}", typeCode);
        return catalogRepository.findByType(typeCode, pageable);
    }

    @Override
    public List<ProductCatalog> getAvailableCatalogProducts() {
        log.debug("Fetching available catalog products");
        return catalogRepository.findByStatus(CatalogStatus.AVAILABLE);
    }

    @Override
    @Transactional
    public void deleteCatalogProduct(String catalogProductId) {
        log.info("Deleting catalog product {}", catalogProductId);
        ProductCatalog catalog = catalogRepository.findByCatalogProductId(catalogProductId)
                .orElseThrow(() -> new RuntimeException("Catalog product not found: " + catalogProductId));
        catalogRepository.delete(catalog);
        log.info("Catalog product {} deleted successfully", catalogProductId);
    }
}