package com.bank.product.domain.solution.service;

import com.bank.product.client.WorkflowClient;
import com.bank.product.client.dto.WorkflowSubmitRequest;
import com.bank.product.client.dto.WorkflowSubmitResponse;
import com.bank.product.domain.solution.model.Solution;
import com.bank.product.enums.WorkflowSubmissionStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
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
     * Updates solution with workflow submission status and metadata
     */
    @Async("workflowExecutor")
    public CompletableFuture<WorkflowSubmitResponse> submitWorkflowAsync(
            Solution solution,
            WorkflowSubmitRequest workflowRequest) {

        try {
            log.info("Async workflow submission started for solution: {}", solution.getId());

            // Call workflow service
            WorkflowSubmitResponse response = workflowClient.submitWorkflow(workflowRequest);

            // Update solution with workflow metadata
            solution.setWorkflowId(response.getWorkflowId());
            solution.setWorkflowSubmissionStatus(WorkflowSubmissionStatus.SUBMITTED);
            solution.setApprovalRequired(response.isApprovalRequired());
            solution.setRequiredApprovals(response.getRequiredApprovals());
            solution.setApproverRoles(response.getApproverRoles());
            solution.setSequential(response.isSequential());
            solution.setSlaHours(response.getSlaHours());

            if (response.getEstimatedCompletion() != null) {
                // Convert LocalDateTime to Instant
                solution.setEstimatedCompletion(
                    response.getEstimatedCompletion()
                        .atZone(java.time.ZoneId.systemDefault())
                        .toInstant()
                );
            }

            solution.setUpdatedAt(java.time.LocalDateTime.now());
            solution.setUpdatedBy("system");
            solutionService.saveSolution(solution);

            log.info("Async workflow submission completed: workflowId={}, status=SUBMITTED",
                response.getWorkflowId());
            return CompletableFuture.completedFuture(response);

        } catch (Exception e) {
            log.error("Async workflow submission failed for solution: {}", solution.getId(), e);

            // Update solution to indicate submission failure
            try {
                solution.setWorkflowSubmissionStatus(WorkflowSubmissionStatus.SUBMISSION_FAILED);
                solution.setWorkflowErrorMessage(e.getMessage());
                solution.setWorkflowRetryAt(Instant.now().plus(30, ChronoUnit.SECONDS));
                solution.setUpdatedAt(java.time.LocalDateTime.now());
                solution.setUpdatedBy("system");
                solutionService.saveSolution(solution);

                log.warn("Solution {} marked as SUBMISSION_FAILED, retry scheduled at {}",
                    solution.getId(), solution.getWorkflowRetryAt());

            } catch (Exception updateException) {
                log.error("Failed to update solution status after workflow failure", updateException);
            }

            return CompletableFuture.failedFuture(e);
        }
    }
}
