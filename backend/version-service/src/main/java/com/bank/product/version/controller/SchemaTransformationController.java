package com.bank.product.version.controller;

import com.bank.product.version.domain.model.SchemaTransformation;
import com.bank.product.version.domain.service.ApiVersionService;
import com.bank.product.version.engine.SchemaTransformer;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Schema Transformation Controller
 * Provides schema transformation services for API versioning
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/transformations")
@RequiredArgsConstructor
public class SchemaTransformationController {

    private final SchemaTransformer schemaTransformer;
    private final ApiVersionService apiVersionService;

    /**
     * Transform request data between versions
     */
    @PostMapping("/request")
    public ResponseEntity<Map<String, Object>> transformRequest(
            @RequestParam String serviceId,
            @RequestParam String fromVersion,
            @RequestParam String toVersion,
            @RequestBody Map<String, Object> requestData) {

        log.info("Transforming request from {} to {} for service {}",
                fromVersion, toVersion, serviceId);

        Map<String, Object> transformed = schemaTransformer.transformRequest(
                requestData, fromVersion, toVersion, serviceId);

        return ResponseEntity.ok(transformed);
    }

    /**
     * Transform response data between versions
     */
    @PostMapping("/response")
    public ResponseEntity<Map<String, Object>> transformResponse(
            @RequestParam String serviceId,
            @RequestParam String fromVersion,
            @RequestParam String toVersion,
            @RequestBody Map<String, Object> responseData) {

        log.info("Transforming response from {} to {} for service {}",
                fromVersion, toVersion, serviceId);

        Map<String, Object> transformed = schemaTransformer.transformResponse(
                responseData, fromVersion, toVersion, serviceId);

        return ResponseEntity.ok(transformed);
    }

    /**
     * Batch transform multiple requests
     */
    @PostMapping("/request/batch")
    public ResponseEntity<List<Map<String, Object>>> transformRequestBatch(
            @RequestParam String serviceId,
            @RequestParam String fromVersion,
            @RequestParam String toVersion,
            @RequestBody List<Map<String, Object>> requestDataList) {

        log.info("Batch transforming {} requests from {} to {} for service {}",
                requestDataList.size(), fromVersion, toVersion, serviceId);

        List<Map<String, Object>> transformed = requestDataList.stream()
                .map(data -> schemaTransformer.transformRequest(data, fromVersion, toVersion, serviceId))
                .collect(Collectors.toList());

        return ResponseEntity.ok(transformed);
    }

    /**
     * Batch transform multiple responses
     */
    @PostMapping("/response/batch")
    public ResponseEntity<List<Map<String, Object>>> transformResponseBatch(
            @RequestParam String serviceId,
            @RequestParam String fromVersion,
            @RequestParam String toVersion,
            @RequestBody List<Map<String, Object>> responseDataList) {

        log.info("Batch transforming {} responses from {} to {} for service {}",
                responseDataList.size(), fromVersion, toVersion, serviceId);

        List<Map<String, Object>> transformed = responseDataList.stream()
                .map(data -> schemaTransformer.transformResponse(data, fromVersion, toVersion, serviceId))
                .collect(Collectors.toList());

        return ResponseEntity.ok(transformed);
    }

    /**
     * Test transformation with sample data
     */
    @PostMapping("/test")
    public ResponseEntity<TransformationTestResult> testTransformation(
            @RequestParam String serviceId,
            @RequestParam String fromVersion,
            @RequestParam String toVersion,
            @RequestBody Map<String, Object> sampleData) {

        log.info("Testing transformation from {} to {} for service {}",
                fromVersion, toVersion, serviceId);

        TransformationTestResult result = new TransformationTestResult();
        result.setServiceId(serviceId);
        result.setFromVersion(fromVersion);
        result.setToVersion(toVersion);
        result.setOriginalData(sampleData);

        try {
            Map<String, Object> transformed = schemaTransformer.transformRequest(
                    sampleData, fromVersion, toVersion, serviceId);
            result.setTransformedData(transformed);
            result.setSuccess(true);
        } catch (Exception e) {
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
            log.error("Transformation test failed", e);
        }

        return ResponseEntity.ok(result);
    }

