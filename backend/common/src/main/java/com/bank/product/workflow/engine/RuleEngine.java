package com.bank.product.workflow.engine;

import com.bank.product.workflow.model.ComputedApprovalPlan;
import com.bank.product.workflow.model.WorkflowTemplate;

import java.util.Map;

/**
 * Rule engine interface for evaluating workflow templates
 */
public interface RuleEngine {

    /**
     * Evaluate workflow template against entity metadata
     * @param template Workflow template with decision tables
     * @param entityMetadata Metadata extracted from entity
     * @return Computed approval plan
     */
    ComputedApprovalPlan evaluate(
        WorkflowTemplate template,
        Map<String, Object> entityMetadata
    ) throws RuleEvaluationException;

    /**
     * Test template evaluation without persistence
     * @param template Template to test
     * @param entityMetadata Test data
     * @return Computed plan for testing
     */
    ComputedApprovalPlan test(
        WorkflowTemplate template,
        Map<String, Object> entityMetadata
    ) throws RuleEvaluationException;

    /**
     * Validate template syntax and structure
     * @param template Template to validate
     * @throws RuleEvaluationException if template is invalid
     */
    void validate(WorkflowTemplate template) throws RuleEvaluationException;

    /**
     * Get engine type
     */
    String getEngineType();
}
