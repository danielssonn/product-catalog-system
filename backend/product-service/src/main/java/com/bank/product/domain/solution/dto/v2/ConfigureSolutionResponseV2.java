package com.bank.product.domain.solution.dto.v2;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * V2 Response after configuring a solution
 * Added metadata field for extensibility
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfigureSolutionResponseV2 {

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

    // New in v2: Additional metadata
    private Map<String, Object> metadata;
}
