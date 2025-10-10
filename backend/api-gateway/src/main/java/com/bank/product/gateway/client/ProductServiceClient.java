package com.bank.product.gateway.client;

import com.bank.product.gateway.dto.ProductConfigurationRecord;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

/**
 * Client for Product Service API
 */
@Slf4j
@Component
public class ProductServiceClient {

    private final WebClient webClient;

    public ProductServiceClient(@Value("${services.product-service.url}") String productServiceUrl) {
        this.webClient = WebClient.builder()
                .baseUrl(productServiceUrl)
                .build();
    }

    /**
     * Configure a solution by posting to product-service
     */
    public Mono<ConfigureSolutionResponse> configureSolution(
            String tenantId,
            String userId,
            ProductConfigurationRecord record) {

        log.debug("Configuring solution for tenant {}: {}", tenantId, record.getSolutionName());

        Map<String, Object> request = buildRequest(record);

        return webClient.post()
                .uri("/api/v1/solutions/configure")
                .header("X-Tenant-ID", tenantId)
                .header("X-User-ID", userId)
                .headers(headers -> headers.setBasicAuth("system", "system123")) // Use system service account
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(ConfigureSolutionResponse.class)
                .doOnSuccess(response -> log.info("Solution configured: solutionId={}, workflowId={}",
                        response.getSolutionId(), response.getWorkflowId()))
                .doOnError(error -> log.error("Failed to configure solution for record at line {}: {}",
                        record.getLineNumber(), error.getMessage()));
    }

    private Map<String, Object> buildRequest(ProductConfigurationRecord record) {
        Map<String, Object> request = new HashMap<>();
        request.put("catalogProductId", record.getCatalogProductId());
        request.put("solutionName", record.getSolutionName());
        request.put("description", record.getDescription());

        if (record.getCustomInterestRate() != null) {
            request.put("customInterestRate", record.getCustomInterestRate());
        }
        if (record.getCustomFees() != null && !record.getCustomFees().isEmpty()) {
            request.put("customFees", record.getCustomFees());
        }
        if (record.getCustomTerms() != null && !record.getCustomTerms().isEmpty()) {
            request.put("customTerms", record.getCustomTerms());
        }
        if (record.getRiskLevel() != null) {
            request.put("riskLevel", record.getRiskLevel());
        }
        if (record.getPricingVariance() != null) {
            request.put("pricingVariance", record.getPricingVariance());
        }
        if (record.getBusinessJustification() != null && !record.getBusinessJustification().isEmpty()) {
            request.put("businessJustification", record.getBusinessJustification());
        }
        if (record.getPriority() != null) {
            request.put("priority", record.getPriority());
        }

        return request;
    }

    /**
     * Response from configure solution endpoint
     */
    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    @lombok.Builder
    public static class ConfigureSolutionResponse {
        private String solutionId;
        private String solutionName;
        private String status;
        private String workflowId;
        private String workflowStatus;
        private String message;
        private String pollingUrl;
    }
}
