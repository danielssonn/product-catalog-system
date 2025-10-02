package com.bank.product.workflow.domain.repository;

import com.bank.product.workflow.domain.model.WorkflowState;
import com.bank.product.workflow.domain.model.WorkflowSubject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for WorkflowSubject
 */
@Repository
public interface WorkflowSubjectRepository extends MongoRepository<WorkflowSubject, String> {

    /**
     * Find by workflow ID
     */
    Optional<WorkflowSubject> findByWorkflowId(String workflowId);

    /**
     * Find by Temporal workflow instance ID
     */
    Optional<WorkflowSubject> findByWorkflowInstanceId(String workflowInstanceId);

    /**
     * Find by entity reference
     */
    Optional<WorkflowSubject> findByEntityTypeAndEntityId(String entityType, String entityId);

    /**
     * Find by state
     */
    List<WorkflowSubject> findByState(WorkflowState state);

    /**
     * Find by state and tenant
     */
    Page<WorkflowSubject> findByStateAndTenantId(WorkflowState state, String tenantId, Pageable pageable);

    /**
     * Find by tenant and initiated by
     */
    Page<WorkflowSubject> findByTenantIdAndInitiatedBy(String tenantId, String initiatedBy, Pageable pageable);

    /**
     * Find workflows initiated after date
     */
    List<WorkflowSubject> findByInitiatedAtAfter(LocalDateTime after);

    /**
     * Count workflows by state
     */
    long countByState(WorkflowState state);

    /**
     * Count workflows by state and tenant
     */
    long countByStateAndTenantId(WorkflowState state, String tenantId);
}
