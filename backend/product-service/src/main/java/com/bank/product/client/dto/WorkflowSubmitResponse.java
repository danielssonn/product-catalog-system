package com.bank.product.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Response from workflow submission
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowSubmitResponse {

    private String workflowId;
    private String workflowInstanceId;
    private String status;
    private boolean approvalRequired;
    private int requiredApprovals;
    private List<String> approverRoles;
    private boolean sequential;
    private int slaHours;
    private LocalDateTime estimatedCompletion;
    private String message;
}
