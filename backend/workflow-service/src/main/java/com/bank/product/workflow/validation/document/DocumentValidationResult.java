package com.bank.product.workflow.validation.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Result of document validation for a product configuration
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentValidationResult {

    /**
     * Overall completeness score (0.0 - 1.0)
     */
    private double completenessScore;

    /**
     * Whether all required documents are present
     */
    private boolean allDocumentsPresent;

    /**
     * Whether all documents are accessible (URLs work)
     */
    private boolean allDocumentsAccessible;

    /**
     * Whether there are any inconsistencies between docs and config
     */
    private boolean hasInconsistencies;

    /**
     * Missing documents
     */
    private List<MissingDocument> missingDocuments;

    /**
     * Inaccessible documents
     */
    private List<InaccessibleDocument> inaccessibleDocuments;

    /**
     * Inconsistencies found
     */
    private List<DocumentInconsistency> inconsistencies;

    /**
     * Compliance gaps
     */
    private List<ComplianceGap> complianceGaps;

    /**
     * Warnings (non-blocking issues)
     */
    private List<String> warnings;

    /**
     * Overall validation status
     */
    private ValidationStatus validationStatus;

    /**
     * Additional metadata
     */
    private Map<String, Object> metadata;

    /**
     * Recommended actions
     */
    private List<String> recommendations;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MissingDocument {
        private String documentType;
        private String documentName;
        private boolean required;
        private String reason;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InaccessibleDocument {
        private String documentType;
        private String url;
        private int statusCode;
        private String error;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DocumentInconsistency {
        private String field;
        private String configuredValue;
        private String documentedValue;
        private String documentType;
        private InconsistencySeverity severity;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ComplianceGap {
        private String regulation;
        private String requirement;
        private String gap;
        private ComplianceSeverity severity;
    }

    public enum ValidationStatus {
        PASS,
        PASS_WITH_WARNINGS,
        FAIL
    }

    public enum InconsistencySeverity {
        LOW,
        MEDIUM,
        HIGH,
        CRITICAL
    }

    public enum ComplianceSeverity {
        INFO,
        WARNING,
        ERROR,
        CRITICAL
    }
}
