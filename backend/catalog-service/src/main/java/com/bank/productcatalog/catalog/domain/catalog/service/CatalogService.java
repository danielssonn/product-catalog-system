package com.bank.productcatalog.catalog.domain.catalog.service;

import com.bank.productcatalog.common.domain.catalog.model.CatalogStatus;
import com.bank.productcatalog.common.domain.catalog.model.ProductCatalog;
import com.bank.productcatalog.common.domain.catalog.model.ProductType;
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

    Page<ProductCatalog> getCatalogProductsByType(ProductType type, Pageable pageable);

    List<ProductCatalog> getAvailableCatalogProducts();

    void deleteCatalogProduct(String catalogProductId);
}