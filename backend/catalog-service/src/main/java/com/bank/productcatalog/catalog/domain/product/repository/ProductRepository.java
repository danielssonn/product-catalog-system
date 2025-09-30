package com.bank.productcatalog.catalog.domain.product.repository;

import com.bank.productcatalog.common.domain.product.model.Product;
import com.bank.productcatalog.common.domain.product.model.ProductStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends MongoRepository<Product, String> {

    Optional<Product> findByTenantIdAndProductId(String tenantId, String productId);

    List<Product> findByTenantIdAndStatus(String tenantId, ProductStatus status);

    Page<Product> findByTenantId(String tenantId, Pageable pageable);

    Page<Product> findByTenantIdAndStatus(String tenantId, ProductStatus status, Pageable pageable);

    Page<Product> findByTenantIdAndCategory(String tenantId, String category, Pageable pageable);

    Page<Product> findByTenantIdAndCatalogProductId(String tenantId, String catalogProductId, Pageable pageable);

    @Query("{ 'tenantId': ?0, 'availableChannels': { $in: [?1] } }")
    Page<Product> findByTenantIdAndChannel(String tenantId, String channel, Pageable pageable);

    boolean existsByTenantIdAndProductId(String tenantId, String productId);

    void deleteByTenantIdAndProductId(String tenantId, String productId);
}