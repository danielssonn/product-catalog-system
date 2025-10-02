package com.bank.product.version.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Breaking Change Documentation
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BreakingChange {

    /**
     * Type of breaking change
     */
    private BreakingChangeType type;

    /**
     * Affected endpoint or entity
     */
    private String affectedEndpoint;

    /**
     * Field or parameter affected
     */
    private String affectedField;

    /**
     * Description of the change
     */
    private String description;

    /**
     * Migration strategy
     */
    private String migrationStrategy;

    /**
     * Example before change
     */
    private String exampleBefore;

    /**
     * Example after change
     */
    private String exampleAfter;
}
