package com.bank.product.version.engine;

import com.bank.product.version.domain.model.SchemaTransformation;

import java.util.List;
import java.util.Map;

/**
 * Schema Transformation Engine
 * Transforms data between API versions
 */
public interface SchemaTransformer {

    /**
     * Transform data from one version to another
     *
     * @param data Source data
     * @param transformation Transformation rules
     * @return Transformed data
     */
    Map<String, Object> transform(Map<String, Object> data, SchemaTransformation transformation);

    /**
     * Transform request data to internal format
     *
     * @param requestData Request data from client
     * @param fromVersion Client API version
     * @param toVersion Internal API version
     * @param serviceId Service identifier
     * @return Transformed data
     */
    Map<String, Object> transformRequest(
            Map<String, Object> requestData,
            String fromVersion,
            String toVersion,
            String serviceId);

    /**
     * Transform response data to client format
     *
     * @param responseData Internal response data
     * @param fromVersion Internal API version
     * @param toVersion Client API version
     * @param serviceId Service identifier
     * @return Transformed data
     */
    Map<String, Object> transformResponse(
            Map<String, Object> responseData,
            String fromVersion,
            String toVersion,
            String serviceId);

    /**
     * Transform through a chain of versions
     * Example: v1.0 → v1.1 → v2.0
     *
     * @param data Source data
     * @param versionChain Ordered list of versions to transform through
     * @param serviceId Service identifier
     * @return Transformed data
     */
    Map<String, Object> transformChain(
            Map<String, Object> data,
            List<String> versionChain,
            String serviceId);
}
