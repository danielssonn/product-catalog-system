package com.bank.product.domain.catalog.repository;

import com.bank.product.domain.catalog.model.Category;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends MongoRepository<Category, String> {

    Optional<Category> findByCategoryId(String categoryId);

    List<Category> findByActive(boolean active);

    List<Category> findByParentCategoryId(String parentCategoryId);

    boolean existsByCategoryId(String categoryId);
}