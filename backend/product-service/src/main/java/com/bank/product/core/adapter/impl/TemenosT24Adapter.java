package com.bank.product.core.adapter.impl;

import com.bank.product.core.adapter.CoreBankingAdapter;
import com.bank.product.core.model.*;
import com.bank.product.domain.solution.model.Solution;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * Adapter for Temenos T24 core banking system.
 * Reference implementation demonstrating vendor integration.
 */
@Slf4j
@Component
public class TemenosT24Adapter implements CoreBankingAdapter {

    private final RestTemplate restTemplate;

    public TemenosT24Adapter() {
        this.restTemplate = new RestTemplate();
    }

    @Override
    public CoreSystemType getType() {
        return CoreSystemType.TEMENOS_T24;
    }

    @Override
    public CoreProvisioningResult provisionProduct(Solution solution, CoreSystemConfig config) {
        log.info("Provisioning product {} in Temenos T24: {}", solution.getId(), config.getCoreSystemId());

        long startTime = System.currentTimeMillis();

        try {
            // Build T24 product creation request
            Map<String, Object> request = buildProductRequest(solution);

            // Call T24 API
            String url = config.getApiEndpoint() + "/products";
            HttpHeaders headers = buildHeaders(config);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    url, HttpMethod.POST, entity, Map.class);

            // Extract core product ID from response
            Map responseBody = response.getBody();
            String coreProductId = (String) responseBody.get("productId");

            long duration = System.currentTimeMillis() - startTime;

            log.info("Successfully provisioned product {} in T24 with ID: {}",
                    solution.getId(), coreProductId);

            return CoreProvisioningResult.builder()
                    .success(true)
                    .coreProductId(coreProductId)
                    .timestamp(Instant.now())
                    .httpStatusCode(response.getStatusCode().value())
                    .durationMs(duration)
                    .metadata(responseBody)
                    .build();

        } catch (HttpClientErrorException | HttpServerErrorException e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("Failed to provision product {} in T24: {}",
                    solution.getId(), e.getMessage());

            return CoreProvisioningResult.builder()
                    .success(false)
                    .errorMessage(e.getMessage())
                    .errorCode(String.valueOf(e.getStatusCode().value()))
                    .retryable(e.getStatusCode().is5xxServerError())
                    .timestamp(Instant.now())
                    .httpStatusCode(e.getStatusCode().value())
                    .durationMs(duration)
                    .build();

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("Unexpected error provisioning product {} in T24", solution.getId(), e);

            return CoreProvisioningResult.builder()
                    .success(false)
                    .errorMessage(e.getMessage())
                    .retryable(true)
                    .timestamp(Instant.now())
                    .durationMs(duration)
                    .build();
        }
    }

    @Override
    public CoreProvisioningResult updateProduct(Solution solution, String coreProductId, CoreSystemConfig config) {
        log.info("Updating product {} in Temenos T24: {}", coreProductId, config.getCoreSystemId());

        long startTime = System.currentTimeMillis();

        try {
            Map<String, Object> request = buildProductRequest(solution);

            String url = config.getApiEndpoint() + "/products/" + coreProductId;
            HttpHeaders headers = buildHeaders(config);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    url, HttpMethod.PUT, entity, Map.class);

            long duration = System.currentTimeMillis() - startTime;

            log.info("Successfully updated product {} in T24", coreProductId);

            return CoreProvisioningResult.builder()
                    .success(true)
                    .coreProductId(coreProductId)
                    .timestamp(Instant.now())
                    .httpStatusCode(response.getStatusCode().value())
                    .durationMs(duration)
                    .metadata(response.getBody())
                    .build();

        } catch (HttpClientErrorException | HttpServerErrorException e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("Failed to update product {} in T24: {}", coreProductId, e.getMessage());

            return CoreProvisioningResult.builder()
                    .success(false)
                    .coreProductId(coreProductId)
                    .errorMessage(e.getMessage())
                    .errorCode(String.valueOf(e.getStatusCode().value()))
                    .retryable(e.getStatusCode().is5xxServerError())
                    .timestamp(Instant.now())
                    .httpStatusCode(e.getStatusCode().value())
                    .durationMs(duration)
                    .build();

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("Unexpected error updating product {} in T24", coreProductId, e);

            return CoreProvisioningResult.builder()
                    .success(false)
                    .coreProductId(coreProductId)
                    .errorMessage(e.getMessage())
                    .retryable(true)
                    .timestamp(Instant.now())
                    .durationMs(duration)
                    .build();
        }
    }

    @Override
    public CoreProvisioningResult deactivateProduct(String coreProductId, CoreSystemConfig config) {
        log.info("Deactivating product {} in Temenos T24: {}", coreProductId, config.getCoreSystemId());

        long startTime = System.currentTimeMillis();

        try {
            String url = config.getApiEndpoint() + "/products/" + coreProductId + "/deactivate";
            HttpHeaders headers = buildHeaders(config);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    url, HttpMethod.POST, entity, Map.class);

            long duration = System.currentTimeMillis() - startTime;

            log.info("Successfully deactivated product {} in T24", coreProductId);

            return CoreProvisioningResult.builder()
                    .success(true)
                    .coreProductId(coreProductId)
                    .timestamp(Instant.now())
                    .httpStatusCode(response.getStatusCode().value())
                    .durationMs(duration)
                    .build();

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("Failed to deactivate product {} in T24: {}", coreProductId, e.getMessage());

            return CoreProvisioningResult.builder()
                    .success(false)
                    .coreProductId(coreProductId)
                    .errorMessage(e.getMessage())
                    .retryable(true)
                    .timestamp(Instant.now())
                    .durationMs(duration)
                    .build();
        }
    }

    @Override
    public CoreProvisioningResult sunsetProduct(String coreProductId, CoreSystemConfig config) {
        log.info("Sunsetting product {} in Temenos T24: {}", coreProductId, config.getCoreSystemId());

        long startTime = System.currentTimeMillis();

        try {
            String url = config.getApiEndpoint() + "/products/" + coreProductId;
            HttpHeaders headers = buildHeaders(config);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    url, HttpMethod.DELETE, entity, Map.class);

            long duration = System.currentTimeMillis() - startTime;

            log.info("Successfully sunset product {} in T24", coreProductId);

            return CoreProvisioningResult.builder()
                    .success(true)
                    .coreProductId(coreProductId)
                    .timestamp(Instant.now())
                    .httpStatusCode(response.getStatusCode().value())
                    .durationMs(duration)
                    .build();

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("Failed to sunset product {} in T24: {}", coreProductId, e.getMessage());

            return CoreProvisioningResult.builder()
                    .success(false)
                    .coreProductId(coreProductId)
                    .errorMessage(e.getMessage())
                    .retryable(false) // Sunset failures are typically not retryable
                    .timestamp(Instant.now())
                    .durationMs(duration)
                    .build();
        }
    }

    @Override
    public boolean verifyProductExists(String coreProductId, CoreSystemConfig config) {
        log.debug("Verifying product {} exists in T24: {}", coreProductId, config.getCoreSystemId());

        try {
            String url = config.getApiEndpoint() + "/products/" + coreProductId;
            HttpHeaders headers = buildHeaders(config);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    url, HttpMethod.HEAD, entity, Map.class);

            return response.getStatusCode().is2xxSuccessful();

        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                return false;
            }
            log.error("Error verifying product {} in T24: {}", coreProductId, e.getMessage());
            throw new RuntimeException("Failed to verify product existence", e);

        } catch (Exception e) {
            log.error("Unexpected error verifying product {} in T24", coreProductId, e);
            throw new RuntimeException("Failed to verify product existence", e);
        }
    }

    @Override
    public CoreProductDetails getProductDetails(String coreProductId, CoreSystemConfig config) {
        log.debug("Fetching product details for {} from T24: {}", coreProductId, config.getCoreSystemId());

        try {
            String url = config.getApiEndpoint() + "/products/" + coreProductId;
            HttpHeaders headers = buildHeaders(config);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, Map.class);

            Map<String, Object> data = response.getBody();

            return CoreProductDetails.builder()
                    .coreProductId(coreProductId)
                    .productName((String) data.get("name"))
                    .productType((String) data.get("type"))
                    .status((String) data.get("status"))
                    .monthlyFee(parseBigDecimal(data.get("monthlyFee")))
                    .interestRate(parseBigDecimal(data.get("interestRate")))
                    .minimumBalance(parseBigDecimal(data.get("minimumBalance")))
                    .overdraftLimit(parseBigDecimal(data.get("overdraftLimit")))
                    .overdraftAllowed((Boolean) data.get("overdraftAllowed"))
                    .features((Map<String, Object>) data.get("features"))
                    .additionalFees((Map<String, BigDecimal>) data.get("fees"))
                    .effectiveDate(parseLocalDate(data.get("effectiveDate")))
                    .endDate(parseLocalDate(data.get("endDate")))
                    .activeAccountCount((Integer) data.get("activeAccounts"))
                    .rawResponse(data)
                    .build();

        } catch (Exception e) {
            log.error("Failed to get product details for {} from T24", coreProductId, e);
            throw new RuntimeException("Failed to retrieve product details", e);
        }
    }

    @Override
    public boolean healthCheck(CoreSystemConfig config) {
        log.debug("Performing health check for T24: {}", config.getCoreSystemId());

        try {
            String url = config.getApiEndpoint() + "/health";
            HttpHeaders headers = buildHeaders(config);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, String.class);

            return response.getStatusCode().is2xxSuccessful();

        } catch (Exception e) {
            log.warn("Health check failed for T24 {}: {}", config.getCoreSystemId(), e.getMessage());
            return false;
        }
    }

    @Override
    public String getAdapterVersion() {
        return "1.0.0-T24";
    }

    /**
     * Build HTTP headers for T24 API calls.
     */
    private HttpHeaders buildHeaders(CoreSystemConfig config) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Add authentication
        if (config.getApiKey() != null) {
            headers.set("X-API-Key", config.getApiKey());
        } else if (config.getUsername() != null && config.getPassword() != null) {
            headers.setBasicAuth(config.getUsername(), config.getPassword());
        }

        return headers;
    }

    /**
     * Build T24 product creation/update request from solution.
     */
    private Map<String, Object> buildProductRequest(Solution solution) {
        Map<String, Object> request = new HashMap<>();

        request.put("name", solution.getName());
        request.put("description", solution.getDescription());
        request.put("type", solution.getCategory());

        // Map pricing configuration
        if (solution.getPricing() != null) {
            request.put("monthlyFee", solution.getPricing().getMonthlyFee());
            request.put("interestRate", solution.getPricing().getInterestRate());
            request.put("minimumBalance", solution.getPricing().getMinimumBalance());
        }

        // Map features
        if (solution.getFeatures() != null) {
            request.put("features", solution.getFeatures());
        }

        // Add metadata
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("catalogSolutionId", solution.getId());
        metadata.put("tenantId", solution.getTenantId());
        metadata.put("catalogProductId", solution.getCatalogProductId());
        request.put("metadata", metadata);

        return request;
    }

    /**
     * Parse BigDecimal from object.
     */
    private BigDecimal parseBigDecimal(Object value) {
        if (value == null) return null;
        if (value instanceof BigDecimal) return (BigDecimal) value;
        if (value instanceof Number) return BigDecimal.valueOf(((Number) value).doubleValue());
        if (value instanceof String) return new BigDecimal((String) value);
        return null;
    }

    /**
     * Parse LocalDate from object.
     */
    private LocalDate parseLocalDate(Object value) {
        if (value == null) return null;
        if (value instanceof String) return LocalDate.parse((String) value);
        return null;
    }
}
