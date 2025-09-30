package com.bank.product.domain.catalog.model;

import lombok.Data;

import java.util.List;

/**
 * Default terms and conditions template for catalog products
 */
@Data
public class CatalogTerms {

    private String termsAndConditionsUrl;

    private String disclosureUrl;

    private Integer minTermLength;

    private Integer maxTermLength;

    private Integer defaultTermLength;

    private String termUnit;

    private boolean allowEarlyWithdrawal;

    private String penaltyDescription;

    private List<String> restrictions;

    private List<String> benefits;
}