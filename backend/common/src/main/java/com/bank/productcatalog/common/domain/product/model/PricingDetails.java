package com.bank.productcatalog.common.domain.product.model;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * Actual pricing details for a tenant's product instance
 */
@Data
public class PricingDetails {

    private String pricingType;

    private BigDecimal basePrice;

    private String currency;

    private BigDecimal interestRate;

    private String interestType;

    private BigDecimal annualFee;

    private BigDecimal monthlyFee;

    private BigDecimal transactionFee;

    private BigDecimal minimumBalance;

    private List<Fee> additionalFees;

    private List<RateTier> rateTiers;
}