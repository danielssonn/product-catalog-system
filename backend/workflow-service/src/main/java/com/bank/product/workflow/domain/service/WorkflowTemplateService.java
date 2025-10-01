package com.bank.product.workflow.domain.service;

import com.bank.product.workflow.domain.model.WorkflowTemplate;
import com.bank.product.workflow.domain.repository.WorkflowTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service for managing workflow templates
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowTemplateService {

    private final WorkflowTemplateRepository templateRepository;

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
}
