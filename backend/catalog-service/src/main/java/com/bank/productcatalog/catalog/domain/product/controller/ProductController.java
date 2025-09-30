package com.bank.productcatalog.catalog.domain.product.controller;

import com.bank.productcatalog.catalog.domain.product.service.ProductService;
import com.bank.productcatalog.common.domain.product.model.Product;
import com.bank.productcatalog.common.domain.product.model.ProductStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Tenant Product Controller
 * Manages tenant's active product instances
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping("/{productId}")
    public ResponseEntity<Product> getProduct(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable String productId) {

        log.info("Fetching product {} for tenant {}", productId, tenantId);
        Product product = productService.getProduct(tenantId, productId);
        return ResponseEntity.ok(product);
    }

    @GetMapping
    public ResponseEntity<Page<Product>> getProducts(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "updatedAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection,
            @RequestParam(required = false) ProductStatus status,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String channel,
            @RequestParam(required = false) String catalogProductId) {

        log.info("Fetching products for tenant {} with filters", tenantId);

        Sort sort = Sort.by(Sort.Direction.fromString(sortDirection), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Product> productPage;
        if (status != null) {
            productPage = productService.getProductsByStatus(tenantId, status, pageable);
        } else if (category != null) {
            productPage = productService.getProductsByCategory(tenantId, category, pageable);
        } else if (channel != null) {
            productPage = productService.getProductsByChannel(tenantId, channel, pageable);
        } else if (catalogProductId != null) {
            productPage = productService.getProductsByCatalogProduct(tenantId, catalogProductId, pageable);
        } else {
            productPage = productService.getProducts(tenantId, pageable);
        }

        return ResponseEntity.ok(productPage);
    }

    @PatchMapping("/{productId}/status")
    public ResponseEntity<Product> updateProductStatus(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestHeader("X-User-ID") String userId,
            @PathVariable String productId,
            @RequestParam ProductStatus status) {

        log.info("Updating product {} status to {} for tenant {}", productId, status, tenantId);
        Product product = productService.updateProductStatus(tenantId, productId, status, userId);
        return ResponseEntity.ok(product);
    }

    @DeleteMapping("/{productId}")
    public ResponseEntity<Void> deleteProduct(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable String productId) {

        log.info("Deleting product {} for tenant {}", productId, tenantId);
        productService.deleteProduct(tenantId, productId);
        return ResponseEntity.noContent().build();
    }
}