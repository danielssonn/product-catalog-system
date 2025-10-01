package com.bank.product.workflow.domain.controller;

import com.bank.product.workflow.domain.service.WorkflowExecutionService;
import com.bank.product.workflow.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for workflow operations
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/workflows")
@RequiredArgsConstructor
public class WorkflowController {

    private final WorkflowExecutionService workflowExecutionService;

    /**
     * Submit a new workflow for approval
     */
    @PostMapping("/submit")
    public ResponseEntity<WorkflowSubmitResponse> submitWorkflow(
            @Valid @RequestBody WorkflowSubmitRequest request) {

        log.info("Received workflow submission: entity={}, id={}",
                request.getEntityType(), request.getEntityId());

        WorkflowSubmitResponse response = workflowExecutionService.submitWorkflow(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Approve a workflow
     */
    @PostMapping("/{workflowId}/approve")
    public ResponseEntity<Void> approveWorkflow(
            @PathVariable String workflowId,
            @Valid @RequestBody ApprovalRequest request) {

        log.info("Received approval for workflow: {} by {}", workflowId, request.getApproverId());

        workflowExecutionService.approveWorkflow(workflowId, request);

        return ResponseEntity.ok().build();
    }

    /**
     * Reject a workflow
     */
    @PostMapping("/{workflowId}/reject")
    public ResponseEntity<Void> rejectWorkflow(
            @PathVariable String workflowId,
            @Valid @RequestBody RejectionRequest request) {

        log.info("Received rejection for workflow: {} by {}", workflowId, request.getRejecterId());

        workflowExecutionService.rejectWorkflow(workflowId, request);

        return ResponseEntity.ok().build();
    }

    /**
     * Get workflow status
     */
    @GetMapping("/{workflowId}")
    public ResponseEntity<WorkflowStatusResponse> getWorkflowStatus(
            @PathVariable String workflowId) {

        log.debug("Getting workflow status: {}", workflowId);

        WorkflowStatusResponse response = workflowExecutionService.getWorkflowStatus(workflowId);

        return ResponseEntity.ok(response);
    }

    /**
     * Cancel a workflow
     */
    @PostMapping("/{workflowId}/cancel")
    public ResponseEntity<Void> cancelWorkflow(
            @PathVariable String workflowId,
            @RequestParam String reason) {

        log.info("Cancelling workflow: {} - {}", workflowId, reason);

        workflowExecutionService.cancelWorkflow(workflowId, reason);

        return ResponseEntity.ok().build();
    }
}
