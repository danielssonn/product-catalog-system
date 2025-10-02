package com.bank.product.workflow.domain.controller;

import com.bank.product.workflow.domain.service.WorkflowExecutionService;
import com.bank.product.workflow.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for workflow operations
 * Implements role-based access control for workflow management
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/workflows")
@RequiredArgsConstructor
public class WorkflowController {

    private final WorkflowExecutionService workflowExecutionService;

    /**
     * Submit a new workflow for approval
     * Requires USER or ADMIN role
     */
    @PostMapping("/submit")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<WorkflowSubmitResponse> submitWorkflow(
            Authentication authentication,
            @Valid @RequestBody WorkflowSubmitRequest request) {

        String userId = authentication.getName();
        log.info("Received workflow submission from {}: entity={}, id={}",
                userId, request.getEntityType(), request.getEntityId());

        // Ensure initiatedBy matches authenticated user (prevent spoofing)
        request.setInitiatedBy(userId);

        WorkflowSubmitResponse response = workflowExecutionService.submitWorkflow(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Approve a workflow
     * Requires USER or ADMIN role
     * User can only approve tasks assigned to them (validated in service layer)
     */
    @PostMapping("/{workflowId}/approve")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<Void> approveWorkflow(
            Authentication authentication,
            @PathVariable String workflowId,
            @Valid @RequestBody ApprovalRequest request) {

        String userId = authentication.getName();
        log.info("Received approval for workflow: {} by authenticated user: {}", workflowId, userId);

        // Ensure approverId matches authenticated user (prevent spoofing)
        request.setApproverId(userId);

        workflowExecutionService.approveWorkflow(workflowId, request);

        return ResponseEntity.ok().build();
    }

    /**
     * Reject a workflow
     * Requires USER or ADMIN role
     * User can only reject tasks assigned to them (validated in service layer)
     */
    @PostMapping("/{workflowId}/reject")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<Void> rejectWorkflow(
            Authentication authentication,
            @PathVariable String workflowId,
            @Valid @RequestBody RejectionRequest request) {

        String userId = authentication.getName();
        log.info("Received rejection for workflow: {} by authenticated user: {}", workflowId, userId);

        // Ensure rejecterId matches authenticated user (prevent spoofing)
        request.setRejecterId(userId);

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
     * Requires ADMIN role (only admins can cancel workflows)
     */
    @PostMapping("/{workflowId}/cancel")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> cancelWorkflow(
            Authentication authentication,
            @PathVariable String workflowId,
            @RequestParam String reason) {

        String userId = authentication.getName();
        log.info("Cancelling workflow: {} by {} - {}", workflowId, userId, reason);

        workflowExecutionService.cancelWorkflow(workflowId, reason);

        return ResponseEntity.ok().build();
    }
}
