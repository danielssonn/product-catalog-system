package com.bank.product.workflow.domain.service;

import com.bank.product.workflow.domain.model.ComputedApprovalPlan;
import com.bank.product.workflow.domain.model.WorkflowTemplate;
import com.bank.product.workflow.domain.repository.WorkflowTemplateRepository;
import com.bank.product.workflow.dto.*;
import com.bank.product.workflow.engine.RuleEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service for managing workflow templates
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowTemplateService {

    private final WorkflowTemplateRepository templateRepository;
    private final RuleEngine ruleEngine;

    /**
     * Create a new workflow template
     */
    @Transactional
    public WorkflowTemplate createTemplate(WorkflowTemplate template) {
        log.info("Creating workflow template: {}", template.getTemplateId());

        // Check if template already exists
        if (templateRepository.existsByTemplateId(template.getTemplateId())) {
            throw new IllegalArgumentException("Template with ID '" + template.getTemplateId() + "' already exists");
        }

        // Set metadata
        template.setCreatedAt(LocalDateTime.now());
        template.setActive(false); // New templates start as inactive

        return templateRepository.save(template);
    }

    /**
     * Update an existing workflow template (creates new version)
     */
    @Transactional
    public WorkflowTemplate updateTemplate(String templateId, WorkflowTemplate updatedTemplate) {
        log.info("Updating workflow template: {}", templateId);

        WorkflowTemplate existing = getTemplateByTemplateId(templateId);

        // Preserve original metadata
        updatedTemplate.setId(existing.getId());
        updatedTemplate.setCreatedAt(existing.getCreatedAt());
        updatedTemplate.setCreatedBy(existing.getCreatedBy());
        updatedTemplate.setUpdatedAt(LocalDateTime.now());

        return templateRepository.save(updatedTemplate);
    }

    /**
     * Publish a template (make it active)
     */
    @Transactional
    public WorkflowTemplate publishTemplate(String templateId, String publishedBy) {
        log.info("Publishing workflow template: {}", templateId);

        WorkflowTemplate template = getTemplateByTemplateId(templateId);

        // Deactivate other templates for the same entity type
        List<WorkflowTemplate> existingTemplates = templateRepository.findByEntityType(template.getEntityType());
        for (WorkflowTemplate existing : existingTemplates) {
            if (existing.isActive()) {
                existing.setActive(false);
                templateRepository.save(existing);
                log.info("Deactivated previous template: {}", existing.getTemplateId());
            }
        }

        // Activate this template
        template.setActive(true);
        template.setPublishedAt(LocalDateTime.now());
        template.setPublishedBy(publishedBy);

        return templateRepository.save(template);
    }

    /**
     * Get template by template ID
     */
    public WorkflowTemplate getTemplateByTemplateId(String templateId) {
        return templateRepository.findByTemplateId(templateId)
                .orElseThrow(() -> new IllegalArgumentException("Template not found: " + templateId));
    }

    /**
     * Get active template for entity type
     */
    public Optional<WorkflowTemplate> getActiveTemplateForEntityType(String entityType) {
        return templateRepository.findByEntityTypeAndActiveTrue(entityType);
    }

    /**
     * Get all active templates
     */
    public List<WorkflowTemplate> getAllActiveTemplates() {
        return templateRepository.findByActiveTrue();
    }

    /**
     * Get all templates for entity type
     */
    public List<WorkflowTemplate> getTemplatesByEntityType(String entityType) {
        return templateRepository.findByEntityType(entityType);
    }

    /**
     * Delete a template
     */
    @Transactional
    public void deleteTemplate(String templateId) {
        log.info("Deleting workflow template: {}", templateId);

        WorkflowTemplate template = getTemplateByTemplateId(templateId);

        if (template.isActive()) {
            throw new IllegalStateException("Cannot delete active template. Deactivate first.");
        }

        templateRepository.delete(template);
    }

    /**
     * Deactivate a template
     */
    @Transactional
    public WorkflowTemplate deactivateTemplate(String templateId) {
        log.info("Deactivating workflow template: {}", templateId);

        WorkflowTemplate template = getTemplateByTemplateId(templateId);
        template.setActive(false);

        return templateRepository.save(template);
    }

    /**
     * Test a template with sample metadata
     */
    public TestTemplateResponse testTemplate(String templateId, TestTemplateRequest request) {
        log.info("Testing template: {}", templateId);

        WorkflowTemplate template = getTemplateByTemplateId(templateId);
        List<String> validationErrors = new ArrayList<>();
        List<String> executionTrace = new ArrayList<>();

        try {
            // Evaluate template with test metadata
            executionTrace.add("Starting template evaluation for: " + templateId);
            executionTrace.add("Input metadata: " + request.getEntityMetadata());

            ComputedApprovalPlan approvalPlan = ruleEngine.evaluate(template, request.getEntityMetadata());

            executionTrace.add("Evaluation completed successfully");
            executionTrace.add("Approval required: " + approvalPlan.isApprovalRequired());
            executionTrace.add("Matched rules: " + approvalPlan.getMatchedRules());

            return TestTemplateResponse.builder()
                    .templateId(templateId)
                    .entityType(template.getEntityType())
                    .inputMetadata(request.getEntityMetadata())
                    .matchedRules(approvalPlan.getMatchedRules())
                    .approvalPlan(approvalPlan)
                    .valid(true)
                    .validationErrors(List.of())
                    .executionTrace(executionTrace)
                    .build();

        } catch (Exception e) {
            log.error("Template test failed: {}", e.getMessage(), e);
            validationErrors.add(e.getMessage());
            executionTrace.add("ERROR: " + e.getMessage());

            return TestTemplateResponse.builder()
                    .templateId(templateId)
                    .entityType(template.getEntityType())
                    .inputMetadata(request.getEntityMetadata())
                    .valid(false)
                    .validationErrors(validationErrors)
                    .executionTrace(executionTrace)
                    .build();
        }
    }

    /**
     * Get all templates (with pagination support if needed)
     */
    public List<WorkflowTemplate> getAllTemplates() {
        return templateRepository.findAll();
    }

    /**
     * Convert WorkflowTemplate to TemplateResponse DTO
     */
    public TemplateResponse toResponse(WorkflowTemplate template) {
        return TemplateResponse.builder()
                .id(template.getId())
                .templateId(template.getTemplateId())
                .version(template.getVersion())
                .name(template.getName())
                .description(template.getDescription())
                .entityType(template.getEntityType())
                .active(template.isActive())
                .decisionTables(template.getDecisionTables())
                .approverSelectionStrategy(template.getApproverSelectionStrategy())
                .escalationRules(template.getEscalationRules())
                .callbackHandlers(template.getCallbackHandlers())
                .createdAt(template.getCreatedAt())
                .createdBy(template.getCreatedBy())
                .updatedAt(template.getUpdatedAt())
                .updatedBy(template.getUpdatedBy())
                .publishedAt(template.getPublishedAt())
                .publishedBy(template.getPublishedBy())
                .build();
    }

    /**
     * Convert list of templates to responses
     */
    public List<TemplateResponse> toResponses(List<WorkflowTemplate> templates) {
        return templates.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Map CreateTemplateRequest to WorkflowTemplate
     */
    public WorkflowTemplate fromCreateRequest(CreateTemplateRequest request) {
        return WorkflowTemplate.builder()
                .templateId(request.getTemplateId())
                .version(request.getVersion())
                .name(request.getName())
                .description(request.getDescription())
                .entityType(request.getEntityType())
                .decisionTables(request.getDecisionTables())
                .approverSelectionStrategy(request.getApproverSelectionStrategy())
                .escalationRules(request.getEscalationRules())
                .callbackHandlers(request.getCallbackHandlers())
                .createdBy(request.getCreatedBy())
                .active(false)
                .build();
    }

    /**
     * Map UpdateTemplateRequest to WorkflowTemplate
     */
    public WorkflowTemplate fromUpdateRequest(String templateId, UpdateTemplateRequest request) {
        return WorkflowTemplate.builder()
                .templateId(templateId)
                .version(request.getVersion())
                .name(request.getName())
                .description(request.getDescription())
                .entityType(request.getEntityType())
                .decisionTables(request.getDecisionTables())
                .approverSelectionStrategy(request.getApproverSelectionStrategy())
                .escalationRules(request.getEscalationRules())
                .callbackHandlers(request.getCallbackHandlers())
                .updatedBy(request.getUpdatedBy())
                .build();
    }
}
