package com.bank.product.workflow.temporal.workflow;

import com.bank.product.workflow.validation.model.ValidationResult;
import com.bank.product.workflow.validation.model.ValidatorConfig;
import com.bank.product.workflow.validation.model.AgentExecutionMode;
import com.bank.product.workflow.validation.model.ValidatorType;
import com.bank.product.workflow.domain.model.*;
import com.bank.product.workflow.temporal.activity.ValidationActivity;
import com.bank.product.workflow.temporal.activity.WorkflowActivities;
import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.failure.CanceledFailure;
import io.temporal.workflow.Workflow;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Workflow implementation V3 with Validation integration
 * Supports rules-based, MCP, and GraphRAG validators before rule evaluation
 */
public class ApprovalWorkflowImplV3 implements ApprovalWorkflow {

        // State
        private WorkflowSubject subject;
        private WorkflowState state = WorkflowState.PENDING_APPROVAL;
        private final List<ApprovalDecision> approvals = new ArrayList<>();
        private boolean isComplete = false;
        private ValidationResult validationResult;

        // Activity stubs
        private final WorkflowActivities workflowActivities;
        private final ValidationActivity validationActivity;

        public ApprovalWorkflowImplV3() {
                // Configure standard workflow activities
                ActivityOptions workflowActivityOptions = ActivityOptions.newBuilder()
                                .setStartToCloseTimeout(Duration.ofMinutes(5))
                                .setRetryOptions(
                                                RetryOptions.newBuilder()
                                                                .setInitialInterval(Duration.ofSeconds(1))
                                                                .setBackoffCoefficient(2.0)
                                                                .setMaximumInterval(Duration.ofMinutes(1))
                                                                .setMaximumAttempts(3)
                                                                .build())
                                .build();

                this.workflowActivities = Workflow.newActivityStub(WorkflowActivities.class, workflowActivityOptions);

                // Configure validation activities (longer timeout for MCP/GraphRAG)
                ActivityOptions validationActivityOptions = ActivityOptions.newBuilder()
                                .setStartToCloseTimeout(Duration.ofMinutes(10))
                                .setRetryOptions(
                                                RetryOptions.newBuilder()
                                                                .setInitialInterval(Duration.ofSeconds(2))
                                                                .setBackoffCoefficient(2.0)
                                                                .setMaximumInterval(Duration.ofMinutes(2))
                                                                .setMaximumAttempts(2) // LLM calls are expensive, retry
                                                                                       // less
                                                                .build())
                                .build();

                this.validationActivity = Workflow.newActivityStub(ValidationActivity.class, validationActivityOptions);
        }

