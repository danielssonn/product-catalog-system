package com.bank.product.events;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Base class for all domain events
 * Provides common metadata
 */
@Data
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "@class")
public abstract class DomainEvent {

    /**
     * Unique event identifier
     */
    private final String eventId = UUID.randomUUID().toString();

    /**
     * When the event occurred
     */
    private final LocalDateTime occurredAt = LocalDateTime.now();

    /**
     * Type of event (class name)
     */
    private final String eventType = this.getClass().getSimpleName();

    /**
     * Event version (for schema evolution)
     */
    private final String eventVersion = "1.0";
}
