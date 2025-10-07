package com.bank.product.workflow.temporal.activity;

import com.bank.product.workflow.validation.model.ValidationResult;
import com.bank.product.workflow.validation.model.ValidatorConfig;
import com.bank.product.workflow.domain.model.WorkflowSubject;
import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

/**
 * Temporal activities for validation execution
 * Supports rules-based, MCP, and GraphRAG validators
 */
@ActivityInterface
public interface ValidationActivity {

    /**
     * Execute document validation
     * Delegates to appropriate validator based on config.type:
     * - RULES_BASED: RulesBasedValidatorService
     * - MCP: MCPValidatorService
     * - GRAPH_RAG: GraphRAGValidatorService
     *
     * @param subject workflow subject
     * @param config validator configuration
     * @return validation result
     */
    @ActivityMethod
    ValidationResult executeDocumentValidation(WorkflowSubject subject, ValidatorConfig config);
}
