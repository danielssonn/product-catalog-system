package com.bank.product.workflow.domain.repository;

import com.bank.product.workflow.domain.model.WorkflowAuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for WorkflowAuditLog
 */
@Repository
public interface WorkflowAuditLogRepository extends MongoRepository<WorkflowAuditLog, String> {

    /**
     * Find all audit logs for a workflow
     */
    List<WorkflowAuditLog> findByWorkflowIdOrderByTimestampAsc(String workflowId);

    /**
     * Find audit logs by performer
     */
    Page<WorkflowAuditLog> findByPerformedBy(String performedBy, Pageable pageable);

    /**
     * Find audit logs in time range
     */
    List<WorkflowAuditLog> findByTimestampBetween(LocalDateTime start, LocalDateTime end);

    /**
     * Find audit logs by tenant
     */
    Page<WorkflowAuditLog> findByTenantId(String tenantId, Pageable pageable);

    /**
     * Find audit logs by action
     */
    List<WorkflowAuditLog> findByAction(String action);
}
