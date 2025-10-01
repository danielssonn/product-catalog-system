package com.bank.product.workflow.model;

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
     * Strategy type
     */
    private StrategyType type;

    /**
     * Strategy configuration
     */
    private Map<String, Object> config;

    /**
     * Fallback strategy if primary fails
     */
    private ApproverSelectionStrategy fallbackStrategy;

    public enum StrategyType {
        /**
         * Select users by role
         */
        ROLE_BASED,

        /**
         * Select users based on entity attributes
         */
        ATTRIBUTE_BASED,

        /**
         * Call external service for assignment
         */
        EXTERNAL_SERVICE,

        /**
         * Execute script (Groovy/JavaScript)
         */
        SCRIPTED,

        /**
         * Round-robin among available approvers
         */
        ROUND_ROBIN,

        /**
         * Least loaded approver
         */
        LOAD_BALANCED,

        /**
         * Based on organizational hierarchy
         */
        HIERARCHICAL
    }
}
