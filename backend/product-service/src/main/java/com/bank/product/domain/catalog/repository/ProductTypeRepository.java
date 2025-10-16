package com.bank.product.domain.catalog.repository;

import com.bank.product.domain.catalog.model.ProductTypeDefinition;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for managing product type definitions
 */
@Repository
public interface ProductTypeRepository extends MongoRepository<ProductTypeDefinition, String> {

    /**
     * Find product type by its unique code
     */
    Optional<ProductTypeDefinition> findByTypeCode(String typeCode);

    /**
     * Check if a product type code exists
     */
    boolean existsByTypeCode(String typeCode);

    /**
     * Find all active product types
     */
    List<ProductTypeDefinition> findByActiveTrue();

    /**
     * Find product types by category
     */
    List<ProductTypeDefinition> findByCategoryAndActiveTrue(String category);

    /**
     * Find product types by category and subcategory
     */
    List<ProductTypeDefinition> findByCategoryAndSubcategoryAndActiveTrue(String category, String subcategory);
}
