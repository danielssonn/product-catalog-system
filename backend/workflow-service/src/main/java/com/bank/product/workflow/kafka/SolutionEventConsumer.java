package com.bank.product.workflow.kafka;

import com.bank.product.events.SolutionCreatedEvent;
import com.bank.product.workflow.domain.model.*;
import com.bank.product.workflow.domain.repository.WorkflowSubjectRepository;
import com.bank.product.workflow.domain.service.RuleEvaluationService;
import com.bank.product.workflow.domain.service.WorkflowTemplateService;
import com.bank.product.workflow.temporal.workflow.ApprovalWorkflow;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Kafka consumer that listens to solution.created events
 * and starts Temporal workflows for approval
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SolutionEventConsumer {

    private final WorkflowClient temporalClient;
    private final WorkflowSubjectRepository subjectRepository;
    private final RuleEvaluationService ruleEvaluationService;
    private final WorkflowTemplateService templateService;
    private final ObjectMapper objectMapper;

    @Value("${temporal.workflows.task-queue:workflow-task-queue}")
    private String taskQueue;

    @Value("${workflow.default-timeout-hours:48}")
    private int defaultTimeoutHours;

    /**
     * Handle SolutionCreatedEvent from Kafka
     * This replaces the old HTTP POST /api/v1/workflows/submit endpoint
     */
    @KafkaListener(
            topics = "solution.created",
            groupId = "workflow-service",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleSolutionCreated(
            @Payload String payload,
            @Header(KafkaHeaders.RECEIVED_KEY) String solutionId,
            @Header(value = KafkaHeaders.RECEIVED_PARTITION, required = false) Integer partition,
            @Header(value = KafkaHeaders.OFFSET, required = false) Long offset) {

        log.info("Received SolutionCreatedEvent: solutionId={}, partition={}, offset={}",
                solutionId, partition, offset);

        try {
            // 1. Parse event
            SolutionCreatedEvent event = objectMapper.readValue(payload, SolutionCreatedEvent.class);

            // 2. Check if workflow already exists (idempotency)
            String workflowId = "solution-approval-" + event.getSolutionId();
            if (subjectRepository.findByWorkflowInstanceId(workflowId).isPresent()) {
                log.info("Workflow already exists for solution: {} (idempotent)", solutionId);
                return;  // Skip duplicate
            }

            // 3. Get active template
            String entityType = "SOLUTION_CONFIGURATION";
            WorkflowTemplate template = templateService.getActiveTemplateForEntityType(entityType)
                    .orElseThrow(() -> new IllegalStateException(
                            "No active workflow template for: " + entityType));

            // 4. Build metadata for rule evaluation
            Map<String, Object> entityMetadata = new HashMap<>();
            entityMetadata.put("solutionType", event.getCategory());
            entityMetadata.put("pricingVariance", event.getPricingVariance());
            entityMetadata.put("riskLevel", event.getRiskLevel());
            entityMetadata.put("tenantTier", "STANDARD");

            // 5. Evaluate rules to get approval plan
            ComputedApprovalPlan approvalPlan = ruleEvaluationService.evaluateWithTemplate(
                    template.getTemplateId(),
                    entityMetadata
            );

            // 6. Build entity data
            Map<String, Object> entityData = new HashMap<>();
            entityData.put("solutionId", event.getSolutionId());
            entityData.put("solutionName", event.getSolutionName());
            entityData.put("catalogProductId", event.getCatalogProductId());
            entityData.put("category", event.getCategory());

            // 7. Create workflow subject
            String workflowInstanceId = workflowId;
            WorkflowSubject subject = WorkflowSubject.builder()
                    .workflowId(UUID.randomUUID().toString())
                    .workflowInstanceId(workflowInstanceId)
                    .entityType(entityType)
                    .entityId(event.getSolutionId())
                    .entityData(entityData)
                    .entityMetadata(entityMetadata)
                    .templateId(template.getTemplateId())
                    .state(WorkflowState.INITIATED)
                    .tenantId(event.getTenantId())
                    .initiatedBy(event.getCreatedBy())
                    .initiatedAt(LocalDateTime.now())
                    .approvalPlan(approvalPlan)
                    .businessJustification(event.getBusinessJustification())
                    .priority(event.getPriority() != null ? event.getPriority() : "MEDIUM")
                    .build();

            // 8. Save subject
            subjectRepository.save(subject);

            // 9. Start Temporal workflow
            WorkflowOptions options = WorkflowOptions.newBuilder()
                    .setWorkflowId(workflowInstanceId)
                    .setTaskQueue(taskQueue)
                    .setWorkflowExecutionTimeout(Duration.ofHours(defaultTimeoutHours))
                    .build();

            ApprovalWorkflow workflow = temporalClient.newWorkflowStub(ApprovalWorkflow.class, options);

            // Start workflow asynchronously (non-blocking)
            WorkflowClient.start(workflow::execute, subject);

            log.info("Temporal workflow started: workflowId={}, workflowInstanceId={}, solutionId={}",
                    subject.getWorkflowId(), workflowInstanceId, event.getSolutionId());

        } catch (Exception e) {
            log.error("Failed to start workflow for solution: {}", solutionId, e);
            // Kafka will NOT commit offset → message will be redelivered
            throw new RuntimeException("Workflow start failed", e);
        }
    }
}
