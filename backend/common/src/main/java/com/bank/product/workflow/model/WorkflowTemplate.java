package com.bank.product.workflow.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Workflow template - defines approval rules for an entity type
 * Stored in database, can be updated without code deployment
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
     * Template identifier (e.g., "DOCUMENT_APPROVAL_V1")
     */
    @Indexed(unique = true)
    private String templateId;

    /**
     * Template version for tracking changes
     */
    private String templateVersion;

    /**
     * Display name
     */
    private String name;

    /**
     * Description
     */
    private String description;

    /**
     * Entity type this template applies to
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
     * Strategy for selecting approvers
     */
    private ApproverSelectionStrategy approverSelectionStrategy;

    /**
     * Escalation rules
     */
    private List<EscalationRule> escalationRules;

    /**
     * Callback handlers (class names or bean names)
     */
    private CallbackHandlers callbackHandlers;

    /**
     * Validation rules to apply before submission
     */
    private ValidationRules validationRules;

    /**
     * Configuration options
     */
    private Map<String, Object> configuration;

    /**
     * Template metadata
     */
    private LocalDateTime createdAt;
    private String createdBy;
    private LocalDateTime updatedAt;
    private String updatedBy;
    private LocalDateTime publishedAt;
    private String publishedBy;

    /**
     * Tags for categorization
     */
    private List<String> tags;

    /**
     * Version for optimistic locking
     */
    @Version
    private Long version;
}
