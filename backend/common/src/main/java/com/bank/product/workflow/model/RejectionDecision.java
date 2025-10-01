package com.bank.product.workflow.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Represents a rejection decision made by a checker
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RejectionDecision {

    /**
     * ID of the user making the decision
     */
    private String rejecterId;

    /**
     * When the decision was made
     */
    private LocalDateTime rejectedAt;

    /**
     * Reason for rejection
     */
    private String reason;

    /**
     * Rejection category
     */
    private RejectionCategory category;

    /**
     * Required changes to resubmit
     */
    private List<String> requiredChanges;

    /**
     * Whether this can be resubmitted
     */
    private boolean allowResubmission;

    /**
     * Digital signature (if required)
     */
    private String signature;

    /**
     * IP address of rejecter
     */
    private String ipAddress;

    /**
     * User agent
     */
    private String userAgent;
}
