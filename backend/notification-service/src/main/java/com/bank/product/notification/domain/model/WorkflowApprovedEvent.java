package com.bank.product.notification.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowApprovedEvent {

    private String workflowId;
    private String workflowInstanceId;
    private String entityType;
    private String entityId;
    private String tenantId;

    private Map<String, Object> entityData;
    private Map<String, Object> entityMetadata;

    private String initiatedBy;
    private List<ApprovalInfo> approvals;

    private LocalDateTime submittedAt;
    private LocalDateTime approvedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ApprovalInfo {
        private String approverId;
        private String approverRole;
        private String comments;
        private LocalDateTime approvedAt;
    }
}
