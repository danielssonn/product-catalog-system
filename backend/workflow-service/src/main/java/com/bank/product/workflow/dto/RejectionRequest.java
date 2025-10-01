package com.bank.product.workflow.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request to reject a workflow
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RejectionRequest {

    /**
     * User ID of the rejector
     */
    @NotBlank(message = "Rejector ID is required")
    private String rejecterId;

    /**
     * Reason for rejection
     */
    @NotBlank(message = "Rejection reason is required")
    private String reason;

    /**
     * Required changes before resubmission
     */
    private List<String> requiredChanges;
}
