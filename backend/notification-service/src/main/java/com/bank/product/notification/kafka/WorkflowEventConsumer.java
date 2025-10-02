package com.bank.product.notification.kafka;

import com.bank.product.notification.domain.model.WorkflowApprovedEvent;
import com.bank.product.notification.domain.model.WorkflowRejectedEvent;
import com.bank.product.notification.domain.service.NotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class WorkflowEventConsumer {

    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "workflow.approved", groupId = "notification-service")
    public void handleWorkflowApproved(Map<String, Object> eventMap) {
        log.info("Received workflow approved event: {}", eventMap.get("workflowId"));

        try {
            WorkflowApprovedEvent event = objectMapper.convertValue(eventMap, WorkflowApprovedEvent.class);
            notificationService.handleWorkflowApproved(event);
            log.info("Successfully processed workflow approved event: workflowId={}", event.getWorkflowId());
        } catch (Exception e) {
            log.error("Error processing workflow approved event: {}", eventMap, e);
            throw e;
        }
    }

    @KafkaListener(topics = "workflow.rejected", groupId = "notification-service")
    public void handleWorkflowRejected(Map<String, Object> eventMap) {
        log.info("Received workflow rejected event: {}", eventMap.get("workflowId"));

        try {
            WorkflowRejectedEvent event = objectMapper.convertValue(eventMap, WorkflowRejectedEvent.class);
            notificationService.handleWorkflowRejected(event);
            log.info("Successfully processed workflow rejected event: workflowId={}", event.getWorkflowId());
        } catch (Exception e) {
            log.error("Error processing workflow rejected event: {}", eventMap, e);
            throw e;
        }
    }
}
