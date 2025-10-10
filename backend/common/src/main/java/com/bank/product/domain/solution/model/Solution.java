package com.bank.product.domain.solution.model;

import com.bank.product.core.model.CoreProvisioningRecord;
import com.bank.product.enums.WorkflowSubmissionStatus;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Tenant's Solution Instance - An active solution offering for a specific tenant
 * This is derived from TenantSolutionConfiguration after activation
 */
@Data
@Document(collection = "solutions")
@CompoundIndex(name = "tenant_solution_idx", def = "{'tenantId': 1, 'solutionId': 1}")
@CompoundIndex(name = "tenant_status_idx", def = "{'tenantId': 1, 'status': 1}")
@CompoundIndex(name = "tenant_catalog_idx", def = "{'tenantId': 1, 'catalogProductId': 1}")
public class Solution {

    @Id
    private String id;

    @Indexed
    private String tenantId;

    @Indexed
    private String solutionId;

    // Reference to master catalog product
    @Indexed
    private String catalogProductId;

    // Reference to tenant configuration
    @Indexed
    private String configurationId;

    // Workflow tracking
    @Indexed
    private String workflowId;

    private String name;

    private String description;

    private String category;

    private SolutionStatus status;

    private PricingDetails pricing;

    private List<String> availableChannels;

    private Map<String, Object> features;

    private List<String> eligibilityCriteria;

    private SolutionTerms terms;

    // Version tracking
    private String version;

    private Integer versionNumber;

    private LocalDateTime effectiveDate;

    private LocalDateTime expirationDate;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private String createdBy;

    private String updatedBy;

    private Map<String, Object> metadata;

    // Workflow submission status tracking
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

    // Core banking system provisioning
    /**
     * Provisioning records for each core system this solution is provisioned to.
     * Supports multi-core and geo-distributed deployments.
     */
    private List<CoreProvisioningRecord> coreProvisioningRecords = new ArrayList<>();
}