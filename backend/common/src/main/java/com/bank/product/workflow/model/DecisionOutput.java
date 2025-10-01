package com.bank.product.workflow.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Output column for decision table
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DecisionOutput {

    /**
     * Output name
     */
    private String name;

    /**
     * Data type
     */
    private String type; // "string", "number", "boolean", "array", "object"

    /**
     * Label for display
     */
    private String label;

    /**
     * Default value if not specified in rule
     */
    private Object defaultValue;

    /**
     * Description
     */
    private String description;
}
