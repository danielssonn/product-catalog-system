package com.bank.product.workflow.domain.repository;

import com.bank.product.workflow.domain.model.ApprovalTask;
import com.bank.product.workflow.domain.model.TaskStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for ApprovalTask
 */
@Repository
public interface ApprovalTaskRepository extends MongoRepository<ApprovalTask, String> {

    /**
     * Find by task ID
     */
    Optional<ApprovalTask> findByTaskId(String taskId);

    /**
     * Find all tasks for a workflow
     */
    List<ApprovalTask> findByWorkflowId(String workflowId);

    /**
     * Find tasks assigned to user
     */
    Page<ApprovalTask> findByAssignedTo(String assignedTo, Pageable pageable);

    /**
     * Find tasks by status
     */
    Page<ApprovalTask> findByStatus(TaskStatus status, Pageable pageable);

    /**
     * Find tasks by status and assigned user
     */
    Page<ApprovalTask> findByStatusAndAssignedTo(TaskStatus status, String assignedTo, Pageable pageable);

    /**
     * Find overdue tasks
     */
    List<ApprovalTask> findByStatusAndDueDateBefore(TaskStatus status, LocalDateTime before);

    /**
     * Find tasks by tenant
     */
    Page<ApprovalTask> findByTenantId(String tenantId, Pageable pageable);

    /**
     * Find tasks by tenant and status
     */
    Page<ApprovalTask> findByTenantIdAndStatus(String tenantId, TaskStatus status, Pageable pageable);

    /**
     * Count pending tasks for user
     */
    long countByStatusAndAssignedTo(TaskStatus status, String assignedTo);

    /**
     * Count tasks by status
     */
    long countByStatus(TaskStatus status);
}
