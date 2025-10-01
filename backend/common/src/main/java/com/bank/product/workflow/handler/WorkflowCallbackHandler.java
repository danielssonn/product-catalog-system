package com.bank.product.workflow.handler;

/**
 * Interface for workflow callback handlers
 * Implementations handle entity-specific actions on workflow events
 */
public interface WorkflowCallbackHandler {

    /**
     * Handle workflow callback
     * @param context Workflow execution context
     * @throws WorkflowException if handling fails
     */
    void handle(WorkflowContext context) throws WorkflowException;

    /**
     * Get handler name for registration
     */
    default String getHandlerName() {
        return this.getClass().getSimpleName();
    }

    /**
     * Whether this handler supports async execution
     */
    default boolean supportsAsync() {
        return false;
    }
}
