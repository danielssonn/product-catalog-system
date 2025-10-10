package com.bank.product.gateway.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Parsed product configuration record from file
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductConfigurationRecord {

    private Integer lineNumber;
    private String recordId; // Optional unique ID in the file

    // Required fields
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
    private Double pricingVariance;

    // Business justification
    private String businessJustification;
    private String priority;  // LOW, MEDIUM, HIGH
}
