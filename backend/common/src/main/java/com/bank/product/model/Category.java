package com.bank.product.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Document(collection = "categories")
@CompoundIndex(name = "tenant_category_idx", def = "{'tenantId': 1, 'categoryId': 1}")
public class Category {

    @Id
    private String id;

    @Indexed
    private String tenantId;

    @Indexed
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