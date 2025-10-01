package com.bank.product.workflow.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Decision table for rule-based workflow routing
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DecisionTable {
    /**
     * Decision table name
     */
    private String name;

    /**
     * Input definitions
     */
    private List<DecisionInput> inputs;

    /**
     * Output definitions
     */
    private List<DecisionOutput> outputs;

    /**
     * Decision rules
     */
    private List<DecisionRule> rules;

    /**
     * ID of rule to apply if no rules match
     */
    private String defaultRuleId;

    /**
     * Hit policy for rule evaluation
     */
    private HitPolicy hitPolicy;

    /**
     * Human-readable description
     */
    private String description;
}
