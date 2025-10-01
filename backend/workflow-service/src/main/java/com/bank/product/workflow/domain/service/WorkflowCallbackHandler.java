package com.bank.product.workflow.domain.service;

import com.bank.product.workflow.domain.model.WorkflowSubject;

/**
 * Interface for workflow callback handlers
 * Entity-specific implementations handle approval/rejection actions
 */
public interface WorkflowCallbackHandler {

    /**
     * Handle workflow callback
     *
     * @param subject workflow subject with entity data
     * @throws Exception if callback execution fails
     */
    void handle(WorkflowSubject subject) throws Exception;
}
