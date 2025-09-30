package com.bank.product.model;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class Fee {

    private String feeType;

    private String description;

    private BigDecimal amount;

    private String frequency;

    private boolean waivable;

    private String waiverConditions;
}