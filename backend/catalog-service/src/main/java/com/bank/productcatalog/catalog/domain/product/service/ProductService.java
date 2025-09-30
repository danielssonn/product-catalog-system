package com.bank.productcatalog.catalog.domain.product.service;

import com.bank.productcatalog.common.domain.product.model.Product;
import com.bank.productcatalog.common.domain.product.model.ProductStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProductService {

    Product getProduct(String tenantId, String productId);

    Page<Product> getProducts(String tenantId, Pageable pageable);

    Page<Product> getProductsByStatus(String tenantId, ProductStatus status, Pageable pageable);

    Page<Product> getProductsByCategory(String tenantId, String category, Pageable pageable);

    Page<Product> getProductsByChannel(String tenantId, String channel, Pageable pageable);

    Page<Product> getProductsByCatalogProduct(String tenantId, String catalogProductId, Pageable pageable);

    Product updateProductStatus(String tenantId, String productId, ProductStatus status, String userId);

    void deleteProduct(String tenantId, String productId);
}