package com.bank.product.workflow.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
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
    private String workflowId;

    /**
     * Workflow type
     */
    private WorkflowType workflowType;

    /**
     * Tenant context
     */
    private String tenantId;

    /**
     * Timestamp of action
     */
    private LocalDateTime timestamp;

    /**
     * Action performed
     */
    private WorkflowAction action;

    /**
     * User who performed the action
     */
    private String performedBy;

    /**
     * Role of the user
     */
    private String userRole;

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
     * IP address
     */
    private String ipAddress;

    /**
     * Session ID
     */
    private String sessionId;
}
