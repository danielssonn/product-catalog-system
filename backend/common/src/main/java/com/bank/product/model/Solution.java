package com.bank.product.model;

import com.bank.product.enums.WorkflowSubmissionStatus;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Document(collection = "solutions")
@CompoundIndex(name = "tenant_solution_idx", def = "{'tenantId': 1, 'solutionId': 1}")
@CompoundIndex(name = "tenant_status_idx", def = "{'tenantId': 1, 'status': 1}")
public class Solution {

    @Id
    private String id;

    @Indexed
    private String tenantId;

    @Indexed
    private String solutionId;

    // Reference to master catalog product (if solution is based on catalog)
    @Indexed
    private String catalogProductId;

    // Reference to tenant configuration (if solution is from tenant config)
    @Indexed
    private String tenantConfigurationId;

    private String name;

    private String description;

    private String category;

    private ProductType type;

    private ProductStatus status;

    private PricingDetails pricing;

    private List<String> availableChannels;

    private Map<String, Object> features;

    private List<String> eligibilityCriteria;

    private Terms terms;

    private String version;

    private Integer versionNumber;

    private LocalDateTime effectiveDate;

    private LocalDateTime expirationDate;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private String createdBy;

    private String updatedBy;

    private Map<String, Object> metadata;

    // Workflow-related fields
    @Indexed
    private String workflowId;

    private WorkflowSubmissionStatus workflowSubmissionStatus;

    private String workflowErrorMessage;

    private Instant workflowRetryAt;

    // Workflow metadata (populated after submission)
    private Boolean approvalRequired;

    private Integer requiredApprovals;

    private List<String> approverRoles;

    private Boolean sequential;

    private Integer slaHours;

    private Instant estimatedCompletion;
}
