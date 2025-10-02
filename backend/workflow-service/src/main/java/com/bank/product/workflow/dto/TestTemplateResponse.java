package com.bank.product.workflow.dto;

import com.bank.product.workflow.domain.model.ComputedApprovalPlan;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Response from testing a workflow template
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestTemplateResponse {

    private String templateId;

    private String entityType;

    /**
     * Input metadata used for testing
     */
    private Map<String, Object> inputMetadata;

    /**
     * Matched rule IDs
     */
    private List<String> matchedRules;

    /**
     * Computed approval plan
     */
    private ComputedApprovalPlan approvalPlan;

    /**
     * Whether the test passed validation
     */
    private boolean valid;

    /**
     * Validation errors (if any)
     */
    private List<String> validationErrors;

    /**
     * Execution trace for debugging
     */
    private List<String> executionTrace;
}
