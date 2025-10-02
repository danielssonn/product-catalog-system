package com.bank.product.workflow.temporal.workflow;

import com.bank.product.workflow.domain.model.*;
import com.bank.product.workflow.temporal.activity.WorkflowActivities;
import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.workflow.Workflow;
import org.slf4j.Logger;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Implementation of generic approval workflow using Temporal
 */
public class ApprovalWorkflowImpl implements ApprovalWorkflow {

    private static final Logger logger = Workflow.getLogger(ApprovalWorkflowImpl.class);

    // Workflow state
    private WorkflowSubject subject;
    private WorkflowState currentState;
    private List<ApprovalDecision> decisions = new ArrayList<>();
    private boolean complete = false;
    private String cancellationReason;

    // Activity stub with retry and timeout configuration
    private final WorkflowActivities activities = Workflow.newActivityStub(
            WorkflowActivities.class,
            ActivityOptions.newBuilder()
                    .setStartToCloseTimeout(Duration.ofMinutes(5))
                    .setRetryOptions(RetryOptions.newBuilder()
                            .setMaximumAttempts(3)
                            .setBackoffCoefficient(2.0)
                            .setInitialInterval(Duration.ofSeconds(1))
                            .build())
                    .build()
    );

    @Override
    public WorkflowResult execute(WorkflowSubject subject) {
        this.subject = subject;
        this.currentState = WorkflowState.INITIATED;

        logger.info("Starting approval workflow for entity: {} ({})",
                subject.getEntityType(), subject.getEntityId());

        try {
            // Step 1: Validate the workflow request
            updateState(WorkflowState.VALIDATION);
            boolean isValid = activities.validateRequest(subject);

            if (!isValid) {
                return failWorkflow("Validation failed");
            }

            // Step 2: Evaluate rules and compute approval plan
            ComputedApprovalPlan approvalPlan = activities.evaluateRules(
                    subject.getTemplateId(),
                    subject.getEntityMetadata()
            );

            subject.setApprovalPlan(approvalPlan);

            // Step 3: Check if approval is required
            if (!approvalPlan.isApprovalRequired()) {
                logger.info("Auto-approval - no approval required");
                updateState(WorkflowState.APPROVED);
                return executeCallback(true);
            }

            // Step 4: Create and assign approval tasks
            updateState(WorkflowState.PENDING_APPROVAL);
            List<ApprovalTask> tasks = activities.createApprovalTasks(subject, approvalPlan);

            // Step 5: Wait for approvals
            if (approvalPlan.isSequential()) {
                handleSequentialApprovals(tasks, approvalPlan);
            } else {
                handleParallelApprovals(tasks, approvalPlan);
            }

            // Step 6: Check workflow outcome
            if (currentState == WorkflowState.REJECTED) {
                return executeCallback(false);
            } else if (currentState == WorkflowState.CANCELLED) {
                return cancelWorkflow();
            }

            // Step 7: All approvals received
            updateState(WorkflowState.APPROVED);
            return executeCallback(true);

        } catch (Exception e) {
            logger.error("Workflow execution failed", e);
            updateState(WorkflowState.FAILED);
            return WorkflowResult.builder()
                    .success(false)
                    .resultCode("WORKFLOW_FAILED")
                    .message("Workflow execution failed: " + e.getMessage())
                    .timestamp(LocalDateTime.now())
                    .build();
        }
    }

    /**
     * Handle sequential approvals (one after another)
     */
    private void handleSequentialApprovals(List<ApprovalTask> tasks, ComputedApprovalPlan plan) {
        logger.info("Handling sequential approvals: {} approvers", tasks.size());

        for (int i = 0; i < tasks.size(); i++) {
            ApprovalTask task = tasks.get(i);
            logger.info("Waiting for approval from: {} (level {})", task.getRequiredRole(), i + 1);

            // Notify approver
            activities.notifyApprover(task);

            // Wait for approval signal with timeout
            Duration timeout = plan.getSla() != null ? plan.getSla() : Duration.ofHours(24);
            boolean approved = Workflow.await(timeout, () -> !decisions.isEmpty() || complete);

            if (!approved || decisions.isEmpty()) {
                // Timeout - handle escalation
                logger.warn("Approval timeout for task: {}", task.getTaskId());
                activities.handleEscalation(task);
                updateState(WorkflowState.TIMEOUT);
                return;
            }

            ApprovalDecision decision = decisions.remove(0);

            if ("REJECT".equals(decision.getDecision())) {
                logger.info("Workflow rejected by: {}", decision.getApproverId());
                updateState(WorkflowState.REJECTED);
                activities.updateTaskStatus(task.getTaskId(), TaskStatus.COMPLETED, decision);
                return;
            }

            // Mark task as completed
            activities.updateTaskStatus(task.getTaskId(), TaskStatus.COMPLETED, decision);
            logger.info("Approval {} of {} completed", i + 1, tasks.size());
        }
    }

