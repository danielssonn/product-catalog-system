package com.bank.product.workflow.temporal.workflow;

import com.bank.product.events.WorkflowCompletedEvent;
import com.bank.product.workflow.domain.model.*;
import com.bank.product.workflow.temporal.activity.EventPublisherActivity;
import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.failure.CanceledFailure;
import io.temporal.workflow.Workflow;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Temporal Workflow Implementation V2 with Event Publishing
 * This version publishes events to Kafka instead of making HTTP callbacks
 */
@Slf4j
public class ApprovalWorkflowImplV2 implements ApprovalWorkflow {

    // State
    private WorkflowSubject subject;
    private WorkflowState state = WorkflowState.PENDING_APPROVAL;
    private final List<ApprovalDecision> approvals = new ArrayList<>();
    private boolean isComplete = false;

    // Activity stub with retry policy
    private final EventPublisherActivity eventPublisher;

    public ApprovalWorkflowImplV2() {
        // Configure activity with retry policy
        ActivityOptions activityOptions = ActivityOptions.newBuilder()
                .setStartToCloseTimeout(Duration.ofMinutes(5))
                .setRetryOptions(
                        RetryOptions.newBuilder()
                                .setInitialInterval(Duration.ofSeconds(1))
                                .setBackoffCoefficient(2.0)
                                .setMaximumInterval(Duration.ofMinutes(1))
                                .setMaximumAttempts(5)  // 5 retries
                                .build()
                )
                .build();

        this.eventPublisher = Workflow.newActivityStub(EventPublisherActivity.class, activityOptions);
    }

    @Override
    public WorkflowResult execute(WorkflowSubject workflowSubject) {
        this.subject = workflowSubject;

        Workflow.getLogger(ApprovalWorkflowImplV2.class).info(
                "Workflow started: workflowId={}, entityId={}",
                subject.getWorkflowId(), subject.getEntityId());

        ComputedApprovalPlan plan = subject.getApprovalPlan();

        // Check if approval required
        if (!plan.isApprovalRequired()) {
            Workflow.getLogger(ApprovalWorkflowImplV2.class).info(
                    "Auto-approval: No approval required for workflow: {}",
                    subject.getWorkflowId());

            state = WorkflowState.COMPLETED;
            isComplete = true;

            // Publish approved event
            publishCompletedEvent("APPROVED", null);

            return WorkflowResult.builder()
                    .success(true)
                    .resultCode("APPROVED")
                    .message("Auto-approved - no approval required")
                    .timestamp(LocalDateTime.now())
                    .build();
        }

        // Wait for approvals
        Workflow.getLogger(ApprovalWorkflowImplV2.class).info(
                "Waiting for {} approvals: workflowId={}",
                plan.getRequiredApprovals(), subject.getWorkflowId());

        try {
            // Wait for required approvals or timeout
            Duration timeout = plan.getSla() != null ? plan.getSla() : Duration.ofHours(48);

            boolean approved = Workflow.await(
                    timeout,
                    () -> approvals.size() >= plan.getRequiredApprovals()
            );

            if (approved) {
                // All approvals received
                Workflow.getLogger(ApprovalWorkflowImplV2.class).info(
                        "All approvals received: workflowId={}, count={}",
                        subject.getWorkflowId(), approvals.size());

                state = WorkflowState.COMPLETED;
                isComplete = true;

                // Publish approved event
                publishCompletedEvent("APPROVED", null);

                return WorkflowResult.builder()
                        .success(true)
                        .resultCode("APPROVED")
                        .message("All approvals received")
                        .timestamp(LocalDateTime.now())
                        .build();

            } else {
                // Timeout
                Workflow.getLogger(ApprovalWorkflowImplV2.class).warn(
                        "Workflow timed out: workflowId={}",
                        subject.getWorkflowId());

                state = WorkflowState.REJECTED;
                isComplete = true;

                // Publish rejected event (timeout)
                publishCompletedEvent("REJECTED", "Workflow timed out - no response within SLA");

                return WorkflowResult.builder()
                        .success(false)
                        .resultCode("TIMEOUT")
                        .message("Workflow timed out - no response within SLA")
                        .timestamp(LocalDateTime.now())
                        .build();
            }

        } catch (CanceledFailure e) {
            // Workflow cancelled (rejection)
            Workflow.getLogger(ApprovalWorkflowImplV2.class).info(
                    "Workflow rejected: workflowId={}",
                    subject.getWorkflowId());

            state = WorkflowState.REJECTED;
            isComplete = true;

            // Extract rejection reason
            String rejectionReason = approvals.stream()
                    .filter(a -> a.getRejectionReason() != null)
                    .map(ApprovalDecision::getRejectionReason)
                    .findFirst()
                    .orElse("Workflow rejected");

            // Publish rejected event
            publishCompletedEvent("REJECTED", rejectionReason);

            return WorkflowResult.builder()
                    .success(false)
                    .resultCode("REJECTED")
                    .message(rejectionReason)
                    .timestamp(LocalDateTime.now())
                    .build();
        }
    }

