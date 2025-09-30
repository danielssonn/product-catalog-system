package com.bank.product.model;

import lombok.Data;

import java.util.List;

@Data
public class Terms {

    private String termsAndConditionsUrl;

    private String disclosureUrl;

    private Integer termLength;

    private String termUnit;

    private boolean earlyWithdrawalPenalty;

    private String penaltyDescription;

    private List<String> restrictions;

    private List<String> benefits;
}