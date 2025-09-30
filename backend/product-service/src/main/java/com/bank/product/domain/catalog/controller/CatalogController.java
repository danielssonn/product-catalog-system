package com.bank.product.domain.catalog.controller;

import com.bank.product.domain.catalog.service.CatalogService;
import com.bank.product.domain.catalog.model.CatalogStatus;
import com.bank.product.domain.catalog.model.ProductCatalog;
import com.bank.product.domain.catalog.model.ProductType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

/**
 * Master Product Catalog Controller
 * Manages the master catalog of products available for tenant selection
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/catalog")
@RequiredArgsConstructor
public class CatalogController {

    private final CatalogService catalogService;

    @PostMapping
    public ResponseEntity<ProductCatalog> createCatalogProduct(
            @RequestHeader("X-User-ID") String userId,
            @Valid @RequestBody ProductCatalog catalog) {

        log.info("Creating catalog product {}", catalog.getCatalogProductId());
        ProductCatalog created = catalogService.createCatalogProduct(catalog, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{catalogProductId}")
    public ResponseEntity<ProductCatalog> updateCatalogProduct(
            @RequestHeader("X-User-ID") String userId,
            @PathVariable String catalogProductId,
            @Valid @RequestBody ProductCatalog catalog) {

        log.info("Updating catalog product {}", catalogProductId);
        ProductCatalog updated = catalogService.updateCatalogProduct(catalogProductId, catalog, userId);
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/{catalogProductId}")
    public ResponseEntity<ProductCatalog> getCatalogProduct(@PathVariable String catalogProductId) {
        log.info("Fetching catalog product {}", catalogProductId);
        ProductCatalog catalog = catalogService.getCatalogProduct(catalogProductId);
        return ResponseEntity.ok(catalog);
    }

    @GetMapping
    public ResponseEntity<Page<ProductCatalog>> getCatalogProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "ASC") String sortDirection,
            @RequestParam(required = false) CatalogStatus status,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) ProductType type) {

        log.info("Fetching catalog products with filters");

        Sort sort = Sort.by(Sort.Direction.fromString(sortDirection), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<ProductCatalog> catalogPage;
        if (status != null) {
            catalogPage = catalogService.getCatalogProductsByStatus(status, pageable);
        } else if (category != null) {
            catalogPage = catalogService.getCatalogProductsByCategory(category, pageable);
        } else if (type != null) {
            catalogPage = catalogService.getCatalogProductsByType(type, pageable);
        } else {
            catalogPage = catalogService.getCatalogProducts(pageable);
        }

        return ResponseEntity.ok(catalogPage);
    }

    @GetMapping("/available")
    public ResponseEntity<List<ProductCatalog>> getAvailableCatalogProducts() {
        log.info("Fetching available catalog products");
        List<ProductCatalog> available = catalogService.getAvailableCatalogProducts();
        return ResponseEntity.ok(available);
    }

    @DeleteMapping("/{catalogProductId}")
    public ResponseEntity<Void> deleteCatalogProduct(@PathVariable String catalogProductId) {
        log.info("Deleting catalog product {}", catalogProductId);
        catalogService.deleteCatalogProduct(catalogProductId);
        return ResponseEntity.noContent().build();
    }
}