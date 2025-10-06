package com.bank.product.workflow.temporal.activity;

import com.bank.product.events.WorkflowCompletedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Implementation of EventPublisherActivity
 * Publishes workflow completion events to Kafka
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EventPublisherActivityImpl implements EventPublisherActivity {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    private static final String TOPIC = "workflow.completed";
    private static final long KAFKA_TIMEOUT_SECONDS = 10;

    @Override
    public void publishWorkflowCompletedEvent(WorkflowCompletedEvent event) {
        try {
            log.info("Publishing WorkflowCompletedEvent: workflowId={}, entityId={}, outcome={}",
                    event.getWorkflowId(), event.getEntityId(), event.getOutcome());

            String payload = objectMapper.writeValueAsString(event);

            // Send to Kafka with timeout
            SendResult<String, String> result = kafkaTemplate.send(
                    TOPIC,
                    event.getEntityId(),  // Key: solutionId (for partitioning)
                    payload               // Value: JSON
            ).get(KAFKA_TIMEOUT_SECONDS, TimeUnit.SECONDS);

            log.info("WorkflowCompletedEvent published: workflowId={}, partition={}, offset={}",
                    event.getWorkflowId(),
                    result.getRecordMetadata().partition(),
                    result.getRecordMetadata().offset());

        } catch (Exception e) {
            log.error("Failed to publish WorkflowCompletedEvent: workflowId={}",
                    event.getWorkflowId(), e);
            // Temporal will retry based on activity retry policy
            throw new RuntimeException("Kafka publish failed", e);
        }
    }
}
