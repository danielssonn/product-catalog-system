package com.bank.product.domain.solution.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Request to configure a new solution from a catalog product
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfigureSolutionRequest {

    private String catalogProductId;
    private String solutionName;
    private String description;

    // Custom pricing
    private BigDecimal customInterestRate;
    private Map<String, BigDecimal> customFees;

    // Custom terms
    private Map<String, Object> customTerms;

    // Configuration metadata for workflow rules
    private String riskLevel;  // LOW, MEDIUM, HIGH
    private Double pricingVariance;  // Percentage variance from catalog

    // Business justification
    private String businessJustification;
    private String priority;  // LOW, MEDIUM, HIGH
}
