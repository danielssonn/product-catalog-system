package com.bank.product.domain.catalog.service;

import com.bank.product.domain.catalog.model.CatalogStatus;
import com.bank.product.domain.catalog.model.ProductCatalog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CatalogService {

    ProductCatalog createCatalogProduct(ProductCatalog catalog, String userId);

    ProductCatalog updateCatalogProduct(String catalogProductId, ProductCatalog catalog, String userId);

    ProductCatalog getCatalogProduct(String catalogProductId);

    Page<ProductCatalog> getCatalogProducts(Pageable pageable);

    Page<ProductCatalog> getCatalogProductsByStatus(CatalogStatus status, Pageable pageable);

    Page<ProductCatalog> getCatalogProductsByCategory(String category, Pageable pageable);

    /**
     * Get catalog products by type code
     * @param typeCode Product type code (e.g., "CHECKING_ACCOUNT", "ACH_TRANSFER")
     */
    Page<ProductCatalog> getCatalogProductsByType(String typeCode, Pageable pageable);

    List<ProductCatalog> getAvailableCatalogProducts();

    void deleteCatalogProduct(String catalogProductId);
}