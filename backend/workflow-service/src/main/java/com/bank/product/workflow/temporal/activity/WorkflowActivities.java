package com.bank.product.workflow.temporal.activity;

import com.bank.product.workflow.domain.model.*;
import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

import java.util.List;
import java.util.Map;

/**
 * Temporal activities for workflow execution steps
 */
@ActivityInterface
public interface WorkflowActivities {

    /**
     * Validate workflow request
     *
     * @param subject workflow subject
     * @return true if valid
     */
    @ActivityMethod
    boolean validateRequest(WorkflowSubject subject);

    /**
     * Evaluate rules to compute approval plan
     *
     * @param templateId template to use
     * @param entityMetadata metadata for evaluation
     * @return computed approval plan
     */
    @ActivityMethod
    ComputedApprovalPlan evaluateRules(String templateId, Map<String, Object> entityMetadata);

    /**
     * Create approval tasks
     *
     * @param subject workflow subject
     * @param approvalPlan computed approval plan
     * @return list of created tasks
     */
    @ActivityMethod
    List<ApprovalTask> createApprovalTasks(WorkflowSubject subject, ComputedApprovalPlan approvalPlan);

    /**
     * Notify approver of pending task
     *
     * @param task approval task
     */
    @ActivityMethod
    void notifyApprover(ApprovalTask task);

    /**
     * Update task status
     *
     * @param taskId task ID
     * @param status new status
     * @param decision approval decision (if applicable)
     */
    @ActivityMethod
    void updateTaskStatus(String taskId, TaskStatus status, ApprovalDecision decision);

    /**
     * Handle escalation for overdue task
     *
     * @param task task to escalate
     */
    @ActivityMethod
    void handleEscalation(ApprovalTask task);

    /**
     * Execute approval callback
     *
     * @param subject workflow subject
     */
    @ActivityMethod
    void executeApprovalCallback(WorkflowSubject subject);

    /**
     * Execute rejection callback
     *
     * @param subject workflow subject
     * @param decisions rejection decisions
     */
    @ActivityMethod
    void executeRejectionCallback(WorkflowSubject subject, List<ApprovalDecision> decisions);

    /**
     * Record audit log entry
     *
     * @param workflowId workflow ID
     * @param previousState previous state
     * @param newState new state
     */
    @ActivityMethod
    void recordAuditLog(String workflowId, WorkflowState previousState, WorkflowState newState);
}
