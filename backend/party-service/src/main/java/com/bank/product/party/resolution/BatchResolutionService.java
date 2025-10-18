package com.bank.product.party.resolution;

import com.bank.product.party.domain.Party;
import com.bank.product.party.domain.PartyStatus;
import com.bank.product.party.repository.PartyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Batch entity resolution service for processing large volumes of parties.
 *
 * Implements chunk-based parallel processing for optimal performance:
 * - Chunks of 1000 parties
 * - 10 parallel threads
 * - Progress tracking in MongoDB
 * - Target: 10K parties in <10 minutes
 *
 * Based on ENTITY_RESOLUTION_DESIGN.md Phase 4A implementation.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BatchResolutionService {

    private final PartyRepository partyRepository;
    private final EntityResolutionService entityResolutionService;

    // Configuration
    private static final int CHUNK_SIZE = 1000;
    private static final int MAX_PARALLEL_THREADS = 10;

    /**
     * Process all unresolved parties in batch mode
     *
     * @return BatchResolutionResult with statistics
     */
    @Async
    @Transactional
    public CompletableFuture<BatchResolutionResult> resolveAllParties() {
        log.info("Starting batch resolution of all parties");
        Instant startTime = Instant.now();

        // Find all parties that need resolution
        List<Party> unresolvedParties = partyRepository.findByStatus(PartyStatus.ACTIVE);
        log.info("Found {} parties to process", unresolvedParties.size());

        // Split into chunks
        List<List<Party>> chunks = chunkList(unresolvedParties, CHUNK_SIZE);
        log.info("Split into {} chunks of size {}", chunks.size(), CHUNK_SIZE);

        // Statistics tracking
        AtomicInteger totalProcessed = new AtomicInteger(0);
        AtomicInteger autoMerged = new AtomicInteger(0);
        AtomicInteger needsReview = new AtomicInteger(0);
        AtomicInteger noMatchFound = new AtomicInteger(0);
        AtomicInteger errors = new AtomicInteger(0);

        // Process chunks in parallel
        List<CompletableFuture<ChunkResult>> futures = new ArrayList<>();

        for (int i = 0; i < chunks.size(); i++) {
            final int chunkIndex = i;
            final List<Party> chunk = chunks.get(i);

            CompletableFuture<ChunkResult> future = CompletableFuture.supplyAsync(() ->
                processChunk(chunkIndex, chunk)
            );

            futures.add(future);

            // Limit parallelism to MAX_PARALLEL_THREADS
            if (futures.size() >= MAX_PARALLEL_THREADS) {
                // Wait for one to complete before submitting more
                CompletableFuture.anyOf(futures.toArray(new CompletableFuture[0])).join();
                futures.removeIf(CompletableFuture::isDone);
            }
        }

        // Wait for all chunks to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        // Aggregate results
        for (CompletableFuture<ChunkResult> future : futures) {
            try {
                ChunkResult result = future.get();
                totalProcessed.addAndGet(result.processed);
                autoMerged.addAndGet(result.autoMerged);
                needsReview.addAndGet(result.needsReview);
                noMatchFound.addAndGet(result.noMatchFound);
                errors.addAndGet(result.errors);
            } catch (Exception e) {
                log.error("Failed to get chunk result", e);
                errors.incrementAndGet();
            }
        }

        Instant endTime = Instant.now();
        Duration duration = Duration.between(startTime, endTime);

        BatchResolutionResult result = BatchResolutionResult.builder()
            .totalParties(unresolvedParties.size())
            .processed(totalProcessed.get())
            .autoMerged(autoMerged.get())
            .needsReview(needsReview.get())
            .noMatchFound(noMatchFound.get())
            .errors(errors.get())
            .durationSeconds(duration.getSeconds())
            .partiesPerSecond(totalProcessed.get() / Math.max(1.0, duration.getSeconds()))
            .build();

        log.info("Batch resolution completed: {}", result);
        return CompletableFuture.completedFuture(result);
    }

    /**
     * Process a single chunk of parties
     */
    private ChunkResult processChunk(int chunkIndex, List<Party> chunk) {
        log.info("Processing chunk {} with {} parties", chunkIndex, chunk.size());
        Instant startTime = Instant.now();

        ChunkResult result = new ChunkResult();
        result.chunkIndex = chunkIndex;
        result.chunkSize = chunk.size();

        for (Party party : chunk) {
            try {
                // Get existing parties to compare against (excluding current chunk)
                List<Party> existingParties = partyRepository.findByStatus(PartyStatus.ACTIVE);
                existingParties.removeIf(p -> p.getFederatedId().equals(party.getFederatedId()));

                // Resolve party
                ResolutionResult resolutionResult = entityResolutionService.resolve(party);

                result.processed++;

                switch (resolutionResult.getAction()) {
                    case MERGED:
                        result.autoMerged++;
                        break;
                    case NEEDS_REVIEW:
                        result.needsReview++;
                        break;
                    case CREATED:
                        result.noMatchFound++;
                        break;
                }

            } catch (Exception e) {
                log.error("Error processing party {}: {}", party.getFederatedId(), e.getMessage(), e);
                result.errors++;
            }
        }

        Instant endTime = Instant.now();
        Duration duration = Duration.between(startTime, endTime);
        result.durationMs = duration.toMillis();

        log.info("Chunk {} completed: {} processed, {} auto-merged, {} needs review, {} no match, {} errors in {}ms",
                chunkIndex, result.processed, result.autoMerged, result.needsReview,
                result.noMatchFound, result.errors, result.durationMs);

        return result;
    }

    /**
     * Split list into chunks
     */
    private <T> List<List<T>> chunkList(List<T> list, int chunkSize) {
        List<List<T>> chunks = new ArrayList<>();
        for (int i = 0; i < list.size(); i += chunkSize) {
            int end = Math.min(i + chunkSize, list.size());
            chunks.add(new ArrayList<>(list.subList(i, end)));
        }
        return chunks;
    }

    /**
     * Result for a single chunk
     */
    private static class ChunkResult {
        int chunkIndex;
        int chunkSize;
        int processed;
        int autoMerged;
        int needsReview;
        int noMatchFound;
        int errors;
        long durationMs;
    }

    /**
     * Overall batch resolution result
     */
    @lombok.Builder
    @lombok.Data
    public static class BatchResolutionResult {
        private int totalParties;
        private int processed;
        private int autoMerged;
        private int needsReview;
        private int noMatchFound;
        private int errors;
        private long durationSeconds;
        private double partiesPerSecond;

        /**
         * Calculate auto-merge rate (percentage)
         */
        public double getAutoMergeRate() {
            return processed > 0 ? (autoMerged * 100.0 / processed) : 0.0;
        }

        /**
         * Calculate manual review rate (percentage)
         */
        public double getManualReviewRate() {
            return processed > 0 ? (needsReview * 100.0 / processed) : 0.0;
        }

        /**
         * Calculate error rate (percentage)
         */
        public double getErrorRate() {
            return totalParties > 0 ? (errors * 100.0 / totalParties) : 0.0;
        }

        @Override
        public String toString() {
            return String.format(
                "BatchResolutionResult{total=%d, processed=%d, autoMerged=%d (%.1f%%), needsReview=%d (%.1f%%), " +
                "noMatch=%d, errors=%d (%.1f%%), duration=%ds, throughput=%.1f parties/sec}",
                totalParties, processed, autoMerged, getAutoMergeRate(), needsReview, getManualReviewRate(),
                noMatchFound, errors, getErrorRate(), durationSeconds, partiesPerSecond
            );
        }
    }
}
