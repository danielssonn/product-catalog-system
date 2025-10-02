package com.bank.product.workflow.domain.service;

import com.bank.product.workflow.domain.model.ComputedApprovalPlan;
import com.bank.product.workflow.domain.model.WorkflowTemplate;
import com.bank.product.workflow.engine.RuleEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Service for evaluating workflow rules
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RuleEvaluationService {

    private final RuleEngine ruleEngine;
    private final WorkflowTemplateService templateService;

    /**
     * Evaluate rules for an entity to compute approval plan
     *
     * @param entityType type of entity
     * @param entityMetadata metadata to evaluate
     * @return computed approval plan
     */
    public ComputedApprovalPlan evaluateForEntity(String entityType, Map<String, Object> entityMetadata) {
        log.debug("Evaluating rules for entity type: {}", entityType);

        // Get active template for entity type
        WorkflowTemplate template = templateService.getActiveTemplateForEntityType(entityType)
                .orElseThrow(() -> new IllegalStateException(
                        "No active workflow template found for entity type: " + entityType));

        return ruleEngine.evaluate(template, entityMetadata);
    }

    /**
     * Evaluate rules using a specific template
     *
     * @param templateId template to use
     * @param entityMetadata metadata to evaluate
     * @return computed approval plan
     */
    public ComputedApprovalPlan evaluateWithTemplate(String templateId, Map<String, Object> entityMetadata) {
        log.debug("Evaluating rules with template: {}", templateId);

        WorkflowTemplate template = templateService.getTemplateByTemplateId(templateId);
        return ruleEngine.evaluate(template, entityMetadata);
    }

    /**
     * Test template evaluation (for template testing)
     *
     * @param templateId template to test
     * @param entityMetadata test metadata
     * @return evaluation results
     */
    public Map<String, Object> testTemplateEvaluation(String templateId, Map<String, Object> entityMetadata) {
        log.debug("Test evaluating template: {}", templateId);

        WorkflowTemplate template = templateService.getTemplateByTemplateId(templateId);
        return ruleEngine.testEvaluate(template, entityMetadata);
    }
}
