package com.bank.product.workflow.validation.mcp;

import com.bank.product.workflow.domain.model.WorkflowSubject;
import com.bank.product.workflow.validation.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.anthropic.AnthropicChatOptions;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

/**
 * MCP-based validator service using Claude via Spring AI
 * Provides LLM-powered semantic document analysis
 * Only enabled when ANTHROPIC_API_KEY is configured
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spring.ai.anthropic.api-key")
public class MCPValidatorService {

    private final AnthropicChatModel chatModel;
    private final MCPPromptBuilder promptBuilder;
    private final MCPResponseParser responseParser;

    /**
     * Execute MCP-powered document validation using Claude
     */
    public ValidationResult validateDocuments(WorkflowSubject subject, ValidatorConfig config) {
        log.info("Executing MCP document validation: {} for workflow: {}",
                config.getValidatorId(), subject.getWorkflowId());

        LocalDateTime startTime = LocalDateTime.now();

        try {
            // 1. Build analysis prompt
            String prompt = promptBuilder.buildDocumentAnalysisPrompt(subject, config);

            log.debug("MCP Prompt ({}): {}", subject.getWorkflowId(), prompt);

            // 2. Configure Claude options
            AnthropicChatOptions options = AnthropicChatOptions.builder()
                    .withModel(config.getMcpModel() != null ?
                        config.getMcpModel() : "claude-sonnet-4-5-20250929")
                    .withTemperature(config.getMcpTemperature() != null ?
                        config.getMcpTemperature() : 0.3)
                    .withMaxTokens(config.getMcpMaxTokens() != null ?
                        config.getMcpMaxTokens() : 4096)
                    .build();

            // 3. Call Claude via Spring AI
            log.debug("Calling Claude model: {}", options.getModel());
            ChatResponse response = chatModel.call(new Prompt(prompt, options));

            LocalDateTime endTime = LocalDateTime.now();
            Duration executionTime = Duration.between(startTime, endTime);

            log.info("Claude response received in {}ms", executionTime.toMillis());

            // 4. Parse structured response
            ValidationResult result = responseParser.parseResponse(
                response,
                subject,
                config,
                startTime,
                executionTime
            );

            log.info("MCP validation completed: redFlag={}, confidence={}",
                result.isRedFlagDetected(),
                result.getConfidenceScore());

            return result;

        } catch (Exception e) {
            log.error("MCP document validation failed: {}", e.getMessage(), e);

            LocalDateTime endTime = LocalDateTime.now();
            Duration executionTime = Duration.between(startTime, endTime);

            return ValidationResult.builder()
                    .validatorId(config.getValidatorId())
                    .validatorType(ValidatorType.MCP)
                    .executedAt(startTime)
                    .executionTime(executionTime)
                    .success(false)
                    .errorMessage("MCP validation failed: " + e.getMessage())
                    .build();
        }
    }
}
