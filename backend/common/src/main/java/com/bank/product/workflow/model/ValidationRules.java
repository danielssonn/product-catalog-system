package com.bank.product.workflow.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Validation rules for workflow submission
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidationRules {

    /**
     * Required metadata fields
     */
    private List<String> requiredMetadataFields;

    /**
     * Field validations (fieldName -> validation expression)
     * e.g., "amount": "> 0 && < 10000000"
     */
    private Map<String, String> fieldValidations;

    /**
     * Custom validation script
     */
    private String customValidationScript;

    /**
     * External validation service endpoint
     */
    private String externalValidationUrl;

    /**
     * Whether to fail fast on validation errors
     */
    private boolean failFast;
}
