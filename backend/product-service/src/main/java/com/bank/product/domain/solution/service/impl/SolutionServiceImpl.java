package com.bank.product.domain.solution.service.impl;

import com.bank.product.domain.solution.dto.ConfigureSolutionRequest;
import com.bank.product.domain.solution.repository.SolutionRepository;
import com.bank.product.domain.solution.service.SolutionService;
import com.bank.product.domain.solution.model.Solution;
import com.bank.product.domain.solution.model.SolutionStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SolutionServiceImpl implements SolutionService {

    private final SolutionRepository solutionRepository;

    @Override
    public Solution getSolution(String tenantId, String solutionId) {
        log.debug("Fetching solution {} for tenant {}", solutionId, tenantId);
        return solutionRepository.findByTenantIdAndSolutionId(tenantId, solutionId)
                .orElseThrow(() -> new RuntimeException("Solution not found: " + solutionId));
    }

    @Override
    public Solution getSolutionById(String solutionId) {
        log.debug("Fetching solution by id: {}", solutionId);
        // Automatically filtered by current tenant from TenantContext
        return solutionRepository.findByIdTenantAware(solutionId)
                .orElseThrow(() -> new RuntimeException("Solution not found: " + solutionId));
    }

    @Override
    @Transactional
    public Solution createSolutionFromCatalog(String tenantId, String userId, ConfigureSolutionRequest request) {
        log.info("Creating solution from catalog {} for tenant {}", request.getCatalogProductId(), tenantId);

        Solution solution = new Solution();
        solution.setId(UUID.randomUUID().toString());
        solution.setSolutionId("sol-" + UUID.randomUUID().toString().substring(0, 8));
        solution.setTenantId(tenantId);
        solution.setCatalogProductId(request.getCatalogProductId());
        solution.setName(request.getSolutionName());
        solution.setDescription(request.getDescription());
        solution.setCategory("CHECKING"); // Default, should come from catalog
        solution.setStatus(SolutionStatus.DRAFT);
        solution.setVersion("1.0");
        solution.setCreatedAt(LocalDateTime.now());
        solution.setCreatedBy(userId);
        solution.setUpdatedAt(LocalDateTime.now());
        solution.setUpdatedBy(userId);

        return solutionRepository.save(solution);
    }

    @Override
    public Page<Solution> getSolutions(String tenantId, Pageable pageable) {
        log.debug("Fetching solutions for tenant {} with pagination", tenantId);
        return solutionRepository.findByTenantId(tenantId, pageable);
    }

    @Override
    public Page<Solution> getSolutionsByStatus(String tenantId, SolutionStatus status, Pageable pageable) {
        log.debug("Fetching solutions for tenant {} with status {}", tenantId, status);
        return solutionRepository.findByTenantIdAndStatus(tenantId, status, pageable);
    }

    @Override
    public Page<Solution> getSolutionsByCategory(String tenantId, String category, Pageable pageable) {
        log.debug("Fetching solutions for tenant {} in category {}", tenantId, category);
        return solutionRepository.findByTenantIdAndCategory(tenantId, category, pageable);
    }

    @Override
    public Page<Solution> getSolutionsByChannel(String tenantId, String channel, Pageable pageable) {
        log.debug("Fetching solutions for tenant {} available on channel {}", tenantId, channel);
        return solutionRepository.findByTenantIdAndChannel(tenantId, channel, pageable);
    }

    @Override
    public Page<Solution> getSolutionsByCatalogProduct(String tenantId, String catalogProductId, Pageable pageable) {
        log.debug("Fetching solutions for tenant {} based on catalog {}", tenantId, catalogProductId);
        return solutionRepository.findByTenantIdAndCatalogProductId(tenantId, catalogProductId, pageable);
    }

    @Override
    @Transactional
    public Solution updateSolutionStatus(String tenantId, String solutionId, SolutionStatus status, String userId) {
        log.info("Updating status of solution {} to {} for tenant {}", solutionId, status, tenantId);

        Solution solution = solutionRepository.findByTenantIdAndSolutionId(tenantId, solutionId)
                .orElseThrow(() -> new RuntimeException("Solution not found: " + solutionId));

        solution.setStatus(status);
        solution.setUpdatedAt(LocalDateTime.now());
        solution.setUpdatedBy(userId);

        Solution updated = solutionRepository.save(solution);
        log.info("Solution {} status updated to {}", solutionId, status);

        return updated;
    }

    @Override
    @Transactional
    public Solution saveSolution(Solution solution) {
        log.debug("Saving solution: {}", solution.getId());
        return solutionRepository.save(solution);
    }

    @Override
    @Transactional
    public void deleteSolution(String tenantId, String solutionId) {
        log.info("Deleting solution {} for tenant {}", solutionId, tenantId);
        if (!solutionRepository.existsByTenantIdAndSolutionId(tenantId, solutionId)) {
            throw new RuntimeException("Solution not found: " + solutionId);
        }
        solutionRepository.deleteByTenantIdAndSolutionId(tenantId, solutionId);
        log.info("Solution {} deleted successfully", solutionId);
    }

    @Override
    @Transactional
    public int activateSolution(String solutionId) {
        log.info("Activating solution: {}", solutionId);
        // Uses TenantContext if available, otherwise retrieves solution to get tenant
        Solution solution = solutionRepository.findById(solutionId).orElse(null);
        if (solution == null || solution.getStatus() == SolutionStatus.ACTIVE) {
            return 0;
        }

        // Verify tenant context matches solution (if tenant context is set)
        if (com.bank.product.security.TenantContext.isSet()) {
            String currentTenant = com.bank.product.security.TenantContext.getCurrentTenant();
            if (!currentTenant.equals(solution.getTenantId())) {
                log.warn("Tenant mismatch: context={}, solution={}", currentTenant, solution.getTenantId());
                return 0;
            }
        }

        solution.setStatus(SolutionStatus.ACTIVE);
        solution.setUpdatedAt(LocalDateTime.now());
        solution.setUpdatedBy("system");
        solutionRepository.save(solution);
        return 1;
    }

    @Override
    @Transactional
    public int rejectSolution(String solutionId) {
        log.info("Rejecting solution: {}", solutionId);
        // Uses TenantContext if available, otherwise retrieves solution to get tenant
        Solution solution = solutionRepository.findById(solutionId).orElse(null);
        if (solution == null || solution.getStatus() == SolutionStatus.REJECTED) {
            return 0;
        }

        // Verify tenant context matches solution (if tenant context is set)
        if (com.bank.product.security.TenantContext.isSet()) {
            String currentTenant = com.bank.product.security.TenantContext.getCurrentTenant();
            if (!currentTenant.equals(solution.getTenantId())) {
                log.warn("Tenant mismatch: context={}, solution={}", currentTenant, solution.getTenantId());
                return 0;
            }
        }

        solution.setStatus(SolutionStatus.REJECTED);
        solution.setUpdatedAt(LocalDateTime.now());
        solution.setUpdatedBy("system");
        solutionRepository.save(solution);
        return 1;
    }
}