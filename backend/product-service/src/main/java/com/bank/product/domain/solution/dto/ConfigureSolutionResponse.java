package com.bank.product.domain.solution.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Response after configuring a solution
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfigureSolutionResponse {

    private String solutionId;
    private String solutionName;
    private String status;

    // Workflow information
    private String workflowId;
    private String workflowStatus;
    private boolean approvalRequired;
    private int requiredApprovals;
    private List<String> approverRoles;
    private boolean sequential;
    private int slaHours;
    private LocalDateTime estimatedCompletion;
    private String message;
}
