package com.bank.product.workflow.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Response after submitting a workflow
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowSubmitResponse {

    /**
     * Workflow ID
     */
    private String workflowId;

    /**
     * Temporal workflow instance ID
     */
    private String workflowInstanceId;

    /**
     * Current workflow status
     */
    private String status;

    /**
     * Whether approval is required
     */
    private boolean approvalRequired;

    /**
     * Number of approvals required
     */
    private int requiredApprovals;

    /**
     * Approver roles
     */
    private List<String> approverRoles;

    /**
     * Sequential or parallel approval
     */
    private boolean sequential;

    /**
     * SLA hours
     */
    private Integer slaHours;

    /**
     * Estimated completion time
     */
    private LocalDateTime estimatedCompletion;

    /**
     * Message
     */
    private String message;
}
