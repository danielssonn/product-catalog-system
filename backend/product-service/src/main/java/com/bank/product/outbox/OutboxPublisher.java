package com.bank.product.outbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Outbox Publisher - Polls outbox table and publishes events to Kafka
 * Ensures at-least-once delivery semantics
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxPublisher {

    private final OutboxEventRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    private static final int BATCH_SIZE = 10;
    private static final int MAX_RETRIES = 10;
    private static final long KAFKA_TIMEOUT_SECONDS = 10;

    /**
     * Publish unpublished events to Kafka
     * Runs every 100ms for low latency
     */
    @Scheduled(fixedDelay = 100)
    public void publishEvents() {
        try {
            // Find events ready for publishing
            List<OutboxEvent> readyEvents = outboxRepository
                    .findByPublishedFalseAndNextRetryAtIsNullOrderByCreatedAtAsc(
                            PageRequest.of(0, BATCH_SIZE));

            // Find events ready for retry
            List<OutboxEvent> retryEvents = outboxRepository
                    .findByPublishedFalseAndNextRetryAtBeforeOrderByCreatedAtAsc(
                            LocalDateTime.now(), PageRequest.of(0, BATCH_SIZE));

            // Combine both lists
            readyEvents.addAll(retryEvents);

            if (readyEvents.isEmpty()) {
                return;
            }

            log.debug("Publishing {} outbox events", readyEvents.size());

            for (OutboxEvent event : readyEvents) {
                publishEvent(event);
            }

        } catch (Exception e) {
            log.error("Error in outbox publisher", e);
        }
    }

    /**
     * Publish single event to Kafka
     */
    private void publishEvent(OutboxEvent event) {
        try {
            log.debug("Publishing event: eventId={}, type={}, topic={}",
                    event.getEventId(), event.getEventType(), event.getTopic());

            // Send to Kafka (async with timeout)
            CompletableFuture<SendResult<String, String>> future = kafkaTemplate.send(
                    event.getTopic(),
                    event.getAggregateId(),  // Key: for partitioning
                    event.getPayload()        // Value: JSON payload
            );

            // Wait for acknowledgment
            SendResult<String, String> result = future.get(KAFKA_TIMEOUT_SECONDS, TimeUnit.SECONDS);

            // Mark as published
            markAsPublished(event);

            log.info("Event published successfully: eventId={}, type={}, partition={}",
                    event.getEventId(), event.getEventType(),
                    result.getRecordMetadata().partition());

        } catch (Exception e) {
            log.error("Failed to publish event: eventId={}, attempt={}",
                    event.getEventId(), event.getRetryCount() + 1, e);

            handlePublishFailure(event, e);
        }
    }

    /**
     * Mark event as published (transactional)
     */
    @Transactional
    protected void markAsPublished(OutboxEvent event) {
        event.setPublished(true);
        event.setPublishedAt(LocalDateTime.now());
        event.setLastError(null);
        outboxRepository.save(event);
    }

    /**
     * Handle publish failure with exponential backoff
     */
    @Transactional
    protected void handlePublishFailure(OutboxEvent event, Exception error) {
        event.setRetryCount(event.getRetryCount() + 1);
        event.setLastError(error.getMessage());

        if (event.getRetryCount() >= MAX_RETRIES) {
            log.error("Event exceeded max retries ({}): eventId={}, type={}",
                    MAX_RETRIES, event.getEventId(), event.getEventType());
            // Keep trying but log as error
            // In production, consider moving to dead-letter queue
        }

        // Exponential backoff: 1s, 2s, 4s, 8s, 16s, ... max 5 minutes
        long backoffSeconds = Math.min(
                (long) Math.pow(2, event.getRetryCount()),
                300
        );
        event.setNextRetryAt(LocalDateTime.now().plusSeconds(backoffSeconds));

        outboxRepository.save(event);

        log.warn("Event scheduled for retry in {}s: eventId={}, attempt={}",
                backoffSeconds, event.getEventId(), event.getRetryCount());
    }

    /**
     * Cleanup old published events (runs daily)
     * Keeps events for 7 days for debugging/audit
     */
    @Scheduled(cron = "0 0 2 * * *")  // 2 AM daily
    @Transactional
    public void cleanupOldEvents() {
        try {
            LocalDateTime threshold = LocalDateTime.now().minusDays(7);
            long countBefore = outboxRepository.count();

            outboxRepository.deleteByPublishedTrueAndPublishedAtBefore(threshold);

            long countAfter = outboxRepository.count();
            log.info("Outbox cleanup completed: deleted {} events",
                    countBefore - countAfter);

        } catch (Exception e) {
            log.error("Error during outbox cleanup", e);
        }
    }

    /**
     * Get metrics for monitoring
     */
    public OutboxMetrics getMetrics() {
        long unpublishedCount = outboxRepository.countByPublishedFalse();
        long totalCount = outboxRepository.count();

        return OutboxMetrics.builder()
                .unpublishedCount(unpublishedCount)
                .totalCount(totalCount)
                .publishedCount(totalCount - unpublishedCount)
                .build();
    }

    @lombok.Data
    @lombok.Builder
    public static class OutboxMetrics {
        private long unpublishedCount;
        private long publishedCount;
        private long totalCount;
    }
}
