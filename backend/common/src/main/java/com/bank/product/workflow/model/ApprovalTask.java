package com.bank.product.workflow.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Represents an approval task assigned to a checker
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "approval_tasks")
public class ApprovalTask {

    @Id
    private String id;

    /**
     * Unique task identifier
     */
    private String taskId;

    /**
     * Associated workflow ID
     */
    private String workflowId;

    /**
     * Workflow type
     */
    private WorkflowType workflowType;

    /**
     * User assigned to approve (checker)
     */
    private String assignedTo;

    /**
     * Role of the approver
     */
    private String assignedRole;

    /**
     * Approval level (for multi-level approvals)
     */
    private int approvalLevel;

    /**
     * Required role for this approval
     */
    private String requiredRole;

    /**
     * Task status
     */
    private TaskStatus status;

    /**
     * Priority
     */
    private Priority priority;

    /**
     * Tenant context
     */
    private String tenantId;

    /**
     * When the task was created
     */
    private LocalDateTime createdAt;

    /**
     * When the task is due
     */
    private LocalDateTime dueDate;

    /**
     * When the task was completed
     */
    private LocalDateTime completedAt;

    /**
     * Decision made (if completed)
     */
    private ApprovalDecision decision;

    /**
     * Summary of the request
     */
    private String requestSummary;

    /**
     * Full request data reference
     */
    private String requestDataRef;

    /**
     * Reminder timestamps
     */
    private List<LocalDateTime> reminders;

    /**
     * Whether this task has been escalated
     */
    private boolean escalated;

    /**
     * Original assignee if escalated
     */
    private String originalAssignee;
}