        @Override
        public WorkflowResult execute(WorkflowSubject workflowSubject) {
                this.subject = workflowSubject;

                Workflow.getLogger(ApprovalWorkflowImplV3.class).info(
                                "Workflow V3 started (with validation): workflowId={}, entityId={}",
                                subject.getWorkflowId(), subject.getEntityId());

                try {
                        // PHASE 1: VALIDATION EXECUTION (Document Validation)
                        Workflow.getLogger(ApprovalWorkflowImplV3.class).info(
                                        "Phase 1: Executing document validation");

                        // Create document validation config
                        ValidatorConfig validatorConfig = createDocumentValidatorConfig();

                        // Execute document validation
                        validationResult = validationActivity.executeDocumentValidation(subject, validatorConfig);

                        Workflow.getLogger(ApprovalWorkflowImplV3.class).info(
                                        "Document validation completed: success={}, redFlag={}, completeness={}",
                                        validationResult.isSuccess(),
                                        validationResult.isRedFlagDetected(),
                                        validationResult.getEnrichmentData().get("documentCompleteness"));

                        // Check for red flags
                        if (validationResult.isRedFlagDetected()) {
                                Workflow.getLogger(ApprovalWorkflowImplV3.class).warn(
                                                "RED FLAG DETECTED: {}", validationResult.getRedFlagReason());

                                // Check if we should auto-reject
                                if (validationResult.getRecommendedAction() != null &&
                                                validationResult.getRecommendedAction().isAutoReject()) {

                                        state = WorkflowState.REJECTED;
                                        isComplete = true;

                                        return WorkflowResult.builder()
                                                        .success(false)
                                                        .resultCode("VALIDATION_RED_FLAG")
                                                        .message("Document validation failed: "
                                                                        + validationResult.getRedFlagReason())
                                                        .timestamp(LocalDateTime.now())
                                                        .data(buildResultData(validationResult))
                                                        .build();
                                }
                        }

                        // PHASE 2: ENRICH METADATA with validation outputs
                        Map<String, Object> enrichedMetadata = new HashMap<>(subject.getEntityMetadata());
                        if (validationResult.getEnrichmentData() != null) {
                                enrichedMetadata.putAll(validationResult.getEnrichmentData());
                        }

                        Workflow.getLogger(ApprovalWorkflowImplV3.class).info(
                                        "Phase 2: Metadata enriched with {} validation outputs",
                                        validationResult.getEnrichmentData() != null
                                                        ? validationResult.getEnrichmentData().size()
                                                        : 0);

                        // PHASE 3: EVALUATE RULES with enriched data
                        ComputedApprovalPlan approvalPlan = workflowActivities.evaluateRules(
                                        subject.getTemplateId(),
                                        enrichedMetadata);

                        subject.setApprovalPlan(approvalPlan);

                        Workflow.getLogger(ApprovalWorkflowImplV3.class).info(
                                        "Phase 3: Rules evaluated: approvalRequired={}, requiredApprovals={}",
                                        approvalPlan.isApprovalRequired(),
                                        approvalPlan.getRequiredApprovals());

                        // PHASE 4: CHECK IF APPROVAL REQUIRED
                        if (!approvalPlan.isApprovalRequired()) {
                                Workflow.getLogger(ApprovalWorkflowImplV3.class).info(
                                                "Auto-approval: No approval required");

                                state = WorkflowState.COMPLETED;
                                isComplete = true;

                                return WorkflowResult.builder()
                                                .success(true)
                                                .resultCode("AUTO_APPROVED")
                                                .message("Auto-approved - no approval required")
                                                .timestamp(LocalDateTime.now())
                                                .data(buildResultData(validationResult))
                                                .build();
                        }

                        // PHASE 5: WAIT FOR APPROVALS
                        Workflow.getLogger(ApprovalWorkflowImplV3.class).info(
                                        "Phase 4: Waiting for {} approvals",
                                        approvalPlan.getRequiredApprovals());

                        Duration timeout = approvalPlan.getSla() != null ? approvalPlan.getSla() : Duration.ofHours(48);

                        boolean approved = Workflow.await(
                                        timeout,
                                        () -> approvals.size() >= approvalPlan.getRequiredApprovals());

                        if (approved) {
                                // All approvals received
                                Workflow.getLogger(ApprovalWorkflowImplV3.class).info(
                                                "All approvals received: count={}", approvals.size());

                                state = WorkflowState.COMPLETED;
                                isComplete = true;

                                return WorkflowResult.builder()
                                                .success(true)
                                                .resultCode("APPROVED")
                                                .message("All approvals received")
                                                .timestamp(LocalDateTime.now())
                                                .data(buildResultData(validationResult))
                                                .build();

                        } else {
                                // Timeout
                                Workflow.getLogger(ApprovalWorkflowImplV3.class).warn(
                                                "Workflow timed out");

                                state = WorkflowState.REJECTED;
                                isComplete = true;

                                return WorkflowResult.builder()
                                                .success(false)
                                                .resultCode("TIMEOUT")
                                                .message("Workflow timed out - no response within SLA")
                                                .timestamp(LocalDateTime.now())
                                                .data(buildResultData(validationResult))
                                                .build();
                        }

                } catch (CanceledFailure e) {
                        // Workflow rejected
                        Workflow.getLogger(ApprovalWorkflowImplV3.class).info(
                                        "Workflow rejected");

                        state = WorkflowState.REJECTED;
                        isComplete = true;

                        String rejectionReason = approvals.stream()
                                        .filter(a -> a.getRejectionReason() != null)
                                        .map(ApprovalDecision::getRejectionReason)
                                        .findFirst()
                                        .orElse("Workflow rejected");

                        return WorkflowResult.builder()
                                        .success(false)
                                        .resultCode("REJECTED")
                                        .message(rejectionReason)
                                        .timestamp(LocalDateTime.now())
                                        .data(buildResultData(validationResult))
                                        .build();

                } catch (Exception e) {
                        Workflow.getLogger(ApprovalWorkflowImplV3.class).error(
                                        "Workflow failed: {}", e.getMessage());

                        state = WorkflowState.FAILED;
                        isComplete = true;

                        return WorkflowResult.builder()
                                        .success(false)
                                        .resultCode("FAILED")
                                        .message("Workflow failed: " + e.getMessage())
                                        .timestamp(LocalDateTime.now())
                                        .build();
                }
        }

