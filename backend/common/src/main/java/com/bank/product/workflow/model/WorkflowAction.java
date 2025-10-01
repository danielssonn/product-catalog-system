package com.bank.product.workflow.model;

public enum WorkflowAction {
    INITIATED,
    VALIDATED,
    VALIDATION_FAILED,
    TASK_CREATED,
    TASK_ASSIGNED,
    APPROVED,
    REJECTED,
    ESCALATED,
    CANCELLED,
    COMPLETED,
    FAILED,
    TIMED_OUT,
    RESUBMITTED,
    REMINDER_SENT
}
