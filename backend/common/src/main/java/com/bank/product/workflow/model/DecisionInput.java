package com.bank.product.workflow.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Input column for decision table
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DecisionInput {

    /**
     * Input name (must match entityMetadata key)
     */
    private String name;

    /**
     * Data type
     */
    private String type; // "string", "number", "boolean", "date"

    /**
     * Label for display
     */
    private String label;

    /**
     * Expression to extract value (JSONPath or SpEL)
     * e.g., "$.entityMetadata.riskScore" or "#root.entityMetadata.amount"
     */
    private String expression;

    /**
     * Allowed values (for validation)
     */
    private String[] allowedValues;

    /**
     * Whether this input is required
     */
    private boolean required;
}
