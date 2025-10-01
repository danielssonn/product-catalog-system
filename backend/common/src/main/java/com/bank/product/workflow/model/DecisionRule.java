package com.bank.product.workflow.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Individual rule in a decision table
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DecisionRule {

    /**
     * Unique rule ID
     */
    private String ruleId;

    /**
     * Rule priority (higher = more important)
     */
    private int priority;

    /**
     * Conditions map (inputName -> condition expression)
     * Examples:
     *   "riskScore": ">= 80"
     *   "documentType": "PASSPORT"
     *   "amount": "> 1000000"
     *   "customerType": "BUSINESS|ENTERPRISE"  (OR condition)
     */
    private Map<String, String> conditions;

    /**
     * Output values (outputName -> value)
     */
    private Map<String, Object> outputs;

    /**
     * Rule description
     */
    private String description;

    /**
     * Whether this rule is active
     */
    private boolean active;

    /**
     * Effective date range
     */
    private String effectiveFrom;
    private String effectiveTo;
}
