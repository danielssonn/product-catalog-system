package com.bank.product.workflow.domain.service;

import com.bank.product.workflow.domain.model.*;
import com.bank.product.workflow.domain.repository.WorkflowSubjectRepository;
import com.bank.product.workflow.dto.*;
import com.bank.product.workflow.temporal.workflow.ApprovalWorkflow;
import io.temporal.api.enums.v1.WorkflowExecutionStatus;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for executing and managing workflows
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowExecutionService {

    private final WorkflowClient workflowClient;
    private final WorkflowSubjectRepository subjectRepository;
    private final RuleEvaluationService ruleEvaluationService;
    private final WorkflowTemplateService templateService;

    @Value("${temporal.workflows.task-queue:workflow-task-queue}")
    private String taskQueue;

    @Value("${workflow.default-timeout-seconds:3600}")
    private int defaultTimeoutSeconds;

    /**
     * Submit a new workflow for approval
     */
    public WorkflowSubmitResponse submitWorkflow(WorkflowSubmitRequest request) {
        log.info("Submitting workflow for entity: {} ({})", request.getEntityType(), request.getEntityId());

        // Create workflow subject
        String workflowId = UUID.randomUUID().toString();
        String workflowInstanceId = "workflow-" + workflowId;

        // Determine template to use
        String templateId = request.getTemplateId();
        if (templateId == null) {
            WorkflowTemplate activeTemplate = templateService.getActiveTemplateForEntityType(request.getEntityType())
                    .orElseThrow(() -> new IllegalStateException(
                            "No active template found for entity type: " + request.getEntityType()));
            templateId = activeTemplate.getTemplateId();
        }

        // Evaluate rules to get approval plan
        ComputedApprovalPlan approvalPlan = ruleEvaluationService.evaluateWithTemplate(
                templateId,
                request.getEntityMetadata()
        );

        // Create workflow subject
        WorkflowSubject subject = WorkflowSubject.builder()
                .workflowId(workflowId)
                .workflowInstanceId(workflowInstanceId)
                .entityType(request.getEntityType())
                .entityId(request.getEntityId())
                .entityData(request.getEntityData())
                .entityMetadata(request.getEntityMetadata())
                .templateId(templateId)
                .state(WorkflowState.INITIATED)
                .tenantId(request.getTenantId())
                .initiatedBy(request.getInitiatedBy())
                .initiatedAt(LocalDateTime.now())
                .approvalPlan(approvalPlan)
                .businessJustification(request.getBusinessJustification())
                .priority(request.getPriority() != null ? request.getPriority() : "MEDIUM")
                .build();

        // Save subject
        subjectRepository.save(subject);

        // Start Temporal workflow
        WorkflowOptions options = WorkflowOptions.newBuilder()
                .setWorkflowId(workflowInstanceId)
                .setTaskQueue(taskQueue)
                .setWorkflowExecutionTimeout(Duration.ofSeconds(defaultTimeoutSeconds))
                .build();

        ApprovalWorkflow workflow = workflowClient.newWorkflowStub(ApprovalWorkflow.class, options);

        // Start workflow asynchronously
        WorkflowClient.start(workflow::execute, subject);

        log.info("Workflow started: {} (instance: {})", workflowId, workflowInstanceId);

        // Calculate estimated completion
        LocalDateTime estimatedCompletion = LocalDateTime.now();
        if (approvalPlan.getSla() != null) {
            estimatedCompletion = estimatedCompletion.plus(approvalPlan.getSla());
        }

        // Build response
        return WorkflowSubmitResponse.builder()
                .workflowId(workflowId)
                .workflowInstanceId(workflowInstanceId)
                .status(WorkflowState.PENDING_APPROVAL.name())
                .approvalRequired(approvalPlan.isApprovalRequired())
                .requiredApprovals(approvalPlan.getRequiredApprovals())
                .approverRoles(approvalPlan.getApproverRoles())
                .sequential(approvalPlan.isSequential())
                .slaHours(approvalPlan.getSla() != null ? (int) approvalPlan.getSla().toHours() : null)
                .estimatedCompletion(estimatedCompletion)
                .message(approvalPlan.isApprovalRequired() ?
                        "Workflow submitted for approval" :
                        "Auto-approved - no approval required")
                .build();
    }

    /**
     * Approve a workflow
     */
    public void approveWorkflow(String workflowId, ApprovalRequest request) {
        log.info("Approving workflow: {} by {}", workflowId, request.getApproverId());

        WorkflowSubject subject = getWorkflowSubject(workflowId);

        // Get workflow stub
        ApprovalWorkflow workflow = workflowClient.newWorkflowStub(
                ApprovalWorkflow.class,
                subject.getWorkflowInstanceId()
        );

        // Create approval decision
        ApprovalDecision decision = ApprovalDecision.builder()
                .approverId(request.getApproverId())
                .decision("APPROVE")
                .comments(request.getComments())
                .conditions(request.getConditions())
                .timestamp(LocalDateTime.now())
                .build();

        // Signal workflow
        workflow.approve(decision);

        log.info("Approval signal sent to workflow: {}", workflowId);
    }

    /**
     * Reject a workflow
     */
    public void rejectWorkflow(String workflowId, RejectionRequest request) {
        log.info("Rejecting workflow: {} by {}", workflowId, request.getRejecterId());

        WorkflowSubject subject = getWorkflowSubject(workflowId);

        // Get workflow stub
        ApprovalWorkflow workflow = workflowClient.newWorkflowStub(
                ApprovalWorkflow.class,
                subject.getWorkflowInstanceId()
        );

        // Create rejection decision
        ApprovalDecision decision = ApprovalDecision.builder()
                .approverId(request.getRejecterId())
                .decision("REJECT")
                .rejectionReason(request.getReason())
                .requiredChanges(request.getRequiredChanges())
                .timestamp(LocalDateTime.now())
                .build();

        // Signal workflow
        workflow.reject(decision);

        log.info("Rejection signal sent to workflow: {}", workflowId);
    }

    /**
     * Get workflow status
     */
    public WorkflowStatusResponse getWorkflowStatus(String workflowId) {
        log.debug("Getting workflow status: {}", workflowId);

        WorkflowSubject subject = getWorkflowSubject(workflowId);

        // Query workflow for latest status
        try {
            ApprovalWorkflow workflow = workflowClient.newWorkflowStub(
                    ApprovalWorkflow.class,
                    subject.getWorkflowInstanceId()
            );

            WorkflowSubject currentStatus = workflow.getStatus();
            boolean isComplete = workflow.isComplete();

            return WorkflowStatusResponse.builder()
                    .workflowId(currentStatus.getWorkflowId())
                    .entityType(currentStatus.getEntityType())
                    .entityId(currentStatus.getEntityId())
                    .state(currentStatus.getState().name())
                    .initiatedBy(currentStatus.getInitiatedBy())
                    .initiatedAt(currentStatus.getInitiatedAt())
                    .completedAt(currentStatus.getCompletedAt())
                    .complete(isComplete)
                    .errorMessage(currentStatus.getErrorMessage())
                    .build();

        } catch (Exception e) {
            log.warn("Could not query workflow, using database status: {}", e.getMessage());

            // Fallback to database status
            return WorkflowStatusResponse.builder()
                    .workflowId(subject.getWorkflowId())
                    .entityType(subject.getEntityType())
                    .entityId(subject.getEntityId())
                    .state(subject.getState().name())
                    .initiatedBy(subject.getInitiatedBy())
                    .initiatedAt(subject.getInitiatedAt())
                    .completedAt(subject.getCompletedAt())
                    .complete(subject.getState() == WorkflowState.COMPLETED ||
                            subject.getState() == WorkflowState.REJECTED ||
                            subject.getState() == WorkflowState.CANCELLED ||
                            subject.getState() == WorkflowState.FAILED)
                    .errorMessage(subject.getErrorMessage())
                    .build();
        }
    }

    /**
     * Cancel a workflow
     */
    public void cancelWorkflow(String workflowId, String reason) {
        log.info("Cancelling workflow: {} - {}", workflowId, reason);

        WorkflowSubject subject = getWorkflowSubject(workflowId);

        // Get workflow stub
        WorkflowStub workflowStub = workflowClient.newUntypedWorkflowStub(
                subject.getWorkflowInstanceId()
        );

        // Cancel workflow
        workflowStub.cancel();

        // Update database
        subject.setState(WorkflowState.CANCELLED);
        subject.setErrorMessage(reason);
        subject.setCompletedAt(LocalDateTime.now());
        subjectRepository.save(subject);

        log.info("Workflow cancelled: {}", workflowId);
    }

    /**
     * Get workflow subject from database
     */
    private WorkflowSubject getWorkflowSubject(String workflowId) {
        return subjectRepository.findByWorkflowId(workflowId)
                .orElseThrow(() -> new IllegalArgumentException("Workflow not found: " + workflowId));
    }
}
