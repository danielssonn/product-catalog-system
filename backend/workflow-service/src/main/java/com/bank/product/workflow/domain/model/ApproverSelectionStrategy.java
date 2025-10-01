package com.bank.product.workflow.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Strategy for selecting approvers dynamically
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApproverSelectionStrategy {
    /**
     * Strategy type: ROLE_BASED, ATTRIBUTE_BASED, EXTERNAL_SERVICE, SCRIPT
     */
    private String type;

    /**
     * Configuration specific to strategy type
     */
    private Map<String, Object> config;

    /**
     * Human-readable description
     */
    private String description;
}
