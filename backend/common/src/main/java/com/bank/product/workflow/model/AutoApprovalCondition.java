package com.bank.product.workflow.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AutoApprovalCondition {

    /**
     * Whether auto-approval is enabled
     */
    private boolean enabled;

    /**
     * Maximum amount for auto-approval
     */
    private BigDecimal maxAmount;

    /**
     * Minimum risk level for auto-approval
     */
    private RiskLevel maxRiskLevel;

    /**
     * Specific conditions that must be met
     */
    private Map<String, Object> conditions;

    /**
     * Roles that can use auto-approval
     */
    private String[] allowedRoles;

    /**
     * Time window for auto-approval
     */
    private String timeWindow;
}
