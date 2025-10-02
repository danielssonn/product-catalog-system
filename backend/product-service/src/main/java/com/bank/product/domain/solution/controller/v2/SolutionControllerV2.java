package com.bank.product.domain.solution.controller.v2;

import com.bank.product.client.WorkflowClient;
import com.bank.product.client.dto.WorkflowSubmitRequest;
import com.bank.product.client.dto.WorkflowSubmitResponse;
import com.bank.product.domain.solution.dto.ConfigureSolutionRequest;
import com.bank.product.domain.solution.dto.v2.ConfigureSolutionRequestV2;
import com.bank.product.domain.solution.dto.v2.ConfigureSolutionResponseV2;
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
 * V2 Tenant Solution Controller
 * Breaking change: customFees renamed to customFeesFX
 */
@Slf4j
@RestController
@RequestMapping("/api/v2/solutions")
@RequiredArgsConstructor
public class SolutionControllerV2 {

    private final SolutionService solutionService;
    private final WorkflowClient workflowClient;

    /**
     * V2: Configure a new solution from catalog product with workflow approval
     * Breaking change: customFees -> customFeesFX
     */
    @PostMapping("/configure")
    public ResponseEntity<ConfigureSolutionResponseV2> configureSolution(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestHeader("X-User-ID") String userId,
            @RequestBody ConfigureSolutionRequestV2 requestV2) {

        log.info("V2: Configuring solution from catalog {} for tenant {}",
                requestV2.getCatalogProductId(), tenantId);

        // Transform V2 request to V1 internal format for service layer
        ConfigureSolutionRequest requestV1 = transformV2toV1Request(requestV2);

        // Create solution in DRAFT status
        Solution solution = solutionService.createSolutionFromCatalog(
                tenantId, userId, requestV1);

        // Build workflow metadata for rule evaluation
        Map<String, Object> entityMetadata = new HashMap<>();
        entityMetadata.put("solutionType", solution.getCategory());
        entityMetadata.put("pricingVariance", requestV2.getPricingVariance() != null ?
                requestV2.getPricingVariance() : 0.0);
        entityMetadata.put("riskLevel", requestV2.getRiskLevel() != null ?
                requestV2.getRiskLevel() : "LOW");
        entityMetadata.put("tenantTier", "STANDARD");

        // Build entity data
        Map<String, Object> entityData = new HashMap<>();
        entityData.put("solutionId", solution.getId());
        entityData.put("solutionName", solution.getName());
        entityData.put("catalogProductId", requestV2.getCatalogProductId());
        entityData.put("customFeesFX", requestV2.getCustomFeesFX());  // V2 field name

        // Submit workflow for approval
        WorkflowSubmitRequest workflowRequest = WorkflowSubmitRequest.builder()
                .entityType("SOLUTION_CONFIGURATION")
                .entityId(solution.getId())
                .entityData(entityData)
                .entityMetadata(entityMetadata)
                .initiatedBy(userId)
                .tenantId(tenantId)
                .businessJustification(requestV2.getBusinessJustification())
                .priority(requestV2.getPriority())
                .build();

        WorkflowSubmitResponse workflowResponse = workflowClient.submitWorkflow(workflowRequest);

        // Build V2 response
        ConfigureSolutionResponseV2 response = ConfigureSolutionResponseV2.builder()
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
                .metadata(requestV2.getMetadata() != null ? requestV2.getMetadata() : new HashMap<>())
                .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get solution by ID (V2)
     */
    @GetMapping("/{solutionId}")
    public ResponseEntity<Solution> getSolution(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable String solutionId) {

        log.info("V2: Fetching solution {} for tenant {}", solutionId, tenantId);
        Solution solution = solutionService.getSolution(tenantId, solutionId);
        return ResponseEntity.ok(solution);
    }

    /**
     * List solutions (V2)
     */
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

        log.info("V2: Fetching solutions for tenant {} with filters", tenantId);

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

    /**
     * Transform V2 request to V1 internal format
     */
    private ConfigureSolutionRequest transformV2toV1Request(ConfigureSolutionRequestV2 v2) {
        return ConfigureSolutionRequest.builder()
                .catalogProductId(v2.getCatalogProductId())
                .solutionName(v2.getSolutionName())
                .description(v2.getDescription())
                .customInterestRate(v2.getCustomInterestRate())
                .customFees(v2.getCustomFeesFX())  // Transform: customFeesFX -> customFees
                .customTerms(v2.getCustomTerms())
                .riskLevel(v2.getRiskLevel())
                .pricingVariance(v2.getPricingVariance())
                .businessJustification(v2.getBusinessJustification())
                .priority(v2.getPriority())
                .build();
    }
}
