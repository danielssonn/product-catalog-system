package com.bank.product.workflow.agent.mcp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Configuration for MCP (Model Context Protocol) agent
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MCPAgentConfig {

    /**
     * MCP server URL
     */
    private String mcpServerUrl;

    /**
     * Model to use (e.g., "claude-3-7-sonnet")
     */
    private String model;

    /**
     * Available MCP tools
     */
    private List<String> tools;

    /**
     * Multi-step reasoning configuration
     */
    private List<MCPReasoningStep> reasoningSteps;

    /**
     * Timeout for MCP calls (milliseconds)
     */
    private int timeout;

    /**
     * HTTP headers for MCP requests
     */
    private Map<String, String> headers;

    /**
     * Temperature for model (0.0 - 1.0)
     */
    private double temperature;

    /**
     * Max tokens for response
     */
    private int maxTokens;

    /**
     * System prompt for agent
     */
    private String systemPrompt;

    /**
     * Additional model parameters
     */
    private Map<String, Object> modelParameters;
}
