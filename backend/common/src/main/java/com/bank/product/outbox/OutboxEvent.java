package com.bank.product.outbox;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * Transactional Outbox Pattern: Ensures events are published exactly once
 * Events are saved in the same transaction as domain entities
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "outbox_events")
@CompoundIndex(name = "published_created_idx", def = "{'published': 1, 'createdAt': 1}")
@CompoundIndex(name = "eventType_aggregateId_idx", def = "{'eventType': 1, 'aggregateId': 1}")
public class OutboxEvent {

    @Id
    private String id;

    /**
     * Unique event identifier (UUID)
     */
    @Indexed(unique = true)
    private String eventId;

    /**
     * Type of event (e.g., "SolutionCreated", "WorkflowCompleted")
     */
    @Indexed
    private String eventType;

    /**
     * Type of aggregate (e.g., "Solution", "Workflow")
     */
    private String aggregateType;

    /**
     * ID of the aggregate (e.g., solutionId, workflowId)
     */
    @Indexed
    private String aggregateId;

    /**
     * Tenant ID for multi-tenancy
     */
    @Indexed
    private String tenantId;

    /**
     * Event payload (JSON)
     */
    private String payload;

    /**
     * Kafka topic to publish to
     */
    private String topic;

    /**
     * Publishing status
     */
    @Indexed
    private boolean published;

    /**
     * When the event was created
     */
    @Indexed
    private LocalDateTime createdAt;

    /**
     * When the event was successfully published
     */
    private LocalDateTime publishedAt;

    /**
     * Number of publish attempts
     */
    @Builder.Default
    private int retryCount = 0;

    /**
     * Last error message (if publish failed)
     */
    private String lastError;

    /**
     * Next retry time (for exponential backoff)
     */
    private LocalDateTime nextRetryAt;
}
