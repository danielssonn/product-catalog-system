package com.bank.product.version;

import com.bank.product.domain.solution.dto.ConfigureSolutionRequest;
import com.bank.product.domain.solution.dto.ConfigureSolutionResponse;
import com.bank.product.domain.solution.dto.v2.ConfigureSolutionRequestV2;
import com.bank.product.domain.solution.dto.v2.ConfigureSolutionResponseV2;
import org.springframework.stereotype.Component;

import java.util.HashMap;

/**
 * Version Transformation Service
 * Transforms requests and responses between API versions
 */
@Component
public class VersionTransformer {

    /**
     * Transform V1 request to V2 format
     */
    public ConfigureSolutionRequestV2 v1ToV2Request(ConfigureSolutionRequest v1) {
        return ConfigureSolutionRequestV2.builder()
                .catalogProductId(v1.getCatalogProductId())
                .solutionName(v1.getSolutionName())
                .description(v1.getDescription())
                .customInterestRate(v1.getCustomInterestRate())
                .customFeesFX(v1.getCustomFees())  // Transform: customFees -> customFeesFX
                .customTerms(v1.getCustomTerms())
                .riskLevel(v1.getRiskLevel())
                .pricingVariance(v1.getPricingVariance())
                .businessJustification(v1.getBusinessJustification())
                .priority(v1.getPriority())
                .metadata(new HashMap<>())  // New in v2
                .build();
    }

    /**
     * Transform V2 request to V1 format
     */
    public ConfigureSolutionRequest v2ToV1Request(ConfigureSolutionRequestV2 v2) {
        return ConfigureSolutionRequest.builder()
                .catalogProductId(v2.getCatalogProductId())
                .solutionName(v2.getSolutionName())
                .description(v2.getDescription())
                .customInterestRate(v2.getCustomInterestRate())
                .customFees(v2.getCustomFeesFX())  // Transform: customFeesFX -> customFees
                .customTerms(v2.getCustomTerms())
                .riskLevel(v2.getRiskLevel())
                .pricingVariance(v2.getPricingVariance())
                .businessJustification(v2.getBusinessJustification())
                .priority(v2.getPriority())
                .build();
    }

    /**
     * Transform V1 response to V2 format
     */
    public ConfigureSolutionResponseV2 v1ToV2Response(ConfigureSolutionResponse v1) {
        return ConfigureSolutionResponseV2.builder()
                .solutionId(v1.getSolutionId())
                .solutionName(v1.getSolutionName())
                .status(v1.getStatus())
                .workflowId(v1.getWorkflowId())
                .workflowStatus(v1.getWorkflowStatus())
                .approvalRequired(v1.isApprovalRequired())
                .requiredApprovals(v1.getRequiredApprovals())
                .approverRoles(v1.getApproverRoles())
                .sequential(v1.isSequential())
                .slaHours(v1.getSlaHours())
                .estimatedCompletion(v1.getEstimatedCompletion())
                .message(v1.getMessage())
                .metadata(new HashMap<>())  // New in v2
                .build();
    }

    /**
     * Transform V2 response to V1 format
     */
    public ConfigureSolutionResponse v2ToV1Response(ConfigureSolutionResponseV2 v2) {
        return ConfigureSolutionResponse.builder()
                .solutionId(v2.getSolutionId())
                .solutionName(v2.getSolutionName())
                .status(v2.getStatus())
                .workflowId(v2.getWorkflowId())
                .workflowStatus(v2.getWorkflowStatus())
                .approvalRequired(v2.isApprovalRequired())
                .requiredApprovals(v2.getRequiredApprovals())
                .approverRoles(v2.getApproverRoles())
                .sequential(v2.isSequential())
                .slaHours(v2.getSlaHours())
                .estimatedCompletion(v2.getEstimatedCompletion())
                .message(v2.getMessage())
                // metadata field removed in v1
                .build();
    }
}
