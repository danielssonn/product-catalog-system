package com.bank.product.workflow.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Workflow template defining rules and configuration for entity approval
 * Stored in MongoDB for dynamic configuration
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "workflow_templates")
public class WorkflowTemplate {
    @Id
    private String id;

    /**
     * Template identifier (e.g., "SOLUTION_CONFIG_V1")
     */
    @Indexed(unique = true)
    private String templateId;

    /**
     * Template version
     */
    private String version;

    /**
     * Template name
     */
    private String name;

    /**
     * Description
     */
    private String description;

    /**
     * Entity type this template applies to
     * (e.g., "SOLUTION_CONFIGURATION", "DOCUMENT_VERIFICATION", "LOAN_APPLICATION")
     */
    @Indexed
    private String entityType;

    /**
     * Whether this template is active
     */
    private boolean active;

    /**
     * Decision tables for rule evaluation
     */
    private List<DecisionTable> decisionTables;

    /**
     * Approver selection strategy
     */
    private ApproverSelectionStrategy approverSelectionStrategy;

    /**
     * Escalation rules
     */
    private List<EscalationRule> escalationRules;

    /**
     * Callback handler configuration
     */
    private CallbackHandlers callbackHandlers;

    /**
     * Metadata
     */
    private LocalDateTime createdAt;
    private String createdBy;
    private LocalDateTime updatedAt;
    private String updatedBy;
    private LocalDateTime publishedAt;
    private String publishedBy;
}
