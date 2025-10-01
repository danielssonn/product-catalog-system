package com.bank.product.workflow.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request to approve a workflow
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalRequest {

    /**
     * User ID of the approver
     */
    @NotBlank(message = "Approver ID is required")
    private String approverId;

    /**
     * Comments from approver
     */
    private String comments;

    /**
     * Conditions attached to approval
     */
    private List<String> conditions;
}
