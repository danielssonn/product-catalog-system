package com.bank.product.events;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Map;

/**
 * Event published when a solution is created and needs workflow approval
 */
@Data
@EqualsAndHashCode(callSuper = true)
@JsonTypeName("SolutionCreatedEvent")
public class SolutionCreatedEvent extends DomainEvent {

    private final String solutionId;
    private final String tenantId;
    private final String catalogProductId;
    private final String solutionName;
    private final String description;
    private final String category;
    private final String createdBy;

    // Metadata for workflow rule evaluation
    private final double pricingVariance;
    private final String riskLevel;
    private final String businessJustification;
    private final String priority;
    private final Map<String, Object> additionalMetadata;

    @Builder
    public SolutionCreatedEvent(String solutionId, String tenantId, String catalogProductId,
                                 String solutionName, String description, String category,
                                 String createdBy, double pricingVariance, String riskLevel,
                                 String businessJustification, String priority,
                                 Map<String, Object> additionalMetadata) {
        super();
        this.solutionId = solutionId;
        this.tenantId = tenantId;
        this.catalogProductId = catalogProductId;
        this.solutionName = solutionName;
        this.description = description;
        this.category = category;
        this.createdBy = createdBy;
        this.pricingVariance = pricingVariance;
        this.riskLevel = riskLevel;
        this.businessJustification = businessJustification;
        this.priority = priority;
        this.additionalMetadata = additionalMetadata;
    }
}
