package com.bank.product.domain.solution.service;

import com.bank.product.client.WorkflowClient;
import com.bank.product.client.dto.WorkflowSubmitRequest;
import com.bank.product.client.dto.WorkflowSubmitResponse;
import com.bank.product.domain.solution.model.Solution;
import com.bank.product.domain.solution.model.SolutionStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * Service for async workflow operations
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AsyncWorkflowService {

    private final WorkflowClient workflowClient;
    private final SolutionService solutionService;

    /**
     * Submit workflow asynchronously
     */
    @Async("workflowExecutor")
    public CompletableFuture<WorkflowSubmitResponse> submitWorkflowAsync(
            Solution solution,
            WorkflowSubmitRequest workflowRequest) {

        try {
            log.info("Async workflow submission started for solution: {}", solution.getId());
            WorkflowSubmitResponse response = workflowClient.submitWorkflow(workflowRequest);

            // Update solution with workflow info
            solution.setWorkflowId(response.getWorkflowId());
            solution.setUpdatedAt(java.time.LocalDateTime.now());
            solutionService.saveSolution(solution);

            log.info("Async workflow submission completed: workflowId={}", response.getWorkflowId());
            return CompletableFuture.completedFuture(response);

        } catch (Exception e) {
            log.error("Async workflow submission failed for solution: {}", solution.getId(), e);

            // Update solution status to indicate workflow submission failure
            try {
                solutionService.updateSolutionStatus(
                    solution.getTenantId(),
                    solution.getId(),
                    SolutionStatus.REJECTED,
                    "system"
                );
            } catch (Exception updateException) {
                log.error("Failed to update solution status after workflow failure", updateException);
            }

            return CompletableFuture.failedFuture(e);
        }
    }
}
