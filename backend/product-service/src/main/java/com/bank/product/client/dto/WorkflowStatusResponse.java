package com.bank.product.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Response with workflow status
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowStatusResponse {

    private String workflowId;
    private String workflowInstanceId;
    private String entityType;
    private String entityId;
    private String state;
    private List<ApprovalTaskInfo> pendingTasks;
    private List<ApprovalTaskInfo> completedTasks;
    private LocalDateTime initiatedAt;
    private LocalDateTime completedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ApprovalTaskInfo {
        private String taskId;
        private String assignedTo;
        private String requiredRole;
        private String status;
        private LocalDateTime createdAt;
        private LocalDateTime completedAt;
    }
}
