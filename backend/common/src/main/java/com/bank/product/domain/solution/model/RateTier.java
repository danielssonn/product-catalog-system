package com.bank.product.domain.solution.model;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class RateTier {

    private BigDecimal minBalance;

    private BigDecimal maxBalance;

    private BigDecimal interestRate;

    private String description;
}