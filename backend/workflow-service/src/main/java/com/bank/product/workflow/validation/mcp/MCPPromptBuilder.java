package com.bank.product.workflow.validation.mcp;

import com.bank.product.workflow.domain.model.WorkflowSubject;
import com.bank.product.workflow.validation.model.ValidatorConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Builds prompts for Claude MCP analysis
 */
@Slf4j
@Component
public class MCPPromptBuilder {

    /**
     * Build document analysis prompt for Claude
     */
    public String buildDocumentAnalysisPrompt(WorkflowSubject subject, ValidatorConfig config) {
        Map<String, Object> entityData = subject.getEntityData();
        Map<String, Object> entityMetadata = subject.getEntityMetadata();

        String solutionName = (String) entityData.getOrDefault("solutionName", "Unknown Product");
        String productType = (String) entityMetadata.getOrDefault("productType", "Unknown Type");
        String description = (String) entityData.getOrDefault("description", "No description");

        // Extract documents if present
        String documentsInfo = formatDocuments(entityData);

        // Build comprehensive prompt
        return String.format("""
            You are a banking product compliance analyst with expertise in regulatory requirements \
            including Regulation DD (Truth in Savings), Regulation E (Electronic Fund Transfers), \
            and FDIC disclosure requirements.

            Analyze the following product configuration for completeness, regulatory compliance, and risk.

            PRODUCT DETAILS:
            - Product Name: %s
            - Product Type: %s
            - Description: %s

            DOCUMENTS:
            %s

            METADATA:
            %s

            ANALYSIS REQUIRED:

            1. **Document Completeness**: Are all required documents present?
               Required documents:
               - Terms & Conditions (T&C)
               - Fee Schedule
               - Disclosure Statement
               - Privacy Policy (optional but recommended)

            2. **Regulatory Compliance**: Do documents meet regulatory requirements?
               Check for:
               - Regulation DD: APY disclosure, fee disclosure, minimum balance requirements
               - Regulation E: Electronic transfer rights, error resolution procedures
               - FDIC: Deposit insurance disclosure

            3. **Pricing Consistency**: Are pricing terms consistent across documents?
               Verify:
               - Interest rates match across T&C and disclosures
               - Fee amounts consistent in Fee Schedule and T&C
               - APY calculations are correct

            4. **Risk Assessment**: Identify any red flags or compliance gaps

            RESPONSE FORMAT (JSON):
            Respond with a JSON object containing:
            {
              "redFlagDetected": boolean,
              "redFlagReason": "string (if red flag detected)",
              "severity": "LOW|MEDIUM|HIGH|CRITICAL",
              "confidenceScore": 0.0-1.0,
              "documentCompleteness": 0.0-1.0,
              "regulatoryComplianceStatus": "COMPLIANT|PARTIAL|NON_COMPLIANT",
              "pricingConsistency": "CONSISTENT|MINOR_ISSUES|MAJOR_ISSUES",
              "identifiedRisks": ["risk1", "risk2", ...],
              "requiredActions": ["action1", "action2", ...],
              "agentRecommendation": "APPROVE|CONDITIONAL_APPROVE|REJECT|REQUIRES_REVIEW",
              "reasoning": [
                {
                  "stepName": "string",
                  "reasoning": "string",
                  "finding": "string"
                }
              ]
            }

            Provide your analysis now.
            """,
            solutionName,
            productType,
            description,
            documentsInfo,
            formatMetadata(entityMetadata)
        );
    }

    /**
     * Format documents section
     */
    private String formatDocuments(Map<String, Object> entityData) {
        @SuppressWarnings("unchecked")
        Map<String, String> documents = (Map<String, String>) entityData.get("documents");

        if (documents == null || documents.isEmpty()) {
            return "No documents provided";
        }

        StringBuilder sb = new StringBuilder();
        documents.forEach((key, value) -> {
            sb.append(String.format("  - %s: %s%n", key, value));
        });

        return sb.toString();
    }

    /**
     * Format metadata section
     */
    private String formatMetadata(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return "No metadata provided";
        }

        StringBuilder sb = new StringBuilder();
        metadata.forEach((key, value) -> {
            sb.append(String.format("  - %s: %s%n", key, value));
        });

        return sb.toString();
    }
}
