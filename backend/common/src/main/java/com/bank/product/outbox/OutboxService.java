package com.bank.product.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Service for saving events to outbox
 * Must be called within a transaction to ensure atomicity
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxService {

    private final OutboxEventRepository outboxRepository;
    private final ObjectMapper objectMapper;

    /**
     * Save event to outbox (transactional)
     *
     * @param event Domain event object
     * @param topic Kafka topic to publish to
     * @param aggregateType Type of aggregate (e.g., "Solution")
     * @param aggregateId ID of aggregate (e.g., solutionId)
     * @param tenantId Tenant ID
     * @return Saved outbox event
     */
    @Transactional
    public OutboxEvent saveEvent(Object event, String topic, String aggregateType,
                                   String aggregateId, String tenantId) {
        try {
            String eventType = event.getClass().getSimpleName();
            String payload = objectMapper.writeValueAsString(event);

            OutboxEvent outboxEvent = OutboxEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .eventType(eventType)
                    .aggregateType(aggregateType)
                    .aggregateId(aggregateId)
                    .tenantId(tenantId)
                    .payload(payload)
                    .topic(topic)
                    .published(false)
                    .createdAt(LocalDateTime.now())
                    .retryCount(0)
                    .build();

            OutboxEvent saved = outboxRepository.save(outboxEvent);
            log.debug("Event saved to outbox: eventId={}, type={}, aggregate={}",
                    saved.getEventId(), eventType, aggregateId);

            return saved;

        } catch (Exception e) {
            log.error("Failed to save event to outbox: event={}, aggregate={}",
                    event.getClass().getSimpleName(), aggregateId, e);
            throw new RuntimeException("Failed to save event to outbox", e);
        }
    }

    /**
     * Convenience method for simple events
     */
    @Transactional
    public OutboxEvent saveEvent(Object event, String topic, String aggregateId, String tenantId) {
        String aggregateType = extractAggregateType(event.getClass().getSimpleName());
        return saveEvent(event, topic, aggregateType, aggregateId, tenantId);
    }

    /**
     * Extract aggregate type from event name
     * E.g., "SolutionCreatedEvent" -> "Solution"
     */
    private String extractAggregateType(String eventName) {
        if (eventName.endsWith("Event")) {
            eventName = eventName.substring(0, eventName.length() - 5);
        }
        // Remove "Created", "Updated", etc.
        for (String suffix : new String[]{"Created", "Updated", "Deleted", "Completed", "Approved", "Rejected"}) {
            if (eventName.endsWith(suffix)) {
                return eventName.substring(0, eventName.length() - suffix.length());
            }
        }
        return eventName;
    }

    /**
     * Check if event already exists (for idempotency)
     */
    public boolean eventExists(String eventId) {
        return outboxRepository.findByEventId(eventId).isPresent();
    }
}
