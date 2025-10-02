package com.bank.product.workflow.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Decision table output definition
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DecisionOutput {
    /**
     * Output field name
     */
    private String name;

    /**
     * Data type: string, number, boolean, array, object
     */
    private String type;

    /**
     * Default value if no rule matches
     */
    private Object defaultValue;

    /**
     * Human-readable description
     */
    private String description;
}
