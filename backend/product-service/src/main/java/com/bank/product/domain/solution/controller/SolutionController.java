package com.bank.product.domain.solution.controller;

import com.bank.product.client.dto.WorkflowSubmitRequest;
import com.bank.product.domain.solution.dto.ConfigureSolutionRequest;
import com.bank.product.domain.solution.dto.ConfigureSolutionResponse;
import com.bank.product.domain.solution.dto.SolutionWorkflowStatusResponse;
import com.bank.product.domain.solution.service.AsyncWorkflowService;
import com.bank.product.domain.solution.service.SolutionService;
import com.bank.product.domain.solution.model.Solution;
import com.bank.product.domain.solution.model.SolutionStatus;
import com.github.benmanes.caffeine.cache.Cache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Tenant Solution Controller
 * Manages tenant's active solution instances
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/solutions")
@RequiredArgsConstructor
public class SolutionController {

    private final SolutionService solutionService;
    private final AsyncWorkflowService asyncWorkflowService;
    private final Cache<String, Boolean> idempotencyCache;

    /**
     * Configure a new solution from catalog product with workflow approval
     */
    @PostMapping("/configure")
    public ResponseEntity<ConfigureSolutionResponse> configureSolution(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestHeader("X-User-ID") String userId,
            @RequestBody ConfigureSolutionRequest request) {

        log.info("Configuring solution from catalog {} for tenant {}",
                request.getCatalogProductId(), tenantId);

        // Create solution in DRAFT status
        Solution solution = solutionService.createSolutionFromCatalog(
                tenantId, userId, request);

        // Set workflow submission status to PENDING before async call
        solution.setWorkflowSubmissionStatus(com.bank.product.enums.WorkflowSubmissionStatus.PENDING_SUBMISSION);
        solutionService.saveSolution(solution);

        // Build workflow metadata for rule evaluation
        Map<String, Object> entityMetadata = new HashMap<>();
        entityMetadata.put("solutionType", solution.getCategory());
        entityMetadata.put("pricingVariance", request.getPricingVariance() != null ?
                request.getPricingVariance() : 0.0);
        entityMetadata.put("riskLevel", request.getRiskLevel() != null ?
                request.getRiskLevel() : "LOW");
        entityMetadata.put("tenantTier", "STANDARD");

        // Build entity data
        Map<String, Object> entityData = new HashMap<>();
        entityData.put("solutionId", solution.getId());
        entityData.put("solutionName", solution.getName());
        entityData.put("catalogProductId", request.getCatalogProductId());
        entityData.put("customPricing", request.getCustomFees());

        // Build workflow request
        WorkflowSubmitRequest workflowRequest = WorkflowSubmitRequest.builder()
                .entityType("SOLUTION_CONFIGURATION")
                .entityId(solution.getId())
                .entityData(entityData)
                .entityMetadata(entityMetadata)
                .initiatedBy(userId)
                .tenantId(tenantId)
                .businessJustification(request.getBusinessJustification())
                .priority(request.getPriority())
                .build();

        // Submit workflow asynchronously (fire-and-forget)
        asyncWorkflowService.submitWorkflowAsync(solution, workflowRequest)
                .exceptionally(ex -> {
                    log.error("Workflow submission failed for solution: {}", solution.getId(), ex);
                    return null;
                });

        // Return immediately with 202 Accepted and polling guidance
        ConfigureSolutionResponse response = ConfigureSolutionResponse.builder()
                .solutionId(solution.getId())
                .solutionName(solution.getName())
                .status(solution.getStatus().name())
                .workflowId(null) // Will be updated asynchronously
                .workflowStatus("PENDING_SUBMISSION")
                .pollUrl("/api/v1/solutions/" + solution.getId() + "/workflow-status")
                .pollIntervalMs(1000)  // Poll every second
                .message("Solution created. Workflow submission in progress. Poll the workflow-status endpoint for updates.")
                .build();

        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .header("Location", "/api/v1/solutions/" + solution.getId())
                .body(response);
    }

