package com.bank.product.domain.solution.service;

import com.bank.product.domain.solution.dto.ConfigureSolutionRequest;
import com.bank.product.domain.solution.dto.SolutionWorkflowStatusResponse;
import com.bank.product.domain.solution.model.Solution;
import com.bank.product.domain.solution.model.SolutionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface SolutionService {

    Solution getSolution(String tenantId, String solutionId);

    Solution getSolutionById(String solutionId);

    Solution createSolutionFromCatalog(String tenantId, String userId, ConfigureSolutionRequest request);

    Page<Solution> getSolutions(String tenantId, Pageable pageable);

    Page<Solution> getSolutionsByStatus(String tenantId, SolutionStatus status, Pageable pageable);

    Page<Solution> getSolutionsByCategory(String tenantId, String category, Pageable pageable);

    Page<Solution> getSolutionsByChannel(String tenantId, String channel, Pageable pageable);

    Page<Solution> getSolutionsByCatalogProduct(String tenantId, String catalogProductId, Pageable pageable);

    Solution updateSolutionStatus(String tenantId, String solutionId, SolutionStatus status, String userId);

    Solution saveSolution(Solution solution);

    void deleteSolution(String tenantId, String solutionId);

    /**
     * Activate solution (single optimized update query)
     * @return number of documents updated
     */
    int activateSolution(String solutionId);

    /**
     * Reject solution (single optimized update query)
     * @return number of documents updated
     */
    int rejectSolution(String solutionId);

    /**
     * Get workflow submission status for polling
     * Returns current state and polling guidance
     */
    SolutionWorkflowStatusResponse getWorkflowSubmissionStatus(String tenantId, String solutionId);
}