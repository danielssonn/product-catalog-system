package com.bank.product.workflow.agent.mcp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Single reasoning step for MCP agent
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MCPReasoningStep {

    /**
     * Step identifier
     */
    private String step;

    /**
     * MCP tool to invoke
     */
    private String tool;

    /**
     * Input source (JSONPath expression)
     * e.g., "$.entityData.financialStatements"
     */
    private String input;

    /**
     * Additional parameters for tool
     */
    private Map<String, Object> parameters;

    /**
     * Description of what this step does
     */
    private String description;

    /**
     * Whether this step is optional
     */
    private boolean optional;

    /**
     * Condition for executing this step (SpEL expression)
     */
    private String condition;
}
