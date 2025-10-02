package com.bank.product.version.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Schema Transformation Rules
 * Defines how to transform data between API versions
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SchemaTransformation {

    /**
     * Source version
     */
    private String fromVersion;

    /**
     * Target version
     */
    private String toVersion;

    /**
     * Field mappings (source field -> target field)
     */
    private Map<String, String> fieldMappings;

    /**
     * Field transformations (complex mappings with logic)
     */
    private List<FieldTransformation> fieldTransformations;

    /**
     * Fields to add with default values
     */
    private Map<String, Object> defaultValues;

    /**
     * Fields to remove
     */
    private List<String> fieldsToRemove;

    /**
     * Custom transformation script (SpEL or Groovy)
     */
    private String customScript;

    /**
     * Transformation type
     */
    private TransformationType type;
}
