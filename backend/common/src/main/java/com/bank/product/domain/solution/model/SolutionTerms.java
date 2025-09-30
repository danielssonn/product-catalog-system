package com.bank.product.domain.solution.model;

import lombok.Data;

import java.util.List;

@Data
public class SolutionTerms {

    private String termsAndConditionsUrl;

    private String disclosureUrl;

    private Integer termLength;

    private String termUnit;

    private boolean earlyWithdrawalPenalty;

    private String penaltyDescription;

    private List<String> restrictions;

    private List<String> benefits;
}