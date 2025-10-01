package com.bank.product.workflow.temporal.activity;

import com.bank.product.workflow.domain.model.*;
import com.bank.product.workflow.domain.repository.ApprovalTaskRepository;
import com.bank.product.workflow.domain.repository.WorkflowAuditLogRepository;
import com.bank.product.workflow.domain.repository.WorkflowSubjectRepository;
import com.bank.product.workflow.domain.service.RuleEvaluationService;
import com.bank.product.workflow.domain.service.WorkflowCallbackHandler;
import com.bank.product.workflow.domain.service.WorkflowHandlerRegistry;
import com.bank.product.workflow.kafka.WorkflowEventProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Implementation of Temporal workflow activities
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WorkflowActivitiesImpl implements WorkflowActivities {

    private final RuleEvaluationService ruleEvaluationService;
    private final ApprovalTaskRepository taskRepository;
    private final WorkflowAuditLogRepository auditLogRepository;
    private final WorkflowSubjectRepository subjectRepository;
    private final WorkflowHandlerRegistry handlerRegistry;
    private final WorkflowEventProducer eventProducer;

    @Override
    public boolean validateRequest(WorkflowSubject subject) {
        log.info("Validating workflow request for entity: {}", subject.getEntityId());

        // Basic validation
        if (subject.getEntityType() == null || subject.getEntityType().isEmpty()) {
            log.error("Entity type is required");
            return false;
        }

        if (subject.getEntityId() == null || subject.getEntityId().isEmpty()) {
            log.error("Entity ID is required");
            return false;
        }

        if (subject.getEntityMetadata() == null || subject.getEntityMetadata().isEmpty()) {
            log.warn("Entity metadata is empty - rule evaluation may not work correctly");
        }

        // Save subject to database
        subject.setInitiatedAt(LocalDateTime.now());
        subjectRepository.save(subject);

        log.info("Workflow request validated successfully");
        return true;
    }

    @Override
    public ComputedApprovalPlan evaluateRules(String templateId, Map<String, Object> entityMetadata) {
        log.info("Evaluating rules with template: {}", templateId);

        try {
            ComputedApprovalPlan plan = ruleEvaluationService.evaluateWithTemplate(templateId, entityMetadata);
            log.info("Rules evaluated: approvalRequired={}, requiredApprovals={}",
                    plan.isApprovalRequired(), plan.getRequiredApprovals());
            return plan;
        } catch (Exception e) {
            log.error("Rule evaluation failed", e);
            throw new RuntimeException("Rule evaluation failed: " + e.getMessage(), e);
        }
    }

    @Override
    public List<ApprovalTask> createApprovalTasks(WorkflowSubject subject, ComputedApprovalPlan approvalPlan) {
        log.info("Creating approval tasks for workflow: {}", subject.getWorkflowId());

        List<ApprovalTask> tasks = new ArrayList<>();
        List<String> approverRoles = approvalPlan.getApproverRoles();

        if (approverRoles == null || approverRoles.isEmpty()) {
            log.warn("No approver roles specified in approval plan");
            return tasks;
        }

        LocalDateTime dueDate = LocalDateTime.now().plus(
                approvalPlan.getSla() != null ? approvalPlan.getSla() : java.time.Duration.ofHours(24)
        );

        for (int i = 0; i < approverRoles.size(); i++) {
            String role = approverRoles.get(i);

            ApprovalTask task = ApprovalTask.builder()
                    .taskId(UUID.randomUUID().toString())
                    .workflowId(subject.getWorkflowId())
                    .requiredRole(role)
                    .approvalLevel(i + 1)
                    .status(i == 0 && approvalPlan.isSequential() ? TaskStatus.PENDING : TaskStatus.PENDING)
                    .priority(subject.getPriority() != null ? subject.getPriority() : "MEDIUM")
                    .dueDate(dueDate)
                    .createdAt(LocalDateTime.now())
                    .tenantId(subject.getTenantId())
                    .build();

            // Assign to specific approver if available
            if (approvalPlan.getSpecificApprovers() != null && i < approvalPlan.getSpecificApprovers().size()) {
                task.setAssignedTo(approvalPlan.getSpecificApprovers().get(i));
            }

            taskRepository.save(task);
            tasks.add(task);

            log.info("Created task: {} for role: {}", task.getTaskId(), role);
        }

        return tasks;
    }

    @Override
    public void notifyApprover(ApprovalTask task) {
        log.info("Notifying approver for task: {} (role: {})", task.getTaskId(), task.getRequiredRole());

        // TODO: Integration with notification service
        // For now, just log the notification
        log.info("Notification sent to role: {} for task: {}", task.getRequiredRole(), task.getTaskId());
    }

    @Override
    public void updateTaskStatus(String taskId, TaskStatus status, ApprovalDecision decision) {
        log.info("Updating task status: {} -> {}", taskId, status);

        ApprovalTask task = taskRepository.findByTaskId(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));

        task.setStatus(status);
        task.setCompletedAt(LocalDateTime.now());

        if (decision != null) {
            task.setDecision(decision);
        }

        taskRepository.save(task);
        log.info("Task status updated successfully");
    }

    @Override
    public void handleEscalation(ApprovalTask task) {
        log.warn("Handling escalation for overdue task: {}", task.getTaskId());

        // TODO: Implement escalation logic
        // - Send notification to escalation contact
        // - Create new task for escalated approver
        // - Update original task status

        log.info("Escalation handled for task: {}", task.getTaskId());
    }

    @Override
    public void executeApprovalCallback(WorkflowSubject subject) {
        log.info("Executing approval callback for entity: {} ({})",
                subject.getEntityType(), subject.getEntityId());

        try {
            // Get callback handler from registry
            WorkflowCallbackHandler handler = handlerRegistry.getHandler("onApprove", subject.getEntityType());

            if (handler != null) {
                handler.handle(subject);
                log.info("Approval callback executed successfully");
            } else {
                log.warn("No approval callback handler found for entity type: {}", subject.getEntityType());
            }

            // Update subject status
            subject.setState(WorkflowState.COMPLETED);
            subject.setCompletedAt(LocalDateTime.now());
            subjectRepository.save(subject);

            // Publish Kafka event for workflow approved
            List<ApprovalDecision> approvals = new ArrayList<>();
            eventProducer.publishWorkflowApproved(subject, approvals);

        } catch (Exception e) {
            log.error("Approval callback execution failed", e);
            throw new RuntimeException("Approval callback failed: " + e.getMessage(), e);
        }
    }

    @Override
    public void executeRejectionCallback(WorkflowSubject subject, List<ApprovalDecision> decisions) {
        log.info("Executing rejection callback for entity: {} ({})",
                subject.getEntityType(), subject.getEntityId());

        try {
            // Get callback handler from registry
            WorkflowCallbackHandler handler = handlerRegistry.getHandler("onReject", subject.getEntityType());

            if (handler != null) {
                handler.handle(subject);
                log.info("Rejection callback executed successfully");
            } else {
                log.warn("No rejection callback handler found for entity type: {}", subject.getEntityType());
            }

            // Update subject status with rejection reason
            subject.setState(WorkflowState.REJECTED);
            subject.setCompletedAt(LocalDateTime.now());

            if (!decisions.isEmpty()) {
                ApprovalDecision lastDecision = decisions.get(decisions.size() - 1);
                subject.setErrorMessage(lastDecision.getRejectionReason());
            }

            subjectRepository.save(subject);

            // Publish Kafka event for workflow rejected
            eventProducer.publishWorkflowRejected(subject, decisions);

        } catch (Exception e) {
            log.error("Rejection callback execution failed", e);
            throw new RuntimeException("Rejection callback failed: " + e.getMessage(), e);
        }
    }

    @Override
    public void recordAuditLog(String workflowId, WorkflowState previousState, WorkflowState newState) {
        log.debug("Recording audit log: {} -> {} for workflow: {}", previousState, newState, workflowId);

        WorkflowAuditLog auditLog = WorkflowAuditLog.builder()
                .workflowId(workflowId)
                .timestamp(LocalDateTime.now())
                .action("STATE_CHANGE")
                .previousState(previousState)
                .newState(newState)
                .metadata(new HashMap<>())
                .build();

        auditLogRepository.save(auditLog);
    }
}
