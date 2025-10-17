package com.bank.product.domain.catalog.controller;

import com.bank.product.domain.catalog.model.ProductTypeDefinition;
import com.bank.product.domain.catalog.service.ProductTypeService;
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
 * Admin API for Product Type Management
 * Enables business users to self-serve product type definitions
 *
 * All endpoints require ROLE_ADMIN
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/product-types")
@RequiredArgsConstructor
public class AdminProductTypeController {

    private final ProductTypeService productTypeService;

    /**
     * Create a new product type definition
     * POST /api/v1/admin/product-types
     */
    @PostMapping
    public ResponseEntity<ProductTypeDefinition> createProductType(
            @RequestBody ProductTypeDefinition productType,
            Principal principal) {

        log.info("Admin API: Creating product type {} by user {}",
                productType.getTypeCode(), principal.getName());

        ProductTypeDefinition created = productTypeService.createProductType(productType, principal.getName());

        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Update an existing product type
     * PUT /api/v1/admin/product-types/{typeCode}
     */
    @PutMapping("/{typeCode}")
    public ResponseEntity<ProductTypeDefinition> updateProductType(
            @PathVariable String typeCode,
            @RequestBody ProductTypeDefinition productType,
            Principal principal) {

        log.info("Admin API: Updating product type {} by user {}", typeCode, principal.getName());

        ProductTypeDefinition updated = productTypeService.updateProductType(typeCode, productType, principal.getName());

        return ResponseEntity.ok(updated);
    }

    /**
     * Get a specific product type by code
     * GET /api/v1/admin/product-types/{typeCode}
     */
    @GetMapping("/{typeCode}")
    public ResponseEntity<ProductTypeDefinition> getProductType(@PathVariable String typeCode) {
        log.debug("Admin API: Fetching product type {}", typeCode);

        ProductTypeDefinition productType = productTypeService.getProductType(typeCode);

        return ResponseEntity.ok(productType);
    }

    /**
     * Get all product types (with pagination)
     * GET /api/v1/admin/product-types?page=0&size=20&sort=displayOrder,asc
     */
    @GetMapping
    public ResponseEntity<Page<ProductTypeDefinition>> getAllProductTypes(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "displayOrder") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {

        log.debug("Admin API: Fetching all product types (page={}, size={})", page, size);

        Sort.Direction direction = sortDir.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

        Page<ProductTypeDefinition> productTypes = productTypeService.getAllProductTypes(pageable);

        return ResponseEntity.ok(productTypes);
    }

    /**
     * Get all active product types (no pagination)
     * GET /api/v1/admin/product-types/active
     */
    @GetMapping("/active")
    public ResponseEntity<List<ProductTypeDefinition>> getAllActiveProductTypes() {
        log.debug("Admin API: Fetching all active product types");

        List<ProductTypeDefinition> productTypes = productTypeService.getAllActiveProductTypes();

        return ResponseEntity.ok(productTypes);
    }

    /**
     * Get product types by category
     * GET /api/v1/admin/product-types/by-category/{category}?page=0&size=20
     */
    @GetMapping("/by-category/{category}")
    public ResponseEntity<Page<ProductTypeDefinition>> getProductTypesByCategory(
            @PathVariable String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        log.debug("Admin API: Fetching product types in category {}", category);

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "displayOrder"));
        Page<ProductTypeDefinition> productTypes = productTypeService.getProductTypesByCategory(category, pageable);

        return ResponseEntity.ok(productTypes);
    }

    /**
     * Get active product types by category (no pagination)
     * GET /api/v1/admin/product-types/active/by-category/{category}
     */
    @GetMapping("/active/by-category/{category}")
    public ResponseEntity<List<ProductTypeDefinition>> getActiveProductTypesByCategory(
            @PathVariable String category) {

        log.debug("Admin API: Fetching active product types in category {}", category);

        List<ProductTypeDefinition> productTypes = productTypeService.getActiveProductTypesByCategory(category);

        return ResponseEntity.ok(productTypes);
    }

    /**
     * Deactivate a product type (soft delete)
     * PATCH /api/v1/admin/product-types/{typeCode}/deactivate
     */
    @PatchMapping("/{typeCode}/deactivate")
    public ResponseEntity<Map<String, String>> deactivateProductType(
            @PathVariable String typeCode,
            Principal principal) {

        log.info("Admin API: Deactivating product type {} by user {}", typeCode, principal.getName());

        productTypeService.deactivateProductType(typeCode, principal.getName());

        return ResponseEntity.ok(Map.of(
                "message", "Product type deactivated successfully",
                "typeCode", typeCode,
                "deactivatedBy", principal.getName()
        ));
    }

    /**
     * Reactivate a product type
     * PATCH /api/v1/admin/product-types/{typeCode}/reactivate
     */
    @PatchMapping("/{typeCode}/reactivate")
    public ResponseEntity<Map<String, String>> reactivateProductType(
            @PathVariable String typeCode,
            Principal principal) {

        log.info("Admin API: Reactivating product type {} by user {}", typeCode, principal.getName());

        productTypeService.reactivateProductType(typeCode, principal.getName());

        return ResponseEntity.ok(Map.of(
                "message", "Product type reactivated successfully",
                "typeCode", typeCode,
                "reactivatedBy", principal.getName()
        ));
    }

    /**
     * Delete a product type permanently (hard delete)
     * DELETE /api/v1/admin/product-types/{typeCode}
     *
     * Only allowed if no catalog products reference this type
     */
    @DeleteMapping("/{typeCode}")
    public ResponseEntity<Map<String, String>> deleteProductType(
            @PathVariable String typeCode,
            Principal principal) {

        log.warn("Admin API: Attempting to permanently delete product type {} by user {}",
                typeCode, principal.getName());

        productTypeService.deleteProductType(typeCode);

        return ResponseEntity.ok(Map.of(
                "message", "Product type deleted permanently",
                "typeCode", typeCode,
                "deletedBy", principal.getName()
        ));
    }

    /**
     * Check if a type code is available
     * GET /api/v1/admin/product-types/check-availability/{typeCode}
     */
    @GetMapping("/check-availability/{typeCode}")
    public ResponseEntity<Map<String, Object>> checkTypeCodeAvailability(@PathVariable String typeCode) {
        log.debug("Admin API: Checking availability of type code {}", typeCode);

        boolean available = productTypeService.isTypeCodeAvailable(typeCode);

        return ResponseEntity.ok(Map.of(
                "typeCode", typeCode,
                "available", available,
                "message", available ? "Type code is available" : "Type code already exists"
        ));
    }

    /**
     * Exception handler for this controller
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException ex) {
        log.error("Validation error: {}", ex.getMessage());
        return ResponseEntity.badRequest().body(Map.of(
                "error", "Validation Error",
                "message", ex.getMessage()
        ));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleIllegalState(IllegalStateException ex) {
        log.error("Operation not allowed: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                "error", "Operation Not Allowed",
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
