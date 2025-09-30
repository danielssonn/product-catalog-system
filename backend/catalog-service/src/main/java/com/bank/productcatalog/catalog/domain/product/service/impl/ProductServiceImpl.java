package com.bank.productcatalog.catalog.domain.product.service.impl;

import com.bank.productcatalog.catalog.domain.product.repository.ProductRepository;
import com.bank.productcatalog.catalog.domain.product.service.ProductService;
import com.bank.productcatalog.common.domain.product.model.Product;
import com.bank.productcatalog.common.domain.product.model.ProductStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;

    @Override
    public Product getProduct(String tenantId, String productId) {
        log.debug("Fetching product {} for tenant {}", productId, tenantId);
        return productRepository.findByTenantIdAndProductId(tenantId, productId)
                .orElseThrow(() -> new RuntimeException("Product not found: " + productId));
    }

    @Override
    public Page<Product> getProducts(String tenantId, Pageable pageable) {
        log.debug("Fetching products for tenant {} with pagination", tenantId);
        return productRepository.findByTenantId(tenantId, pageable);
    }

    @Override
    public Page<Product> getProductsByStatus(String tenantId, ProductStatus status, Pageable pageable) {
        log.debug("Fetching products for tenant {} with status {}", tenantId, status);
        return productRepository.findByTenantIdAndStatus(tenantId, status, pageable);
    }

    @Override
    public Page<Product> getProductsByCategory(String tenantId, String category, Pageable pageable) {
        log.debug("Fetching products for tenant {} in category {}", tenantId, category);
        return productRepository.findByTenantIdAndCategory(tenantId, category, pageable);
    }

    @Override
    public Page<Product> getProductsByChannel(String tenantId, String channel, Pageable pageable) {
        log.debug("Fetching products for tenant {} available on channel {}", tenantId, channel);
        return productRepository.findByTenantIdAndChannel(tenantId, channel, pageable);
    }

    @Override
    public Page<Product> getProductsByCatalogProduct(String tenantId, String catalogProductId, Pageable pageable) {
        log.debug("Fetching products for tenant {} based on catalog {}", tenantId, catalogProductId);
        return productRepository.findByTenantIdAndCatalogProductId(tenantId, catalogProductId, pageable);
    }

    @Override
    @Transactional
    public Product updateProductStatus(String tenantId, String productId, ProductStatus status, String userId) {
        log.info("Updating status of product {} to {} for tenant {}", productId, status, tenantId);

        Product product = productRepository.findByTenantIdAndProductId(tenantId, productId)
                .orElseThrow(() -> new RuntimeException("Product not found: " + productId));

        product.setStatus(status);
        product.setUpdatedAt(LocalDateTime.now());
        product.setUpdatedBy(userId);

        Product updated = productRepository.save(product);
        log.info("Product {} status updated to {}", productId, status);

        return updated;
    }

    @Override
    @Transactional
    public void deleteProduct(String tenantId, String productId) {
        log.info("Deleting product {} for tenant {}", productId, tenantId);
        if (!productRepository.existsByTenantIdAndProductId(tenantId, productId)) {
            throw new RuntimeException("Product not found: " + productId);
        }
        productRepository.deleteByTenantIdAndProductId(tenantId, productId);
        log.info("Product {} deleted successfully", productId);
    }
}