    /**
     * Handle parallel approvals (all at once)
     */
    private void handleParallelApprovals(List<ApprovalTask> tasks, ComputedApprovalPlan plan) {
        logger.info("Handling parallel approvals: {} approvers", tasks.size());

        // Notify all approvers
        for (ApprovalTask task : tasks) {
            activities.notifyApprover(task);
        }

        int requiredApprovals = plan.getRequiredApprovals();
        int approvalsReceived = 0;
        Duration timeout = plan.getSla() != null ? plan.getSla() : Duration.ofHours(24);

        // Wait for required number of approvals
        while (approvalsReceived < requiredApprovals) {
            boolean signalReceived = Workflow.await(timeout, () -> !decisions.isEmpty() || complete);

            if (!signalReceived || decisions.isEmpty()) {
                // Timeout
                logger.warn("Approval timeout - received {}/{} approvals",
                        approvalsReceived, requiredApprovals);
                updateState(WorkflowState.TIMEOUT);
                return;
            }

            ApprovalDecision decision = decisions.remove(0);

            if ("REJECT".equals(decision.getDecision())) {
                logger.info("Workflow rejected by: {}", decision.getApproverId());
                updateState(WorkflowState.REJECTED);
                // Mark all pending tasks as cancelled
                for (ApprovalTask task : tasks) {
                    activities.updateTaskStatus(task.getTaskId(), TaskStatus.CANCELLED, null);
                }
                return;
            }

            approvalsReceived++;
            logger.info("Received approval {}/{}", approvalsReceived, requiredApprovals);
        }

        // All required approvals received
        logger.info("All approvals received");
    }

    /**
     * Execute callback handler
     */
    private WorkflowResult executeCallback(boolean approved) {
        try {
            if (approved) {
                logger.info("Executing approval callback");
                activities.executeApprovalCallback(subject);
                updateState(WorkflowState.COMPLETED);

                return WorkflowResult.builder()
                        .success(true)
                        .resultCode("APPROVED")
                        .message("Workflow approved and completed successfully")
                        .timestamp(LocalDateTime.now())
                        .data(new HashMap<>())
                        .build();
            } else {
                logger.info("Executing rejection callback");
                activities.executeRejectionCallback(subject, decisions);
                updateState(WorkflowState.REJECTED);

                return WorkflowResult.builder()
                        .success(false)
                        .resultCode("REJECTED")
                        .message("Workflow was rejected")
                        .timestamp(LocalDateTime.now())
                        .data(new HashMap<>())
                        .build();
            }
        } catch (Exception e) {
            logger.error("Callback execution failed", e);
            return failWorkflow("Callback execution failed: " + e.getMessage());
        } finally {
            complete = true;
        }
    }

    /**
     * Handle workflow cancellation
     */
    private WorkflowResult cancelWorkflow() {
        logger.info("Workflow cancelled: {}", cancellationReason);
        complete = true;

        return WorkflowResult.builder()
                .success(false)
                .resultCode("CANCELLED")
                .message("Workflow was cancelled: " + cancellationReason)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Fail the workflow
     */
    private WorkflowResult failWorkflow(String message) {
        logger.error("Workflow failed: {}", message);
        updateState(WorkflowState.FAILED);
        complete = true;

        return WorkflowResult.builder()
                .success(false)
                .resultCode("FAILED")
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Update workflow state and log
     */
    private void updateState(WorkflowState newState) {
        WorkflowState previousState = currentState;
        currentState = newState;
        subject.setState(newState);

        logger.info("Workflow state transition: {} -> {}", previousState, newState);

        // Record audit log
        activities.recordAuditLog(subject.getWorkflowId(), previousState, newState);
    }

    // Signal handlers

    @Override
    public void approve(ApprovalDecision decision) {
        logger.info("Received approval signal from: {}", decision.getApproverId());
        decision.setDecision("APPROVE");
        decision.setTimestamp(LocalDateTime.now());
        decisions.add(decision);
    }

    @Override
    public void reject(ApprovalDecision decision) {
        logger.info("Received rejection signal from: {}", decision.getApproverId());
        decision.setDecision("REJECT");
        decision.setTimestamp(LocalDateTime.now());
        decisions.add(decision);
    }

    @Override
    public void cancel(String reason) {
        logger.info("Received cancellation signal: {}", reason);
        this.cancellationReason = reason;
        updateState(WorkflowState.CANCELLED);
        complete = true;
    }

    // Query handlers

    @Override
    public WorkflowSubject getStatus() {
        return subject;
    }

    @Override
    public boolean isComplete() {
        return complete;
    }
}
