package com.bank.productcatalog.common.domain.catalog.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Product category in the master catalog
 */
@Data
@Document(collection = "catalog_categories")
public class Category {

    @Id
    private String id;

    @Indexed(unique = true)
    private String categoryId;

    private String name;

    private String description;

    private String parentCategoryId;

    private List<String> subCategories;

    private Integer displayOrder;

    private boolean active;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}