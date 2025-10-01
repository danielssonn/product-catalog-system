package com.bank.product.workflow.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Decision table rule
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DecisionRule {
    /**
     * Unique rule identifier
     */
    private String ruleId;

    /**
     * Rule priority (higher = evaluated first)
     */
    private int priority;

    /**
     * Map of input name to condition expression
     * Examples: "== 'CHECKING'", "> 1000", ">= 80 && <= 100"
     */
    private Map<String, String> conditions;

    /**
     * Map of output name to output value
     */
    private Map<String, Object> outputs;

    /**
     * Human-readable description
     */
    private String description;
}
