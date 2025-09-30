package com.bank.product.model;

import lombok.Data;

/**
 * Configurable feature option
 */
@Data
public class FeatureOption {

    private String featureKey;

    private String featureName;

    private String description;

    private boolean enabledByDefault;

    private boolean required;

    private FeatureType type;

    private Object defaultValue;

    private Object minValue;

    private Object maxValue;

    private String[] allowedValues;
}

enum FeatureType {
    BOOLEAN,
    NUMBER,
    STRING,
    ENUM,
    OBJECT
}