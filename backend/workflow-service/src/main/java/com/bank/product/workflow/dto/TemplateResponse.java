package com.bank.product.workflow.dto;

import com.bank.product.workflow.domain.model.ApproverSelectionStrategy;
import com.bank.product.workflow.domain.model.CallbackHandlers;
import com.bank.product.workflow.domain.model.DecisionTable;
import com.bank.product.workflow.domain.model.EscalationRule;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Response containing workflow template details
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TemplateResponse {

    private String id;

    private String templateId;

    private String version;

    private String name;

    private String description;

    private String entityType;

    private boolean active;

    private List<DecisionTable> decisionTables;

    private ApproverSelectionStrategy approverSelectionStrategy;

    private List<EscalationRule> escalationRules;

    private CallbackHandlers callbackHandlers;

    private LocalDateTime createdAt;

    private String createdBy;

    private LocalDateTime updatedAt;

    private String updatedBy;

    private LocalDateTime publishedAt;

    private String publishedBy;
}
