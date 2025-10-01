package com.bank.product.workflow.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Business context for a workflow request
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BusinessContext {

    /**
     * Business justification for the request
     */
    private String justification;

    /**
     * Expected business impact
     */
    private String businessImpact;

    /**
     * Target effective date
     */
    private String effectiveDate;

    /**
     * Reference to related entity (e.g., catalogProductId, solutionId)
     */
    private String entityId;

    /**
     * Type of entity being affected
     */
    private String entityType;

    /**
     * Department or cost center
     */
    private String department;

    /**
     * Priority level
     */
    private Priority priority;

    /**
     * Compliance requirements
     */
    private String complianceNotes;

    /**
     * Risk assessment
     */
    private RiskLevel riskLevel;

    /**
     * Additional context data
     */
    private Map<String, Object> additionalContext;
}
