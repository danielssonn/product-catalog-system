package com.bank.product.party.controller;

import com.bank.product.party.resolution.BatchResolutionService;
import com.bank.product.party.resolution.EntityResolutionService;
import com.bank.product.party.resolution.ResolutionResult;
import com.bank.product.party.domain.Party;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * REST API for entity resolution operations.
 *
 * Endpoints:
 * - POST /api/v1/entity-resolution/resolve - Resolve a single party
 * - POST /api/v1/entity-resolution/batch - Start batch resolution
 * - POST /api/v1/entity-resolution/merge/approve - Approve a merge
 * - POST /api/v1/entity-resolution/merge/reject - Reject a merge
 * - GET /api/v1/entity-resolution/duplicates - Find duplicate candidates
 *
 * TODO: Add Spring Security with @PreAuthorize for ROLE_ADMIN and ROLE_DATA_STEWARD
 */
@RestController
@RequestMapping("/api/v1/entity-resolution")
@RequiredArgsConstructor
@Slf4j
public class EntityResolutionController {

    private final EntityResolutionService entityResolutionService;
    private final BatchResolutionService batchResolutionService;

    /**
     * Resolve a single party against existing parties
     *
     * POST /api/v1/entity-resolution/resolve
     */
    @PostMapping("/resolve")
    public ResponseEntity<ResolutionResult> resolveParty(@RequestBody Party party) {
        log.info("Resolving party: {}", party.getFederatedId());

        ResolutionResult result = entityResolutionService.resolve(party);

        log.info("Resolution completed: action={}, matchScore={}",
                result.getAction(), result.getMatchScore());

        return ResponseEntity.ok(result);
    }

    /**
     * Start batch resolution of all parties
     *
     * POST /api/v1/entity-resolution/batch
     *
     * Returns immediately with 202 Accepted. Poll /batch/status for progress.
     */
    @PostMapping("/batch")
    public ResponseEntity<BatchStartResponse> startBatchResolution() {
        log.info("Starting batch resolution");

        CompletableFuture<BatchResolutionService.BatchResolutionResult> future =
                batchResolutionService.resolveAllParties();

        BatchStartResponse response = BatchStartResponse.builder()
                .message("Batch resolution started")
                .status("RUNNING")
                .build();

        return ResponseEntity.accepted().body(response);
    }

    /**
     * Approve a merge between two parties
     *
     * POST /api/v1/entity-resolution/merge/approve
     */
    @PostMapping("/merge/approve")
    public ResponseEntity<Party> approveMerge(@RequestBody MergeApprovalRequest request) {
        log.info("Approving merge: source={}, target={}, approvedBy={}",
                request.getSourceId(), request.getTargetId(), request.getApprovedBy());

        Party result = entityResolutionService.approveMerge(
                request.getSourceId(),
                request.getTargetId(),
                request.getApprovedBy()
        );

        log.info("Merge approved: resultParty={}", result.getFederatedId());

        return ResponseEntity.ok(result);
    }

    /**
     * Reject a merge (mark as not duplicate)
     *
     * POST /api/v1/entity-resolution/merge/reject
     */
    @PostMapping("/merge/reject")
    public ResponseEntity<Void> rejectMerge(@RequestBody MergeRejectionRequest request) {
        log.info("Rejecting merge: party={}, candidate={}, reviewedBy={}",
                request.getPartyId(), request.getCandidateId(), request.getReviewedBy());

        entityResolutionService.markNotDuplicate(
                request.getPartyId(),
                request.getCandidateId(),
                request.getReviewedBy()
        );

        log.info("Merge rejected");

        return ResponseEntity.noContent().build();
    }

    /**
     * Find duplicate candidates above threshold
     *
     * GET /api/v1/entity-resolution/duplicates?threshold=0.75
     */
    @GetMapping("/duplicates")
    public ResponseEntity<List<Party>> findDuplicates(
            @RequestParam(defaultValue = "0.75") Double threshold) {
        log.info("Finding duplicate candidates above threshold: {}", threshold);

        List<Party> duplicates = entityResolutionService.findDuplicates(threshold);

        log.info("Found {} duplicate candidates", duplicates.size());

        return ResponseEntity.ok(duplicates);
    }

    // === DTOs ===

    @lombok.Builder
    @lombok.Data
    public static class BatchStartResponse {
        private String message;
        private String status;
    }

    @lombok.Data
    public static class MergeApprovalRequest {
        private String sourceId;
        private String targetId;
        private String approvedBy;
    }

    @lombok.Data
    public static class MergeRejectionRequest {
        private String partyId;
        private String candidateId;
        private String reviewedBy;
    }
}