    /**
     * Validate transformation rules
     */
    @PostMapping("/validate")
    public ResponseEntity<TransformationValidationResult> validateTransformation(
            @RequestBody SchemaTransformation transformation) {

        log.info("Validating transformation from {} to {}",
                transformation.getFromVersion(), transformation.getToVersion());

        TransformationValidationResult result = new TransformationValidationResult();
        result.setValid(true);
        result.setErrors(new ArrayList<>());
        result.setWarnings(new ArrayList<>());

        // Validate field mappings
        if (transformation.getFieldMappings() != null) {
            transformation.getFieldMappings().forEach((source, target) -> {
                if (source == null || source.isEmpty()) {
                    result.getErrors().add("Field mapping has empty source field");
                    result.setValid(false);
                }
                if (target == null || target.isEmpty()) {
                    result.getErrors().add("Field mapping has empty target field for source: " + source);
                    result.setValid(false);
                }
            });
        }

        // Validate field transformations
        if (transformation.getFieldTransformations() != null) {
            transformation.getFieldTransformations().forEach(ft -> {
                if (ft.getSourceField() == null || ft.getSourceField().isEmpty()) {
                    result.getErrors().add("Field transformation has empty source field");
                    result.setValid(false);
                }
                if (ft.getTargetField() == null || ft.getTargetField().isEmpty()) {
                    result.getErrors().add("Field transformation has empty target field");
                    result.setValid(false);
                }
            });
        }

        // Add warnings for potential issues
        if (transformation.getFieldMappings() == null && transformation.getFieldTransformations() == null) {
            result.getWarnings().add("Transformation has no field mappings or transformations");
        }

        return ResponseEntity.ok(result);
    }

    /**
     * Get available transformations for a service
     */
    @GetMapping("/available")
    public ResponseEntity<List<TransformationInfo>> getAvailableTransformations(
            @RequestParam String serviceId) {

        log.info("Getting available transformations for service {}", serviceId);

        List<TransformationInfo> transformations = new ArrayList<>();

        apiVersionService.getAllVersions(serviceId).forEach(apiVersion -> {
            if (apiVersion.getTransformations() != null) {
                apiVersion.getTransformations().forEach((toVersion, transformation) -> {
                    TransformationInfo info = new TransformationInfo();
                    info.setServiceId(serviceId);
                    info.setFromVersion(apiVersion.getVersion());
                    info.setToVersion(toVersion);
                    info.setType(transformation.getType());
                    info.setHasFieldMappings(transformation.getFieldMappings() != null && !transformation.getFieldMappings().isEmpty());
                    info.setHasFieldTransformations(transformation.getFieldTransformations() != null && !transformation.getFieldTransformations().isEmpty());
                    transformations.add(info);
                });
            }
        });

        return ResponseEntity.ok(transformations);
    }

    /**
     * Get transformation details
     */
    @GetMapping("/details")
    public ResponseEntity<SchemaTransformation> getTransformationDetails(
            @RequestParam String serviceId,
            @RequestParam String fromVersion,
            @RequestParam String toVersion) {

        log.info("Getting transformation details from {} to {} for service {}",
                fromVersion, toVersion, serviceId);

        return apiVersionService.getVersion(serviceId, fromVersion)
                .map(apiVersion -> apiVersion.getTransformations())
                .map(transformations -> transformations.get(toVersion))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    /**
     * Transform through a chain of versions
     * Useful for multi-hop transformations (e.g., v1.0 -> v1.1 -> v2.0)
     */
    @PostMapping("/chain")
    public ResponseEntity<Map<String, Object>> transformChain(
            @RequestParam String serviceId,
            @RequestBody TransformChainRequest request) {

        log.info("Chain transforming through versions {} for service {}",
                request.getVersionChain(), serviceId);

        if (request.getVersionChain() == null || request.getVersionChain().size() < 2) {
            return ResponseEntity.badRequest().build();
        }

        try {
            Map<String, Object> transformed = schemaTransformer.transformChain(
                    request.getData(),
                    request.getVersionChain(),
                    serviceId);

            return ResponseEntity.ok(transformed);
        } catch (IllegalStateException e) {
            log.error("Chain transformation failed", e);
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).build();
        }
    }

    // DTOs

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TransformationTestResult {
        private String serviceId;
        private String fromVersion;
        private String toVersion;
        private Map<String, Object> originalData;
        private Map<String, Object> transformedData;
        private boolean success;
        private String errorMessage;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TransformationValidationResult {
        private boolean valid;
        private List<String> errors;
        private List<String> warnings;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TransformationInfo {
        private String serviceId;
        private String fromVersion;
        private String toVersion;
        private com.bank.product.version.domain.model.TransformationType type;
        private boolean hasFieldMappings;
        private boolean hasFieldTransformations;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TransformChainRequest {
        private Map<String, Object> data;
        private List<String> versionChain;
    }
}