    @Override
    public void approve(ApprovalDecision decision) {
        if (state != WorkflowState.PENDING_APPROVAL) {
            throw new IllegalStateException("Workflow not in pending state: " + state);
        }

        Workflow.getLogger(ApprovalWorkflowImplV2.class).info(
                "Approval received: workflowId={}, approver={}, count={}/{}",
                subject.getWorkflowId(),
                decision.getApproverId(),
                approvals.size() + 1,
                subject.getApprovalPlan().getRequiredApprovals());

        decision.setTimestamp(LocalDateTime.now());
        decision.setDecision("APPROVE");
        approvals.add(decision);
    }

    @Override
    public void reject(ApprovalDecision decision) {
        if (state != WorkflowState.PENDING_APPROVAL) {
            throw new IllegalStateException("Workflow not in pending state: " + state);
        }

        Workflow.getLogger(ApprovalWorkflowImplV2.class).info(
                "Rejection received: workflowId={}, rejecter={}",
                subject.getWorkflowId(),
                decision.getApproverId());

        decision.setTimestamp(LocalDateTime.now());
        decision.setDecision("REJECT");
        approvals.add(decision);

        // Cancel workflow (triggers CanceledFailure)
        throw Workflow.wrap(new CanceledFailure("Workflow rejected by: " + decision.getApproverId()));
    }

    @Override
    public void cancel(String reason) {
        if (state != WorkflowState.PENDING_APPROVAL) {
            throw new IllegalStateException("Workflow not in pending state: " + state);
        }

        Workflow.getLogger(ApprovalWorkflowImplV2.class).info(
                "Cancellation received: workflowId={}, reason={}",
                subject.getWorkflowId(), reason);

        state = WorkflowState.CANCELLED;
        isComplete = true;

        // Cancel workflow
        throw Workflow.wrap(new CanceledFailure("Workflow cancelled: " + reason));
    }

    @Override
    public WorkflowSubject getStatus() {
        subject.setState(state);
        if (isComplete) {
            subject.setCompletedAt(LocalDateTime.now());
        }
        return subject;
    }

    @Override
    public boolean isComplete() {
        return isComplete;
    }

    /**
     * Publish WorkflowCompletedEvent via Temporal activity
     * Temporal will retry if Kafka is unavailable
     */
    private void publishCompletedEvent(String outcome, String rejectionReason) {
        // Convert domain model approvals to common model approvals
        List<com.bank.product.workflow.model.ApprovalDecision> commonApprovals = approvals.stream()
                .map(a -> {
                    // Map decision string to DecisionType enum
                    com.bank.product.workflow.model.DecisionType decisionType =
                            "APPROVE".equals(a.getDecision())
                                ? com.bank.product.workflow.model.DecisionType.APPROVE
                                : com.bank.product.workflow.model.DecisionType.REQUEST_MORE_INFO;

                    return com.bank.product.workflow.model.ApprovalDecision.builder()
                            .approverId(a.getApproverId())
                            .decisionType(decisionType)
                            .decidedAt(a.getTimestamp())
                            .comments(a.getComments())
                            .conditions(a.getConditions())
                            .build();
                })
                .collect(Collectors.toList());

        WorkflowCompletedEvent event = WorkflowCompletedEvent.builder()
                .workflowId(subject.getWorkflowId())
                .workflowInstanceId(subject.getWorkflowInstanceId())
                .entityType(subject.getEntityType())
                .entityId(subject.getEntityId())
                .tenantId(subject.getTenantId())
                .outcome(outcome)
                .approvals(commonApprovals)
                .completedAt(LocalDateTime.now())
                .rejectionReason(rejectionReason)
                .build();

        // Publish via activity (Temporal handles retries)
        eventPublisher.publishWorkflowCompletedEvent(event);

        Workflow.getLogger(ApprovalWorkflowImplV2.class).info(
                "WorkflowCompletedEvent published: workflowId={}, outcome={}",
                subject.getWorkflowId(), outcome);
    }
}
