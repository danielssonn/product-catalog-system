package com.bank.product.outbox;

import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for outbox events
 */
@Repository
public interface OutboxEventRepository extends MongoRepository<OutboxEvent, String> {

    /**
     * Find unpublished events ready for publishing
     * Orders by creation time to maintain event ordering
     */
    List<OutboxEvent> findByPublishedFalseAndNextRetryAtBeforeOrderByCreatedAtAsc(
            LocalDateTime now, Pageable pageable);

    /**
     * Find unpublished events (no retry scheduled yet)
     */
    List<OutboxEvent> findByPublishedFalseAndNextRetryAtIsNullOrderByCreatedAtAsc(Pageable pageable);

    /**
     * Find by event ID (for idempotency checks)
     */
    Optional<OutboxEvent> findByEventId(String eventId);

    /**
     * Find events for a specific aggregate
     */
    List<OutboxEvent> findByAggregateTypeAndAggregateIdOrderByCreatedAtAsc(
            String aggregateType, String aggregateId);

    /**
     * Count unpublished events
     */
    long countByPublishedFalse();

    /**
     * Delete old published events (for cleanup)
     */
    void deleteByPublishedTrueAndPublishedAtBefore(LocalDateTime threshold);
}
