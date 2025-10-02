package com.bank.product.domain.solution.controller;

import com.bank.product.client.WorkflowClient;
import com.bank.product.client.dto.WorkflowSubmitRequest;
import com.bank.product.client.dto.WorkflowSubmitResponse;
import com.bank.product.domain.solution.dto.ConfigureSolutionRequest;
import com.bank.product.domain.solution.dto.ConfigureSolutionResponse;
import com.bank.product.domain.solution.service.SolutionService;
import com.bank.product.domain.solution.model.Solution;
import com.bank.product.domain.solution.model.SolutionStatus;
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
    private final WorkflowClient workflowClient;

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

        // Submit workflow for approval
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

        WorkflowSubmitResponse workflowResponse = workflowClient.submitWorkflow(workflowRequest);

        // Build response
        ConfigureSolutionResponse response = ConfigureSolutionResponse.builder()
                .solutionId(solution.getId())
                .solutionName(solution.getName())
                .status(solution.getStatus().name())
                .workflowId(workflowResponse.getWorkflowId())
                .workflowStatus(workflowResponse.getStatus())
                .approvalRequired(workflowResponse.isApprovalRequired())
                .requiredApprovals(workflowResponse.getRequiredApprovals())
                .approverRoles(workflowResponse.getApproverRoles())
                .sequential(workflowResponse.isSequential())
                .slaHours(workflowResponse.getSlaHours())
                .estimatedCompletion(workflowResponse.getEstimatedCompletion())
                .message(workflowResponse.getMessage())
                .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Activate solution (called by workflow callback)
     */
    @PutMapping("/{solutionId}/activate")
    public ResponseEntity<Void> activateSolution(
            @PathVariable String solutionId) {

        log.info("Activating solution: {}", solutionId);
        // Get solution by MongoDB ID and update status
        Solution solution = solutionService.getSolutionById(solutionId);
        solution.setStatus(SolutionStatus.ACTIVE);
        solution.setUpdatedAt(java.time.LocalDateTime.now());
        solution.setUpdatedBy("system");
        solutionService.saveSolution(solution);
        return ResponseEntity.ok().build();
    }

    /**
     * Reject solution (called by workflow callback)
     */
    @PutMapping("/{solutionId}/reject")
    public ResponseEntity<Void> rejectSolution(
            @PathVariable String solutionId,
            @RequestBody Map<String, String> request) {

        log.info("Rejecting solution: {}", solutionId);
        Solution solution = solutionService.getSolutionById(solutionId);
        solutionService.updateSolutionStatus(
                solution.getTenantId(), solutionId, SolutionStatus.REJECTED, "system");
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{solutionId}")
    public ResponseEntity<Solution> getSolution(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable String solutionId) {

        log.info("Fetching solution {} for tenant {}", solutionId, tenantId);
        Solution solution = solutionService.getSolution(tenantId, solutionId);
        return ResponseEntity.ok(solution);
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