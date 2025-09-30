package com.bank.productcatalog.common.domain.catalog.model;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * Pricing template with configurable ranges for tenant customization
 */
@Data
public class PricingTemplate {

    private PricingType pricingType;

    private String currency;

    // Min/Max ranges for tenant configuration
    private BigDecimal minInterestRate;
    private BigDecimal maxInterestRate;
    private BigDecimal defaultInterestRate;

    private BigDecimal minAnnualFee;
    private BigDecimal maxAnnualFee;
    private BigDecimal defaultAnnualFee;

    private BigDecimal minMonthlyFee;
    private BigDecimal maxMonthlyFee;
    private BigDecimal defaultMonthlyFee;

    private BigDecimal minTransactionFee;
    private BigDecimal maxTransactionFee;
    private BigDecimal defaultTransactionFee;

    private BigDecimal minBalance;
    private BigDecimal recommendedMinBalance;

    private InterestType interestType;

    private List<FeeTemplate> availableFees;

    private List<RateTierTemplate> rateTierTemplates;

    private boolean allowCustomPricing;
}