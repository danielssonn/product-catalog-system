package com.bank.product.workflow.domain.controller;

import com.bank.product.workflow.domain.model.WorkflowTemplate;
import com.bank.product.workflow.domain.service.WorkflowTemplateService;
import com.bank.product.workflow.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for workflow template management
 * Provides CRUD operations, testing, and publishing capabilities
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/workflow-templates")
@RequiredArgsConstructor
public class WorkflowTemplateController {

    private final WorkflowTemplateService templateService;

    /**
     * Create a new workflow template
     * Requires ADMIN role
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TemplateResponse> createTemplate(
            @Valid @RequestBody CreateTemplateRequest request) {

        log.info("Creating workflow template: {}", request.getTemplateId());

        WorkflowTemplate template = templateService.fromCreateRequest(request);
        WorkflowTemplate created = templateService.createTemplate(template);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(templateService.toResponse(created));
    }

    /**
     * Get all workflow templates
     */
    @GetMapping
    public ResponseEntity<List<TemplateResponse>> getAllTemplates(
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) Boolean active) {

        log.debug("Getting workflow templates - entityType: {}, active: {}", entityType, active);

        List<WorkflowTemplate> templates;

        if (entityType != null && active != null && active) {
            templates = templateService.getActiveTemplateForEntityType(entityType)
                    .map(List::of)
                    .orElse(List.of());
        } else if (entityType != null) {
            templates = templateService.getTemplatesByEntityType(entityType);
        } else if (active != null && active) {
            templates = templateService.getAllActiveTemplates();
        } else {
            templates = templateService.getAllTemplates();
        }

        return ResponseEntity.ok(templateService.toResponses(templates));
    }

    /**
     * Get a specific workflow template by template ID
     */
    @GetMapping("/{templateId}")
    public ResponseEntity<TemplateResponse> getTemplate(
            @PathVariable String templateId) {

        log.debug("Getting workflow template: {}", templateId);

        WorkflowTemplate template = templateService.getTemplateByTemplateId(templateId);

        return ResponseEntity.ok(templateService.toResponse(template));
    }

    /**
     * Update an existing workflow template
     * Requires ADMIN role
     */
    @PutMapping("/{templateId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TemplateResponse> updateTemplate(
            @PathVariable String templateId,
            @Valid @RequestBody UpdateTemplateRequest request) {

        log.info("Updating workflow template: {}", templateId);

        WorkflowTemplate updatedTemplate = templateService.fromUpdateRequest(templateId, request);
        WorkflowTemplate saved = templateService.updateTemplate(templateId, updatedTemplate);

        return ResponseEntity.ok(templateService.toResponse(saved));
    }

    /**
     * Delete a workflow template
     * Requires ADMIN role
     * Can only delete inactive templates
     */
    @DeleteMapping("/{templateId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteTemplate(
            @PathVariable String templateId) {

        log.info("Deleting workflow template: {}", templateId);

        templateService.deleteTemplate(templateId);

        return ResponseEntity.noContent().build();
    }

    /**
     * Publish a workflow template (activate it)
     * Requires ADMIN role
     * Deactivates other templates for the same entity type
     */
    @PostMapping("/{templateId}/publish")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TemplateResponse> publishTemplate(
            @PathVariable String templateId,
            @Valid @RequestBody PublishTemplateRequest request) {

        log.info("Publishing workflow template: {} by {}", templateId, request.getPublishedBy());

        WorkflowTemplate published = templateService.publishTemplate(
                templateId,
                request.getPublishedBy()
        );

        return ResponseEntity.ok(templateService.toResponse(published));
    }

    /**
     * Deactivate a workflow template
     * Requires ADMIN role
     */
    @PostMapping("/{templateId}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TemplateResponse> deactivateTemplate(
            @PathVariable String templateId) {

        log.info("Deactivating workflow template: {}", templateId);

        WorkflowTemplate deactivated = templateService.deactivateTemplate(templateId);

        return ResponseEntity.ok(templateService.toResponse(deactivated));
    }

    /**
     * Test a workflow template with sample metadata
     * This endpoint allows testing rule evaluation before publishing
     */
    @PostMapping("/{templateId}/test")
    public ResponseEntity<TestTemplateResponse> testTemplate(
            @PathVariable String templateId,
            @Valid @RequestBody TestTemplateRequest request) {

        log.info("Testing workflow template: {}", templateId);

        TestTemplateResponse response = templateService.testTemplate(templateId, request);

        return ResponseEntity.ok(response);
    }

    /**
     * Get active template for a specific entity type
     * Useful for checking which template will be used for an entity
     */
    @GetMapping("/active/{entityType}")
    public ResponseEntity<TemplateResponse> getActiveTemplateForEntityType(
            @PathVariable String entityType) {

        log.debug("Getting active template for entity type: {}", entityType);

        return templateService.getActiveTemplateForEntityType(entityType)
                .map(template -> ResponseEntity.ok(templateService.toResponse(template)))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Validate template configuration without creating it
     * Useful for pre-validation in UI
     */
    @PostMapping("/validate")
    public ResponseEntity<ValidationResponse> validateTemplate(
            @Valid @RequestBody CreateTemplateRequest request) {

        log.debug("Validating workflow template configuration: {}", request.getTemplateId());

        // Perform validation checks
        List<String> errors = validateTemplateRequest(request);

        ValidationResponse response = ValidationResponse.builder()
                .valid(errors.isEmpty())
                .errors(errors)
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Validate template request
     */
    private List<String> validateTemplateRequest(CreateTemplateRequest request) {
        List<String> errors = new java.util.ArrayList<>();

        // Check if template ID already exists
        try {
            templateService.getTemplateByTemplateId(request.getTemplateId());
            errors.add("Template with ID '" + request.getTemplateId() + "' already exists");
        } catch (IllegalArgumentException e) {
            // Template doesn't exist - this is good for new templates
        }

        // Validate decision tables
        if (request.getDecisionTables() == null || request.getDecisionTables().isEmpty()) {
            errors.add("At least one decision table is required");
        }

        // Validate callback handlers
        if (request.getCallbackHandlers() == null) {
            errors.add("Callback handlers configuration is required");
        }

        return errors;
    }
}
