package com.bank.product.model;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * Configuration options that tenants can customize
 */
@Data
public class SolutionConfigOptions {

    private boolean canCustomizeName;

    private boolean canCustomizeDescription;

    private boolean canCustomizePricing;

    private boolean canCustomizeFeatures;

    private boolean canCustomizeTerms;

    private boolean canCustomizeEligibility;

    private boolean canSelectChannels;

    private List<String> configurableFields;

    // Feature toggles that tenants can enable/disable
    private Map<String, FeatureOption> featureOptions;

    private Map<String, String> validationRules;
}