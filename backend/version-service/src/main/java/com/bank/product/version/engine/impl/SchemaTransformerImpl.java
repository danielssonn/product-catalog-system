package com.bank.product.version.engine.impl;

import com.bank.product.version.domain.model.FieldTransformation;
import com.bank.product.version.domain.model.SchemaTransformation;
import com.bank.product.version.domain.service.ApiVersionService;
import com.bank.product.version.engine.SchemaTransformer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class SchemaTransformerImpl implements SchemaTransformer {

    private final ApiVersionService apiVersionService;

    @Override
    public Map<String, Object> transform(Map<String, Object> data, SchemaTransformation transformation) {
        if (transformation == null) {
            return data;
        }

        Map<String, Object> result = new HashMap<>();

        // 1. Apply simple field mappings
        if (transformation.getFieldMappings() != null) {
            transformation.getFieldMappings().forEach((sourceField, targetField) -> {
                Object value = getNestedValue(data, sourceField);
                if (value != null) {
                    setNestedValue(result, targetField, value);
                }
            });
        }

        // 2. Apply complex field transformations
        if (transformation.getFieldTransformations() != null) {
            for (FieldTransformation fieldTransform : transformation.getFieldTransformations()) {
                applyFieldTransformation(data, result, fieldTransform);
            }
        }

        // 3. Add default values for new fields
        if (transformation.getDefaultValues() != null) {
            transformation.getDefaultValues().forEach((field, defaultValue) -> {
                if (!result.containsKey(field)) {
                    result.put(field, defaultValue);
                }
            });
        }

        // 4. Copy unmapped fields (if not in fieldsToRemove)
        data.forEach((key, value) -> {
            if (!result.containsKey(key) &&
                (transformation.getFieldsToRemove() == null ||
                 !transformation.getFieldsToRemove().contains(key))) {
                result.put(key, value);
            }
        });

        return result;
    }

    @Override
    public Map<String, Object> transformRequest(
            Map<String, Object> requestData,
            String fromVersion,
            String toVersion,
            String serviceId) {

        log.debug("Transforming request from {} to {} for service {}",
                fromVersion, toVersion, serviceId);

        // Get transformation rules
        SchemaTransformation transformation = getTransformation(
                serviceId, fromVersion, toVersion);

        if (transformation == null) {
            log.warn("No transformation found from {} to {} for service {}",
                    fromVersion, toVersion, serviceId);
            return requestData;
        }

        return transform(requestData, transformation);
    }

    @Override
    public Map<String, Object> transformResponse(
            Map<String, Object> responseData,
            String fromVersion,
            String toVersion,
            String serviceId) {

        log.debug("Transforming response from {} to {} for service {}",
                fromVersion, toVersion, serviceId);

        // Get transformation rules (reverse direction)
        SchemaTransformation transformation = getTransformation(
                serviceId, fromVersion, toVersion);

        if (transformation == null) {
            log.warn("No transformation found from {} to {} for service {}",
                    fromVersion, toVersion, serviceId);
            return responseData;
        }

        return transform(responseData, transformation);
    }

    /**
     * Apply field-level transformation
     */
    private void applyFieldTransformation(
            Map<String, Object> source,
            Map<String, Object> target,
            FieldTransformation fieldTransform) {

        Object value = getNestedValue(source, fieldTransform.getSourceField());

        if (value == null) {
            value = fieldTransform.getDefaultValue();
        }

        if (value != null) {
            // Apply transformation function
            Object transformedValue = applyTransformFunction(
                    value,
                    fieldTransform.getTransformFunction(),
                    fieldTransform.getFunctionParams());

            setNestedValue(target, fieldTransform.getTargetField(), transformedValue);
        }
    }

    /**
     * Apply transformation function to a value
     */
    private Object applyTransformFunction(Object value, String function, String params) {
        if (function == null) {
            return value;
        }

        switch (function.toLowerCase()) {
            case "tolowercase":
                return value.toString().toLowerCase();
            case "touppercase":
                return value.toString().toUpperCase();
            case "trim":
                return value.toString().trim();
            case "format":
                return String.format(params, value);
            case "tonumber":
                return Double.parseDouble(value.toString());
            case "tostring":
                return value.toString();
            case "toboolean":
                return Boolean.parseBoolean(value.toString());
            default:
                log.warn("Unknown transformation function: {}", function);
                return value;
        }
    }

    /**
     * Get nested value from map using dot notation
     */
    private Object getNestedValue(Map<String, Object> map, String path) {
        String[] parts = path.split("\\.");
        Object current = map;

        for (String part : parts) {
            if (current instanceof Map) {
                current = ((Map<?, ?>) current).get(part);
            } else {
                return null;
            }
        }

        return current;
    }

    /**
     * Set nested value in map using dot notation
     */
    @SuppressWarnings("unchecked")
    private void setNestedValue(Map<String, Object> map, String path, Object value) {
        String[] parts = path.split("\\.");
        Map<String, Object> current = map;

        for (int i = 0; i < parts.length - 1; i++) {
            String part = parts[i];
            if (!current.containsKey(part)) {
                current.put(part, new HashMap<String, Object>());
            }
            current = (Map<String, Object>) current.get(part);
        }

        current.put(parts[parts.length - 1], value);
    }

    @Override
    public Map<String, Object> transformChain(
            Map<String, Object> data,
            List<String> versionChain,
            String serviceId) {

        if (versionChain == null || versionChain.size() < 2) {
            log.warn("Invalid version chain: {}", versionChain);
            return data;
        }

        log.info("Transforming through version chain: {} for service {}",
                versionChain, serviceId);

        Map<String, Object> result = data;

        // Transform through each version step
        for (int i = 0; i < versionChain.size() - 1; i++) {
            String fromVersion = versionChain.get(i);
            String toVersion = versionChain.get(i + 1);

            log.debug("Chain step {}/{}: {} -> {}",
                    i + 1, versionChain.size() - 1, fromVersion, toVersion);

            SchemaTransformation transformation = getTransformation(
                    serviceId, fromVersion, toVersion);

            if (transformation == null) {
                log.error("Missing transformation in chain: {} -> {} for service {}",
                        fromVersion, toVersion, serviceId);
                throw new IllegalStateException(
                        "Missing transformation from " + fromVersion + " to " + toVersion);
            }

            result = transform(result, transformation);
        }

        log.info("Completed chain transformation from {} to {}",
                versionChain.get(0), versionChain.get(versionChain.size() - 1));

        return result;
    }

    /**
     * Get transformation from version registry
     */
    private SchemaTransformation getTransformation(
            String serviceId,
            String fromVersion,
            String toVersion) {

        return apiVersionService.getVersion(serviceId, fromVersion)
                .map(apiVersion -> apiVersion.getTransformations())
                .map(transformations -> transformations.get(toVersion))
                .orElse(null);
    }
}
