package com.bank.product.domain.solution.dto.v2;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

/**
 * V2 Request to configure a new solution from a catalog product
 * Breaking change: customFees renamed to customFeesFX
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfigureSolutionRequestV2 {

    private String catalogProductId;
    private String solutionName;
    private String description;

    // Custom pricing
    private BigDecimal customInterestRate;

    /**
     * Breaking change in v2: renamed from customFees to customFeesFX
     * Reason: Better clarity that fees are configurable/flexible
     */
    private Map<String, BigDecimal> customFeesFX;

    // Custom terms
    private Map<String, Object> customTerms;

    // Configuration metadata for workflow rules
    private String riskLevel;  // LOW, MEDIUM, HIGH
    private Double pricingVariance;  // Percentage variance from catalog

    // Business justification
    private String businessJustification;
    private String priority;  // LOW, MEDIUM, HIGH

    // New in v2: Additional metadata for extensibility
    private Map<String, Object> metadata;
}
