package com.bank.product.workflow.dto;

import com.bank.product.workflow.domain.model.ApproverSelectionStrategy;
import com.bank.product.workflow.domain.model.CallbackHandlers;
import com.bank.product.workflow.domain.model.DecisionTable;
import com.bank.product.workflow.domain.model.EscalationRule;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request to update an existing workflow template
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateTemplateRequest {

    @NotBlank(message = "Template name is required")
    private String name;

    private String description;

    @NotBlank(message = "Version is required")
    private String version;

    @NotBlank(message = "Entity type is required")
    private String entityType;

    @NotEmpty(message = "At least one decision table is required")
    @Valid
    private List<DecisionTable> decisionTables;

    private ApproverSelectionStrategy approverSelectionStrategy;

    private List<EscalationRule> escalationRules;

    @NotNull(message = "Callback handlers configuration is required")
    @Valid
    private CallbackHandlers callbackHandlers;

    private String updatedBy;
}