    /**
     * Activate solution (called by workflow callback)
     * Idempotent - safe to call multiple times
     */
    @PutMapping("/{solutionId}/activate")
    public ResponseEntity<Void> activateSolution(
            @PathVariable String solutionId,
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey) {

        // Check idempotency
        if (idempotencyKey != null) {
            Boolean alreadyProcessed = idempotencyCache.getIfPresent(idempotencyKey);
            if (Boolean.TRUE.equals(alreadyProcessed)) {
                log.info("Duplicate activation request (idempotency key: {}), returning success", idempotencyKey);
                return ResponseEntity.ok().build();
            }
        }

        log.info("Activating solution: {}", solutionId);

        // Single optimized update query
        int updated = solutionService.activateSolution(solutionId);

        if (updated > 0) {
            // Store idempotency key
            if (idempotencyKey != null) {
                idempotencyCache.put(idempotencyKey, true);
            }
            log.info("Solution {} activated successfully", solutionId);
            return ResponseEntity.ok().build();
        } else {
            log.warn("Solution {} not found or already active", solutionId);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Reject solution (called by workflow callback)
     * Idempotent - safe to call multiple times
     */
    @PutMapping("/{solutionId}/reject")
    public ResponseEntity<Void> rejectSolution(
            @PathVariable String solutionId,
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody(required = false) Map<String, String> request) {

        // Check idempotency
        if (idempotencyKey != null) {
            Boolean alreadyProcessed = idempotencyCache.getIfPresent(idempotencyKey);
            if (Boolean.TRUE.equals(alreadyProcessed)) {
                log.info("Duplicate rejection request (idempotency key: {}), returning success", idempotencyKey);
                return ResponseEntity.ok().build();
            }
        }

        log.info("Rejecting solution: {}", solutionId);

        // Single optimized update query
        int updated = solutionService.rejectSolution(solutionId);

        if (updated > 0) {
            // Store idempotency key
            if (idempotencyKey != null) {
                idempotencyCache.put(idempotencyKey, true);
            }
            log.info("Solution {} rejected successfully", solutionId);
            return ResponseEntity.ok().build();
        } else {
            log.warn("Solution {} not found or already rejected", solutionId);
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{solutionId}")
    public ResponseEntity<Solution> getSolution(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable String solutionId) {

        log.info("Fetching solution {} for tenant {}", solutionId, tenantId);
        Solution solution = solutionService.getSolution(tenantId, solutionId);
        return ResponseEntity.ok(solution);
    }

    /**
     * Get workflow submission status (lightweight endpoint for polling)
     * Returns current workflow submission state and provides polling guidance
     */
    @GetMapping("/{solutionId}/workflow-status")
    public ResponseEntity<SolutionWorkflowStatusResponse> getWorkflowStatus(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable String solutionId) {

        log.debug("Fetching workflow status for solution {} (tenant: {})", solutionId, tenantId);

        SolutionWorkflowStatusResponse status = solutionService.getWorkflowSubmissionStatus(tenantId, solutionId);

        return ResponseEntity.ok(status);
    }

    @GetMapping
    public ResponseEntity<Page<Solution>> getSolutions(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "updatedAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection,
            @RequestParam(required = false) SolutionStatus status,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String channel,
            @RequestParam(required = false) String catalogProductId) {

        log.info("Fetching solutions for tenant {} with filters", tenantId);

        Sort sort = Sort.by(Sort.Direction.fromString(sortDirection), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Solution> solutionPage;
        if (status != null) {
            solutionPage = solutionService.getSolutionsByStatus(tenantId, status, pageable);
        } else if (category != null) {
            solutionPage = solutionService.getSolutionsByCategory(tenantId, category, pageable);
        } else if (channel != null) {
            solutionPage = solutionService.getSolutionsByChannel(tenantId, channel, pageable);
        } else if (catalogProductId != null) {
            solutionPage = solutionService.getSolutionsByCatalogProduct(tenantId, catalogProductId, pageable);
        } else {
            solutionPage = solutionService.getSolutions(tenantId, pageable);
        }

        return ResponseEntity.ok(solutionPage);
    }

    @PatchMapping("/{solutionId}/status")
    public ResponseEntity<Solution> updateSolutionStatus(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestHeader("X-User-ID") String userId,
            @PathVariable String solutionId,
            @RequestParam SolutionStatus status) {

        log.info("Updating solution {} status to {} for tenant {}", solutionId, status, tenantId);
        Solution solution = solutionService.updateSolutionStatus(tenantId, solutionId, status, userId);
        return ResponseEntity.ok(solution);
    }

    @DeleteMapping("/{solutionId}")
    public ResponseEntity<Void> deleteSolution(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable String solutionId) {

        log.info("Deleting solution {} for tenant {}", solutionId, tenantId);
        solutionService.deleteSolution(tenantId, solutionId);
        return ResponseEntity.noContent().build();
    }
}