package com.bank.product.workflow.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Decision table input definition
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DecisionInput {
    /**
     * Input field name
     */
    private String name;

    /**
     * Data type: string, number, boolean, date
     */
    private String type;

    /**
     * Optional JSONPath or SpEL expression to extract value
     */
    private String expression;

    /**
     * Human-readable description
     */
    private String description;
}
