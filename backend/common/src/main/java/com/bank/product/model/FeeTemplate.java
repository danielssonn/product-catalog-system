package com.bank.product.model;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class FeeTemplate {

    private String feeType;

    private String description;

    private BigDecimal minAmount;

    private BigDecimal maxAmount;

    private BigDecimal defaultAmount;

    private String frequency;

    private boolean waivable;

    private String waiverConditions;

    private boolean required;
}