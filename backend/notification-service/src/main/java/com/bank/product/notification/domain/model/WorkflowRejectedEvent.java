package com.bank.product.notification.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowRejectedEvent {

    private String workflowId;
    private String workflowInstanceId;
    private String entityType;
    private String entityId;
    private String tenantId;

    private Map<String, Object> entityData;

    private String initiatedBy;
    private String rejectedBy;
    private String rejectionReason;
    private String rejectionComments;

    private LocalDateTime submittedAt;
    private LocalDateTime rejectedAt;
}
