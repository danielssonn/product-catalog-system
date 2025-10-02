package com.bank.product.workflow.temporal.workflow;

import com.bank.product.workflow.domain.model.ApprovalDecision;
import com.bank.product.workflow.domain.model.WorkflowResult;
import com.bank.product.workflow.domain.model.WorkflowSubject;
import io.temporal.workflow.QueryMethod;
import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

/**
 * Temporal workflow interface for generic approval processes
 */
@WorkflowInterface
public interface ApprovalWorkflow {

    /**
     * Main workflow method - executes the approval process
     *
     * @param subject workflow subject containing entity and metadata
     * @return workflow result
     */
    @WorkflowMethod
    WorkflowResult execute(WorkflowSubject subject);

    /**
     * Signal to approve the workflow
     *
     * @param decision approval decision with comments
     */
    @SignalMethod
    void approve(ApprovalDecision decision);

    /**
     * Signal to reject the workflow
     *
     * @param decision rejection decision with reason
     */
    @SignalMethod
    void reject(ApprovalDecision decision);

    /**
     * Signal to cancel the workflow
     *
     * @param reason cancellation reason
     */
    @SignalMethod
    void cancel(String reason);

    /**
     * Query to get current workflow status
     *
     * @return workflow subject with current state
     */
    @QueryMethod
    WorkflowSubject getStatus();

    /**
     * Query to check if workflow is complete
     *
     * @return true if workflow has completed
     */
    @QueryMethod
    boolean isComplete();
}
