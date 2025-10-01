package com.bank.product.workflow.agent;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Individual reasoning step in agent execution
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentReasoningStep {

    /**
     * Step number in sequence
     */
    private int stepNumber;

    /**
     * Step name/description
     */
    private String stepName;

    /**
     * Tool invoked (for MCP agents)
     */
    private String tool;

    /**
     * Input to the step
     */
    private Map<String, Object> input;

    /**
     * Output from the step
     */
    private Map<String, Object> output;

    /**
     * Agent's reasoning for this step
     */
    private String reasoning;

    /**
     * When the step was executed
     */
    private LocalDateTime timestamp;

    /**
     * Step duration
     */
    private Duration duration;

    /**
     * Whether step was successful
     */
    private boolean success;

    /**
     * Error message if failed
     */
    private String errorMessage;
}
