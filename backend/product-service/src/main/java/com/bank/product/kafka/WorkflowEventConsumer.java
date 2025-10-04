package com.bank.product.kafka;

import com.bank.product.domain.solution.model.Solution;
import com.bank.product.domain.solution.model.SolutionStatus;
import com.bank.product.domain.solution.service.impl.SolutionServiceWithOutbox;
import com.bank.product.events.WorkflowCompletedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Kafka consumer that listens to workflow.completed events
 * and updates solution status accordingly
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowEventConsumer {

    private final SolutionServiceWithOutbox solutionService;
    private final ObjectMapper objectMapper;

    /**
     * Handle WorkflowCompletedEvent from Kafka
     * This replaces the old HTTP callback from workflow-service
     */
    @KafkaListener(
            topics = "workflow.completed",
            groupId = "product-service",
            containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void handleWorkflowCompleted(
            @Payload String payload,
            @Header(KafkaHeaders.RECEIVED_KEY) String entityId,
            @Header(value = KafkaHeaders.RECEIVED_PARTITION, required = false) Integer partition,
            @Header(value = KafkaHeaders.OFFSET, required = false) Long offset) {

        log.info("Received WorkflowCompletedEvent: entityId={}, partition={}, offset={}",
                entityId, partition, offset);

        try {
            // 1. Parse event
            WorkflowCompletedEvent event = objectMapper.readValue(payload, WorkflowCompletedEvent.class);

            // 2. Validate event is for a solution
            if (!"SOLUTION_CONFIGURATION".equals(event.getEntityType())) {
                log.debug("Ignoring workflow event for non-solution entity: {}",
                        event.getEntityType());
                return;  // Not for us
            }

            String solutionId = event.getEntityId();
            String workflowId = event.getWorkflowId();
            String outcome = event.getOutcome();

            log.info("Processing workflow completion: solutionId={}, workflowId={}, outcome={}",
                    solutionId, workflowId, outcome);

            // 3. Update solution status based on outcome
            if ("APPROVED".equals(outcome)) {
                handleApproval(solutionId, workflowId, event);
            } else if ("REJECTED".equals(outcome)) {
                handleRejection(solutionId, workflowId, event);
            } else {
                log.warn("Unknown workflow outcome: {}", outcome);
            }

        } catch (Exception e) {
            log.error("Failed to process WorkflowCompletedEvent for entity: {}", entityId, e);
            // Kafka will NOT commit offset → message will be redelivered
            throw new RuntimeException("Workflow event processing failed", e);
        }
    }

    /**
     * Handle workflow approval - activate solution
     */
    private void handleApproval(String solutionId, String workflowId, WorkflowCompletedEvent event) {
        try {
            log.info("Activating solution: solutionId={}, workflowId={}", solutionId, workflowId);

            // Update solution status to ACTIVE (with outbox event)
            Solution solution = solutionService.updateSolutionStatusWithEvent(
                    solutionId,
                    SolutionStatus.ACTIVE,
                    workflowId,
                    "system",
                    String.format("Approved by %d approver(s)",
                            event.getApprovals() != null ? event.getApprovals().size() : 0)
            );

            log.info("Solution activated successfully: solutionId={}, status={}",
                    solution.getId(), solution.getStatus());

        } catch (Exception e) {
            log.error("Failed to activate solution: solutionId={}", solutionId, e);
            throw new RuntimeException("Solution activation failed", e);
        }
    }

    /**
     * Handle workflow rejection - mark solution as rejected
     */
    private void handleRejection(String solutionId, String workflowId, WorkflowCompletedEvent event) {
        try {
            log.info("Rejecting solution: solutionId={}, workflowId={}, reason={}",
                    solutionId, workflowId, event.getRejectionReason());

            // Update solution status to REJECTED (with outbox event)
            Solution solution = solutionService.updateSolutionStatusWithEvent(
                    solutionId,
                    SolutionStatus.REJECTED,
                    workflowId,
                    "system",
                    event.getRejectionReason() != null ? event.getRejectionReason() : "Workflow rejected"
            );

            log.info("Solution rejected successfully: solutionId={}, status={}",
                    solution.getId(), solution.getStatus());

        } catch (Exception e) {
            log.error("Failed to reject solution: solutionId={}", solutionId, e);
            throw new RuntimeException("Solution rejection failed", e);
        }
    }
}
