package com.bank.product.domain.catalog.repository;

import com.bank.product.domain.catalog.model.CatalogStatus;
import com.bank.product.domain.catalog.model.ProductCatalog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CatalogRepository extends MongoRepository<ProductCatalog, String> {

    Optional<ProductCatalog> findByCatalogProductId(String catalogProductId);

    List<ProductCatalog> findByStatus(CatalogStatus status);

    Page<ProductCatalog> findByStatus(CatalogStatus status, Pageable pageable);

    Page<ProductCatalog> findByCategory(String category, Pageable pageable);

    /**
     * Find catalog products by type code
     * @param type Product type code (e.g., "CHECKING_ACCOUNT", "ACH_TRANSFER")
     */
    Page<ProductCatalog> findByType(String type, Pageable pageable);

    Page<ProductCatalog> findByProductTier(String productTier, Pageable pageable);

    boolean existsByCatalogProductId(String catalogProductId);
}