package com.bank.product.workflow.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Result of workflow execution
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowResult {
    /**
     * Whether workflow completed successfully
     */
    private boolean success;

    /**
     * Result code
     */
    private String resultCode;

    /**
     * Result message
     */
    private String message;

    /**
     * Timestamp when result was determined
     */
    private LocalDateTime timestamp;

    /**
     * Additional result data
     */
    private Map<String, Object> data;
}
