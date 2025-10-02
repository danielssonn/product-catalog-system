package com.bank.product.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Request to submit a workflow for approval
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowSubmitRequest {

    private String entityType;
    private String entityId;
    private Map<String, Object> entityData;
    private Map<String, Object> entityMetadata;
    private String initiatedBy;
    private String tenantId;
    private String templateId;
    private String businessJustification;
    private String priority;
}
