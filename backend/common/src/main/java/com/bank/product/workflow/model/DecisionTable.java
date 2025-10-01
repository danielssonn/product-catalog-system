package com.bank.product.workflow.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Decision table for rule-based approval routing
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DecisionTable {

    /**
     * Table name
     */
    private String name;

    /**
     * Description
     */
    private String description;

    /**
     * Input columns (conditions)
     */
    private List<DecisionInput> inputs;

    /**
     * Output columns (results)
     */
    private List<DecisionOutput> outputs;

    /**
     * Decision rules
     */
    private List<DecisionRule> rules;

    /**
     * Default rule if no matches
     */
    private String defaultRuleId;

    /**
     * Hit policy (how to handle multiple matching rules)
     */
    private HitPolicy hitPolicy;

    /**
     * Rule evaluation order
     */
    private RuleOrder ruleOrder;
}
