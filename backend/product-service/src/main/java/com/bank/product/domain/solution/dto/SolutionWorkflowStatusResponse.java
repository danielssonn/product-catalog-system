package com.bank.product.domain.solution.dto;

import com.bank.product.domain.solution.model.SolutionStatus;
import com.bank.product.enums.WorkflowSubmissionStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;

/**
 * Response DTO for workflow submission status
 * Provides polling guidance and workflow metadata
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SolutionWorkflowStatusResponse {

    /**
     * Solution identifier
     */
    private String solutionId;

    /**
     * Current solution status (DRAFT, ACTIVE, REJECTED, etc.)
     */
    private SolutionStatus solutionStatus;

    /**
     * Workflow submission status (tracks async gap)
     */
    private WorkflowSubmissionStatus workflowSubmissionStatus;

    /**
     * Workflow ID (populated after submission completes)
     */
    private String workflowId;

    /**
     * Whether approval is required for this solution
     */
    private Boolean approvalRequired;

    /**
     * Number of required approvals
     */
    private Integer requiredApprovals;

    /**
     * Roles of approvers
     */
    private List<String> approverRoles;

    /**
     * Whether approvals must be sequential
     */
    private Boolean sequential;

    /**
     * SLA in hours
     */
    private Integer slaHours;

    /**
     * Estimated workflow completion time
     */
    private Instant estimatedCompletion;

    /**
     * Error message if submission failed
     */
    private String errorMessage;

    /**
     * When to retry if submission failed
     */
    private Instant retryAt;

    /**
     * URL to poll for status updates (provided while PENDING_SUBMISSION)
     */
    private String pollUrl;

    /**
     * Recommended polling interval in milliseconds
     */
    private Integer pollIntervalMs;

    /**
     * Human-readable message
     */
    private String message;
}
