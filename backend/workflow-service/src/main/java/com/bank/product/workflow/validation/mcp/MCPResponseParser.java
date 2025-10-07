package com.bank.product.workflow.validation.mcp;

import com.bank.product.workflow.domain.model.WorkflowSubject;
import com.bank.product.workflow.validation.model.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Parses Claude MCP responses into ValidationResult
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MCPResponseParser {

    private final ObjectMapper objectMapper;

    /**
     * Parse Claude chat response into ValidationResult
     */
    public ValidationResult parseResponse(ChatResponse response,
                                         WorkflowSubject subject,
                                         ValidatorConfig config,
                                         LocalDateTime startTime,
                                         Duration executionTime) {
        try {
            // Extract response text
            String responseText = response.getResult().getOutput().getContent();
            log.debug("Claude response: {}", responseText);

            // Extract JSON from response (Claude sometimes wraps it in markdown)
            String jsonText = extractJson(responseText);

            // Parse JSON
            JsonNode root = objectMapper.readTree(jsonText);

            // Extract fields
            boolean redFlagDetected = root.path("redFlagDetected").asBoolean(false);
            String redFlagReason = root.path("redFlagReason").asText(null);
            String severityStr = root.path("severity").asText("LOW");
            double confidenceScore = root.path("confidenceScore").asDouble(0.0);

            RedFlagSeverity severity = parseRedFlagSeverity(severityStr);

            // Build enrichment data
            Map<String, Object> enrichmentData = buildEnrichmentData(root);

            // Build validation steps from reasoning
            List<ValidationStep> validationSteps = buildValidationSteps(root);

            // Get metadata from response
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("model", response.getMetadata().getModel());
            metadata.put("tokenUsage", response.getMetadata().getUsage());

            return ValidationResult.builder()
                    .validatorId(config.getValidatorId())
                    .validatorType(ValidatorType.MCP)
                    .executedAt(startTime)
                    .executionTime(executionTime)
                    .redFlagDetected(redFlagDetected)
                    .redFlagReason(redFlagReason)
                    .severity(severity)
                    .recommendedAction(redFlagDetected ? config.getRedFlagAction() : null)
                    .enrichmentData(enrichmentData)
                    .validationSteps(validationSteps)
                    .confidenceScore(confidenceScore)
                    .model(response.getMetadata().getModel())
                    .success(true)
                    .metadata(metadata)
                    .build();

        } catch (Exception e) {
            log.error("Failed to parse MCP response: {}", e.getMessage(), e);

            // Return error result
            return ValidationResult.builder()
                    .validatorId(config.getValidatorId())
                    .validatorType(ValidatorType.MCP)
                    .executedAt(startTime)
                    .executionTime(executionTime)
                    .success(false)
                    .errorMessage("Failed to parse MCP response: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Extract JSON from response (handle markdown code blocks)
     */
    private String extractJson(String responseText) {
        // Remove markdown code blocks if present
        if (responseText.contains("```json")) {
            int start = responseText.indexOf("```json") + 7;
            int end = responseText.lastIndexOf("```");
            if (end > start) {
                return responseText.substring(start, end).trim();
            }
        } else if (responseText.contains("```")) {
            int start = responseText.indexOf("```") + 3;
            int end = responseText.lastIndexOf("```");
            if (end > start) {
                return responseText.substring(start, end).trim();
            }
        }

        // Try to find JSON object
        int start = responseText.indexOf("{");
        int end = responseText.lastIndexOf("}");
        if (start >= 0 && end > start) {
            return responseText.substring(start, end + 1);
        }

        return responseText;
    }

    /**
     * Parse severity string to enum
     */
    private RedFlagSeverity parseRedFlagSeverity(String severityStr) {
        try {
            return RedFlagSeverity.valueOf(severityStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("Unknown severity: {}, defaulting to LOW", severityStr);
            return RedFlagSeverity.LOW;
        }
    }

    /**
     * Build enrichment data from JSON response
     */
    private Map<String, Object> buildEnrichmentData(JsonNode root) {
        Map<String, Object> enrichment = new HashMap<>();

        // Extract enrichment fields
        enrichment.put("documentCompleteness", root.path("documentCompleteness").asDouble(0.0));
        enrichment.put("documentValidationStatus",
            root.path("regulatoryComplianceStatus").asText("UNKNOWN"));
        enrichment.put("regulatoryComplianceStatus",
            root.path("regulatoryComplianceStatus").asText("UNKNOWN"));
        enrichment.put("pricingConsistency",
            root.path("pricingConsistency").asText("UNKNOWN"));
        enrichment.put("agentRecommendation",
            root.path("agentRecommendation").asText("REQUIRES_REVIEW"));

        // Extract risk arrays
        List<String> identifiedRisks = new ArrayList<>();
        JsonNode risksNode = root.path("identifiedRisks");
        if (risksNode.isArray()) {
            risksNode.forEach(risk -> identifiedRisks.add(risk.asText()));
        }
        enrichment.put("identifiedRisks", identifiedRisks);

        List<String> requiredActions = new ArrayList<>();
        JsonNode actionsNode = root.path("requiredActions");
        if (actionsNode.isArray()) {
            actionsNode.forEach(action -> requiredActions.add(action.asText()));
        }
        enrichment.put("requiredActions", requiredActions);
        enrichment.put("documentRecommendations", requiredActions);

        // Count metrics
        enrichment.put("missingDocumentCount", identifiedRisks.stream()
            .filter(r -> r.toLowerCase().contains("missing")).count());
        enrichment.put("inconsistencyCount", identifiedRisks.stream()
            .filter(r -> r.toLowerCase().contains("inconsist")).count());
        enrichment.put("complianceGapCount", identifiedRisks.stream()
            .filter(r -> r.toLowerCase().contains("compliance") ||
                         r.toLowerCase().contains("regulatory")).count());

        return enrichment;
    }

    /**
     * Build validation steps from reasoning array
     */
    private List<ValidationStep> buildValidationSteps(JsonNode root) {
        List<ValidationStep> steps = new ArrayList<>();

        JsonNode reasoningNode = root.path("reasoning");
        if (reasoningNode.isArray()) {
            int stepNumber = 1;
            for (JsonNode reasoningStep : reasoningNode) {
                String stepName = reasoningStep.path("stepName").asText("Analysis Step");
                String reasoning = reasoningStep.path("reasoning").asText("");
                String finding = reasoningStep.path("finding").asText("");

                Map<String, Object> output = new HashMap<>();
                output.put("finding", finding);

                steps.add(ValidationStep.builder()
                        .stepNumber(stepNumber++)
                        .stepName(stepName)
                        .tool("claude_analyze")
                        .input(Map.of("analysis", stepName))
                        .output(output)
                        .reasoning(reasoning + (finding.isEmpty() ? "" : " Finding: " + finding))
                        .timestamp(LocalDateTime.now())
                        .success(true)
                        .build());
            }
        }

        // Add summary step if no reasoning provided
        if (steps.isEmpty()) {
            steps.add(ValidationStep.builder()
                    .stepNumber(1)
                    .stepName("Claude MCP Analysis")
                    .tool("claude_analyze")
                    .input(Map.of("type", "document_validation"))
                    .output(Map.of("completed", true))
                    .reasoning("LLM-powered semantic analysis completed")
                    .timestamp(LocalDateTime.now())
                    .success(true)
                    .build());
        }

        return steps;
    }
}
