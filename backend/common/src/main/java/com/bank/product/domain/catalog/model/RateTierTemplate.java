package com.bank.product.domain.catalog.model;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class RateTierTemplate {

    private String tierName;

    private BigDecimal suggestedMinBalance;

    private BigDecimal suggestedMaxBalance;

    private BigDecimal minInterestRate;

    private BigDecimal maxInterestRate;

    private BigDecimal defaultInterestRate;

    private String description;
}