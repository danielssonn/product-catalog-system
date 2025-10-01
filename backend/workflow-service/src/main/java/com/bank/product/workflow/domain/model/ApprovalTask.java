package com.bank.product.workflow.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * Approval task assigned to a checker
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
     * Task ID (business identifier)
     */
    @Indexed(unique = true)
    private String taskId;

    /**
     * Workflow this task belongs to
     */
    @Indexed
    private String workflowId;

    /**
     * User assigned to this task
     */
    @Indexed
    private String assignedTo;

    /**
     * Approval level (for sequential approvals)
     */
    private int approvalLevel;

    /**
     * Required role for approval
     */
    private String requiredRole;

    /**
     * Due date
     */
    @Indexed
    private LocalDateTime dueDate;

    /**
     * Task status
     */
    @Indexed
    private TaskStatus status;

    /**
     * Priority: LOW, MEDIUM, HIGH, CRITICAL
     */
    private String priority;

    /**
     * Timestamps
     */
    private LocalDateTime createdAt;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;

    /**
     * Tenant context
     */
    @Indexed
    private String tenantId;

    /**
     * Decision made (if completed)
     */
    private ApprovalDecision decision;
}
