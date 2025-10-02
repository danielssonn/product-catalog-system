package com.bank.product.version.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Field-level transformation rule
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FieldTransformation {

    /**
     * Source field path (supports nested: "customer.address.city")
     */
    private String sourceField;

    /**
     * Target field path
     */
    private String targetField;

    /**
     * Transformation function (e.g., "toLowerCase", "toUpperCase", "format")
     */
    private String transformFunction;

    /**
     * Function parameters
     */
    private String functionParams;

    /**
     * Conditional transformation (SpEL expression)
     */
    private String condition;

    /**
     * Default value if source is null
     */
    private Object defaultValue;
}
