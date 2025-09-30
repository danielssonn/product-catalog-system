package com.bank.productcatalog.common.domain.product.model;

import lombok.Data;

import java.util.List;

@Data
public class ProductTerms {

    private String termsAndConditionsUrl;

    private String disclosureUrl;

    private Integer termLength;

    private String termUnit;

    private boolean earlyWithdrawalPenalty;

    private String penaltyDescription;

    private List<String> restrictions;

    private List<String> benefits;
}