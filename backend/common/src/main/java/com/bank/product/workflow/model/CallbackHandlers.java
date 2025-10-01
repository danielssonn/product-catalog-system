package com.bank.product.workflow.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Callback handlers for workflow events
 * Values are bean names or fully qualified class names
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CallbackHandlers {

    /**
     * Handler to execute on approval
     * e.g., "com.bank.product.workflow.handlers.SolutionConfigApprovalHandler"
     */
    private String onApprove;

    /**
     * Handler to execute on rejection
     */
    private String onReject;

    /**
     * Handler to execute on timeout
     */
    private String onTimeout;

    /**
     * Handler to execute on cancellation
     */
    private String onCancel;

    /**
     * Handler for pre-submission validation
     */
    private String onValidate;

    /**
     * Handler for workflow initiation
     */
    private String onInitiate;

    /**
     * Handler for workflow completion
     */
    private String onComplete;
}
