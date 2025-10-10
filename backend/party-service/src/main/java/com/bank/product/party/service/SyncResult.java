package com.bank.product.party.service;

import lombok.Data;

import java.time.Duration;
import java.time.Instant;

/**
 * Result of a sync operation
 */
@Data
public class SyncResult {
    private String sourceSystem;
    private Instant startTime;
    private Instant endTime;
    private Integer totalRecords;
    private Integer processedRecords = 0;
    private Integer createdRecords = 0;
    private Integer mergedRecords = 0;
    private Integer needsReviewRecords = 0;
    private Integer failedRecords = 0;

    public void incrementProcessed() {
        this.processedRecords++;
    }

    public void incrementCreated() {
        this.createdRecords++;
    }

    public void incrementMerged() {
        this.mergedRecords++;
    }

    public void incrementNeedsReview() {
        this.needsReviewRecords++;
    }

    public void incrementFailed() {
        this.failedRecords++;
    }

    public Duration getDuration() {
        if (startTime != null && endTime != null) {
            return Duration.between(startTime, endTime);
        }
        return Duration.ZERO;
    }

    @Override
    public String toString() {
        return String.format(
                "SyncResult{source=%s, total=%d, processed=%d, created=%d, merged=%d, needsReview=%d, failed=%d, duration=%s}",
                sourceSystem, totalRecords, processedRecords, createdRecords, mergedRecords,
                needsReviewRecords, failedRecords, getDuration()
        );
    }
}
