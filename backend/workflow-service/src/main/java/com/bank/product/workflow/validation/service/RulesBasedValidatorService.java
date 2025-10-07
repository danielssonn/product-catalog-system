package com.bank.product.workflow.validation.service;

import com.bank.product.workflow.validation.document.DocumentValidationResult;
import com.bank.product.workflow.validation.document.DocumentValidator;
import com.bank.product.workflow.validation.model.*;
import com.bank.product.workflow.domain.model.WorkflowSubject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Rules-based validator service (deterministic validation logic)
 * This is NOT an AI agent - it uses hardcoded business rules
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RulesBasedValidatorService {

    private final DocumentValidator documentValidator;

    /**
     * Execute rules-based document validation
     */
    public ValidationResult validateDocuments(WorkflowSubject subject,
                                               ValidatorConfig config) {
        log.info("Executing rules-based document validation: {} for workflow: {}",
                config.getValidatorId(), subject.getWorkflowId());

        LocalDateTime startTime = LocalDateTime.now();

        try {
            // Run document validation
            DocumentValidationResult validationResult = documentValidator.validate(subject);

            LocalDateTime endTime = LocalDateTime.now();
            Duration executionTime = Duration.between(startTime, endTime);

            // Build validation steps
            List<ValidationStep> validationSteps = buildValidationSteps(validationResult);

            // Check for red flags
            boolean redFlagDetected = isRedFlag(validationResult, config);
            String redFlagReason = redFlagDetected ? buildRedFlagReason(validationResult) : null;
            RedFlagSeverity severity = redFlagDetected ? determineSeverity(validationResult) : null;

            // Build enrichment data
            Map<String, Object> enrichmentData = buildEnrichmentData(validationResult, config);

            // Confidence score is deterministic for rules-based (always 1.0 if success)
            double confidenceScore = validationResult.getCompletenessScore();

            return ValidationResult.builder()
                    .validatorId(config.getValidatorId())
                    .validatorType(ValidatorType.RULES_BASED)
                    .executedAt(startTime)
                    .executionTime(executionTime)
                    .redFlagDetected(redFlagDetected)
                    .redFlagReason(redFlagReason)
                    .severity(severity)
                    .recommendedAction(redFlagDetected ? config.getRedFlagAction() : null)
                    .enrichmentData(enrichmentData)
                    .validationSteps(validationSteps)
                    .confidenceScore(confidenceScore)
                    .model("rules-based-v1")
                    .success(true)
                    .metadata(validationResult.getMetadata())
                    .build();

        } catch (Exception e) {
            log.error("Rules-based document validation failed: {}", e.getMessage(), e);

            LocalDateTime endTime = LocalDateTime.now();
            Duration executionTime = Duration.between(startTime, endTime);

            return ValidationResult.builder()
                    .validatorId(config.getValidatorId())
                    .validatorType(ValidatorType.RULES_BASED)
                    .executedAt(startTime)
                    .executionTime(executionTime)
                    .success(false)
                    .errorMessage(e.getMessage())
                    .build();
        }
    }

    /**
     * Build validation steps from validation result
     */
    private List<ValidationStep> buildValidationSteps(DocumentValidationResult validationResult) {
        List<ValidationStep> steps = new ArrayList<>();

        int stepNumber = 1;

        // Step 1: Document presence check
        Map<String, Object> step1Input = new HashMap<>();
        step1Input.put("checkType", "document_presence");

        Map<String, Object> step1Output = new HashMap<>();
        step1Output.put("allDocumentsPresent", validationResult.isAllDocumentsPresent());
        step1Output.put("missingCount", validationResult.getMissingDocuments().size());
        step1Output.put("missingDocuments", validationResult.getMissingDocuments());

        steps.add(ValidationStep.builder()
                .stepNumber(stepNumber++)
                .stepName("Check Required Documents")
                .tool("document_presence_checker")
                .input(step1Input)
                .output(step1Output)
                .reasoning(validationResult.isAllDocumentsPresent() ?
                        "All required documents are present" :
                        "Found " + validationResult.getMissingDocuments().size() + " missing document(s)")
                .timestamp(LocalDateTime.now())
                .success(true)
                .build());

        // Step 2: Document accessibility check
        Map<String, Object> step2Input = new HashMap<>();
        step2Input.put("checkType", "document_accessibility");

        Map<String, Object> step2Output = new HashMap<>();
        step2Output.put("allDocumentsAccessible", validationResult.isAllDocumentsAccessible());
        step2Output.put("inaccessibleCount", validationResult.getInaccessibleDocuments().size());
        step2Output.put("inaccessibleDocuments", validationResult.getInaccessibleDocuments());

        steps.add(ValidationStep.builder()
                .stepNumber(stepNumber++)
                .stepName("Check Document Accessibility")
                .tool("url_validator")
                .input(step2Input)
                .output(step2Output)
                .reasoning(validationResult.isAllDocumentsAccessible() ?
                        "All document URLs are accessible" :
                        "Found " + validationResult.getInaccessibleDocuments().size() + " inaccessible document(s)")
                .timestamp(LocalDateTime.now())
                .success(true)
                .build());

        // Step 3: Consistency check
        Map<String, Object> step3Input = new HashMap<>();
        step3Input.put("checkType", "pricing_consistency");

        Map<String, Object> step3Output = new HashMap<>();
        step3Output.put("hasInconsistencies", validationResult.isHasInconsistencies());
        step3Output.put("inconsistencyCount", validationResult.getInconsistencies().size());
        step3Output.put("inconsistencies", validationResult.getInconsistencies());

        steps.add(ValidationStep.builder()
                .stepNumber(stepNumber++)
                .stepName("Check Pricing Consistency")
                .tool("consistency_checker")
                .input(step3Input)
                .output(step3Output)
                .reasoning(!validationResult.isHasInconsistencies() ?
                        "Configuration matches documented values" :
                        "Found " + validationResult.getInconsistencies().size() + " inconsistency/inconsistencies")
                .timestamp(LocalDateTime.now())
                .success(true)
                .build());

        // Step 4: Compliance check
        Map<String, Object> step4Input = new HashMap<>();
        step4Input.put("checkType", "regulatory_compliance");

        Map<String, Object> step4Output = new HashMap<>();
        step4Output.put("complianceGapCount", validationResult.getComplianceGaps().size());
        step4Output.put("complianceGaps", validationResult.getComplianceGaps());

        steps.add(ValidationStep.builder()
                .stepNumber(stepNumber++)
                .stepName("Check Regulatory Compliance")
                .tool("compliance_checker")
                .input(step4Input)
                .output(step4Output)
                .reasoning(validationResult.getComplianceGaps().isEmpty() ?
                        "All regulatory requirements met" :
                        "Found " + validationResult.getComplianceGaps().size() + " compliance gap(s)")
                .timestamp(LocalDateTime.now())
                .success(true)
                .build());

        return steps;
    }

    /**
     * Check if validation result should trigger red flag
     */
    private boolean isRedFlag(DocumentValidationResult validationResult,
                             ValidatorConfig config) {
        // Check configured red flag conditions
        if (config.getRedFlagConditions() != null) {
            Map<String, String> conditions = config.getRedFlagConditions();

            // Check completeness score
            if (conditions.containsKey("completenessScore")) {
                String condition = conditions.get("completenessScore");
                if (evaluateCondition(validationResult.getCompletenessScore(), condition)) {
                    return true;
                }
            }

            // Check validation status
            if (conditions.containsKey("validationStatus")) {
                String condition = conditions.get("validationStatus");
                if (validationResult.getValidationStatus().name().equals(condition) ||
                    condition.contains(validationResult.getValidationStatus().name())) {
                    return true;
                }
            }
        }

        // Default: red flag if validation fails
        return validationResult.getValidationStatus() == DocumentValidationResult.ValidationStatus.FAIL;
    }

    /**
     * Simple condition evaluator
     */
    private boolean evaluateCondition(double value, String condition) {
        condition = condition.trim();

        if (condition.startsWith("< ")) {
            double threshold = Double.parseDouble(condition.substring(2).trim());
            return value < threshold;
        } else if (condition.startsWith("> ")) {
            double threshold = Double.parseDouble(condition.substring(2).trim());
            return value > threshold;
        } else if (condition.startsWith("== ")) {
            double threshold = Double.parseDouble(condition.substring(3).trim());
            return Math.abs(value - threshold) < 0.001;
        }

        return false;
    }

    /**
     * Build red flag reason from validation result
     */
    private String buildRedFlagReason(DocumentValidationResult validationResult) {
        StringBuilder reason = new StringBuilder("Document validation failed: ");

        if (!validationResult.getMissingDocuments().isEmpty()) {
            long requiredMissing = validationResult.getMissingDocuments().stream()
                    .filter(DocumentValidationResult.MissingDocument::isRequired)
                    .count();
            if (requiredMissing > 0) {
                reason.append(requiredMissing).append(" required document(s) missing; ");
            }
        }

        if (!validationResult.getInconsistencies().isEmpty()) {
            long criticalInconsistencies = validationResult.getInconsistencies().stream()
                    .filter(inc -> inc.getSeverity() == DocumentValidationResult.InconsistencySeverity.CRITICAL)
                    .count();
            if (criticalInconsistencies > 0) {
                reason.append(criticalInconsistencies).append(" critical inconsistency/inconsistencies; ");
            }
        }

        if (!validationResult.getComplianceGaps().isEmpty()) {
            long criticalGaps = validationResult.getComplianceGaps().stream()
                    .filter(gap -> gap.getSeverity() == DocumentValidationResult.ComplianceSeverity.CRITICAL)
                    .count();
            if (criticalGaps > 0) {
                reason.append(criticalGaps).append(" critical compliance gap(s); ");
            }
        }

        return reason.toString();
    }

    /**
     * Determine severity based on validation result
     */
    private RedFlagSeverity determineSeverity(DocumentValidationResult validationResult) {
        // Critical if there are critical compliance gaps
        long criticalGaps = validationResult.getComplianceGaps().stream()
                .filter(gap -> gap.getSeverity() == DocumentValidationResult.ComplianceSeverity.CRITICAL)
                .count();

        if (criticalGaps > 0) {
            return RedFlagSeverity.CRITICAL;
        }

        // High if there are critical inconsistencies
        long criticalInconsistencies = validationResult.getInconsistencies().stream()
                .filter(inc -> inc.getSeverity() == DocumentValidationResult.InconsistencySeverity.CRITICAL)
                .count();

        if (criticalInconsistencies > 0) {
            return RedFlagSeverity.HIGH;
        }

        // High if required documents are missing
        long requiredMissing = validationResult.getMissingDocuments().stream()
                .filter(DocumentValidationResult.MissingDocument::isRequired)
                .count();

        if (requiredMissing > 0) {
            return RedFlagSeverity.HIGH;
        }

        // Medium if there are any issues
        if (!validationResult.getInconsistencies().isEmpty() ||
            !validationResult.getComplianceGaps().isEmpty()) {
            return RedFlagSeverity.MEDIUM;
        }

        return RedFlagSeverity.LOW;
    }

    /**
     * Build enrichment data from validation result
     */
    private Map<String, Object> buildEnrichmentData(DocumentValidationResult validationResult,
                                                    ValidatorConfig config) {
        Map<String, Object> enrichment = new HashMap<>();

        // Add all fields if no specific enrichment outputs configured
        List<String> outputFields = config.getEnrichmentOutputs();
        if (outputFields == null || outputFields.isEmpty()) {
            enrichment.put("documentCompleteness", validationResult.getCompletenessScore());
            enrichment.put("documentValidationStatus", validationResult.getValidationStatus().name());
            enrichment.put("missingDocumentCount", validationResult.getMissingDocuments().size());
            enrichment.put("inconsistencyCount", validationResult.getInconsistencies().size());
            enrichment.put("complianceGapCount", validationResult.getComplianceGaps().size());
            enrichment.put("documentRecommendations", validationResult.getRecommendations());
        } else {
            // Add only specified fields
            for (String field : outputFields) {
                switch (field) {
                    case "documentCompleteness":
                        enrichment.put(field, validationResult.getCompletenessScore());
                        break;
                    case "documentValidationStatus":
                        enrichment.put(field, validationResult.getValidationStatus().name());
                        break;
                    case "missingDocumentCount":
                        enrichment.put(field, validationResult.getMissingDocuments().size());
                        break;
                    case "inconsistencyCount":
                        enrichment.put(field, validationResult.getInconsistencies().size());
                        break;
                    case "complianceGapCount":
                        enrichment.put(field, validationResult.getComplianceGaps().size());
                        break;
                    case "documentRecommendations":
                        enrichment.put(field, validationResult.getRecommendations());
                        break;
                    case "missingDocuments":
                        enrichment.put(field, validationResult.getMissingDocuments());
                        break;
                    case "inconsistencies":
                        enrichment.put(field, validationResult.getInconsistencies());
                        break;
                    case "complianceGaps":
                        enrichment.put(field, validationResult.getComplianceGaps());
                        break;
                }
            }
        }

        return enrichment;
    }
}
