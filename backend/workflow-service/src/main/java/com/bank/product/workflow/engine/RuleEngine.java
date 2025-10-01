package com.bank.product.workflow.engine;

import com.bank.product.workflow.domain.model.ComputedApprovalPlan;
import com.bank.product.workflow.domain.model.WorkflowTemplate;

import java.util.Map;

/**
 * Rule engine interface for evaluating workflow decision tables
 */
public interface RuleEngine {

    /**
     * Evaluate decision tables against entity metadata to compute approval plan
     *
     * @param template workflow template with decision tables
     * @param entityMetadata metadata to evaluate against
     * @return computed approval plan
     */
    ComputedApprovalPlan evaluate(WorkflowTemplate template, Map<String, Object> entityMetadata);

    /**
     * Test evaluation without full template (for template testing)
     *
     * @param template workflow template
     * @param entityMetadata test metadata
     * @return evaluation result
     */
    Map<String, Object> testEvaluate(WorkflowTemplate template, Map<String, Object> entityMetadata);
}
