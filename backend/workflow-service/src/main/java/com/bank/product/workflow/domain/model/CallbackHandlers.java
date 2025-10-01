package com.bank.product.workflow.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Callback handler configuration for entity-specific actions
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CallbackHandlers {
    /**
     * Handler to invoke on approval (bean name or fully qualified class name)
     */
    private String onApprove;

    /**
     * Handler to invoke on rejection
     */
    private String onReject;

    /**
     * Handler to invoke on timeout
     */
    private String onTimeout;

    /**
     * Handler to invoke on cancellation
     */
    private String onCancel;

    /**
     * Handler to invoke for pre-submission validation
     */
    private String onValidate;
}
