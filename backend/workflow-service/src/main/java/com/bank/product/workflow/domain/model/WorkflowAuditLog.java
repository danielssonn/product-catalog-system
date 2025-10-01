package com.bank.product.workflow.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Audit log entry for workflow state changes
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "workflow_audit_logs")
public class WorkflowAuditLog {
    @Id
    private String id;

    /**
     * Workflow ID
     */
    @Indexed
    private String workflowId;

    /**
     * Timestamp
     */
    @Indexed
    private LocalDateTime timestamp;

    /**
     * Action performed
     */
    private String action;

    /**
     * User who performed the action
     */
    @Indexed
    private String performedBy;

    /**
     * Previous state
     */
    private WorkflowState previousState;

    /**
     * New state
     */
    private WorkflowState newState;

    /**
     * Comments
     */
    private String comments;

    /**
     * Additional metadata
     */
    private Map<String, Object> metadata;

    /**
     * Tenant context
     */
    @Indexed
    private String tenantId;
}
