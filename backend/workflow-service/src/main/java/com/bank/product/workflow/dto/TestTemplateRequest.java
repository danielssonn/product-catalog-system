package com.bank.product.workflow.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Request to test a workflow template with sample metadata
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestTemplateRequest {

    @NotEmpty(message = "Entity metadata is required for testing")
    private Map<String, Object> entityMetadata;

    /**
     * Optional: Specific rule ID to test
     */
    private String ruleId;

    /**
     * Optional: Decision table index to test (if template has multiple tables)
     */
    private Integer tableIndex;
}