        @Override
        public void approve(ApprovalDecision decision) {
                if (state != WorkflowState.PENDING_APPROVAL) {
                        throw new IllegalStateException("Workflow not in pending state: " + state);
                }

                Workflow.getLogger(ApprovalWorkflowImplV3.class).info(
                                "Approval received: workflowId={}, approver={}, count={}/{}",
                                subject.getWorkflowId(),
                                decision.getApproverId(),
                                approvals.size() + 1,
                                subject.getApprovalPlan().getRequiredApprovals());

                decision.setTimestamp(LocalDateTime.now());
                decision.setDecision("APPROVE");
                approvals.add(decision);
        }

        @Override
        public void reject(ApprovalDecision decision) {
                if (state != WorkflowState.PENDING_APPROVAL) {
                        throw new IllegalStateException("Workflow not in pending state: " + state);
                }

                Workflow.getLogger(ApprovalWorkflowImplV3.class).info(
                                "Rejection received: workflowId={}, rejecter={}",
                                subject.getWorkflowId(),
                                decision.getApproverId());

                decision.setTimestamp(LocalDateTime.now());
                decision.setDecision("REJECT");
                approvals.add(decision);

                // Cancel workflow (triggers CanceledFailure)
                throw Workflow.wrap(new CanceledFailure("Workflow rejected by: " + decision.getApproverId()));
        }

        @Override
        public void cancel(String reason) {
                if (state != WorkflowState.PENDING_APPROVAL) {
                        throw new IllegalStateException("Workflow not in pending state: " + state);
                }

                Workflow.getLogger(ApprovalWorkflowImplV3.class).info(
                                "Cancellation received: workflowId={}, reason={}",
                                subject.getWorkflowId(), reason);

                state = WorkflowState.CANCELLED;
                isComplete = true;

                throw Workflow.wrap(new CanceledFailure("Workflow cancelled: " + reason));
        }

        @Override
        public WorkflowSubject getStatus() {
                subject.setState(state);
                if (isComplete) {
                        subject.setCompletedAt(LocalDateTime.now());
                }
                return subject;
        }

        @Override
        public boolean isComplete() {
                return isComplete;
        }

        /**
         * Create document validator configuration
         * Default: rules-based validator
         * Can be changed to MCP or GRAPH_RAG via template configuration
         */
        private ValidatorConfig createDocumentValidatorConfig() {
                // Create default config - in production would come from template
                Map<String, String> redFlagConditions = new HashMap<>();
                redFlagConditions.put("completenessScore", "< 0.5");
                redFlagConditions.put("validationStatus", "FAIL");

                return ValidatorConfig.builder()
                                .validatorId("document-validator-mcp")
                                .type(ValidatorType.MCP) // Using Claude for semantic analysis
                                .mode(AgentExecutionMode.SYNC_ENRICHMENT)
                                .priority(1)
                                .timeoutMs(60000)
                                .redFlagConditions(redFlagConditions)
                                .enrichmentOutputs(Arrays.asList(
                                                "documentCompleteness",
                                                "documentValidationStatus",
                                                "missingDocumentCount",
                                                "inconsistencyCount",
                                                "complianceGapCount",
                                                "documentRecommendations"))
                                .required(false) // Don't fail workflow if validation fails
                                .build();
        }

        /**
         * Build result data including validation insights
         */
        private Map<String, Object> buildResultData(ValidationResult validationResult) {
                Map<String, Object> data = new HashMap<>();

                if (validationResult != null) {
                        data.put("validationExecution", Map.of(
                                        "validatorId", validationResult.getValidatorId(),
                                        "validatorType", validationResult.getValidatorType().name(),
                                        "executionTime",
                                        validationResult.getExecutionTime() != null
                                                        ? validationResult.getExecutionTime().toMillis() + "ms"
                                                        : "N/A",
                                        "success", validationResult.isSuccess(),
                                        "redFlagDetected", validationResult.isRedFlagDetected(),
                                        "confidenceScore", validationResult.getConfidenceScore(),
                                        "model",
                                        validationResult.getModel() != null ? validationResult.getModel() : "N/A"));

                        if (validationResult.getEnrichmentData() != null) {
                                data.put("documentValidation", validationResult.getEnrichmentData());
                        }

                        if (validationResult.getValidationSteps() != null
                                        && !validationResult.getValidationSteps().isEmpty()) {
                                data.put("validationSteps", validationResult.getValidationSteps().size());
                        }
                }

                return data;
        }
}
