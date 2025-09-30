package com.bank.product.model;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class PricingDetails {

    private PricingType pricingType;

    private BigDecimal basePrice;

    private String currency;

    private BigDecimal interestRate;

    private InterestType interestType;

    private BigDecimal annualFee;

    private BigDecimal monthlyFee;

    private BigDecimal transactionFee;

    private BigDecimal minimumBalance;

    private List<Fee> additionalFees;

    private List<RateTier> rateTiers;
}