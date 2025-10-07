package com.bank.product.workflow.temporal.activity;

import com.bank.product.workflow.validation.model.ValidationResult;
import com.bank.product.workflow.validation.model.ValidatorConfig;
import com.bank.product.workflow.validation.model.ValidatorType;
import com.bank.product.workflow.validation.service.RulesBasedValidatorService;
import com.bank.product.workflow.validation.mcp.MCPValidatorService;
import com.bank.product.workflow.domain.model.WorkflowSubject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Implementation of validation Temporal activities
 * Routes validation requests to appropriate validator service
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ValidationActivityImpl implements ValidationActivity {

    private final RulesBasedValidatorService rulesBasedValidatorService;

    @Autowired(required = false)
    private MCPValidatorService mcpValidatorService;

    // private final GraphRAGValidatorService graphRAGValidatorService; // To be implemented

    @Override
    public ValidationResult executeDocumentValidation(WorkflowSubject subject, ValidatorConfig config) {
        log.info("Executing document validation activity for workflow: {}, validator type: {}",
                subject.getWorkflowId(), config.getType());

        ValidatorType type = config.getType() != null ? config.getType() : ValidatorType.RULES_BASED;

        switch (type) {
            case RULES_BASED:
                return rulesBasedValidatorService.validateDocuments(subject, config);

            case MCP:
                if (mcpValidatorService != null) {
                    log.info("Executing MCP validator (Claude-powered)");
                    return mcpValidatorService.validateDocuments(subject, config);
                } else {
                    log.warn("MCP validator not available (missing API key?), falling back to rules-based");
                    return rulesBasedValidatorService.validateDocuments(subject, config);
                }

            case GRAPH_RAG:
                log.warn("GraphRAG validator not yet implemented, falling back to rules-based");
                // TODO: return graphRAGValidatorService.validateDocuments(subject, config);
                return rulesBasedValidatorService.validateDocuments(subject, config);

            case CUSTOM:
                throw new UnsupportedOperationException("Custom validators not yet supported");

            default:
                throw new IllegalArgumentException("Unknown validator type: " + type);
        }
    }
}
