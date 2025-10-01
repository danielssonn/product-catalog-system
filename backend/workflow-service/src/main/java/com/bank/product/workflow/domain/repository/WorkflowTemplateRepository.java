package com.bank.product.workflow.domain.repository;

import com.bank.product.workflow.domain.model.WorkflowTemplate;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for WorkflowTemplate
 */
@Repository
public interface WorkflowTemplateRepository extends MongoRepository<WorkflowTemplate, String> {

    /**
     * Find template by template ID
     */
    Optional<WorkflowTemplate> findByTemplateId(String templateId);

    /**
     * Find active template by entity type
     */
    Optional<WorkflowTemplate> findByEntityTypeAndActiveTrue(String entityType);

    /**
     * Find all active templates
     */
    List<WorkflowTemplate> findByActiveTrue();

    /**
     * Find all templates for an entity type
     */
    List<WorkflowTemplate> findByEntityType(String entityType);

    /**
     * Check if template exists
     */
    boolean existsByTemplateId(String templateId);
}
