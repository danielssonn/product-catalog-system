package com.bank.product.domain.catalog.controller;

import com.bank.product.domain.catalog.model.CatalogStatus;
import com.bank.product.domain.catalog.model.ProductCatalog;
import com.bank.product.domain.catalog.service.CatalogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

/**
 * Admin API for Product Catalog Management
 * Enables business users to seed and manage product catalog entries
 *
 * All endpoints require ROLE_ADMIN
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/catalog")
@RequiredArgsConstructor
public class AdminCatalogController {

    private final CatalogService catalogService;

    /**
     * Create a new catalog product (seed product)
     * POST /api/v1/admin/catalog
     */
    @PostMapping
    public ResponseEntity<ProductCatalog> createCatalogProduct(
            @RequestBody ProductCatalog catalog,
            Principal principal) {

        log.info("Admin API: Creating catalog product {} by user {}",
                catalog.getCatalogProductId(), principal.getName());

        ProductCatalog created = catalogService.createCatalogProduct(catalog, principal.getName());

        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Update an existing catalog product
     * PUT /api/v1/admin/catalog/{catalogProductId}
     */
    @PutMapping("/{catalogProductId}")
    public ResponseEntity<ProductCatalog> updateCatalogProduct(
            @PathVariable String catalogProductId,
            @RequestBody ProductCatalog catalog,
            Principal principal) {

        log.info("Admin API: Updating catalog product {} by user {}",
                catalogProductId, principal.getName());

        ProductCatalog updated = catalogService.updateCatalogProduct(catalogProductId, catalog, principal.getName());

        return ResponseEntity.ok(updated);
    }

    /**
     * Get a specific catalog product
     * GET /api/v1/admin/catalog/{catalogProductId}
     */
    @GetMapping("/{catalogProductId}")
    public ResponseEntity<ProductCatalog> getCatalogProduct(@PathVariable String catalogProductId) {
        log.debug("Admin API: Fetching catalog product {}", catalogProductId);

        ProductCatalog catalog = catalogService.getCatalogProduct(catalogProductId);

        return ResponseEntity.ok(catalog);
    }

    /**
     * Get all catalog products (with pagination)
     * GET /api/v1/admin/catalog?page=0&size=20&sort=createdAt,desc
     */
    @GetMapping
    public ResponseEntity<Page<ProductCatalog>> getAllCatalogProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        log.debug("Admin API: Fetching all catalog products (page={}, size={})", page, size);

        Sort.Direction direction = sortDir.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

        Page<ProductCatalog> catalogs = catalogService.getCatalogProducts(pageable);

        return ResponseEntity.ok(catalogs);
    }

    /**
     * Get catalog products by status
     * GET /api/v1/admin/catalog/by-status/{status}?page=0&size=20
     */
    @GetMapping("/by-status/{status}")
    public ResponseEntity<Page<ProductCatalog>> getCatalogProductsByStatus(
            @PathVariable CatalogStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        log.debug("Admin API: Fetching catalog products with status {}", status);

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<ProductCatalog> catalogs = catalogService.getCatalogProductsByStatus(status, pageable);

        return ResponseEntity.ok(catalogs);
    }

    /**
     * Get available catalog products (no pagination)
     * GET /api/v1/admin/catalog/available
     */
    @GetMapping("/available")
    public ResponseEntity<List<ProductCatalog>> getAvailableCatalogProducts() {
        log.debug("Admin API: Fetching available catalog products");

        List<ProductCatalog> catalogs = catalogService.getAvailableCatalogProducts();

        return ResponseEntity.ok(catalogs);
    }

    /**
     * Get catalog products by category
     * GET /api/v1/admin/catalog/by-category/{category}?page=0&size=20
     */
    @GetMapping("/by-category/{category}")
    public ResponseEntity<Page<ProductCatalog>> getCatalogProductsByCategory(
            @PathVariable String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        log.debug("Admin API: Fetching catalog products in category {}", category);

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "name"));
        Page<ProductCatalog> catalogs = catalogService.getCatalogProductsByCategory(category, pageable);

        return ResponseEntity.ok(catalogs);
    }

    /**
     * Get catalog products by type
     * GET /api/v1/admin/catalog/by-type/{typeCode}?page=0&size=20
     */
    @GetMapping("/by-type/{typeCode}")
    public ResponseEntity<Page<ProductCatalog>> getCatalogProductsByType(
            @PathVariable String typeCode,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        log.debug("Admin API: Fetching catalog products of type {}", typeCode);

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "name"));
        Page<ProductCatalog> catalogs = catalogService.getCatalogProductsByType(typeCode, pageable);

        return ResponseEntity.ok(catalogs);
    }

    /**
     * Delete a catalog product
     * DELETE /api/v1/admin/catalog/{catalogProductId}
     */
    @DeleteMapping("/{catalogProductId}")
    public ResponseEntity<Map<String, String>> deleteCatalogProduct(
            @PathVariable String catalogProductId,
            Principal principal) {

        log.warn("Admin API: Deleting catalog product {} by user {}",
                catalogProductId, principal.getName());

        catalogService.deleteCatalogProduct(catalogProductId);

        return ResponseEntity.ok(Map.of(
                "message", "Catalog product deleted successfully",
                "catalogProductId", catalogProductId,
                "deletedBy", principal.getName()
        ));
    }

    /**
     * Bulk create catalog products (for initial seeding)
     * POST /api/v1/admin/catalog/bulk
     */
    @PostMapping("/bulk")
    public ResponseEntity<Map<String, Object>> bulkCreateCatalogProducts(
            @RequestBody List<ProductCatalog> catalogs,
            Principal principal) {

        log.info("Admin API: Bulk creating {} catalog products by user {}",
                catalogs.size(), principal.getName());

        int successCount = 0;
        int failureCount = 0;
        List<String> errors = new java.util.ArrayList<>();

        for (ProductCatalog catalog : catalogs) {
            try {
                catalogService.createCatalogProduct(catalog, principal.getName());
                successCount++;
            } catch (Exception e) {
                failureCount++;
                errors.add(String.format("Failed to create %s: %s",
                        catalog.getCatalogProductId(), e.getMessage()));
                log.error("Failed to create catalog product {}: {}",
                        catalog.getCatalogProductId(), e.getMessage());
            }
        }

        log.info("Bulk create completed: {} succeeded, {} failed", successCount, failureCount);

        return ResponseEntity.ok(Map.of(
                "totalSubmitted", catalogs.size(),
                "successCount", successCount,
                "failureCount", failureCount,
                "errors", errors
        ));
    }

    /**
     * Exception handlers
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException ex) {
        log.error("Validation error: {}", ex.getMessage());
        return ResponseEntity.badRequest().body(Map.of(
                "error", "Validation Error",
                "message", ex.getMessage()
        ));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleRuntimeException(RuntimeException ex) {
        log.error("Error processing request: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "error", "Internal Server Error",
                "message", ex.getMessage()
        ));
    }
}
