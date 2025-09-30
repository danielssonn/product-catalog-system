package com.bank.product.domain.solution.controller;

import com.bank.product.domain.solution.service.SolutionService;
import com.bank.product.domain.solution.model.Solution;
import com.bank.product.domain.solution.model.SolutionStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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