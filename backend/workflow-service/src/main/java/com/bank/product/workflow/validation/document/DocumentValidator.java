package com.bank.product.workflow.validation.document;

import com.bank.product.workflow.domain.model.WorkflowSubject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;

/**
 * Validates documents for product configurations
 * This is a simple implementation - in production would integrate with document management system
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentValidator {

    /**
     * Validate documents for a workflow subject
     */
    public DocumentValidationResult validate(WorkflowSubject subject) {
        log.info("Validating documents for workflow: {}, entity: {}",
                subject.getWorkflowId(), subject.getEntityId());

        Map<String, Object> entityData = subject.getEntityData();
        Map<String, Object> entityMetadata = subject.getEntityMetadata();

        // Initialize result
        DocumentValidationResult.DocumentValidationResultBuilder resultBuilder =
                DocumentValidationResult.builder();

        List<DocumentValidationResult.MissingDocument> missingDocs = new ArrayList<>();
        List<DocumentValidationResult.InaccessibleDocument> inaccessibleDocs = new ArrayList<>();
        List<DocumentValidationResult.DocumentInconsistency> inconsistencies = new ArrayList<>();
        List<DocumentValidationResult.ComplianceGap> complianceGaps = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        List<String> recommendations = new ArrayList<>();

        // 1. Check required documents
        checkRequiredDocuments(entityData, missingDocs);

        // 2. Validate document accessibility (URLs)
        validateDocumentUrls(entityData, inaccessibleDocs);

        // 3. Check pricing consistency
        checkPricingConsistency(entityData, entityMetadata, inconsistencies);

        // 4. Check regulatory compliance
        checkRegulatoryCompliance(entityData, entityMetadata, complianceGaps);

        // 5. Check terms coverage
        checkTermsCoverage(entityData, warnings);

        // 6. Generate recommendations
        generateRecommendations(missingDocs, inconsistencies, complianceGaps, recommendations);

        // Calculate completeness score
        double completenessScore = calculateCompletenessScore(
                missingDocs, inaccessibleDocs, inconsistencies, complianceGaps);

        // Determine validation status
        DocumentValidationResult.ValidationStatus status = determineValidationStatus(
                missingDocs, inconsistencies, complianceGaps);

        return resultBuilder
                .completenessScore(completenessScore)
                .allDocumentsPresent(missingDocs.isEmpty())
                .allDocumentsAccessible(inaccessibleDocs.isEmpty())
                .hasInconsistencies(!inconsistencies.isEmpty())
                .missingDocuments(missingDocs)
                .inaccessibleDocuments(inaccessibleDocs)
                .inconsistencies(inconsistencies)
                .complianceGaps(complianceGaps)
                .warnings(warnings)
                .validationStatus(status)
                .recommendations(recommendations)
                .metadata(buildMetadata(subject))
                .build();
    }

    private void checkRequiredDocuments(Map<String, Object> entityData,
                                        List<DocumentValidationResult.MissingDocument> missingDocs) {
        // Check for terms and conditions
        String termsUrl = getStringValue(entityData, "termsAndConditionsUrl");
        if (termsUrl == null || termsUrl.isEmpty()) {
            missingDocs.add(DocumentValidationResult.MissingDocument.builder()
                    .documentType("TERMS_AND_CONDITIONS")
                    .documentName("Terms and Conditions")
                    .required(true)
                    .reason("Required for all products")
                    .build());
        }

        // Check for disclosure document
        String disclosureUrl = getStringValue(entityData, "disclosureUrl");
        if (disclosureUrl == null || disclosureUrl.isEmpty()) {
            missingDocs.add(DocumentValidationResult.MissingDocument.builder()
                    .documentType("DISCLOSURE")
                    .documentName("Product Disclosure")
                    .required(true)
                    .reason("Required for regulatory compliance")
                    .build());
        }

        // Check for fee schedule
        String feeScheduleUrl = getStringValue(entityData, "feeScheduleUrl");
        if (feeScheduleUrl == null || feeScheduleUrl.isEmpty()) {
            missingDocs.add(DocumentValidationResult.MissingDocument.builder()
                    .documentType("FEE_SCHEDULE")
                    .documentName("Fee Schedule")
                    .required(true)
                    .reason("Required to document all fees")
                    .build());
        }

        // Check for product documentation
        String documentationUrl = getStringValue(entityData, "documentationUrl");
        if (documentationUrl == null || documentationUrl.isEmpty()) {
            missingDocs.add(DocumentValidationResult.MissingDocument.builder()
                    .documentType("PRODUCT_DOCUMENTATION")
                    .documentName("Product Documentation")
                    .required(false)
                    .reason("Recommended for customer support")
                    .build());
        }
    }

    private void validateDocumentUrls(Map<String, Object> entityData,
                                      List<DocumentValidationResult.InaccessibleDocument> inaccessibleDocs) {
        // In production, would actually fetch URLs and check accessibility
        // For now, just validate URL format

        validateUrl("termsAndConditionsUrl", entityData, inaccessibleDocs, "TERMS_AND_CONDITIONS");
        validateUrl("disclosureUrl", entityData, inaccessibleDocs, "DISCLOSURE");
        validateUrl("feeScheduleUrl", entityData, inaccessibleDocs, "FEE_SCHEDULE");
        validateUrl("documentationUrl", entityData, inaccessibleDocs, "PRODUCT_DOCUMENTATION");
    }

    private void validateUrl(String field, Map<String, Object> entityData,
                            List<DocumentValidationResult.InaccessibleDocument> inaccessibleDocs,
                            String documentType) {
        String url = getStringValue(entityData, field);
        if (url != null && !url.isEmpty()) {
            // Basic URL validation
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                inaccessibleDocs.add(DocumentValidationResult.InaccessibleDocument.builder()
                        .documentType(documentType)
                        .url(url)
                        .statusCode(0)
                        .error("Invalid URL format - must start with http:// or https://")
                        .build());
            }
        }
    }

    private void checkPricingConsistency(Map<String, Object> entityData,
                                        Map<String, Object> entityMetadata,
                                        List<DocumentValidationResult.DocumentInconsistency> inconsistencies) {
        // Check if configured fees match documented fees
        // In production, would parse fee schedule document and compare

        // Example: Check monthly fee
        BigDecimal configuredMonthlyFee = getBigDecimalValue(entityData, "monthlyFee");
        BigDecimal documentedMonthlyFee = getBigDecimalValue(entityMetadata, "documentedMonthlyFee");

        if (configuredMonthlyFee != null && documentedMonthlyFee != null &&
                configuredMonthlyFee.compareTo(documentedMonthlyFee) != 0) {
            inconsistencies.add(DocumentValidationResult.DocumentInconsistency.builder()
                    .field("monthlyFee")
                    .configuredValue("$" + configuredMonthlyFee)
                    .documentedValue("$" + documentedMonthlyFee)
                    .documentType("FEE_SCHEDULE")
                    .severity(DocumentValidationResult.InconsistencySeverity.HIGH)
                    .build());
        }

        // Check interest rates
        BigDecimal configuredInterestRate = getBigDecimalValue(entityData, "interestRate");
        BigDecimal documentedInterestRate = getBigDecimalValue(entityMetadata, "documentedInterestRate");

        if (configuredInterestRate != null && documentedInterestRate != null &&
                configuredInterestRate.compareTo(documentedInterestRate) != 0) {
            inconsistencies.add(DocumentValidationResult.DocumentInconsistency.builder()
                    .field("interestRate")
                    .configuredValue(configuredInterestRate + "%")
                    .documentedValue(documentedInterestRate + "%")
                    .documentType("DISCLOSURE")
                    .severity(DocumentValidationResult.InconsistencySeverity.CRITICAL)
                    .build());
        }
    }

    private void checkRegulatoryCompliance(Map<String, Object> entityData,
                                          Map<String, Object> entityMetadata,
                                          List<DocumentValidationResult.ComplianceGap> complianceGaps) {
        // Check for required regulatory disclosures based on product type
        String productType = getStringValue(entityData, "productType");

        if ("CHECKING".equalsIgnoreCase(productType) || "SAVINGS".equalsIgnoreCase(productType)) {
            // Check for Reg DD compliance
            if (!hasDisclosure(entityMetadata, "REG_DD")) {
                complianceGaps.add(DocumentValidationResult.ComplianceGap.builder()
                        .regulation("Regulation DD (Truth in Savings)")
                        .requirement("Account disclosure statement")
                        .gap("Missing Reg DD disclosure in documentation")
                        .severity(DocumentValidationResult.ComplianceSeverity.CRITICAL)
                        .build());
            }

            // Check for FDIC notice
            if (!hasDisclosure(entityMetadata, "FDIC_NOTICE")) {
                complianceGaps.add(DocumentValidationResult.ComplianceGap.builder()
                        .regulation("FDIC Insurance")
                        .requirement("FDIC insurance notice")
                        .gap("Missing FDIC insurance disclosure")
                        .severity(DocumentValidationResult.ComplianceSeverity.ERROR)
                        .build());
            }
        }

        // Check if product has overdraft feature
        Boolean hasOverdraft = getBooleanValue(entityData, "hasOverdraft");
        if (Boolean.TRUE.equals(hasOverdraft)) {
            if (!hasDisclosure(entityMetadata, "REG_E_OVERDRAFT")) {
                complianceGaps.add(DocumentValidationResult.ComplianceGap.builder()
                        .regulation("Regulation E")
                        .requirement("Overdraft opt-in disclosure")
                        .gap("Missing Reg E overdraft opt-in form")
                        .severity(DocumentValidationResult.ComplianceSeverity.CRITICAL)
                        .build());
            }
        }
    }

    private void checkTermsCoverage(Map<String, Object> entityData, List<String> warnings) {
        // Check if terms cover all configured features
        @SuppressWarnings("unchecked")
        Map<String, Object> features = (Map<String, Object>) entityData.get("features");

        if (features != null && !features.isEmpty()) {
            // In production, would parse terms document and verify each feature is covered
            warnings.add("Terms and conditions should explicitly cover all " + features.size() + " configured features");
        }

        // Check for early withdrawal penalties
        Boolean hasEarlyWithdrawal = getBooleanValue(entityData, "earlyWithdrawalPenalty");
        String penaltyDescription = getStringValue(entityData, "penaltyDescription");

        if (Boolean.TRUE.equals(hasEarlyWithdrawal) &&
            (penaltyDescription == null || penaltyDescription.isEmpty())) {
            warnings.add("Early withdrawal penalty is enabled but description is missing");
        }
    }

    private void generateRecommendations(List<DocumentValidationResult.MissingDocument> missingDocs,
                                        List<DocumentValidationResult.DocumentInconsistency> inconsistencies,
                                        List<DocumentValidationResult.ComplianceGap> complianceGaps,
                                        List<String> recommendations) {
        if (!missingDocs.isEmpty()) {
            long requiredCount = missingDocs.stream().filter(DocumentValidationResult.MissingDocument::isRequired).count();
            if (requiredCount > 0) {
                recommendations.add("Upload " + requiredCount + " required document(s) before submission");
            }
        }

        if (!inconsistencies.isEmpty()) {
            recommendations.add("Resolve " + inconsistencies.size() + " inconsistency/inconsistencies between configuration and documents");
        }

        if (!complianceGaps.isEmpty()) {
            long criticalCount = complianceGaps.stream()
                    .filter(gap -> gap.getSeverity() == DocumentValidationResult.ComplianceSeverity.CRITICAL)
                    .count();
            if (criticalCount > 0) {
                recommendations.add("Address " + criticalCount + " critical compliance gap(s)");
            }
        }

        if (missingDocs.isEmpty() && inconsistencies.isEmpty() && complianceGaps.isEmpty()) {
            recommendations.add("Documentation is complete and consistent");
        }
    }

    private double calculateCompletenessScore(List<DocumentValidationResult.MissingDocument> missingDocs,
                                             List<DocumentValidationResult.InaccessibleDocument> inaccessibleDocs,
                                             List<DocumentValidationResult.DocumentInconsistency> inconsistencies,
                                             List<DocumentValidationResult.ComplianceGap> complianceGaps) {
        // Simple scoring algorithm
        double score = 1.0;

        // Deduct for missing required documents
        long requiredMissing = missingDocs.stream()
                .filter(DocumentValidationResult.MissingDocument::isRequired)
                .count();
        score -= requiredMissing * 0.25;

        // Deduct for inaccessible documents
        score -= inaccessibleDocs.size() * 0.15;

        // Deduct for inconsistencies
        score -= inconsistencies.size() * 0.1;

        // Deduct for compliance gaps
        long criticalGaps = complianceGaps.stream()
                .filter(gap -> gap.getSeverity() == DocumentValidationResult.ComplianceSeverity.CRITICAL)
                .count();
        score -= criticalGaps * 0.2;

        return Math.max(0.0, Math.min(1.0, score));
    }

    private DocumentValidationResult.ValidationStatus determineValidationStatus(
            List<DocumentValidationResult.MissingDocument> missingDocs,
            List<DocumentValidationResult.DocumentInconsistency> inconsistencies,
            List<DocumentValidationResult.ComplianceGap> complianceGaps) {

        // Check for critical failures
        long requiredMissing = missingDocs.stream()
                .filter(DocumentValidationResult.MissingDocument::isRequired)
                .count();

        long criticalInconsistencies = inconsistencies.stream()
                .filter(inc -> inc.getSeverity() == DocumentValidationResult.InconsistencySeverity.CRITICAL)
                .count();

        long criticalGaps = complianceGaps.stream()
                .filter(gap -> gap.getSeverity() == DocumentValidationResult.ComplianceSeverity.CRITICAL)
                .count();

        if (requiredMissing > 0 || criticalInconsistencies > 0 || criticalGaps > 0) {
            return DocumentValidationResult.ValidationStatus.FAIL;
        }

        // Check for warnings
        if (!missingDocs.isEmpty() || !inconsistencies.isEmpty() || !complianceGaps.isEmpty()) {
            return DocumentValidationResult.ValidationStatus.PASS_WITH_WARNINGS;
        }

        return DocumentValidationResult.ValidationStatus.PASS;
    }

    private Map<String, Object> buildMetadata(WorkflowSubject subject) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("workflowId", subject.getWorkflowId());
        metadata.put("entityType", subject.getEntityType());
        metadata.put("entityId", subject.getEntityId());
        metadata.put("validatedAt", java.time.LocalDateTime.now());
        return metadata;
    }

    private boolean hasDisclosure(Map<String, Object> metadata, String disclosureType) {
        @SuppressWarnings("unchecked")
        List<String> disclosures = (List<String>) metadata.get("disclosures");
        return disclosures != null && disclosures.contains(disclosureType);
    }

    // Helper methods
    private String getStringValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString() : null;
    }

    private BigDecimal getBigDecimalValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return null;
        if (value instanceof BigDecimal) return (BigDecimal) value;
        if (value instanceof Number) return BigDecimal.valueOf(((Number) value).doubleValue());
        try {
            return new BigDecimal(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Boolean getBooleanValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return null;
        if (value instanceof Boolean) return (Boolean) value;
        return Boolean.parseBoolean(value.toString());
    }
}
